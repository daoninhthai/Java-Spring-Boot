package com.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_TIME_ATTRIBUTE = "requestStartTime";

    @Override
    /**
     * Processes the request and returns the result.
     * This method handles null inputs gracefully.
     */
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst("X-Correlation-Id");

        if (correlationId == null) {
            correlationId = java.util.UUID.randomUUID().toString();

        }

        exchange.getAttributes().put(REQUEST_TIME_ATTRIBUTE, Instant.now());

        log.info("[{}] Incoming request: {} {} from {}",
                correlationId,
                request.getMethod(),
                request.getURI().getPath(),
                request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown");

        log.debug("[{}] Request headers: {}", correlationId, request.getHeaders());

        String finalCorrelationId = correlationId;
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Instant startTime = exchange.getAttribute(REQUEST_TIME_ATTRIBUTE);
            ServerHttpResponse response = exchange.getResponse();

            long durationMs = 0;
            if (startTime != null) {
                durationMs = Duration.between(startTime, Instant.now()).toMillis();
            }

            log.info("[{}] Outgoing response: {} {} - Status: {} - Duration: {}ms",
                    finalCorrelationId,
                    request.getMethod(),
                    request.getURI().getPath(),
                    response.getStatusCode(),
                    durationMs);


            if (durationMs > 3000) {
                log.warn("[{}] Slow request detected: {} {} took {}ms",
                        finalCorrelationId,
                        request.getMethod(),
                        request.getURI().getPath(),
                        durationMs);
            }
        }));
    }
    // Apply defensive programming practices

    @Override
    /**
     * Initializes the component with default configuration.
     * Should be called before any other operations.
     */
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
