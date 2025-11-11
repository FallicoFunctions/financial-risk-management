package com.nickfallico.financialriskmanagement.eventstore;

import com.nickfallico.financialriskmanagement.config.TestR2dbcConfig;
import com.nickfallico.financialriskmanagement.eventstore.model.EventType;
import com.nickfallico.financialriskmanagement.eventstore.service.EventStoreService;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.repository.ImmutableUserRiskProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Event Replay functionality.
 *
 * Tests verify:
 * - Single user profile rebuild from events
 * - Time-travel queries (state at specific timestamp)
 * - Full system replay
 * - Incremental replay
 * - Error handling
 * - Performance metrics
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestR2dbcConfig.class)
@ActiveProfiles("test")
class EventReplayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private EventStoreService eventStoreService;

    @Autowired
    private ImmutableUserRiskProfileRepository profileRepository;

    private static final String TEST_USER_ID = "replay-test-user-123";
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(10);

    @BeforeEach
    void setup() {
        // Clean up test data
        profileRepository.deleteById(TEST_USER_ID).block();
    }

    @Test
    @DisplayName("Should rebuild user profile from events")
    void testReplayUserProfile() {
        // Create some test events for the user
        createTestEventsForUser(TEST_USER_ID);

        // Replay the user's profile
        webTestClient.post()
            .uri("/api/v1/admin/event-replay/user/" + TEST_USER_ID + "/rebuild")
            .exchange()
            .expectStatus().isOk()
            .expectBody(ImmutableUserRiskProfile.class)
            .value(profile -> {
                assertThat(profile).isNotNull();
                assertThat(profile.getUserId()).isEqualTo(TEST_USER_ID);
                assertThat(profile.getTotalTransactions()).isGreaterThan(0);
            });

        // Verify the profile was saved
        ImmutableUserRiskProfile savedProfile = profileRepository.findById(TEST_USER_ID)
            .block(TEST_TIMEOUT);

        assertThat(savedProfile).isNotNull();
        assertThat(savedProfile.getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return 404 for user with no events")
    void testReplayUserProfileNoEvents() {
        String nonExistentUser = "user-does-not-exist-999";

        webTestClient.post()
            .uri("/api/v1/admin/event-replay/user/" + nonExistentUser + "/rebuild")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should perform time-travel query to get historical state")
    void testTimeTravelQuery() {
        // Create events with specific timestamps
        Instant futureTime = Instant.now().plusSeconds(3600); // 1 hour from now

        createTestEventsForUser(TEST_USER_ID);

        // Query state as of future time (should include all current events)
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/admin/event-replay/user/" + TEST_USER_ID + "/as-of")
                .queryParam("timestamp", futureTime.toString())
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(ImmutableUserRiskProfile.class)
            .value(profile -> {
                assertThat(profile).isNotNull();
                assertThat(profile.getUserId()).isEqualTo(TEST_USER_ID);
            });
    }

    @Test
    @DisplayName("Should return 404 for time-travel query before any events")
    void testTimeTravelQueryBeforeEvents() {
        createTestEventsForUser(TEST_USER_ID);

        // Query state from way in the past (before any events)
        Instant wayInPast = Instant.parse("2020-01-01T00:00:00Z");

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/admin/event-replay/user/" + TEST_USER_ID + "/as-of")
                .queryParam("timestamp", wayInPast.toString())
                .build())
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should perform full system replay")
    void testFullSystemReplay() {
        // Create events for multiple users
        createTestEventsForUser("user-1");
        createTestEventsForUser("user-2");
        createTestEventsForUser("user-3");

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/admin/event-replay/full-system")
                .queryParam("batchSize", 10)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("completed")
            .jsonPath("$.usersRebuilt").isNumber()
            .jsonPath("$.eventsProcessed").isNumber()
            .jsonPath("$.durationMs").isNumber()
            .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("Should perform incremental replay since timestamp")
    void testIncrementalReplay() {
        Instant startTime = Instant.now();

        // Create some events
        createTestEventsForUser(TEST_USER_ID);

        // Wait a moment to ensure events have different timestamps
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/admin/event-replay/incremental")
                .queryParam("since", startTime.toString())
                .queryParam("batchSize", 10)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("completed")
            .jsonPath("$.usersUpdated").isNumber()
            .jsonPath("$.eventsProcessed").isNumber()
            .jsonPath("$.durationMs").isNumber();
    }

    @Test
    @DisplayName("Event replay health check should be operational")
    void testEventReplayHealthCheck() {
        webTestClient.get()
            .uri("/api/v1/admin/event-replay/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("healthy")
            .jsonPath("$.service").isEqualTo("event-replay")
            .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("Should handle invalid timestamp format gracefully")
    void testInvalidTimestampFormat() {
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/admin/event-replay/user/" + TEST_USER_ID + "/as-of")
                .queryParam("timestamp", "invalid-timestamp")
                .build())
            .exchange()
            .expectStatus().is5xxServerError(); // Should return error for invalid format
    }

    @Test
    @DisplayName("Should complete replay within acceptable time limit")
    void testReplayPerformance() {
        createTestEventsForUser(TEST_USER_ID);

        long startTime = System.currentTimeMillis();

        webTestClient.post()
            .uri("/api/v1/admin/event-replay/user/" + TEST_USER_ID + "/rebuild")
            .exchange()
            .expectStatus().isOk();

        long duration = System.currentTimeMillis() - startTime;

        // Replay should complete within 5 seconds for a single user
        assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("Should track replay metrics in Prometheus")
    void testReplayMetrics() {
        createTestEventsForUser(TEST_USER_ID);

        // Perform a replay
        webTestClient.post()
            .uri("/api/v1/admin/event-replay/user/" + TEST_USER_ID + "/rebuild")
            .exchange()
            .expectStatus().isOk();

        // Check that metrics are being exported
        String metricsResponse = webTestClient.get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertThat(metricsResponse).isNotNull();
        // Check for event replay metrics
        assertThat(metricsResponse).containsAnyOf(
            "event_replay_duration",
            "event_replay_completed_total"
        );
    }

    /**
     * Helper method to create test events for a user.
     * Uses the correct EventStoreService API.
     * Uses the correct EventStoreService API.
     */
     private void createTestEventsForUser(String userId) {
        // Create event data with all required fields
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("transactionId", "txn-" + userId);
        eventData.put("amount", 100.00);
        eventData.put("merchantId", "merchant-123");
        eventData.put("userId", userId);
        eventData.put("isInternational", false);
        eventData.put("riskScore", 0.3);
 
        // Create metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");
        metadata.put("test_run", true);
 
        // Store event using the correct API signature
        eventStoreService.storeEvent(
            EventType.TRANSACTION_CREATED,
            userId,
            "USER",
            eventData,
            metadata
        ).block(TEST_TIMEOUT);
    }
}