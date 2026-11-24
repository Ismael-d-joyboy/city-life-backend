package ci.abidjan.citylife.player;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.economy.Transaction;
import ci.abidjan.citylife.economy.TransactionRepository;
import ci.abidjan.citylife.economy.Wallet;
import ci.abidjan.citylife.economy.WalletRepository;
import ci.abidjan.citylife.factions.FactionMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private static final long HOSPITAL_CARE_FEE_FCFA = 2000L;

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final CharacterAppearanceRepository appearanceRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final FactionMemberRepository factionMemberRepository;

    @Transactional
    public CharacterResponse createCharacter(String email, CreateCharacterRequest request) {
        Player player = getPlayerByEmail(email);

        if (characterRepository.countByPlayerIdAndActiveTrue(player.getId()) >= 3) {
            throw new IllegalStateException("Limite de 3 personnages atteinte pour ce compte.");
        }

        Character character = Character.builder()
                .player(player)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .gender(request.gender())
                .district(Character.District.PLATEAU)
                .build();
        character = characterRepository.save(character);

        CharacterAppearance appearance = CharacterAppearance.builder()
                .character(character)
                .skinTone(request.skinTone() != null ? request.skinTone() : "DEFAULT")
                .hairStyle(request.hairStyle() != null ? request.hairStyle() : "SHORT_01")
                .hairColor(request.hairColor() != null ? request.hairColor() : "#1A1A1A")
                .outfitData(request.outfitData() != null ? request.outfitData() : Map.of(
                        "top", "tshirt_basic",
                        "bottom", "jeans_basic",
                        "shoes", "sneakers_01"
                ))
                .build();
        appearance = appearanceRepository.save(appearance);

        Wallet wallet = Wallet.builder()
                .character(character)
                .build();
        wallet = walletRepository.save(wallet);

        return CharacterResponse.from(character, appearance, wallet);
    }

    @Transactional(readOnly = true)
    public List<CharacterResponse> getMyCharacters(String email) {
        Player player = getPlayerByEmail(email);
        return characterRepository.findByPlayerIdAndActiveTrue(player.getId())
                .stream()
                .map(c -> {
                    CharacterAppearance a = appearanceRepository.findByCharacterId(c.getId())
                            .orElseThrow(() -> new IllegalStateException("Apparence introuvable."));
                    Wallet w = walletRepository.findByCharacterId(c.getId())
                            .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));
                    String factionTag = factionMemberRepository.findByCharacterId(c.getId())
                            .map(m -> m.getFaction().getTag())
                            .orElse("");
                    return CharacterResponse.from(c, a, w, factionTag);
                })
                .toList();
    }

    @Transactional
    public CharacterResponse saveState(String email, UUID characterId, SaveCharacterStateRequest request) {
        Player player = getPlayerByEmail(email);
        Character character = characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));

        character.setDistrict(request.district());
        character.setPosX(request.posX());
        character.setPosY(request.posY());
        character.setPosZ(request.posZ());
        character.setRotationY(request.rotationY());
        character.setHealth(request.health());
        character.setStamina(request.stamina());
        character.setHunger(request.hunger());
        character.setThirst(request.thirst());
        character = characterRepository.save(character);

        CharacterAppearance a = appearanceRepository.findByCharacterId(character.getId()).orElseThrow();
        Wallet w = walletRepository.findByCharacterId(character.getId()).orElseThrow();

        return CharacterResponse.from(character, a, w);
    }

    @Transactional
    public CharacterResponse updateAppearance(String email, UUID characterId, UpdateAppearanceRequest request) {
        Player player = getPlayerByEmail(email);
        Character character = characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));

        CharacterAppearance appearance = appearanceRepository.findByCharacterId(character.getId())
                .orElseGet(() -> CharacterAppearance.builder().character(character).build());

        if (request.skinTone() != null && !request.skinTone().isBlank()) {
            appearance.setSkinTone(request.skinTone().trim());
        }
        if (request.hairStyle() != null && !request.hairStyle().isBlank()) {
            appearance.setHairStyle(request.hairStyle().trim());
        }
        if (request.hairColor() != null && !request.hairColor().isBlank()) {
            appearance.setHairColor(request.hairColor().trim());
        }
        if (request.outfitData() != null && !request.outfitData().isEmpty()) {
            Map<String, Object> merged = new HashMap<>(appearance.getOutfitData() != null ? appearance.getOutfitData() : Map.of());
            merged.putAll(request.outfitData());
            appearance.setOutfitData(merged);
        }

        appearance = appearanceRepository.save(appearance);
        Wallet wallet = walletRepository.findByCharacterId(character.getId()).orElseThrow();
        return CharacterResponse.from(character, appearance, wallet);
    }

    @Transactional
    public HospitalCareResponse hospitalCare(String email, UUID characterId, HospitalCareRequest request) {
        Player player = getPlayerByEmail(email);
        Character character = characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
        Wallet wallet = walletRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Portefeuille introuvable."));

        boolean alreadyProcessed = transactionRepository.existsByIdempotencyKey(request.idempotencyKey());
        long charged = 0L;

        if (!alreadyProcessed) {
            if (!wallet.isFrozen() && wallet.getCashBalance() >= HOSPITAL_CARE_FEE_FCFA) {
                charged = HOSPITAL_CARE_FEE_FCFA;
                wallet.setCashBalance(wallet.getCashBalance() - charged);
                walletRepository.save(wallet);

                transactionRepository.save(Transaction.builder()
                        .idempotencyKey(request.idempotencyKey())
                        .sourceWallet(wallet)
                        .amount(charged)
                        .transactionType(Transaction.Type.PURCHASE)
                        .status(Transaction.Status.COMPLETED)
                        .description(request.emergencyRespawn()
                                ? "Prise en charge d'urgence — CHU d'Abidjan"
                                : "Consultation & soins — CHU d'Abidjan")
                        .metadata(Map.of("service", "HOSPITAL_CHU_CARE", "emergency", request.emergencyRespawn()))
                        .build());
            }

            character.setHealth(100);
            character.setStamina(100);
            character.setHunger(Math.max(character.getHunger() != null ? character.getHunger() : 75, 75));
            character.setThirst(Math.max(character.getThirst() != null ? character.getThirst() : 75, 75));
            int visits = character.getHospitalVisits() != null ? character.getHospitalVisits() : 0;
            character.setHospitalVisits(visits + 1);
            character = characterRepository.save(character);
        }

        String msg = charged > 0
                ? "Soins reçus au CHU d'Abidjan (" + charged + " FCFA). Santé restaurée à 100 % !"
                : "Soins d'urgence gratuits au CHU d'Abidjan. Santé restaurée à 100 % !";

        return new HospitalCareResponse(
                character.getHealth(),
                character.getStamina(),
                character.getHunger(),
                character.getThirst(),
                charged,
                wallet.getCashBalance(),
                wallet.getBankBalance(),
                character.getHospitalVisits(),
                msg
        );
    }

    private Player getPlayerByEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        return playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur introuvable."));
    }
}