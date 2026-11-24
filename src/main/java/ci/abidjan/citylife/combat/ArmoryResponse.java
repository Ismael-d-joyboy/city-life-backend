package ci.abidjan.citylife.combat;

public record ArmoryResponse(
        String itemCode,
        String itemName,
        int quantityOwned,
        boolean hasWeaponLicense,
        long chargedFcfa,
        long cashBalance,
        long bankBalance,
        String message
) {
}
