package ci.abidjan.citylife.inventory;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/catalog")
    public ResponseEntity<List<Item>> getCatalog() {
        return ResponseEntity.ok(inventoryService.getCatalog());
    }

    @GetMapping("/{characterId}")
    public ResponseEntity<List<InventoryItemResponse>> getInventory(
            Authentication authentication,
            @PathVariable UUID characterId) {
        return ResponseEntity.ok(inventoryService.getInventory(authentication.getName(), characterId));
    }

    @PostMapping("/{characterId}/buy")
    public ResponseEntity<InventoryItemResponse> buyItem(
            Authentication authentication,
            @PathVariable UUID characterId,
            @Valid @RequestBody BuyItemRequest request) {
        return ResponseEntity.ok(inventoryService.buyItem(authentication.getName(), characterId, request));
    }
}