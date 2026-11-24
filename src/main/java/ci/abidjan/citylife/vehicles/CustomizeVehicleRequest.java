package ci.abidjan.citylife.vehicles;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomizeVehicleRequest(
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Couleur hexadécimale #RRGGBB requise.")
        String colorHex
) {
}
