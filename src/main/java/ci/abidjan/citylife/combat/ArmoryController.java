package ci.abidjan.citylife.combat;

import ci.abidjan.citylife.inventory.Item;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/armory")
@RequiredArgsConstructor
public class ArmoryController {

    private final ArmoryService armoryService;

    @GetMapping("/catalog")
    public ResponseEntity<List<Item>> getCatalog() {
        return ResponseEntity.ok(armoryService.getArmoryCatalog());
    }

    @PostMapping("/license")
    public ResponseEntity<ArmoryResponse> buyLicense(
            Authentication authentication,
            @RequestParam UUID characterId,
            @RequestParam(defaultValue = "") String idempotencyKey
    ) {
        String key = idempotencyKey.isBlank() ? ("lic-" + characterId + "-" + System.currentTimeMillis()) : idempotencyKey;
        return ResponseEntity.ok(armoryService.buyWeaponLicense(authentication.getName(), characterId, key));
    }

    @PostMapping("/buy")
    public ResponseEntity<ArmoryResponse> buyItem(
            Authentication authentication,
            @Valid @RequestBody ArmoryBuyRequest request
    ) {
        return ResponseEntity.ok(armoryService.buyArmoryItem(authentication.getName(), request));
    }
}
