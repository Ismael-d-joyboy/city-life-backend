package ci.abidjan.citylife.economy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobPayoutController {

    private final JobPayoutService jobPayoutService;

    @PostMapping("/payout")
    public ResponseEntity<JobPayoutResponse> claimPayout(
            Authentication authentication,
            @Valid @RequestBody JobPayoutRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = httpRequest.getRemoteAddr();
        }
        return ResponseEntity.ok(jobPayoutService.claimPayout(authentication.getName(), request, ip));
    }
}