package ci.abidjan.citylife.moderation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ModerationService moderationService;

    @PostMapping
    public ResponseEntity<ReportResponse> submitReport(
            Authentication authentication,
            @Valid @RequestBody CreateReportRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moderationService.createReport(authentication.getName(), request, ip));
    }
}
