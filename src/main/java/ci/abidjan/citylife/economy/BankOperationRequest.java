package ci.abidjan.citylife.economy;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BankOperationRequest(
        @NotNull(message = "L'identifiant du personnage est obligatoire.")
        UUID characterId,

        @NotNull(message = "Le montant est obligatoire.")
        @Min(value = 1, message = "Le montant doit être supérieur à 0 FCFA.")
        Long amount,

        @NotBlank(message = "La clé d'idempotence est obligatoire.")
        @Size(max = 100)
        String idempotencyKey
) {}