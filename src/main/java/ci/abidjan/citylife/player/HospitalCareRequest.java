package ci.abidjan.citylife.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HospitalCareRequest(
        @NotBlank @Size(max = 100) String idempotencyKey,
        boolean emergencyRespawn
) {}
