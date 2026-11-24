package ci.abidjan.citylife.combat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ArmoryBuyRequest(
        @NotNull UUID characterId,
        @NotBlank String itemCode,
        @Min(1) @Max(20) int quantity,
        @NotBlank String idempotencyKey
) {
}
