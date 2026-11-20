package ci.abidjan.citylife.inventory;

import java.util.Map;
import java.util.UUID;

public record InventoryItemResponse(
        UUID inventoryId,
        String itemCode,
        String name,
        Item.Category category,
        Integer quantity,
        Long basePrice,
        Map<String, Object> effects
) {
    public static InventoryItemResponse from(InventoryItem inv) {
        Item item = inv.getItem();
        return new InventoryItemResponse(
                inv.getId(),
                item.getItemCode(),
                item.getName(),
                item.getCategory(),
                inv.getQuantity(),
                item.getBasePrice(),
                item.getEffects()
        );
    }
}