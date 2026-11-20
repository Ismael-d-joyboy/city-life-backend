package ci.abidjan.citylife.missions;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @GetMapping("/catalog")
    public List<Mission> getCatalog() {
        return missionService.getCatalog();
    }

    @GetMapping("/{characterId}")
    public List<PlayerMissionResponse> getMyMissions(
            Authentication authentication,
            @PathVariable UUID characterId) {
        return missionService.getMyMissions(authentication.getName(), characterId);
    }

    @PostMapping("/{characterId}/start")
    public ResponseEntity<PlayerMissionResponse> startMission(
            Authentication authentication,
            @PathVariable UUID characterId,
            @Valid @RequestBody StartMissionRequest request) {
        return ResponseEntity.ok(missionService.startMission(authentication.getName(), characterId, request));
    }

    @PostMapping("/{characterId}/complete")
    public ResponseEntity<PlayerMissionResponse> completeMission(
            Authentication authentication,
            @PathVariable UUID characterId,
            @Valid @RequestBody CompleteMissionRequest request) {
        return ResponseEntity.ok(missionService.completeMission(authentication.getName(), characterId, request));
    }
}