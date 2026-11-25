package ci.abidjan.citylife.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UUID accountId,
        UUID playerId,
        String username,
        String role,
        String mutedUntil
) {
    public AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInMs,
            UUID accountId,
            UUID playerId,
            String username,
            String role
    ) {
        this(accessToken, refreshToken, tokenType, expiresInMs, accountId, playerId, username, role, null);
    }
}