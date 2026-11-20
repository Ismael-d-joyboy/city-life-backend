package ci.abidjan.citylife.properties;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping("/catalog")
    public List<Property> getCatalog() {
        return propertyService.getCatalog();
    }

    @GetMapping("/{characterId}")
    public List<PlayerPropertyResponse> getMyProperties(
            Authentication authentication,
            @PathVariable UUID characterId) {
        return propertyService.getMyProperties(authentication.getName(), characterId);
    }

    @PostMapping("/{characterId}/acquire")
    public ResponseEntity<PlayerPropertyResponse> acquireProperty(
            Authentication authentication,
            @PathVariable UUID characterId,
            @Valid @RequestBody AcquirePropertyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.acquireProperty(authentication.getName(), characterId, request));
    }
}