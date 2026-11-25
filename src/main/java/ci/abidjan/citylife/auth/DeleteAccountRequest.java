package ci.abidjan.citylife.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire pour supprimer le compte.")
        @Size(min = 6, max = 128)
        String password
) {}
