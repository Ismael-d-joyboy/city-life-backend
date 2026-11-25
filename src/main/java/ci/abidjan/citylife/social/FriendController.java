package ci.abidjan.citylife.social;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public ResponseEntity<FriendListResponse> listFriends(Authentication authentication) {
        return ResponseEntity.ok(friendService.listFriends(authentication.getName()));
    }

    @PostMapping("/request")
    public ResponseEntity<FriendResponse> requestFriend(
            Authentication authentication,
            @Valid @RequestBody FriendRequestDto request) {
        return ResponseEntity.ok(friendService.sendFriendRequest(authentication.getName(), request));
    }

    @PostMapping("/{friendshipId}/accept")
    public ResponseEntity<FriendResponse> acceptFriend(
            Authentication authentication,
            @PathVariable UUID friendshipId) {
        return ResponseEntity.ok(friendService.acceptFriendRequest(authentication.getName(), friendshipId));
    }

    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<Map<String, String>> removeFriend(
            Authentication authentication,
            @PathVariable UUID friendshipId) {
        friendService.removeFriendship(authentication.getName(), friendshipId);
        return ResponseEntity.ok(Map.of("message", "Ami ou demande supprimé(e)."));
    }
}
