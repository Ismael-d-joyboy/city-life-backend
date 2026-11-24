package ci.abidjan.citylife.vehicles;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record RefuelRequest(
        UUID playerVehicleId,
        @Min(0) @Max(100) int currentFuelPercent,
        @NotBlank String idempotencyKey
) {
}
