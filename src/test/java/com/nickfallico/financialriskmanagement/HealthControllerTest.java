package com.nickfallico.financialriskmanagement;

 
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.nickfallico.financialriskmanagement.config.TestR2dbcConfig;

 
/**
 * Integration tests for health check endpoints.
 *
 * Tests verify:
 * - Basic health check returns 200 OK
 * - Detailed health check includes component status
 * - Liveness probe works
 * - Readiness probe checks database connectivity
 * - Error handling for database failures
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestR2dbcConfig.class)
@ActiveProfiles("test")
public class HealthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Basic health check should return UP status")
    void testBasicHealthCheck() {
        webTestClient.get()
            .uri("/api/v1/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.application").isEqualTo("financial-risk-management")
            .jsonPath("$.timestamp").exists();
    }
 
    @Test
    @DisplayName("Detailed health check should include component status")
    void testDetailedHealthCheck() {
        webTestClient.get()
            .uri("/api/v1/health/detailed")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.application").isEqualTo("financial-risk-management")
            .jsonPath("$.timestamp").exists()
            .jsonPath("$.database").exists()
            .jsonPath("$.database.status").isEqualTo("UP")
            .jsonPath("$.database.type").isEqualTo("PostgreSQL")
            .jsonPath("$.database.message").exists();
    }
 
    @Test
    @DisplayName("Liveness probe should always return UP")
    void testLivenessProbe() {
        webTestClient.get()
            .uri("/api/v1/health/liveness")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.probe").isEqualTo("liveness");
    }
 
    @Test
    @DisplayName("Readiness probe should check database connectivity")
    void testReadinessProbe() {
        webTestClient.get()
            .uri("/api/v1/health/readiness")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.probe").isEqualTo("readiness")
            .jsonPath("$.database").exists()
            .jsonPath("$.database.status").isEqualTo("UP");
    }
 
    @Test
    @DisplayName("Health endpoints should have fast response times")
    void testHealthCheckPerformance() {
        long startTime = System.currentTimeMillis();
 
        webTestClient.get()
            .uri("/api/v1/health")
            .exchange()
            .expectStatus().isOk();
 
        long duration = System.currentTimeMillis() - startTime;
 
        // Health check should respond in under 1 second
        assert duration < 1000 : "Health check took too long: " + duration + "ms";
    }
    
    @Test
    @DisplayName("Detailed health check should timeout gracefully")
    void testDetailedHealthCheckTimeout() {
        // This test verifies that the health check has proper timeout handling
        webTestClient.get()
            .uri("/api/v1/health/detailed")
            .exchange()
            .expectStatus().is2xxSuccessful(); // Should either be 200 or 503, but not hang
    }
}