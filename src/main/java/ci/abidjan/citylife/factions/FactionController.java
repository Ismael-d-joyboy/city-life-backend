package ci.abidjan.citylife.factions;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/factions")
@RequiredArgsConstructor
public class FactionController {

    private final FactionService factionService;

    @GetMapping
    public ResponseEntity<List<FactionResponse>> listFactions(
            Authentication authentication,
            @RequestParam(required = false) UUID characterId
    ) {
        return ResponseEntity.ok(factionService.listFactions(authentication.getName(), characterId));
    }

    @PostMapping
    public ResponseEntity<FactionResponse> createFaction(
            Authentication authentication,
            @Valid @RequestBody CreateFactionRequest request
    ) {
        return ResponseEntity.ok(factionService.createFaction(authentication.getName(), request));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<FactionResponse> joinFaction(
            Authentication authentication,
            @PathVariable String code,
            @RequestParam UUID characterId
    ) {
        return ResponseEntity.ok(factionService.joinFaction(authentication.getName(), characterId, code));
    }

    @PostMapping("/leave")
    public ResponseEntity<Map<String, String>> leaveFaction(
            Authentication authentication,
            @RequestParam UUID characterId
    ) {
        String message = factionService.leaveFaction(authentication.getName(), characterId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/territories")
    public ResponseEntity<List<FactionResponse.TerritoryResponse>> listTerritories() {
        return ResponseEntity.ok(factionService.listTerritories());
    }

    @PostMapping("/territories/{zoneCode}/capture")
    public ResponseEntity<FactionResponse.TerritoryResponse> captureTerritory(
            Authentication authentication,
            @PathVariable String zoneCode,
            @RequestParam UUID characterId
    ) {
        return ResponseEntity.ok(factionService.captureTerritory(authentication.getName(), characterId, zoneCode));
    }
}
