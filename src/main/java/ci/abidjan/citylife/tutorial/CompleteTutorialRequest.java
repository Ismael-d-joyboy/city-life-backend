package ci.abidjan.citylife.tutorial;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CompleteTutorialRequest(
        UUID characterId,

        @Size(max = 100, message = "La clé d'idempotence ne doit pas dépasser 100 caractères.")
        String idempotencyKey,

        boolean skipped
) {}
