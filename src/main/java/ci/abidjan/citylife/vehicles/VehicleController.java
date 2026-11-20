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
}