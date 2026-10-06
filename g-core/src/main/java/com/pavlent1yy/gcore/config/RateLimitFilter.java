package com.pavlent1yy.gcore.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int TOO_MANY_REQUESTS = 429;

    private static final Set<String> AUTH_PATHS = Set.of(
            "/core/auth/login",
            "/core/auth/register"
    );

    private static final Set<String> EMAIL_PATHS = Set.of(
            "/core/auth/resend-verification"
    );

    private static final Pattern INTERNAL_ADDRESS = Pattern.compile(
            "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
                    + "|192\\.168\\.\\d{1,3}\\.\\d{1,3}"
                    + "|169\\.254\\.\\d{1,3}\\.\\d{1,3}"
                    + "|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
                    + "|172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3}"
                    + "|0:0:0:0:0:0:0:1|::1"
    );

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofMinutes(15))
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

        String category = getCategory(request);
        String ip = getClientIp(request);

        Bucket bucket = buckets.get(
                ip + ":" + category,
                key -> createBucket(getLimit(category))
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long waitSeconds = (long) Math.ceil(
                probe.getNanosToWaitForRefill() / 1_000_000_000.0
        );

        log.warn(
                "Rate limit exceeded: ip={}, method={}, uri={}, category={}",
                ip,
                request.getMethod(),
                request.getRequestURI(),
                category
        );
        response.setStatus(TOO_MANY_REQUESTS);
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
        {
            "error": "Слишком много запросов, попробуйте позже",
            "message": "Rate limit exceeded"
        }
        """);
    }

    private String getCategory(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (AUTH_PATHS.contains(path)) {
            return "auth";
        }

        if (EMAIL_PATHS.contains(path)) {
            return "email";
        }

        return "general";
    }

    private RateLimitProperties.Limit getLimit(String category) {
        return switch (category) {
            case "auth" -> properties.getAuth();
            case "email" -> properties.getEmail();
            default -> properties.getGeneral();
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (!isInternal(remoteAddr) || forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddr;
        }

        String[] chain = forwardedFor.split(",");
        String candidate = remoteAddr;

        for (int i = chain.length - 1; i >= 0; i--) {
            String address = chain[i].trim();
            if (address.isEmpty()) {
                continue;
            }
            candidate = address;
            if (!isInternal(address)) {
                return address;
            }
        }

        return candidate;
    }

    private boolean isInternal(String address) {
        return INTERNAL_ADDRESS.matcher(address).matches();
    }
}
