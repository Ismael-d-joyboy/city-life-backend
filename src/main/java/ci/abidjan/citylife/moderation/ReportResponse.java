package ci.abidjan.citylife.moderation;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String reporterUsername,
        UUID targetPlayerId,
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
        return from(r, null, msg);
    }

    public static ReportResponse from(PlayerReport r, UUID targetPlayerId, String msg) {
        return new ReportResponse(
                r.getId(),
                r.getReporter() != null ? r.getReporter().getUsername() : "",
                targetPlayerId,
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
