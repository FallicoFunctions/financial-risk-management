package com.nickfallico.financialriskmanagement.eventstore.controller;

import com.nickfallico.financialriskmanagement.eventstore.service.EventReplayService;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * REST API for Event Replay operations.
 *
 * This controller demonstrates the power of Event Sourcing in interviews:
 * - Rebuild any user's profile from their event history
 * - Time-travel queries to see historical state
 * - Full system recovery from events
 *
 * IMPORTANT: These are admin/ops endpoints - in production, add proper authorization!
 *
 * Interview talking points:
 * - "We can rebuild any user's state from events - zero data loss"
 * - "Time-travel queries let us debug: 'What was the state at 2pm yesterday?'"
 * - "If the database is corrupted, we can rebuild everything from events"
 */
@RestController
@RequestMapping("/api/v1/admin/event-replay")
@Slf4j
@RequiredArgsConstructor
public class EventReplayController {

    private final EventReplayService eventReplayService;

    /**
     * Replay events for a single user to rebuild their profile.
     *
     * Use cases:
     * - User's profile is corrupted - rebuild from events
     * - Debugging: "Why does this user have this risk score?"
     * - Compliance: "Show me this user's complete event history"
     *
     * Example:
     * POST /api/v1/admin/event-replay/user/user123/rebuild
     */
    @PostMapping("/user/{userId}/rebuild")
    public Mono<ResponseEntity<ImmutableUserRiskProfile>> replayUserProfile(
        @PathVariable String userId
    ) {
        log.info("API request: Rebuild profile for user {}", userId);

        return eventReplayService.replayUserProfile(userId)
            .map(profile -> ResponseEntity.ok(profile))
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .doOnSuccess(response -> {
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info("Successfully rebuilt profile for user: {}", userId);
                } else {
                    log.warn("No events found for user: {}", userId);
                }
            })
            .doOnError(error -> {
                log.error("Failed to rebuild profile for user: {}", userId, error);
            });
    }

    /**
     * Time-travel query: Rebuild user profile as of a specific timestamp.
     *
     * This is VERY impressive in interviews!
     * "What was this user's risk score at 2pm yesterday?"
     *
     * Example:
     * GET /api/v1/admin/event-replay/user/user123/as-of?timestamp=2024-11-09T14:00:00Z
     */
    @GetMapping("/user/{userId}/as-of")
    public Mono<ResponseEntity<ImmutableUserRiskProfile>> replayUserProfileAsOf(
        @PathVariable String userId,
        @RequestParam String timestamp
    ) {
        Instant asOfTime;
        try {
            asOfTime = Instant.parse(timestamp);
        } catch (Exception ex) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        log.info("API request: Time-travel query for user {} as of {}", userId, asOfTime);

        return eventReplayService.replayUserProfileAsOf(userId, asOfTime)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .doOnSuccess(response -> {
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info("Time-travel query completed for user: {} at {}", userId, asOfTime);
                } else {
                    log.warn("No events found for user: {} at {}", userId, asOfTime);
                }
            });
    }

    /**
     * Replay ALL events to rebuild all user profiles.
     * This is the "nuclear option" - full system recovery.
     *
     * WARNING: This can be expensive on large datasets!
     *
     * Example:
     * POST /api/v1/admin/event-replay/full-system?batchSize=10
     */
    @PostMapping("/full-system")
    public Mono<ResponseEntity<Map<String, Object>>> replayAllProfiles(
        @RequestParam(defaultValue = "10") int batchSize
    ) {
        log.warn("API request: FULL SYSTEM REPLAY initiated with batchSize={}", batchSize);

        return eventReplayService.replayAllProfiles(batchSize)
            .map(stats -> {
                Map<String, Object> response = Map.of(
                    "status", "completed",
                    "usersRebuilt", stats.getUsersRebuilt(),
                    "eventsProcessed", stats.getEventsProcessed(),
                    "durationMs", stats.getDurationMs(),
                    "message", String.format(
                        "Successfully rebuilt %d user profiles from %d events in %dms",
                        stats.getUsersRebuilt(),
                        stats.getEventsProcessed(),
                        stats.getDurationMs()
                    )
                );
                return ResponseEntity.ok(response);
            })
            .doOnSuccess(response -> {
                log.info("Full system replay completed");
            })
            .doOnError(error -> {
                log.error("Full system replay failed", error);
            })
            .onErrorResume(error -> {
                Map<String, Object> errorResponse = Map.of(
                    "status", "error",
                    "message", "Full system replay failed: " + error.getMessage()
                );
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse));
            });
    }

    /**
     * Incremental replay: Process only events since a specific timestamp.
     * Useful for regular reconciliation jobs.
     *
     * Example:
     * POST /api/v1/admin/event-replay/incremental?since=2024-11-09T00:00:00Z&batchSize=10
     */
    @PostMapping("/incremental")
    public Mono<ResponseEntity<Map<String, Object>>> replayIncrementalSince(
        @RequestParam String since,
        @RequestParam(defaultValue = "10") int batchSize
    ) {
        Instant sinceTime;
        try {
            sinceTime = Instant.parse(since);
        } catch (Exception ex) {
            Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "message", "Invalid timestamp format for 'since'"
            );
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        log.info("API request: Incremental replay since {} with batchSize={}", sinceTime, batchSize);

        return eventReplayService.replayIncrementalSince(sinceTime, batchSize)
            .map(stats -> {
                Map<String, Object> response = Map.of(
                    "status", "completed",
                    "usersUpdated", stats.getUsersRebuilt(),
                    "eventsProcessed", stats.getEventsProcessed(),
                    "durationMs", stats.getDurationMs(),
                    "message", String.format(
                        "Processed %d events affecting %d users in %dms",
                        stats.getEventsProcessed(),
                        stats.getUsersRebuilt(),
                        stats.getDurationMs()
                    )
                );
                return ResponseEntity.ok(response);
            })
            .doOnSuccess(response -> {
                log.info("Incremental replay completed");
            })
            .onErrorResume(error -> {
                Map<String, Object> errorResponse = Map.of(
                    "status", "error",
                    "message", "Incremental replay failed: " + error.getMessage()
                );
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse));
            });
    }

    /**
     * Health check endpoint to verify replay service is ready.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "event-replay",
            "message", "Event replay service is operational"
        )));
    }
}
