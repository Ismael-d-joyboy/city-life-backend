package ci.abidjan.citylife.economy;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "L'identifiant du personnage est obligatoire.")
        UUID characterId,

        @NotBlank(message = "Le pseudo du destinataire est obligatoire.")
        @Size(min = 2, max = 50, message = "Le pseudo destinataire doit contenir entre 2 et 50 caractères.")
        String recipientUsername,

        @NotNull(message = "Le montant FCFA est obligatoire.")
        @Min(value = 100, message = "Le montant minimum d'un transfert Lagune Money est de 100 FCFA.")
        @Max(value = 500000, message = "Le plafond par transfert Lagune Money est de 500 000 FCFA.")
        Long amount,

        @Size(max = 120, message = "Le motif ne doit pas dépasser 120 caractères.")
        String reason,

        @NotBlank(message = "La clé d'idempotence est obligatoire.")
        @Size(max = 100)
        String idempotencyKey
) {
}
