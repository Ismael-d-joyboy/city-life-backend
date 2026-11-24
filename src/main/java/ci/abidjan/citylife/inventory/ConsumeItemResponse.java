package ci.abidjan.citylife.inventory;

public record ConsumeItemResponse(
        String itemCode,
        String itemName,
        int remainingQuantity,
        Integer health,
        Integer stamina,
        Integer hunger,
        Integer thirst,
        String message
) {}
