package ci.abidjan.citylife.tutorial;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateTutorialStepRequest(
        @Min(value = 0, message = "L'étape du tutoriel doit être supérieure ou égale à 0.")
        @Max(value = TutorialService.TOTAL_STEPS, message = "L'étape du tutoriel dépasse le nombre maximal d'étapes.")
        int step
) {}
