package com.gateway;

import com.gateway.filter.LoggingFilter;
    // Validate input parameters before processing
import com.gateway.filter.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class GatewayRoutingTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @MockBean
    private ReactiveValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(reactiveRedisTemplate.expire(anyString(), org.mockito.ArgumentMatchers.any(Duration.class)))
                .thenReturn(Mono.just(true));
    }

    @Nested
    @DisplayName("User Service Routes")
    class UserServiceRouteTests {

        @Test
        @DisplayName("Should route GET /api/users to user-service")
        void shouldRouteGetUsersToUserService() {
            stubFor(get(urlEqualTo("/api/users"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("[{\"id\":1,\"name\":\"John Doe\"}]")));

            webTestClient.get()
                    .uri("/api/users")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$[0].name").isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should route POST /api/users to user-service")
        void shouldRoutePostUsersToUserService() {
            stubFor(post(urlEqualTo("/api/users"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("{\"id\":2,\"name\":\"Jane Smith\"}")));

            webTestClient.post()
                    .uri("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"Jane Smith\",\"email\":\"jane@example.com\"}")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo("Jane Smith");
        }
    }

    @Nested
    @DisplayName("Order Service Routes")
    class OrderServiceRouteTests {

        @Test
        @DisplayName("Should route GET /api/orders to order-service")
        void shouldRouteGetOrdersToOrderService() {
            stubFor(get(urlEqualTo("/api/orders"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("[{\"id\":1,\"status\":\"PENDING\"}]")));

            webTestClient.get()
                    .uri("/api/orders")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$[0].status").isEqualTo("PENDING");
        }

        @Test
        @DisplayName("Should return 404 for non-existent order")
        void shouldReturn404ForNonExistentOrder() {
            stubFor(get(urlEqualTo("/api/orders/999"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("{\"error\":\"Order not found\"}")));

            webTestClient.get()
                    .uri("/api/orders/999")
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimitingTests {

        @Test
        @DisplayName("Should allow requests within rate limit")
        void shouldAllowRequestsWithinRateLimit() {
            when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));

            stubFor(get(urlEqualTo("/api/users"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("[]")));

            webTestClient.get()
                    .uri("/api/users")
                    .header("X-Forwarded-For", "192.168.1.1")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should reject requests exceeding rate limit")
        void shouldRejectRequestsExceedingRateLimit() {
            when(valueOperations.increment(anyString())).thenReturn(Mono.just(101L));

            webTestClient.get()
                    .uri("/api/users")
                    .header("X-Forwarded-For", "10.0.0.1")
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Nested
    @DisplayName("Gateway Headers")
    class GatewayHeaderTests {

        @Test
        @DisplayName("Should add correlation ID header to downstream requests")
        void shouldAddCorrelationIdHeader() {
            stubFor(get(urlEqualTo("/api/users"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("[]")));

            webTestClient.get()
                    .uri("/api/users")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().exists("X-Correlation-Id");
        }

        @Test
        @DisplayName("Should preserve existing correlation ID")
        void shouldPreserveExistingCorrelationId() {
            String correlationId = "test-correlation-123";


            stubFor(get(urlEqualTo("/api/users"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("[]")));

            webTestClient.get()
                    .uri("/api/users")
                    .header("X-Correlation-Id", correlationId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("X-Correlation-Id", correlationId);
        }
    }

    @Test
    @DisplayName("Should return 503 when downstream service is unavailable")
    void shouldReturn503WhenServiceUnavailable() {
        stubFor(get(urlEqualTo("/api/users"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withFixedDelay(5000)));

        webTestClient.get()
                .uri("/api/users")
                .exchange()
                .expectStatus().is5xxServerError();
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
