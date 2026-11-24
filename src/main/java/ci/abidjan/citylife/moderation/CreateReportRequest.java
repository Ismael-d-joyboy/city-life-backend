package ci.abidjan.citylife.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotBlank @Size(max = 80) String targetUsername,
        @NotNull PlayerReport.ReasonCode reasonCode,
        @Size(max = 400) String messageExcerpt
) {
}
