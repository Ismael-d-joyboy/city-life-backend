package ci.abidjan.citylife.missions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteMissionRequest(
        @NotBlank(message = "Le code mission est obligatoire.")
        String missionCode,

        @NotBlank(message = "La clé d'idempotence est obligatoire.")
        @Size(max = 100)
        String idempotencyKey
) {}