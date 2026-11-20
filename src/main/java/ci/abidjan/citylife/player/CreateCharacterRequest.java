package ci.abidjan.citylife.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateCharacterRequest(
        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(min = 2, max = 60, message = "Le prénom doit contenir entre 2 et 60 caractères.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(min = 2, max = 60, message = "Le nom doit contenir entre 2 et 60 caractères.")
        String lastName,

        @NotNull(message = "Le genre (MALE ou FEMALE) est obligatoire.")
        Character.Gender gender,

        String skinTone,
        String hairStyle,
        String hairColor,
        Map<String, Object> outfitData
) {}