package com.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Global gateway filter that logs detailed request and response information.
 * <p>
 * Adds a correlation ID to each request for distributed tracing and logs
 * method, path, status code, and response time for every request passing
 * through the gateway.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String START_TIME_ATTRIBUTE = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or preserve correlation ID
        String correlationId = resolveCorrelationId(request);

        // Add correlation ID to request headers for downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // Record start time
        long startTime = Instant.now().toEpochMilli();
        mutatedExchange.getAttributes().put(START_TIME_ATTRIBUTE, startTime);

        logIncomingRequest(mutatedRequest, correlationId);

        return chain.filter(mutatedExchange)
                .then(Mono.fromRunnable(() -> {
                    logOutgoingResponse(mutatedExchange, correlationId, startTime);
                }))
                .doOnError(throwable -> {
                    logError(mutatedRequest, correlationId, startTime, throwable);
                });
    }

    @Override
    public int getOrder() {
        // Execute before most other filters to ensure logging captures full lifecycle
        return -1;
    }

    private String resolveCorrelationId(ServerHttpRequest request) {
        String existingCorrelationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (existingCorrelationId != null && !existingCorrelationId.isBlank()) {
            return existingCorrelationId;
        }
        return UUID.randomUUID().toString();
    }

    private void logIncomingRequest(ServerHttpRequest request, String correlationId) {
        String clientIp = resolveClientIp(request);
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getURI().getPath();
        String queryString = request.getURI().getQuery();
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

        log.info("[{}] >>> {} {} | Client: {} | Query: {} | Content-Type: {} | User-Agent: {}",
                correlationId,
                method,
                path,
                clientIp,
                queryString != null ? queryString : "-",
                contentType != null ? contentType : "-",
                userAgent != null ? truncate(userAgent, 100) : "-"
        );

        if (log.isDebugEnabled()) {
            request.getHeaders().forEach((name, values) -> {
                if (!isSensitiveHeader(name)) {
                    log.debug("[{}] Request Header: {} = {}", correlationId, name, values);
                }
            });
        }
    }

    private void logOutgoingResponse(ServerWebExchange exchange, String correlationId, long startTime) {
        ServerHttpResponse response = exchange.getResponse();
        long duration = Instant.now().toEpochMilli() - startTime;

        int statusCode = response.getStatusCode() != null
                ? response.getStatusCode().value()
                : 0;

        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null
                ? exchange.getRequest().getMethod().name()
                : "UNKNOWN";

        // Add correlation ID to response headers
        response.getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        String logLevel = statusCode >= 500 ? "ERROR" : statusCode >= 400 ? "WARN" : "INFO";

        if (statusCode >= 500) {
            log.error("[{}] <<< {} {} | Status: {} | Duration: {}ms",
                    correlationId, method, path, statusCode, duration);
        } else if (statusCode >= 400) {
            log.warn("[{}] <<< {} {} | Status: {} | Duration: {}ms",
                    correlationId, method, path, statusCode, duration);
        } else {
            log.info("[{}] <<< {} {} | Status: {} | Duration: {}ms",
                    correlationId, method, path, statusCode, duration);
        }

        if (duration > 3000) {
            log.warn("[{}] Slow response detected: {} {} took {}ms",
                    correlationId, method, path, duration);
        }
    }

    private void logError(ServerHttpRequest request, String correlationId,
                          long startTime, Throwable throwable) {
        long duration = Instant.now().toEpochMilli() - startTime;
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getURI().getPath();

        log.error("[{}] !!! {} {} | Error: {} | Duration: {}ms",
                correlationId, method, path, throwable.getMessage(), duration, throwable);
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return Optional.ofNullable(request.getRemoteAddress())
                .map(InetSocketAddress::getHostString)
                .orElse("unknown");
    }

    private boolean isSensitiveHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.contains("authorization") ||
                lower.contains("cookie") ||
                lower.contains("token") ||
                lower.contains("secret") ||
                lower.contains("api-key");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
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


    /**
     * Safely parses an integer from a string value.
     * @param value the string to parse
     * @param defaultValue the fallback value
     * @return parsed integer or default value
     */
    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
