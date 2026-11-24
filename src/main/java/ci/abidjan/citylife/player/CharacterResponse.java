package ci.abidjan.citylife.player;

import ci.abidjan.citylife.economy.Wallet;

import java.util.Map;
import java.util.UUID;

public record CharacterResponse(
        UUID id,
        String firstName,
        String lastName,
        Character.Gender gender,
        Integer health,
        Integer stamina,
        Integer hunger,
        Integer thirst,
        Character.District district,
        Double posX,
        Double posY,
        Double posZ,
        Double rotationY,
        Long cashBalance,
        Long bankBalance,
        String currency,
        String skinTone,
        String hairStyle,
        String hairColor,
        Map<String, Object> outfitData,
        boolean hasWeaponLicense,
        String factionTag
) {
    public static CharacterResponse from(Character c, CharacterAppearance a, Wallet w) {
        return from(c, a, w, "");
    }

    public static CharacterResponse from(Character c, CharacterAppearance a, Wallet w, String factionTag) {
        return new CharacterResponse(
                c.getId(),
                c.getFirstName(),
                c.getLastName(),
                c.getGender(),
                c.getHealth(),
                c.getStamina(),
                c.getHunger(),
                c.getThirst(),
                c.getDistrict(),
                c.getPosX(),
                c.getPosY(),
                c.getPosZ(),
                c.getRotationY(),
                w.getCashBalance(),
                w.getBankBalance(),
                w.getCurrency(),
                a.getSkinTone(),
                a.getHairStyle(),
                a.getHairColor(),
                a.getOutfitData(),
                c.isHasWeaponLicense(),
                factionTag != null ? factionTag : ""
        );
    }
}