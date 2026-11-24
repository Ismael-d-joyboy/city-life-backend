package ci.abidjan.citylife.moderation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminModerationController {

    private final ModerationService moderationService;

    @GetMapping("/reports")
    public ResponseEntity<List<ReportResponse>> listReports(
            @RequestParam(required = false) PlayerReport.Status status
    ) {
        return ResponseEntity.ok(moderationService.listReports(status));
    }

    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<ReportResponse> resolveReport(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody ResolveReportRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        return ResponseEntity.ok(moderationService.resolveReport(authentication.getName(), id, request, ip));
    }

    @PostMapping("/players/{id}/mute")
    public ResponseEntity<Map<String, Object>> mutePlayer(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody MutePlayerRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        return ResponseEntity.ok(moderationService.mutePlayer(authentication.getName(), id, request, ip));
    }
}
