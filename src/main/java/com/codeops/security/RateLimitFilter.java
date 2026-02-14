package com.codeops.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servlet filter that enforces per-IP rate limiting on authentication endpoints
 * ({@code /api/v1/auth/**}) to prevent brute-force and credential-stuffing attacks.
 *
 * <p>Uses an in-memory sliding window strategy backed by a {@link ConcurrentHashMap}.
 * Each client IP is allowed a maximum of {@value #MAX_AUTH_REQUESTS_PER_MINUTE} requests
 * per {@value #WINDOW_MS}ms window. Requests exceeding the limit receive a
 * {@code 429 Too Many Requests} JSON response.</p>
 *
 * <p>Client IP is resolved from the {@code X-Forwarded-For} header (first entry) when
 * present, falling back to {@link HttpServletRequest#getRemoteAddr()} for direct connections.</p>
 *
 * @see SecurityConfig
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, RateWindow> buckets = new ConcurrentHashMap<>();

    /**
     * Applies rate limiting to authentication endpoint requests and passes all other
     * requests through without restriction.
     *
     * <p>When the rate limit is exceeded, writes a JSON error response with HTTP 429 status
     * and short-circuits the filter chain (the request is not forwarded to downstream filters
     * or the servlet).</p>
     *
     * @param request  the incoming HTTP request
     * @param response the HTTP response
     * @param chain    the filter chain to pass the request/response to
     * @throws ServletException if a servlet error occurs during filtering
     * @throws IOException      if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/v1/auth/")) {
            String key = getClientIp(request);
            RateWindow window = buckets.compute(key, (k, existing) -> {
                long now = System.currentTimeMillis();
                if (existing == null || now - existing.windowStart > WINDOW_MS) {
                    return new RateWindow(now, new AtomicInteger(1));
                }
                existing.count.incrementAndGet();
                return existing;
            });
            if (window.count.get() > MAX_AUTH_REQUESTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"message\":\"Rate limit exceeded. Try again later.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateWindow {
        final long windowStart;
        final AtomicInteger count;

        RateWindow(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
