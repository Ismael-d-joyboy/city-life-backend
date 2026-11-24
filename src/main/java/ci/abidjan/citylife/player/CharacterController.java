package ci.abidjan.citylife.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @PostMapping
    public ResponseEntity<CharacterResponse> createCharacter(
            Authentication authentication,
            @Valid @RequestBody CreateCharacterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(characterService.createCharacter(authentication.getName(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<CharacterResponse>> getMyCharacters(Authentication authentication) {
        return ResponseEntity.ok(characterService.getMyCharacters(authentication.getName()));
    }

    @PutMapping("/{characterId}/save-state")
    public ResponseEntity<CharacterResponse> saveState(
            Authentication authentication,
            @PathVariable UUID characterId,
            @Valid @RequestBody SaveCharacterStateRequest request) {
        return ResponseEntity.ok(characterService.saveState(authentication.getName(), characterId, request));
    }

    @PutMapping("/{characterId}/appearance")
    public ResponseEntity<CharacterResponse> updateAppearance(
            Authentication authentication,
            @PathVariable UUID characterId,
            @Valid @RequestBody UpdateAppearanceRequest request) {
        return ResponseEntity.ok(characterService.updateAppearance(authentication.getName(), characterId, request));
    }

    @PostMapping("/{characterId}/hospital-care")
    public ResponseEntity<HospitalCareResponse> hospitalCare(
            Authentication authentication,
            @PathVariable UUID characterId,
            @Valid @RequestBody HospitalCareRequest request) {
        return ResponseEntity.ok(characterService.hospitalCare(authentication.getName(), characterId, request));
    }
}