package ci.abidjan.citylife.inventory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BuyItemRequest(
        @NotBlank(message = "Le code objet est obligatoire.")
        String itemCode,

        @NotNull(message = "La quantité est obligatoire.")
        @Min(value = 1, message = "La quantité minimale est 1.")
        @Max(value = 50, message = "La quantité maximale par achat est 50.")
        Integer quantity,

        @NotBlank(message = "La clé d'idempotence est obligatoire.")
        @Size(max = 100)
        String idempotencyKey
) {}