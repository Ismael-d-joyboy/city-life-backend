package ci.abidjan.citylife.moderation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveReportRequest(
        @NotNull PlayerReport.Status status,
        @Size(max = 300) String resolutionNote
) {
}
