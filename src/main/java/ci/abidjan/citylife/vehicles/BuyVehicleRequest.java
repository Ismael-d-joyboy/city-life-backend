package ci.abidjan.citylife.vehicles;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuyVehicleRequest(
        @NotBlank(message = "Le code véhicule est obligatoire.")
        String vehicleCode,

        String colorHex,

        @NotBlank(message = "La clé d'idempotence est obligatoire.")
        @Size(max = 100)
        String idempotencyKey
) {}