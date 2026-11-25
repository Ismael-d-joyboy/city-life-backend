package ci.abidjan.citylife.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FriendRequestDto(
        @NotBlank(message = "Le pseudo du joueur est obligatoire.")
        @Size(min = 2, max = 50, message = "Le pseudo doit contenir entre 2 et 50 caractères.")
        String targetUsername
) {
}
