package ci.abidjan.citylife.factions;

import ci.abidjan.citylife.auth.Account;
import ci.abidjan.citylife.auth.AccountRepository;
import ci.abidjan.citylife.player.Character;
import ci.abidjan.citylife.player.CharacterRepository;
import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FactionService {

    private final AccountRepository accountRepository;
    private final PlayerRepository playerRepository;
    private final CharacterRepository characterRepository;
    private final FactionRepository factionRepository;
    private final FactionMemberRepository factionMemberRepository;
    private final FactionTerritoryRepository territoryRepository;

    @Transactional(readOnly = true)
    public List<FactionResponse> listFactions(String email, UUID characterId) {
        FactionMember myMembership = null;
        if (characterId != null) {
            Character character = getOwnedCharacter(email, characterId);
            myMembership = factionMemberRepository.findByCharacterId(character.getId()).orElse(null);
        }

        final UUID myFactionId = myMembership != null ? myMembership.getFaction().getId() : null;
        final String myRole = myMembership != null ? myMembership.getRole().name() : "";

        return factionRepository.findAll().stream()
                .map(f -> new FactionResponse(
                        f.getId(),
                        f.getCode(),
                        f.getTag(),
                        f.getName(),
                        f.getColorHex(),
                        factionMemberRepository.countByFactionId(f.getId()),
                        f.getId().equals(myFactionId) ? myRole : "",
                        ""
                ))
                .toList();
    }

    @Transactional
    public FactionResponse createFaction(String email, CreateFactionRequest request) {
        Character character = getOwnedCharacter(email, request.characterId());

        if (factionMemberRepository.findByCharacterId(character.getId()).isPresent()) {
            throw new IllegalStateException("Quittez d'abord votre faction actuelle avant d'en créer une nouvelle.");
        }

        String tag = request.tag().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();
        String code = "FAC_" + tag;
        String colorHex = (request.colorHex() != null && !request.colorHex().isBlank())
                ? request.colorHex().trim()
                : "#F5C518";

        if (factionRepository.existsByTagIgnoreCase(tag) || factionRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Ce tag de faction existe déjà : [" + tag + "].");
        }
        if (factionRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Ce nom de faction est déjà utilisé.");
        }

        Faction faction = factionRepository.save(Faction.builder()
                .code(code)
                .tag(tag)
                .name(name)
                .colorHex(colorHex)
                .leader(character)
                .build());

        factionMemberRepository.save(FactionMember.builder()
                .faction(faction)
                .character(character)
                .role(FactionMember.Role.LEADER)
                .build());

        return new FactionResponse(
                faction.getId(),
                faction.getCode(),
                faction.getTag(),
                faction.getName(),
                faction.getColorHex(),
                1L,
                FactionMember.Role.LEADER.name(),
                "Faction [" + faction.getTag() + "] " + faction.getName() + " créée avec succès !"
        );
    }

    @Transactional
    public FactionResponse joinFaction(String email, UUID characterId, String code) {
        Character character = getOwnedCharacter(email, characterId);
        Faction faction = factionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Faction introuvable : " + code));

        FactionMember existing = factionMemberRepository.findByCharacterId(character.getId()).orElse(null);
        if (existing != null) {
            if (existing.getFaction().getId().equals(faction.getId())) {
                return new FactionResponse(
                        faction.getId(),
                        faction.getCode(),
                        faction.getTag(),
                        faction.getName(),
                        faction.getColorHex(),
                        factionMemberRepository.countByFactionId(faction.getId()),
                        existing.getRole().name(),
                        "Vous êtes déjà membre de [" + faction.getTag() + "]."
                );
            }
            factionMemberRepository.delete(existing);
        }

        factionMemberRepository.save(FactionMember.builder()
                .faction(faction)
                .character(character)
                .role(FactionMember.Role.MEMBER)
                .build());

        return new FactionResponse(
                faction.getId(),
                faction.getCode(),
                faction.getTag(),
                faction.getName(),
                faction.getColorHex(),
                factionMemberRepository.countByFactionId(faction.getId()),
                FactionMember.Role.MEMBER.name(),
                "Vous avez rejoint [" + faction.getTag() + "] " + faction.getName() + " !"
        );
    }

    @Transactional
    public String leaveFaction(String email, UUID characterId) {
        Character character = getOwnedCharacter(email, characterId);
        FactionMember existing = factionMemberRepository.findByCharacterId(character.getId()).orElse(null);
        if (existing == null) {
            return "Vous n'appartenez à aucune faction.";
        }
        String tag = existing.getFaction().getTag();
        factionMemberRepository.delete(existing);
        return "Vous avez quitté la faction [" + tag + "].";
    }

    @Transactional(readOnly = true)
    public List<FactionResponse.TerritoryResponse> listTerritories() {
        return territoryRepository.findAll().stream()
                .map(t -> FactionResponse.TerritoryResponse.from(t, ""))
                .toList();
    }

    @Transactional
    public FactionResponse.TerritoryResponse captureTerritory(String email, UUID characterId, String zoneCode) {
        Character character = getOwnedCharacter(email, characterId);
        FactionMember membership = factionMemberRepository.findByCharacterId(character.getId())
                .orElseThrow(() -> new IllegalStateException("Rejoignez ou créez une faction avant de capturer un territoire."));

        FactionTerritory territory = territoryRepository.findById(zoneCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Zone de territoire introuvable : " + zoneCode));

        Faction faction = membership.getFaction();
        territory.setOwnerFaction(faction);
        territory.setCapturedAt(Instant.now());
        territory = territoryRepository.save(territory);

        return FactionResponse.TerritoryResponse.from(
                territory,
                "Territoire « " + territory.getZoneName() + " » contrôlé par [" + faction.getTag() + "] !"
        );
    }

    private Character getOwnedCharacter(String email, UUID characterId) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));
        Player player = playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Joueur introuvable."));
        return characterRepository.findByIdAndPlayerId(characterId, player.getId())
                .orElseThrow(() -> new IllegalArgumentException("Personnage introuvable ou accès refusé."));
    }
}
