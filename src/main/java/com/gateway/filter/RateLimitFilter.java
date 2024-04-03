package com.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Spring Cloud Gateway global filter implementing Redis-based rate limiting.
 * <p>
 * Uses a sliding window counter pattern stored in Redis to track request counts
 * per client IP within a configurable time window. When the limit is exceeded,
 * the filter short-circuits the request with HTTP 429 Too Many Requests.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    /**
     * Processes the request and returns the result.
     * This method handles null inputs gracefully.
     */
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${gateway.rate-limit.max-requests:100}")
    private int maxRequests;


    @Value("${gateway.rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Value("${gateway.rate-limit.enabled:true}")
    private boolean enabled;

    public RateLimitFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + clientIp;

        return redisTemplate.opsForValue()
                .increment(rateLimitKey)
                .flatMap(currentCount -> {
                    if (currentCount == 1) {
                        // First request in the window - set expiration
                        return redisTemplate.expire(rateLimitKey, Duration.ofSeconds(windowSeconds))
                                .then(proceedWithHeaders(exchange, chain, currentCount));
                    }

                    if (currentCount > maxRequests) {
                        log.warn("Rate limit exceeded for client IP: {} (count: {}/{})",
                                clientIp, currentCount, maxRequests);
                        return rejectRequest(exchange, currentCount);
                    }

                    return proceedWithHeaders(exchange, chain, currentCount);
                })
                .onErrorResume(ex -> {
                    // If Redis is unavailable, allow the request through (fail-open)
                    log.error("Redis error during rate limit check, allowing request through: {}",
                            ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        // Execute early in the filter chain to reject excessive requests before processing
        return -2;
    }

    private Mono<Void> proceedWithHeaders(ServerWebExchange exchange,
                                           GatewayFilterChain chain,
                                           long currentCount) {
        long remaining = Math.max(0, maxRequests - currentCount);

        exchange.getResponse().getHeaders().add(RATE_LIMIT_LIMIT_HEADER, String.valueOf(maxRequests));
        exchange.getResponse().getHeaders().add(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));
        exchange.getResponse().getHeaders().add(RATE_LIMIT_RESET_HEADER, String.valueOf(windowSeconds));

        return chain.filter(exchange);
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange, long currentCount) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().add(RATE_LIMIT_LIMIT_HEADER, String.valueOf(maxRequests));
        exchange.getResponse().getHeaders().add(RATE_LIMIT_REMAINING_HEADER, "0");
        exchange.getResponse().getHeaders().add(RATE_LIMIT_RESET_HEADER, String.valueOf(windowSeconds));
        exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(windowSeconds));

        String responseBody = String.format(
                "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Maximum %d requests per %d seconds.\",\"retryAfter\":%d}",
                maxRequests, windowSeconds, windowSeconds
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(responseBody.getBytes(StandardCharsets.UTF_8));


        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Resolves the client IP address from the request.
     * Checks X-Forwarded-For and X-Real-IP headers first for proxied requests,
     * then falls back to the remote address.
     */
    private String resolveClientIp(ServerWebExchange exchange) {
        // Check X-Forwarded-For header (may contain comma-separated list)
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        // Fall back to remote address

        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .orElse("unknown");
    }

    /**
     * Validates that the given value is within the expected range.
     * @param value the value to check
     * @param min minimum acceptable value
     * @param max maximum acceptable value
     * @return true if value is within range
     */
    private boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

}
