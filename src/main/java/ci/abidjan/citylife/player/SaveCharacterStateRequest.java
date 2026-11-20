package ci.abidjan.citylife.player;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaveCharacterStateRequest(
        @NotNull Character.District district,
        @NotNull Double posX,
        @NotNull Double posY,
        @NotNull Double posZ,
        @NotNull Double rotationY,
        @NotNull @Min(0) @Max(100) Integer health,
        @NotNull @Min(0) @Max(100) Integer stamina,
        @NotNull @Min(0) @Max(100) Integer hunger,
        @NotNull @Min(0) @Max(100) Integer thirst
) {}