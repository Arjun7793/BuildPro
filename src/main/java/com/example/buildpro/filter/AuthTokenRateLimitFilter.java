package com.example.buildpro.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate-limits POST /api/auth/token (the mobile app's sign-in) per client IP, so
 * nobody can script unlimited password guesses against the admin account.
 *
 * Same sliding-window approach as LeadsRateLimitFilter: at most maxRequests
 * attempts per window, configurable via app.auth-rate-limit.*
 * (AUTH_RATE_LIMIT_MAX_REQUESTS / AUTH_RATE_LIMIT_WINDOW_MINUTES), defaulting to
 * 10 per 15 minutes. Every attempt counts, successful or not - a real admin signs
 * in rarely, so this never gets in their way. Over the limit gets a 429 with a
 * Retry-After header.
 *
 * In-memory, so per instance - fine for the single Railway instance this runs on
 * (see LeadsRateLimitFilter for the multi-instance caveat).
 */
@Slf4j
@Component
@Order(2)
public class AuthTokenRateLimitFilter extends OncePerRequestFilter {

    private static final String TOKEN_PATH = "/api/auth/token";

    private final int maxRequests;
    private final Duration window;

    private final ConcurrentHashMap<String, Deque<Long>> attemptsByIp = new ConcurrentHashMap<>();

    public AuthTokenRateLimitFilter(
            @Value("${app.auth-rate-limit.max-requests:10}") int maxRequests,
            @Value("${app.auth-rate-limit.window-minutes:15}") long windowMinutes) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && TOKEN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = clientIp(request);
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        Deque<Long> timestamps = attemptsByIp.computeIfAbsent(clientIp, key -> new ConcurrentLinkedDeque<>());
        boolean limited;
        long retryAfterSeconds = 0;
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            limited = timestamps.size() >= maxRequests;
            if (limited) {
                retryAfterSeconds = Math.max(1, (timestamps.peekFirst() + window.toMillis() - now) / 1000);
            } else {
                timestamps.addLast(now);
            }
        }

        if (limited) {
            log.warn("Sign-in rate limit exceeded for {} on POST {}", clientIp, TOKEN_PATH);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            // Hand-built for the same reason as LeadsRateLimitFilter: this runs
            // outside Spring MVC's converters. Every value is fixed or numeric.
            response.getWriter().write("{"
                    + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                    + "\"status\":429,"
                    + "\"error\":\"Too Many Requests\","
                    + "\"message\":\"Too many sign-in attempts. Please try again later.\","
                    + "\"path\":\"" + TOKEN_PATH + "\""
                    + "}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        // Real client IP behind Railway's proxy - see LeadsRateLimitFilter.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
