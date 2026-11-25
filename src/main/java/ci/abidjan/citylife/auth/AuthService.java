package ci.abidjan.citylife.auth;

import ci.abidjan.citylife.player.Player;
import ci.abidjan.citylife.player.PlayerProfile;
import ci.abidjan.citylife.player.PlayerProfileRepository;
import ci.abidjan.citylife.player.PlayerRepository;
import ci.abidjan.citylife.security.AccountUserDetailsService;
import ci.abidjan.citylife.security.JwtService;
import ci.abidjan.citylife.security.SecurityAuditLog;
import ci.abidjan.citylife.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PlayerRepository playerRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecurityAuditService auditService;
    private final AccountUserDetailsService userDetailsService;

    @Value("${app.jwt.expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        String cleanEmail = request.email().trim().toLowerCase();
        String cleanUsername = request.username().trim();

        if (accountRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
        if (playerRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris.");
        }

        Account account = Account.builder()
                .email(cleanEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Account.Role.PLAYER)
                .status(Account.Status.ACTIVE)
                .lastLoginAt(Instant.now())
                .build();
        account = accountRepository.save(account);

        Player player = Player.builder()
                .account(account)
                .username(cleanUsername)
                .build();
        player = playerRepository.save(player);

        PlayerProfile profile = PlayerProfile.builder()
                .player(player)
                .build();
        playerProfileRepository.save(profile);

        auditService.log(account, "REGISTER_SUCCESS", SecurityAuditLog.Severity.INFO, ipAddress,
                Map.of("username", cleanUsername));

        return issueTokens(account, player, request.deviceInfo(), ipAddress);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String cleanEmail = request.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(cleanEmail, request.password())
        );

        Account account = accountRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));

        account.setLastLoginAt(Instant.now());
        accountRepository.save(account);

        Player player = playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Profil joueur introuvable."));

        auditService.log(account, "LOGIN_SUCCESS", SecurityAuditLog.Severity.INFO, ipAddress,
                Map.of("device", request.deviceInfo() != null ? request.deviceInfo() : "unknown"));

        return issueTokens(account, player, request.deviceInfo(), ipAddress);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request, String ipAddress) {
        String hash = sha256(request.refreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalStateException("Refresh token invalide."));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Refresh token expiré ou révoqué.");
        }

        Account account = storedToken.getAccount();
        if (account.getStatus() != Account.Status.ACTIVE) {
            throw new IllegalStateException("Compte inactif ou bloqué.");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        Player player = playerRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new IllegalStateException("Profil joueur introuvable."));

        return issueTokens(account, player, storedToken.getDeviceInfo(), ipAddress);
    }

    /**
     * Suppression définitive de compte exigée par Google Play Data Safety (Jalon 0).
     * Exige le mot de passe actuel, consigne un audit anonyme et supprime le compte en cascade.
     */
    @Transactional
    public Map<String, Object> deleteAccount(String email, DeleteAccountRequest request, String ipAddress) {
        String cleanEmail = email.trim().toLowerCase();
        Account account = accountRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalStateException("Compte introuvable."));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            auditService.log(account, "ACCOUNT_DELETE_WRONG_PASSWORD", SecurityAuditLog.Severity.WARNING, ipAddress,
                    Map.of("reason", "Mot de passe incorrect"));
            throw new IllegalArgumentException("Mot de passe incorrect. Suppression annulée.");
        }

        auditService.log(null, "ACCOUNT_DELETED_BY_USER", SecurityAuditLog.Severity.INFO, ipAddress,
                Map.of("deletedAccountId", account.getId().toString()));

        if (userDetailsService != null) {
            userDetailsService.evictCache(cleanEmail);
        }
        accountRepository.delete(account);

        return Map.of(
                "deleted", true,
                "message", "Votre compte et vos données de jeu ont été définitivement supprimés."
        );
    }

    private AuthResponse issueTokens(Account account, Player player, String deviceInfo, String ipAddress) {
        String accessToken = jwtService.generateAccessToken(account);
        String rawRefreshToken = generateSecureToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .account(account)
                .tokenHash(sha256(rawRefreshToken))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        String mutedIso = (player.getMutedUntil() != null && player.getMutedUntil().isAfter(Instant.now()))
                ? player.getMutedUntil().toString()
                : null;

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                accessExpirationMs,
                account.getId(),
                player.getId(),
                player.getUsername(),
                account.getRole().name(),
                mutedIso
        );
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithme SHA-256 indisponible", e);
        }
    }
}