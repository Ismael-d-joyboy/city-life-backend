package ci.abidjan.citylife.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BanPlayerRequest(
        @NotBlank(message = "Le motif du bannissement est obligatoire.")
        @Size(min = 3, max = 250)
        String reason
) {}
