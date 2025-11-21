package com.nickfallico.financialriskmanagement.monitoring;

import com.nickfallico.financialriskmanagement.config.TestR2dbcConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Prometheus metrics endpoints.
 *
 * Verifies that:
 * - Actuator prometheus endpoint is accessible
 * - Metrics are being exported in Prometheus format
 * - Key application metrics are present
 * - JVM metrics are exposed
 * - HTTP request metrics are tracked
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestR2dbcConfig.class)
@ActiveProfiles("test")
class PrometheusMetricsIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Prometheus actuator endpoint should be accessible")
    void testPrometheusEndpointAccessible() {
        webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("text/plain;version=0.0.4;charset=utf-8");
    }

    @Test
    @DisplayName("Prometheus endpoint should export JVM metrics")
    void testJvmMetricsExported() {
        String metricsResponse = webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(metricsResponse).isNotNull();

        // JVM Memory metrics
        assertThat(metricsResponse).contains("jvm_memory_used_bytes");
        assertThat(metricsResponse).contains("jvm_memory_max_bytes");
        assertThat(metricsResponse).contains("jvm_memory_committed_bytes");

        // JVM GC metrics
        assertThat(metricsResponse).contains("jvm_gc_pause_seconds");

        // JVM Thread metrics
        assertThat(metricsResponse).contains("jvm_threads_live_threads");
        assertThat(metricsResponse).contains("jvm_threads_daemon_threads");
    }

    @Test
    @DisplayName("Prometheus endpoint should export HTTP server metrics")
    void testHttpServerMetricsExported() {
        // First, make a request to generate some HTTP metrics
        webTestClient.get()
            .uri("/api/v1/health")
            .exchange()
            .expectStatus().isOk();

        // Now check if HTTP metrics are present
        String metricsResponse = webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(metricsResponse).isNotNull();
        assertThat(metricsResponse).contains("http_server_requests_seconds");
    }

    @Test
    @DisplayName("Prometheus endpoint should export system metrics")
    void testSystemMetricsExported() {
        String metricsResponse = webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(metricsResponse).isNotNull();

        // System CPU metrics
        assertThat(metricsResponse).contains("system_cpu_usage");
        assertThat(metricsResponse).contains("process_cpu_usage");

        // System memory metrics
        assertThat(metricsResponse).contains("system_memory_usage");
    }

    @Test
    @DisplayName("Prometheus endpoint should export process metrics")
    void testProcessMetricsExported() {
        String metricsResponse = webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(metricsResponse).isNotNull();

        // Process metrics
        assertThat(metricsResponse).contains("process_uptime_seconds");
        assertThat(metricsResponse).contains("process_start_time_seconds");
    }

    @Test
    @DisplayName("Actuator health endpoint should be accessible")
    void testActuatorHealthEndpoint() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("Actuator metrics endpoint should list available metrics")
    void testActuatorMetricsEndpoint() {
        webTestClient.get()
            .uri("/actuator/metrics")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.names").isArray()
            .jsonPath("$.names").isNotEmpty();
    }

    @Test
    @DisplayName("Specific JVM memory metric should be accessible")
    void testSpecificJvmMemoryMetric() {
        webTestClient.get()
            .uri("/actuator/metrics/jvm.memory.used")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.name").isEqualTo("jvm.memory.used")
            .jsonPath("$.measurements").isArray()
            .jsonPath("$.measurements[0].value").isNumber();
    }

    @Test
    @DisplayName("Metrics should have proper help text and type information")
    void testMetricsFormat() {
        String metricsResponse = webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(metricsResponse).isNotNull();

        // Check for HELP and TYPE comments (Prometheus format requirements)
        assertThat(metricsResponse).containsPattern("# HELP \\w+");
        assertThat(metricsResponse).containsPattern("# TYPE \\w+");
    }

    @Test
    @DisplayName("Metrics should include application labels")
    void testMetricsLabels() {
        // Make a request to generate metrics with labels
        webTestClient.get()
            .uri("/api/v1/health")
            .exchange()
            .expectStatus().isOk();

        String metricsResponse = webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(metricsResponse).isNotNull();

        // HTTP metrics should include uri, method, status, and outcome labels
        assertThat(metricsResponse).containsPattern("http_server_requests_seconds.*uri=\".*\"");
        assertThat(metricsResponse).containsPattern("http_server_requests_seconds.*method=\".*\"");
        assertThat(metricsResponse).containsPattern("http_server_requests_seconds.*status=\".*\"");
    }

    @Test
    @DisplayName("DEBUG: Test if ANY actuator endpoint works")
    void debugActuatorHealth() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(result -> {
                System.out.println("=== HEALTH RESPONSE ===");
                System.out.println(result.getResponseBody());
                System.out.println("=======================");
            });
    }

    @Test
    @DisplayName("DEBUG: Show actual error from actuator")
    void debugShowActuatorError() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectBody(String.class)
            .consumeWith(result -> {
                System.out.println("=== STATUS: " + result.getStatus());
                System.out.println("=== RESPONSE BODY ===");
                System.out.println(result.getResponseBody());
                System.out.println("====================");
            });
    }
}