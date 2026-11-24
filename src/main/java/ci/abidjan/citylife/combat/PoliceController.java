package ci.abidjan.citylife.combat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/police")
@RequiredArgsConstructor
public class PoliceController {

    private final ArmoryService armoryService;

    @PostMapping("/fine")
    public ResponseEntity<PoliceFineResponse> payFine(
            Authentication authentication,
            @Valid @RequestBody PoliceFineRequest request
    ) {
        return ResponseEntity.ok(armoryService.applyPoliceFine(authentication.getName(), request));
    }
}
