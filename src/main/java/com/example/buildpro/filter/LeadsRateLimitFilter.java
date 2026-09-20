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
 * Rate-limits POST /api/leads - the one write endpoint the public can call
 * without an admin login (it's the contact form). Nothing else stops a script
 * from hammering it, since Spring Security intentionally leaves it open so real
 * visitors can submit the form (see SecurityConfig).
 *
 * A simple sliding-window count per client IP: at most maxRequests submissions
 * per window, configurable via app.leads-rate-limit.* (LEADS_RATE_LIMIT_MAX_REQUESTS
 * / LEADS_RATE_LIMIT_WINDOW_MINUTES env vars), defaulting to 5 per 10 minutes.
 * Anything over that gets a 429 with a Retry-After header instead of reaching the
 * controller/database.
 *
 * This is in-memory, not Redis/DB-backed - correct and sufficient for a single
 * instance (which is how this app runs on Railway today). If it's ever scaled to
 * multiple instances behind a load balancer, each instance would track its own
 * counts, so the effective limit would loosely multiply by the instance count
 * instead of being enforced globally.
 */
@Slf4j
@Component
@Order(2)
public class LeadsRateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final Duration window;

    private final ConcurrentHashMap<String, Deque<Long>> requestTimestampsByIp = new ConcurrentHashMap<>();

    public LeadsRateLimitFilter(
            @Value("${app.leads-rate-limit.max-requests:5}") int maxRequests,
            @Value("${app.leads-rate-limit.window-minutes:10}") long windowMinutes) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && "/api/leads".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = clientIp(request);
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        Deque<Long> timestamps = requestTimestampsByIp.computeIfAbsent(clientIp, key -> new ConcurrentLinkedDeque<>());
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
            log.warn("Rate limit exceeded for {} on POST /api/leads", clientIp);
            writeTooManyRequests(response, request, retryAfterSeconds);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        // Railway (like most PaaS platforms) terminates TLS at a proxy in front
        // of the app and forwards the real client IP via X-Forwarded-For -
        // without this, every request would appear to come from the proxy's
        // own address, and the limit would apply to all visitors combined
        // instead of per-visitor.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        // Written by hand rather than via an ObjectMapper bean - this filter
        // runs outside Spring MVC's message-converter pipeline (how every other
        // JSON response in this app gets serialized), and this app's context
        // doesn't publish an injectable ObjectMapper bean to reuse here. Safe to
        // hand-build: the shape mirrors GlobalExceptionHandler's ApiError, and
        // every value here is either a fixed string or the request's own path
        // (escaped), with nothing user-supplied.
        String path = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":429,"
                + "\"error\":\"Too Many Requests\","
                + "\"message\":\"Too many submissions from this address. Please try again in a few minutes.\","
                + "\"path\":\"" + path + "\""
                + "}";
        response.getWriter().write(json);
    }
}
