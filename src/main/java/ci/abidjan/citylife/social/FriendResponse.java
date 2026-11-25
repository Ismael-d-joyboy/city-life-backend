package ci.abidjan.citylife.social;

import java.time.Instant;
import java.util.UUID;

public record FriendResponse(
        UUID friendshipId,
        UUID playerId,
        String username,
        Integer level,
        String status,
        boolean online,
        String lastDistrict,
        Instant updatedAt
) {
}
