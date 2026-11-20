package ci.abidjan.citylife.player;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.economy.Wallet;
import ci.abidjan.citylife.economy.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final CharacterAppearanceRepository appearanceRepository;
    private final WalletRepository walletRepository;

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
                    return CharacterResponse.from(c, a, w);
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

    private Player getPlayerByEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        return playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur introuvable."));
    }
}