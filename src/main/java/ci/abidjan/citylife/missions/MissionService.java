package ci.abidjan.citylife.missions;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final MissionRepository missionRepository;
    private final PlayerMissionRepository playerMissionRepository;

    @Transactional(readOnly = true)
    public List<Mission> getCatalog() {
        return missionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PlayerMissionResponse> getMyMissions(String email, UUID characterId) {
        Character character = getOwnedCharacter(email, characterId);
        return playerMissionRepository.findByCharacterId(character.getId())
                .stream()
                .map(PlayerMissionResponse::from)
                .toList();
    }

    @Transactional
    public PlayerMissionResponse startMission(String email, UUID characterId, StartMissionRequest request) {
        Character character = getOwnedCharacter(email, characterId);
        Player player = character.getPlayer();

        Mission mission = missionRepository.findByMissionCode(request.missionCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Mission introuvable : " + request.missionCode()));

        if (player.getLevel() < mission.getMinLevel()) {
            throw new IllegalStateException("Niveau " + mission.getMinLevel() + " requis.");
        }

        if (playerMissionRepository.existsByCharacterIdAndMissionIdAndStatus(
                character.getId(), mission.getId(), PlayerMission.Status.IN_PROGRESS)) {
            throw new IllegalStateException("Cette mission est déjà en cours.");
        }

        if (!mission.isRepeatable() && playerMissionRepository.existsByCharacterIdAndMissionIdAndStatus(
                character.getId(), mission.getId(), PlayerMission.Status.COMPLETED)) {
            throw new IllegalStateException("Cette mission unique a déjà été terminée.");
        }

        PlayerMission pm = PlayerMission.builder()
                .character(character)
                .mission(mission)
                .status(PlayerMission.Status.IN_PROGRESS)
                .build();
        pm = playerMissionRepository.save(pm);

        return PlayerMissionResponse.from(pm);
    }

    @Transactional
    public PlayerMissionResponse completeMission(String email, UUID characterId, CompleteMissionRequest request) {
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new IllegalArgumentException("Récompense déjà versée (doublon bloqué).");
        }

        Character character = getOwnedCharacter(email, characterId);
        Player player = character.getPlayer();

        Mission mission = missionRepository.findByMissionCode(request.missionCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Mission introuvable : " + request.missionCode()));

        PlayerMission pm = playerMissionRepository
                .findByCharacterIdAndMissionIdAndStatus(character.getId(), mission.getId(), PlayerMission.Status.IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("Aucune mission en cours trouvée pour ce code."));

        // Anti-triche : délai minimum 10 s entre démarrage et complétion d'une mission
        if (pm.getStartedAt() != null && Instant.now().isBefore(pm.getStartedAt().plusSeconds(10L))) {
            throw new IllegalStateException("Mission complétée trop rapidement. Patientez quelques secondes.");
        }

        pm.setStatus(PlayerMission.Status.COMPLETED);
        pm.setCompletedAt(Instant.now());
        pm = playerMissionRepository.save(pm);

        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));

        if (mission.getRewardCash() > 0) {
            wallet.setCashBalance(wallet.getCashBalance() + mission.getRewardCash());
            walletRepository.save(wallet);

            transactionRepository.save(Transaction.builder()
                    .idempotencyKey(request.idempotencyKey())
                    .targetWallet(wallet)
                    .amount(mission.getRewardCash())
                    .transactionType(Transaction.Type.MISSION_REWARD)
                    .status(Transaction.Status.COMPLETED)
                    .description("Récompense mission : " + mission.getTitle())
                    .metadata(Map.of("missionCode", mission.getMissionCode()))
                    .build());
        }

        if (mission.getRewardXp() > 0) {
            player.setExperiencePoints(player.getExperiencePoints() + mission.getRewardXp());
            player.recalculateLevel();
            playerRepository.save(player);
        }

        return PlayerMissionResponse.from(pm);
    }

    private Character getOwnedCharacter(String email, UUID characterId) {
        Account account = accountRepository.findByEmail(email).orElseThrow();
        Player player = playerRepository.findByAccountId(account.getId()).orElseThrow();
        return characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
    }
}