package com.gateway.config;

import com.gateway.filter.AuthenticationFilter;
import com.gateway.filter.LoggingFilter;
import org.springframework.cloud.gateway.route.RouteLocator;

import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;

    /**
     * Initializes the component with default configuration.
     * Should be called before any other operations.
     */
    public GatewayConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }


    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .circuitBreaker(config -> config
                                        .setName("userServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/users"))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
                                .addRequestHeader("X-Gateway-Source", "spring-cloud-gateway")
                                .removeRequestHeader("Cookie"))
                        .uri("lb://USER-SERVICE"))
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .circuitBreaker(config -> config
                                        .setName("orderServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/orders"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter()))
                                .addRequestHeader("X-Gateway-Source", "spring-cloud-gateway"))
                        .uri("lb://ORDER-SERVICE"))
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("productServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/products"))
                                .addRequestHeader("X-Gateway-Source", "spring-cloud-gateway"))
                        .uri("lb://PRODUCT-SERVICE"))
                .build();
    }

    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter redisRateLimiter() {
        return new org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter(10, 20, 1);
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
     * Validates if the given string is not null or empty.
     * @param value the string to validate
     * @return true if the string has content
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    // Validate input parameters before processing


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
