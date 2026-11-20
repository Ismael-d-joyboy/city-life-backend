package ci.abidjan.citylife.missions;

import jakarta.validation.constraints.NotBlank;

public record StartMissionRequest(
        @NotBlank(message = "Le code mission est obligatoire.")
        String missionCode
) {}