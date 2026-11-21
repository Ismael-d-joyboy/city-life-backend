package ci.abidjan.citylife.tutorial;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.economy.Transaction;
import ci.abidjan.citylife.economy.TransactionRepository;
import ci.abidjan.citylife.economy.Wallet;
import ci.abidjan.citylife.economy.WalletRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorialService {

    public static final int TOTAL_STEPS = 6;
    public static final long TUTORIAL_REWARD_CASH = 10_000L;
    public static final long TUTORIAL_REWARD_XP = 50L;

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityAuditService auditService;

    @Transactional(readOnly = true)
    public TutorialStatusResponse getStatus(String email) {
        Player player = getPlayerByEmail(email);
        Wallet wallet = findPrimaryWallet(player.getId()).orElse(null);

        long cash = wallet != null ? wallet.getCashBalance() : 0L;
        long bank = wallet != null ? wallet.getBankBalance() : 0L;
        int currentStep = player.getTutorialStep() != null ? player.getTutorialStep() : 0;

        String msg = player.isTutorialCompleted()
                ? "Tutoriel déjà terminé."
                : "Tutoriel en cours (étape " + currentStep + "/" + TOTAL_STEPS + ").";

        return new TutorialStatusResponse(
                player.isTutorialCompleted(),
                currentStep,
                TOTAL_STEPS,
                formatInstant(player.getTutorialCompletedAt()),
                false,
                player.isTutorialCompleted() ? 0L : TUTORIAL_REWARD_CASH,
                player.isTutorialCompleted() ? 0L : TUTORIAL_REWARD_XP,
                cash,
                bank,
                player.getExperiencePoints(),
                msg
        );
    }

    @Transactional
    public TutorialStatusResponse updateStep(String email, UpdateTutorialStepRequest request) {
        Player player = getPlayerByEmail(email);
        int clampedStep = Math.max(0, Math.min(request.step(), TOTAL_STEPS));
        int currentStep = player.getTutorialStep() != null ? player.getTutorialStep() : 0;

        if (!player.isTutorialCompleted() && clampedStep > currentStep) {
            player.setTutorialStep(clampedStep);
            playerRepository.save(player);
            currentStep = clampedStep;
        }

        Wallet wallet = findPrimaryWallet(player.getId()).orElse(null);
        long cash = wallet != null ? wallet.getCashBalance() : 0L;
        long bank = wallet != null ? wallet.getBankBalance() : 0L;

        return new TutorialStatusResponse(
                player.isTutorialCompleted(),
                currentStep,
                TOTAL_STEPS,
                formatInstant(player.getTutorialCompletedAt()),
                false,
                player.isTutorialCompleted() ? 0L : TUTORIAL_REWARD_CASH,
                player.isTutorialCompleted() ? 0L : TUTORIAL_REWARD_XP,
                cash,
                bank,
                player.getExperiencePoints(),
                "Progression du tutoriel sauvegardée (" + currentStep + "/" + TOTAL_STEPS + ")."
        );
    }

    @Transactional
    public TutorialStatusResponse completeTutorial(String email, CompleteTutorialRequest request, String ipAddress) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        Player player = playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur introuvable."));

        // Déjà terminé : réponse idempotente sans jamais créditer deux fois
        if (player.isTutorialCompleted()) {
            Wallet wallet = resolveWallet(player, request.characterId(), false).orElse(null);
            int step = player.getTutorialStep() != null ? player.getTutorialStep() : TOTAL_STEPS;
            return new TutorialStatusResponse(
                    true,
                    step,
                    TOTAL_STEPS,
                    formatInstant(player.getTutorialCompletedAt()),
                    false,
                    0L,
                    0L,
                    wallet != null ? wallet.getCashBalance() : 0L,
                    wallet != null ? wallet.getBankBalance() : 0L,
                    player.getExperiencePoints(),
                    "Tutoriel déjà validé sur votre compte."
            );
        }

        Instant now = Instant.now();

        // Cas 1 : Le joueur passe le tutoriel (aucune récompense FCFA)
        if (request.skipped()) {
            player.setTutorialCompleted(true);
            player.setTutorialCompletedAt(now);
            playerRepository.save(player);

            auditService.log(account, "TUTORIAL_SKIPPED", SecurityAuditLog.Severity.INFO, ipAddress,
                    Map.of("playerId", player.getId().toString(),
                            "lastStep", player.getTutorialStep() != null ? player.getTutorialStep() : 0));

            Wallet wallet = resolveWallet(player, request.characterId(), false).orElse(null);
            return new TutorialStatusResponse(
                    true,
                    player.getTutorialStep() != null ? player.getTutorialStep() : 0,
                    TOTAL_STEPS,
                    formatInstant(now),
                    false,
                    0L,
                    0L,
                    wallet != null ? wallet.getCashBalance() : 0L,
                    wallet != null ? wallet.getBankBalance() : 0L,
                    player.getExperiencePoints(),
                    "Tutoriel passé. Vous pourrez le rejouer à tout moment dans Paramètres."
            );
        }

        // Cas 2 : Complétion normale avec récompense FCFA unique protégée par idempotencyKey
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new IllegalStateException("La clé d'idempotence est obligatoire pour valider la récompense du tutoriel.");
        }
        String cleanIdempotencyKey = request.idempotencyKey().trim();

        if (transactionRepository.existsByIdempotencyKey(cleanIdempotencyKey)) {
            throw new IllegalArgumentException("Récompense du tutoriel déjà versée (doublon bloqué).");
        }

        Wallet wallet = resolveWallet(player, request.characterId(), true)
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable pour ce joueur."));
        if (wallet.isFrozen()) {
            throw new IllegalStateException("Portefeuille gelé.");
        }

        wallet.setCashBalance(wallet.getCashBalance() + TUTORIAL_REWARD_CASH);
        walletRepository.save(wallet);

        player.setTutorialCompleted(true);
        player.setTutorialStep(TOTAL_STEPS);
        player.setTutorialCompletedAt(now);
        player.setExperiencePoints(player.getExperiencePoints() + TUTORIAL_REWARD_XP);
        playerRepository.save(player);

        transactionRepository.save(Transaction.builder()
                .idempotencyKey(cleanIdempotencyKey)
                .targetWallet(wallet)
                .amount(TUTORIAL_REWARD_CASH)
                .transactionType(Transaction.Type.SYSTEM_GRANT)
                .status(Transaction.Status.COMPLETED)
                .description("Prime de bienvenue — Tutoriel Abidjan terminé")
                .metadata(Map.of(
                        "source", "TUTORIAL_COMPLETION",
                        "playerId", player.getId().toString(),
                        "xpEarned", TUTORIAL_REWARD_XP
                ))
                .build());

        auditService.log(account, "TUTORIAL_COMPLETED", SecurityAuditLog.Severity.INFO, ipAddress,
                Map.of("playerId", player.getId().toString(),
                        "rewardCash", TUTORIAL_REWARD_CASH,
                        "rewardXp", TUTORIAL_REWARD_XP,
                        "idempotencyKey", cleanIdempotencyKey));

        return new TutorialStatusResponse(
                true,
                TOTAL_STEPS,
                TOTAL_STEPS,
                formatInstant(now),
                true,
                TUTORIAL_REWARD_CASH,
                TUTORIAL_REWARD_XP,
                wallet.getCashBalance(),
                wallet.getBankBalance(),
                player.getExperiencePoints(),
                "Tutoriel terminé ! Prime créditée : +10 000 FCFA | +50 XP"
        );
    }

    private Player getPlayerByEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        return playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur introuvable."));
    }

    private Optional<Wallet> resolveWallet(Player player, UUID characterId, boolean strictCharacterCheck) {
        if (characterId != null) {
            Optional<Character> owned = characterRepository.findByIdAndPlayerId(characterId, player.getId());
            if (owned.isEmpty()) {
                if (strictCharacterCheck) {
                    throw new IllegalArgumentException("Personnage introuvable ou accès refusé.");
                }
                return findPrimaryWallet(player.getId());
            }
            return walletRepository.findByCharacterId(owned.get().getId());
        }
        return findPrimaryWallet(player.getId());
    }

    private Optional<Wallet> findPrimaryWallet(UUID playerId) {
        List<Character> activeChars = characterRepository.findByPlayerIdAndActiveTrue(playerId);
        if (activeChars == null || activeChars.isEmpty()) {
            return Optional.empty();
        }
        return walletRepository.findByCharacterId(activeChars.get(0).getId());
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
