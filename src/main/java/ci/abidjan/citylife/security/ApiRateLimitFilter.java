package ci.abidjan.citylife.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtre léger de limitation de débit (Rate Limiting) sans dépendance externe
 * pour préserver la RAM sur Render Free (512 Mo) tout en bloquant le brute-force
 * et le spam d'API (/api/auth/**, /api/jobs/payout, /api/reports).
 */
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_BUCKETS = 4096;

    private record WindowBucket(long windowStartMs, AtomicInteger count) {}

    private final Map<String, WindowBucket> buckets = new ConcurrentHashMap<>();
    private volatile long lastCleanupMs = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        int limit = resolveLimitPerMinute(path, request.getMethod());
        if (limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        cleanupStaleBucketsIfNeeded(now);

        String clientKey = resolveClientIp(request) + "|" + normalizeRouteBucket(path);
        WindowBucket bucket = buckets.compute(clientKey, (k, existing) -> {
            if (existing == null || (now - existing.windowStartMs()) >= WINDOW_MS) {
                return new WindowBucket(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (bucket.count().get() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"timestamp\":\"" + Instant.now() + "\",\"status\":429," +
                    "\"message\":\"Trop de requêtes. Veuillez patienter quelques secondes.\",\"errors\":null}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimitPerMinute(String path, String method) {
        if (path == null) return 0;
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            return 12;
        }
        if (path.startsWith("/api/auth/refresh") || path.startsWith("/api/auth/account")) {
            return 15;
        }
        if (path.equals("/api/jobs/payout") && "POST".equalsIgnoreCase(method)) {
            return 15;
        }
        if (path.equals("/api/reports") && "POST".equalsIgnoreCase(method)) {
            return 6;
        }
        return 0;
    }

    private String normalizeRouteBucket(String path) {
        if (path.startsWith("/api/auth/")) return "AUTH";
        if (path.equals("/api/jobs/payout")) return "JOB_PAYOUT";
        if (path.equals("/api/reports")) return "REPORTS";
        return path;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void cleanupStaleBucketsIfNeeded(long now) {
        if (buckets.size() < MAX_TRACKED_BUCKETS && (now - lastCleanupMs) < 120_000L) {
            return;
        }
        lastCleanupMs = now;
        buckets.entrySet().removeIf(e -> (now - e.getValue().windowStartMs()) >= WINDOW_MS * 2);
    }
}
