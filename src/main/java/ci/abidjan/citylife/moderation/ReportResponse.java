package ci.abidjan.citylife.moderation;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String reporterUsername,
        String targetUsername,
        PlayerReport.ReasonCode reasonCode,
        String messageExcerpt,
        PlayerReport.Status status,
        String resolutionNote,
        Instant createdAt,
        Instant resolvedAt,
        String message
) {
    public static ReportResponse from(PlayerReport r, String msg) {
        return new ReportResponse(
                r.getId(),
                r.getReporter() != null ? r.getReporter().getUsername() : "",
                r.getTargetUsername(),
                r.getReasonCode(),
                r.getMessageExcerpt(),
                r.getStatus(),
                r.getResolutionNote(),
                r.getCreatedAt(),
                r.getResolvedAt(),
                msg
        );
    }
}
