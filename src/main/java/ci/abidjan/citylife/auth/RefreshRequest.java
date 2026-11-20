package ci.abidjan.citylife.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Le refreshToken est obligatoire.")
        String refreshToken
) {}