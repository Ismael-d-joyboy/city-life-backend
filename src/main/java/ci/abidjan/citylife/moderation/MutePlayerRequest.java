package ci.abidjan.citylife.moderation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MutePlayerRequest(
        @Min(1) @Max(10080) int durationMinutes,
        @NotBlank @Size(max = 255) String reason
) {
}
