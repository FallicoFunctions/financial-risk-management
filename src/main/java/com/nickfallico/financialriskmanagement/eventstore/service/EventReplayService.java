package com.nickfallico.financialriskmanagement.eventstore.service;

import com.nickfallico.financialriskmanagement.eventstore.model.EventLog;
import com.nickfallico.financialriskmanagement.eventstore.model.EventType;
import com.nickfallico.financialriskmanagement.eventstore.projection.UserProfileProjection;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.repository.ImmutableUserRiskProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event Replay Service - Rebuilds system state from event history.
 *
 * This is the core of Event Sourcing:
 * - Replay events to rebuild user profiles
 * - Time-travel queries (rebuild state as of specific timestamp)
 * - Full system recovery from events
 *
 * Interview talking points:
 * - "We can rebuild any user's profile from their complete event history"
 * - "This enables audit compliance and debugging production issues"
 * - "We can query historical state: 'What was this user's risk score yesterday?'"
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventReplayService {

    private final EventStoreService eventStoreService;
    private final ImmutableUserRiskProfileRepository userRiskProfileRepository;
    private final UserProfileProjection userProfileProjection;
    private final MeterRegistry meterRegistry;

    /**
     * Replay all events for a single user to rebuild their profile.
     * This is useful for:
     * - Debugging: "Why does this user have this risk score?"
     * - Recovery: "The profile is corrupted, let's rebuild from events"
     * - Compliance: "Show me this user's complete history"
     *
     * @param userId The user to replay events for
     * @return The rebuilt user profile
     */
    public Mono<ImmutableUserRiskProfile> replayUserProfile(String userId) {
        Timer.Sample sample = Timer.start(meterRegistry);

        log.info("Starting event replay for user: {}", userId);

        return eventStoreService.getUserEventHistory(userId)
            .collectList()
            .flatMap(events -> {
                log.info("Found {} events for user {}", events.size(), userId);

                // Apply events to build profile
                return userProfileProjection.buildProfileFromEvents(userId, Flux.fromIterable(events))
                    .flatMap(profile -> {
                        // Save rebuilt profile
                        return userRiskProfileRepository.save(profile);
                    });
            })
            .doOnSuccess(profile -> {
                sample.stop(meterRegistry.timer("event_replay_duration",
                    "replay_type", "single_user"));

                meterRegistry.counter("event_replay_completed_total",
                    "replay_type", "single_user")
                    .increment();

                log.info("Successfully replayed {} events for user: {}",
                    profile != null ? "profile rebuilt" : "no profile", userId);
            })
            .doOnError(error -> {
                log.error("Failed to replay events for user: {}", userId, error);

                meterRegistry.counter("event_replay_errors_total",
                    "replay_type", "single_user")
                    .increment();
            });
    }

    /**
     * Time-travel query: Rebuild user profile as of a specific timestamp.
     *
     * This is VERY impressive in interviews:
     * "What was this user's risk score at 2pm yesterday?"
     * "Replay only events up to that timestamp"
     *
     * Use cases:
     * - Debugging: "The user says they weren't flagged at 2pm, let's verify"
     * - Compliance: "Show the exact state of this account on Dec 31st"
     * - Analytics: "How have risk scores evolved over time?"
     */
    public Mono<ImmutableUserRiskProfile> replayUserProfileAsOf(String userId, Instant asOfTime) {
        Timer.Sample sample = Timer.start(meterRegistry);

        log.info("Time-travel replay for user {} as of {}", userId, asOfTime);

        return eventStoreService.getAggregateHistoryAsOf(userId, "USER", asOfTime)
            .collectList()
            .flatMap(events -> {
                log.info("Found {} events for user {} as of {}",
                    events.size(), userId, asOfTime);

                return userProfileProjection.buildProfileFromEvents(userId, Flux.fromIterable(events));
            })
            .doOnSuccess(profile -> {
                sample.stop(meterRegistry.timer("event_replay_duration",
                    "replay_type", "time_travel"));

                log.info("Time-travel replay completed for user: {}", userId);
            });
    }

    /**
     * Replay ALL events in the system to rebuild all user profiles.
     * This is the nuclear option:
     * - Full system recovery
     * - Database migration from old system
     * - Complete rebuild after schema change
     *
     * WARNING: This can be expensive. Use batching to avoid memory issues.
     *
     * Interview talking point:
     * "If our entire user profile table gets corrupted, we can rebuild
     *  it completely from the immutable event log. Zero data loss."
     */
    public Mono<ReplayStats> replayAllProfiles(int batchSize) {
        Timer.Sample sample = Timer.start(meterRegistry);
        ReplayStats stats = new ReplayStats();

        log.info("Starting FULL SYSTEM replay with batch size {}", batchSize);

        return eventStoreService.getEventsByType(EventType.TRANSACTION_CREATED)
            .concatWith(eventStoreService.getEventsByType(EventType.FRAUD_DETECTED))
            .concatWith(eventStoreService.getEventsByType(EventType.FRAUD_CLEARED))
            .groupBy(EventLog::getAggregateId)  // Group by user
            .flatMap(userEvents -> {
                String userId = userEvents.key();

                return userEvents.collectList()
                    .flatMap(events -> {
                        stats.incrementEventsProcessed(events.size());

                        return userProfileProjection.buildProfileFromEvents(
                            userId,
                            Flux.fromIterable(events)
                        )
                        .flatMap(userRiskProfileRepository::save)
                        .doOnSuccess(profile -> {
                            stats.incrementUsersRebuilt();

                            if (stats.getUsersRebuilt() % 100 == 0) {
                                log.info("Replay progress: {} users, {} events",
                                    stats.getUsersRebuilt(),
                                    stats.getEventsProcessed());
                            }
                        });
                    });
            }, batchSize)  // Process N users concurrently
            .then(Mono.fromCallable(() -> {
                stats.setEndTime(Instant.now());
                return stats;
            }))
            .doOnSuccess(finalStats -> {
                sample.stop(meterRegistry.timer("event_replay_duration",
                    "replay_type", "full_system"));

                meterRegistry.counter("event_replay_completed_total",
                    "replay_type", "full_system")
                    .increment();

                log.info("FULL SYSTEM replay completed: {} users, {} events, duration: {}ms",
                    finalStats.getUsersRebuilt(),
                    finalStats.getEventsProcessed(),
                    finalStats.getDurationMs());
            })
            .doOnError(error -> {
                log.error("Full system replay failed", error);

                meterRegistry.counter("event_replay_errors_total",
                    "replay_type", "full_system")
                    .increment();
            });
    }

    /**
     * Replay events incrementally - only process events since last replay.
     * This is useful for:
     * - Regular reconciliation jobs
     * - Catching up after downtime
     * - Incremental updates
     */
    public Mono<ReplayStats> replayIncrementalSince(Instant since, int batchSize) {
        log.info("Starting incremental replay since {}", since);

        ReplayStats stats = new ReplayStats();

        return eventStoreService.getEventsInTimeRange(since, Instant.now())
            .groupBy(EventLog::getAggregateId)
            .flatMap(userEvents -> {
                String userId = userEvents.key();

                return userEvents.collectList()
                    .flatMap(events -> {
                        stats.incrementEventsProcessed(events.size());

                        // Load existing profile and apply new events
                        return userRiskProfileRepository.findById(userId)
                            .switchIfEmpty(Mono.defer(() -> {
                                // No existing profile, build from scratch
                                return userProfileProjection.buildProfileFromEvents(
                                    userId,
                                    Flux.fromIterable(events)
                                );
                            }))
                            .flatMap(existingProfile -> {
                                // Apply new events to existing profile
                                return userProfileProjection.applyEventsToProfile(
                                    existingProfile,
                                    Flux.fromIterable(events)
                                );
                            })
                            .flatMap(userRiskProfileRepository::save)
                            .doOnSuccess(profile -> stats.incrementUsersRebuilt());
                    });
            }, batchSize)
            .then(Mono.fromCallable(() -> {
                stats.setEndTime(Instant.now());
                return stats;
            }))
            .doOnSuccess(finalStats -> {
                log.info("Incremental replay completed: {} users, {} events",
                    finalStats.getUsersRebuilt(),
                    finalStats.getEventsProcessed());
            });
    }

    /**
     * Statistics tracking for replay operations.
     */
    public static class ReplayStats {
        private final AtomicLong eventsProcessed = new AtomicLong(0);
        private final AtomicLong usersRebuilt = new AtomicLong(0);
        private final Instant startTime = Instant.now();
        private Instant endTime;

        public void incrementEventsProcessed(int count) {
            eventsProcessed.addAndGet(count);
        }

        public void incrementUsersRebuilt() {
            usersRebuilt.incrementAndGet();
        }

        public long getEventsProcessed() {
            return eventsProcessed.get();
        }

        public long getUsersRebuilt() {
            return usersRebuilt.get();
        }

        public void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }

        public long getDurationMs() {
            return Duration.between(startTime, endTime != null ? endTime : Instant.now())
                .toMillis();
        }

        @Override
        public String toString() {
            return String.format("ReplayStats{events=%d, users=%d, duration=%dms}",
                eventsProcessed.get(), usersRebuilt.get(), getDurationMs());
        }
    }
}