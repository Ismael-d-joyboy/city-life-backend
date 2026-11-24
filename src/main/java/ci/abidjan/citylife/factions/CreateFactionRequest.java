package ci.abidjan.citylife.factions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFactionRequest(
        @NotNull UUID characterId,
        @NotBlank @Size(min = 2, max = 6) @Pattern(regexp = "^[A-Za-z0-9]{2,6}$", message = "Tag alphanumérique 2 à 6 caractères requis.") String tag,
        @NotBlank @Size(min = 3, max = 60) String name,
        @Size(max = 16) String colorHex
) {
}
