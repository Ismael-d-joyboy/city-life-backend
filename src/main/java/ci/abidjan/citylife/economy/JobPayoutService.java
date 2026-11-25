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

@Service
@RequiredArgsConstructor
public class JobPayoutService {

    private static final long COOLDOWN_SECONDS = 8L;
    private static final long CONTRABAND_COOLDOWN_SECONDS = 90L;
    private static final int MANAWA_BAGS_FOR_BONUS = 5;

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

        Instant now = Instant.now();
        JobPayoutRequest.JobActionType action = request.actionType();
        int currentStreak = character.getManawaBagStreak() != null ? character.getManawaBagStreak() : 0;

        // Anti-triche persisté en base (Jalon 0) :
        // 1. MANAWA_BONUS_5 exige 5 sacs réellement livrés et validés côté serveur
        if (action == JobPayoutRequest.JobActionType.CONTRABAND_DELIVERY) {
            if (player.isPeacefulMode()) {
                throw new IllegalStateException("Mode Pacifique actif : les livraisons à risque sont désactivées.");
            }
            Instant lastContraband = character.getLastContrabandPayoutAt();
            if (lastContraband != null && now.isBefore(lastContraband.plusSeconds(CONTRABAND_COOLDOWN_SECONDS))) {
                auditService.log(account, "CONTRABAND_COOLDOWN_TRIGGERED", SecurityAuditLog.Severity.WARNING, ipAddress,
                        Map.of("characterId", character.getId().toString()));
                throw new IllegalStateException("Livraison à risque trop rapide. Patientez 90 secondes entre deux trajets.");
            }
            character.setLastContrabandPayoutAt(now);
            character.setLastJobPayoutAt(now);
        } else if (action == JobPayoutRequest.JobActionType.MANAWA_BONUS_5) {
            if (currentStreak < MANAWA_BAGS_FOR_BONUS) {
                auditService.log(account, "MANAWA_BONUS_EXPLOIT_BLOCKED", SecurityAuditLog.Severity.WARNING, ipAddress,
                        Map.of("characterId", character.getId().toString(), "streak", currentStreak));
                throw new IllegalStateException("Bonus Manawa refusé : livrez d'abord 5 sacs de ciment.");
            }
            character.setManawaBagStreak(currentStreak - MANAWA_BAGS_FOR_BONUS);
        } else {
            // 2. Cooldown persisté en DB (survit aux redémarrages Render, pas de fuite RAM)
            Instant last = character.getLastJobPayoutAt();
            if (last != null && now.isBefore(last.plusSeconds(COOLDOWN_SECONDS))) {
                auditService.log(account, "JOB_PAYOUT_COOLDOWN_TRIGGERED", SecurityAuditLog.Severity.WARNING, ipAddress,
                        Map.of("characterId", character.getId().toString(), "actionType", action.name()));
                throw new IllegalStateException("Action trop rapide. Patientez quelques secondes.");
            }
            character.setLastJobPayoutAt(now);
            if (action == JobPayoutRequest.JobActionType.MANAWA_BAG) {
                character.setManawaBagStreak(Math.min(20, currentStreak + 1));
            }
        }
        player.setLastSeenAt(now);
        characterRepository.save(character);

        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));
        if (wallet.isFrozen()) {
            throw new IllegalStateException("Portefeuille gelé.");
        }

        wallet.setCashBalance(wallet.getCashBalance() + action.rewardCash);
        walletRepository.save(wallet);

        player.setExperiencePoints(player.getExperiencePoints() + action.rewardXp);
        player.recalculateLevel();
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