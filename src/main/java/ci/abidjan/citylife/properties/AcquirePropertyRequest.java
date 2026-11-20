package ci.abidjan.citylife.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcquirePropertyRequest(
        @NotBlank(message = "Le code propriété est obligatoire.")
        String propertyCode,

        @NotNull(message = "Le type (OWNED ou RENTED) est obligatoire.")
        PlayerProperty.OwnershipType ownershipType,

        @NotBlank(message = "La clé d'idempotence est obligatoire.")
        @Size(max = 100)
        String idempotencyKey
) {}