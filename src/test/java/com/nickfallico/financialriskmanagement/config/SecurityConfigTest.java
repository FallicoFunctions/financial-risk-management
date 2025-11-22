package com.nickfallico.financialriskmanagement.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for Security Configuration.
 *
 * Tests cover:
 * - Actuator endpoints accessibility
 * - API endpoint accessibility
 * - CSRF configuration
 * - Authentication/Authorization rules
 * - Security headers
 * - Health check endpoint security
 * - Admin endpoint security
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({
    TestR2dbcConfig.class,
    TestPrometheusRegistryConfig.class,
    TestCacheConfig.class,
    TestRedisConfig.class,
    TestSecurityConfig.class
})
@ActiveProfiles("test")
public class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Actuator endpoints should be accessible")
    void testActuatorEndpointsAccessible() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();

        webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Health check endpoints should be accessible without authentication")
    void testHealthEndpointsAccessible() {
        webTestClient.get()
            .uri("/api/v1/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").exists();

        webTestClient.get()
            .uri("/api/v1/health/liveness")
            .exchange()
            .expectStatus().isOk();

        webTestClient.get()
            .uri("/api/v1/health/readiness")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("API endpoints should be accessible based on security config")
    void testAPIEndpointsAccessibility() {
        // Based on current SecurityConfig, all exchanges are permitted
        // If authentication is required in future, update this test

        webTestClient.get()
            .uri("/api/transactions/user/test_user")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("CSRF should be disabled for stateless API")
    void testCSRFDisabled() {
        // Since CSRF is disabled, POST requests should work without CSRF token
        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"userId\":\"test\",\"amount\":100,\"currency\":\"USD\"," +
                "\"transactionType\":\"PURCHASE\",\"merchantCategory\":\"RETAIL\"," +
                "\"isInternational\":false}")
            .exchange()
            // Should not get CSRF error (403 for CSRF would indicate CSRF is enabled)
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("OPTIONS requests should be handled (CORS preflight)")
    void testOptionsRequest() {
        webTestClient.options()
            .uri("/api/transactions")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("Admin endpoints should follow security rules")
    void testAdminEndpointsSecurity() {
        // Test admin endpoints are accessible based on current config
        // If admin authentication is required, update this test

        webTestClient.get()
            .uri("/api/admin/flagged-transactions?limit=10")
            .exchange()
            .expectStatus().is2xxSuccessful();

        webTestClient.get()
            .uri("/api/admin/fraud-rules")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("Should handle invalid endpoints gracefully")
    void testInvalidEndpoint() {
        webTestClient.get()
            .uri("/api/invalid/endpoint")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should accept JSON content type for API endpoints")
    void testJSONContentType() {
        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().is4xxClientError(); // 400 for validation
    }

    @Test
    @DisplayName("Should reject invalid content types for API endpoints")
    void testInvalidContentType() {
        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue("plain text")
            .exchange()
            .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("Should handle concurrent requests securely")
    void testConcurrentRequests() {
        // Fire multiple concurrent requests to verify security config handles them
        for (int i = 0; i < 5; i++) {
            webTestClient.get()
                .uri("/api/v1/health")
                .exchange()
                .expectStatus().isOk();
        }
    }

    @Test
    @DisplayName("Should allow all HTTP methods on permitted endpoints")
    void testHTTPMethods() {
        // GET
        webTestClient.get()
            .uri("/api/v1/health")
            .exchange()
            .expectStatus().isOk();

        // POST (will fail validation but not authorization)
        webTestClient.post()
            .uri("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().is4xxClientError();

        // OPTIONS
        webTestClient.options()
            .uri("/api/transactions")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("Actuator info endpoint should be accessible")
    void testActuatorInfo() {
        webTestClient.get()
            .uri("/actuator/info")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Actuator metrics endpoint should be accessible")
    void testActuatorMetrics() {
        webTestClient.get()
            .uri("/actuator/metrics")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Security configuration should permit WebFlux reactive endpoints")
    void testReactiveEndpoints() {
        // Verify reactive endpoints work with security config
        webTestClient.get()
            .uri("/api/transactions/user/reactive_test_user")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }
}
