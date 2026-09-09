package com.pavlent1yy.gradinator.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int TOO_MANY_REQUESTS = 429;
    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    private Bucket createBucket(RateLimitProperties.Limit limit) {
        Refill refill = Refill.greedy(
                limit.getCapacity(),
                Duration.ofMinutes(limit.getRefillMinutes())
        );

        Bandwidth bandwidth = Bandwidth.classic(
                limit.getCapacity(),
                refill
        );

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        RateLimitProperties.Limit limit = getLimit(request);

        String ip = getClientIp(request);
        String bucketKey = ip + ":" + getCategory(request);

        Bucket bucket = buckets.get(
                bucketKey,
                key -> createBucket(limit)
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long waitSeconds = (long) Math.ceil(
                probe.getNanosToWaitForRefill() / 1_000_000_000.0
        );

        response.setStatus(TOO_MANY_REQUESTS);
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setContentType("application/json");
        response.getWriter().write("""
        {
            "error": "Too many requests",
            "message": "Rate limit exceeded"
        }
        """);
    }

    private String getCategory(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (isPath(path, "/api/schedule")) {
            if (isScheduleAllRequest(request)) {
                return "schedule-all";
            }

            return "schedule";
        }

        if (isPath(path, "/api/admin")) {
            return "admin";
        }

        if (isPath(path, "/api/user")) {
            return "user";
        }

        return "other";
    }

    private RateLimitProperties.Limit getLimit(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (isPath(path, "/api/schedule")) {
            if (isScheduleAllRequest(request)) {
                return properties.getScheduleAll();
            }

            return properties.getSchedule();
        }

        if (isPath(path, "/api/admin")) {
            return properties.getAdmin();
        }

        if (isPath(path, "/api/user")) {
            return properties.getUser();
        }

        return properties.getSchedule();
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private boolean isScheduleAllRequest(HttpServletRequest request) {
        return "/api/schedule".equals(request.getRequestURI())
                && !request.getParameterMap().containsKey("group");
    }
    
    private boolean isPath(String path, String base) {
        return path.equals(base) || path.startsWith(base + "/");
    }
}