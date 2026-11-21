package ci.abidjan.citylife.tutorial;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutorial")
@RequiredArgsConstructor
public class TutorialController {

    private final TutorialService tutorialService;

    @GetMapping("/status")
    public ResponseEntity<TutorialStatusResponse> getStatus(Authentication authentication) {
        return ResponseEntity.ok(tutorialService.getStatus(authentication.getName()));
    }

    @PutMapping("/step")
    public ResponseEntity<TutorialStatusResponse> updateStep(
            Authentication authentication,
            @Valid @RequestBody UpdateTutorialStepRequest request) {
        return ResponseEntity.ok(tutorialService.updateStep(authentication.getName(), request));
    }

    @PostMapping("/complete")
    public ResponseEntity<TutorialStatusResponse> completeTutorial(
            Authentication authentication,
            @Valid @RequestBody CompleteTutorialRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = httpRequest.getRemoteAddr();
        }
        return ResponseEntity.ok(tutorialService.completeTutorial(authentication.getName(), request, ip));
    }
}
