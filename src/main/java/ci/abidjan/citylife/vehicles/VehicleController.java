package ci.abidjan.citylife.vehicles;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping("/catalog")
    public List<Vehicle> getCatalog() {
        return vehicleService.getCatalog();
    }

    @GetMapping("/{characterId}")
    public List<PlayerVehicleResponse> getMyVehicles(
            Authentication authentication,
            @PathVariable UUID characterId) {
        return vehicleService.getMyVehicles(authentication.getName(), characterId);
    }

    @PostMapping("/{characterId}/buy")
    public ResponseEntity<PlayerVehicleResponse> buyVehicle(
            Authentication authentication,
            @PathVariable UUID characterId,
            @Valid @RequestBody BuyVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.buyVehicle(authentication.getName(), characterId, request));
    }

    @PostMapping("/{characterId}/refuel")
    public ResponseEntity<RefuelResponse> refuelVehicle(
            Authentication authentication,
            @PathVariable UUID characterId,
            @Valid @RequestBody RefuelRequest request) {
        return ResponseEntity.ok(vehicleService.refuelVehicle(authentication.getName(), characterId, request));
    }

    @PutMapping("/{characterId}/{vehicleId}/customize")
    public ResponseEntity<PlayerVehicleResponse> customizeVehicle(
            Authentication authentication,
            @PathVariable UUID characterId,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody CustomizeVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.customizeVehicle(authentication.getName(), characterId, vehicleId, request));
    }
}