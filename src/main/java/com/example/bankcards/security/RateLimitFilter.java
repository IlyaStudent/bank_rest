package com.example.bankcards.security;

import com.example.bankcards.config.RateLimitProperties;
import com.example.bankcards.util.constants.ApiErrorMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final LettuceBasedProxyManager<String> proxyManager;
    private final RateLimitProperties rateLimitProperties;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitProperties.LimitRule rule = resolveRule(path, method);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = resolveBucketKey(path, request);
        if (bucketKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        BucketConfiguration configuration = buildConfiguration(rule);
        BucketProxy bucket = proxyManager.builder()
                .build(bucketKey, () -> configuration);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = Duration.ofNanos(
                    probe.getNanosToWaitForRefill()
            ).toSeconds() + 1;

            log.warn("Rate limit exceeded: key={}, path={}, retryAfter={}s",
                    bucketKey, path, retryAfterSeconds);

            sendRateLimitResponse(response, request, retryAfterSeconds);
        }
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator") ||
                path.startsWith("/webjars");
    }

    private RateLimitProperties.LimitRule resolveRule(String path, String method) {
        if (path.equals("/api/auth/login") && "POST".equals(method)) {
            return rateLimitProperties.getLogin();
        }
        if (path.equals("/api/auth/register") && "POST".equals(method)) {
            return rateLimitProperties.getRegister();
        }
        if (path.equals("/api/transfers") && "POST".equals(method)) {
            return rateLimitProperties.getTransfers();
        }
        if (path.startsWith("/api/")) {
            return rateLimitProperties.getGeneral();
        }
        return null;
    }

    private String resolveBucketKey(String path, HttpServletRequest request) {
        if (path.startsWith("/api/auth/")) {
            String ip = resolveClientIp(request);
            return "rate-limit:auth:" + path.substring("/api/auth/".length()) + ":"
                    + ip;
        }

        String username = extractUsernameFromToken(request);
        if (username != null) {
            if (path.startsWith("/api/transfers")) {
                return "rate-limit:transfers:" + username;
            }
            return "rate-limit:api:" + username;
        }

        return null;
    }

    private String extractUsernameFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            return jwtProvider.getUsername(token);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }


    private BucketConfiguration buildConfiguration(RateLimitProperties.LimitRule
                                                           rule) {
        return BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(rule.getCapacity())
                                .refillGreedy(rule.getCapacity(),
                                        Duration.ofMinutes(rule.getMinutes()))
                                .build()
                )
                .build();
    }

    private void sendRateLimitResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            long retryAfterSeconds
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.addHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", ApiErrorMessage.RATE_LIMIT_EXCEEDED.getMessage());
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }

}
