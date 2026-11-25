package ci.abidjan.citylife.social;

import java.util.List;

public record FriendListResponse(
        List<FriendResponse> friends,
        List<FriendResponse> incomingRequests,
        List<FriendResponse> outgoingRequests
) {
}
