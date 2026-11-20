package ci.abidjan.citylife.economy;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import ci.abidjan.citylife.security.SecurityAuditLog;
import ci.abidjan.citylife.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class JobPayoutService {

    private static final long COOLDOWN_SECONDS = 8L;
    private final Map<UUID, Instant> lastPayoutByCharacter = new ConcurrentHashMap<>();

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityAuditService auditService;

    @Transactional
    public JobPayoutResponse claimPayout(String email, JobPayoutRequest request, String ipAddress) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new IllegalArgumentException("Paiement métier déjà traité (doublon bloqué).");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        Player player = playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur introuvable."));
        Character character = characterRepository.findByIdAndPlayerId(request.characterId(), player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));

        // Anti-triche : délai minimum entre deux salaires (sauf bonus immédiat des 5 sacs)
        Instant now = Instant.now();
        if (request.actionType() != JobPayoutRequest.JobActionType.MANAWA_BONUS_5) {
            Instant last = lastPayoutByCharacter.get(character.getId());
            if (last != null && now.isBefore(last.plusSeconds(COOLDOWN_SECONDS))) {
                auditService.log(account, "JOB_PAYOUT_COOLDOWN_TRIGGERED", SecurityAuditLog.Severity.WARNING, ipAddress,
                        Map.of("characterId", character.getId().toString(), "actionType", request.actionType().name()));
                throw new IllegalStateException("Action trop rapide. Patientez quelques secondes.");
            }
            lastPayoutByCharacter.put(character.getId(), now);
        }

        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));
        if (wallet.isFrozen()) {
            throw new IllegalStateException("Portefeuille gelé.");
        }

        JobPayoutRequest.JobActionType action = request.actionType();
        wallet.setCashBalance(wallet.getCashBalance() + action.rewardCash);
        walletRepository.save(wallet);

        player.setExperiencePoints(player.getExperiencePoints() + action.rewardXp);
        playerRepository.save(player);

        transactionRepository.save(Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .targetWallet(wallet)
                .amount(action.rewardCash)
                .transactionType(Transaction.Type.SALARY)
                .status(Transaction.Status.COMPLETED)
                .description(action.label)
                .metadata(Map.of("jobAction", action.name(), "xpEarned", action.rewardXp))
                .build());

        return new JobPayoutResponse(
                action.name(),
                action.rewardCash,
                action.rewardXp,
                wallet.getCashBalance(),
                wallet.getBankBalance(),
                player.getExperiencePoints(),
                action.label + " : +" + action.rewardCash + " FCFA"
        );
    }
}