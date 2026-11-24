package ci.abidjan.citylife.combat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PoliceFineRequest(
        @NotNull UUID characterId,
        @Min(1) @Max(3) int wantedStars,
        @NotBlank String reason,
        boolean shortCustody,
        @NotBlank String idempotencyKey
) {
}
