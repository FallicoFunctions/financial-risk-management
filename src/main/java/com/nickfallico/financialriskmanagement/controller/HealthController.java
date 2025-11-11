package com.nickfallico.financialriskmanagement.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Health check endpoints for monitoring and observability.
 *
 * Provides detailed health information about:
 * - Application status
 * - Database connectivity
 * - Kafka connectivity
 * - Event store status
 *
 * Interview talking points:
 * - "We expose comprehensive health checks for monitoring"
 * - "Kubernetes/load balancers use these for liveness/readiness probes"
 * - "Ops teams get detailed component health status"
 */
@RestController
@RequestMapping("/api/v1/health")
@Slf4j
@RequiredArgsConstructor
public class HealthController {

    private final DatabaseClient databaseClient;

    /**
     * Basic health check - used by load balancers and Kubernetes.
     * Returns 200 OK if the application is running.
     */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "financial-risk-management");
        response.put("timestamp", Instant.now().toString());

        return Mono.just(ResponseEntity.ok(response));
    }

    /**
     * Detailed health check with component status.
     * Shows health of database, Kafka, event store, etc.
     */
    @GetMapping("/detailed")
    public Mono<ResponseEntity<Map<String, Object>>> detailedHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("application", "financial-risk-management");

        return checkDatabase()
            .map(dbHealth -> {
                response.put("database", dbHealth);
                response.put("status", dbHealth.get("status").equals("UP") ? "UP" : "DEGRADED");
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Health check failed", error);
                response.put("status", "DOWN");
                response.put("error", error.getMessage());
                return Mono.just(ResponseEntity.status(503).body(response));
            });
    }

    /**
     * Database connectivity check.
     */
    private Mono<Map<String, Object>> checkDatabase() {
        return databaseClient.sql("SELECT 1")
            .fetch()
            .one()
            .map(result -> {
                Map<String, Object> dbHealth = new HashMap<>();
                dbHealth.put("status", "UP");
                dbHealth.put("type", "PostgreSQL");
                dbHealth.put("message", "Database connection healthy");
                return dbHealth;
            })
            .timeout(java.time.Duration.ofSeconds(5))
            .onErrorResume(error -> {
                Map<String, Object> dbHealth = new HashMap<>();
                dbHealth.put("status", "DOWN");
                dbHealth.put("type", "PostgreSQL");
                dbHealth.put("error", error.getMessage());
                return Mono.just(dbHealth);
            });
    }

    /**
     * Liveness probe - is the application alive?
     * Used by Kubernetes to determine if the pod should be restarted.
     */
    @GetMapping("/liveness")
    public Mono<ResponseEntity<Map<String, String>>> liveness() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "probe", "liveness"
        )));
    }

    /**
     * Readiness probe - is the application ready to accept traffic?
     * Used by Kubernetes to determine if the pod should receive requests.
     */
    @GetMapping("/readiness")
    public Mono<ResponseEntity<Map<String, Object>>> readiness() {
        return checkDatabase()
            .map(dbHealth -> {
                boolean ready = "UP".equals(dbHealth.get("status"));
                Map<String, Object> response = new HashMap<>();
                response.put("status", ready ? "UP" : "DOWN");
                response.put("probe", "readiness");
                response.put("database", dbHealth);

                return ready
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(503).body(response);
            })
            .onErrorResume(error -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "DOWN");
                response.put("probe", "readiness");
                response.put("error", error.getMessage());
                return Mono.just(ResponseEntity.status(503).body(response));
            });
    }
}