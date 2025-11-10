package com.nickfallico.financialriskmanagement.eventstore.projection;

import com.nickfallico.financialriskmanagement.eventstore.model.EventLog;
import com.nickfallico.financialriskmanagement.eventstore.model.EventType;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User Profile Projection - Rebuilds user risk profiles from event history.
 *
 * This is the heart of Event Sourcing:
 * - Takes a stream of events
 * - Applies each event to build current state
 * - No database reads needed - state comes purely from events
 *
 * Interview talking points:
 * - "This demonstrates the 'projection' pattern in event sourcing"
 * - "We can rebuild any user's profile from their complete event history"
 * - "State is derived from events, not stored directly"
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserProfileProjection {

    /**
     * Build a complete user profile from a stream of events.
     * This is pure event sourcing - state is computed from events only.
     *
     * Events processed:
     * - TRANSACTION_CREATED: Updates transaction counts, amounts
     * - FRAUD_DETECTED: Increases risk scores
     * - FRAUD_CLEARED: May adjust risk scores
     * - USER_PROFILE_UPDATED: Direct profile updates
     *
     * @param userId The user whose profile we're rebuilding
     * @param events Stream of events for this user
     * @return Rebuilt user profile
     */
    public Mono<ImmutableUserRiskProfile> buildProfileFromEvents(String userId, Flux<EventLog> events) {
        log.debug("Building profile for user {} from event stream", userId);

        // Start with empty profile
        AtomicReference<ImmutableUserRiskProfile> profile =
            new AtomicReference<>(ImmutableUserRiskProfile.createNew(userId));

        AtomicInteger eventCount = new AtomicInteger(0);

        return events
            .sort((e1, e2) -> Long.compare(e1.getSequenceNumber(), e2.getSequenceNumber()))
            .doOnNext(event -> {
                log.trace("Applying event {}: {}", eventCount.incrementAndGet(), event.getEventType());
            })
            .reduce(profile.get(), (currentProfile, event) -> {
                return applyEventToProfile(currentProfile, event);
            })
            .doOnSuccess(finalProfile -> {
                log.debug("Built profile for user {} from {} events",
                    userId, eventCount.get());
            });
    }

    /**
     * Apply new events to an existing profile (incremental update).
     * Used for incremental replays where we already have a profile.
     */
    public Mono<ImmutableUserRiskProfile> applyEventsToProfile(
        ImmutableUserRiskProfile existingProfile,
        Flux<EventLog> newEvents
    ) {
        log.debug("Applying incremental events to profile for user {}",
            existingProfile.getUserId());

        return newEvents
            .sort((e1, e2) -> Long.compare(e1.getSequenceNumber(), e2.getSequenceNumber()))
            .reduce(existingProfile, this::applyEventToProfile);
    }

    /**
     * Apply a single event to a profile.
     * This is where the business logic lives - how each event type affects state.
     */
    private ImmutableUserRiskProfile applyEventToProfile(
        ImmutableUserRiskProfile currentProfile,
        EventLog event
    ) {
        EventType eventType = EventType.fromTopicName(event.getEventType());
        Map<String, Object> eventData = event.getEventData();

        return switch (eventType) {
            case TRANSACTION_CREATED -> applyTransactionCreated(currentProfile, eventData, event.getCreatedAt());
            case FRAUD_DETECTED -> applyFraudDetected(currentProfile, eventData);
            case FRAUD_CLEARED -> applyFraudCleared(currentProfile, eventData);
            case USER_PROFILE_UPDATED -> applyProfileUpdated(currentProfile, eventData);
            default -> {
                log.trace("Event type {} does not affect user profile", eventType);
                yield currentProfile;
            }
        };
    }

    /**
     * Apply TRANSACTION_CREATED event.
     * Updates transaction counts, amounts, and recalculates risk.
     */
    private ImmutableUserRiskProfile applyTransactionCreated(
        ImmutableUserRiskProfile profile,
        Map<String, Object> eventData,
        Instant transactionTime
    ) {
        // Extract transaction details
        double amount = extractDouble(eventData, "amount");
        boolean isInternational = extractBoolean(eventData, "isInternational");
        double transactionRisk = extractDouble(eventData, "riskScore");

        // Update counters
        int newTotalTx = profile.getTotalTransactions() + 1;
        double newTotalValue = profile.getTotalTransactionValue() + amount;
        double newAvgAmount = newTotalValue / newTotalTx;
        int newIntlTx = profile.getInternationalTransactions() + (isInternational ? 1 : 0);

        // Recalculate risk scores
        double newTxRiskScore = calculateTransactionRiskScore(
            newTotalTx,
            newAvgAmount,
            amount,
            transactionRisk
        );

        double newBehavioralScore = calculateBehavioralScore(
            profile.getBehavioralRiskScore(),
            newTotalTx,
            newIntlTx,
            isInternational
        );

        double newOverallScore = (newTxRiskScore + newBehavioralScore) / 2.0;

        // Update first transaction date if this is the first transaction
        Instant firstTxDate = profile.getFirstTransactionDate();
        if (profile.getTotalTransactions() == 0) {
            firstTxDate = transactionTime;
        }

        return profile.withUpdatedMetrics(
            newAvgAmount,
            newTotalTx,
            newTotalValue,
            profile.getHighRiskTransactions(),
            newIntlTx,
            newBehavioralScore,
            newTxRiskScore,
            newOverallScore,
            transactionTime
        ).toBuilder()
            .firstTransactionDate(firstTxDate)
            .build();
    }

    /**
     * Apply FRAUD_DETECTED event.
     * Increases risk scores and high-risk transaction count.
     */
    private ImmutableUserRiskProfile applyFraudDetected(
        ImmutableUserRiskProfile profile,
        Map<String, Object> eventData
    ) {
        int newHighRiskTx = profile.getHighRiskTransactions() + 1;

        // Fraud detection significantly increases behavioral risk
        double fraudPenalty = 0.2;  // +20% risk
        double newBehavioralScore = Math.min(1.0,
            profile.getBehavioralRiskScore() + fraudPenalty);

        double newOverallScore = (profile.getTransactionRiskScore() + newBehavioralScore) / 2.0;

        return profile.toBuilder()
            .highRiskTransactions(newHighRiskTx)
            .behavioralRiskScore(newBehavioralScore)
            .overallRiskScore(newOverallScore)
            .build();
    }

    /**
     * Apply FRAUD_CLEARED event.
     * May slightly reduce risk scores (but not back to original - caution remains).
     */
    private ImmutableUserRiskProfile applyFraudCleared(
        ImmutableUserRiskProfile profile,
        Map<String, Object> eventData
    ) {
        // Clearing fraud slightly reduces risk, but not completely
        double clearanceReduction = 0.1;  // -10% risk
        double newBehavioralScore = Math.max(0.0,
            profile.getBehavioralRiskScore() - clearanceReduction);

        double newOverallScore = (profile.getTransactionRiskScore() + newBehavioralScore) / 2.0;

        return profile.toBuilder()
            .behavioralRiskScore(newBehavioralScore)
            .overallRiskScore(newOverallScore)
            .build();
    }

    /**
     * Apply USER_PROFILE_UPDATED event.
     * Direct profile updates from admin actions or system corrections.
     */
    private ImmutableUserRiskProfile applyProfileUpdated(
        ImmutableUserRiskProfile profile,
        Map<String, Object> eventData
    ) {
        // Extract updated fields if present
        var builder = profile.toBuilder();

        if (eventData.containsKey("behavioralRiskScore")) {
            builder.behavioralRiskScore(extractDouble(eventData, "behavioralRiskScore"));
        }
        if (eventData.containsKey("transactionRiskScore")) {
            builder.transactionRiskScore(extractDouble(eventData, "transactionRiskScore"));
        }
        if (eventData.containsKey("overallRiskScore")) {
            builder.overallRiskScore(extractDouble(eventData, "overallRiskScore"));
        }

        return builder.build();
    }

    /**
     * Calculate transaction risk score based on transaction patterns.
     * Uses weighted average of historical risk and current transaction.
     */
    private double calculateTransactionRiskScore(
        int totalTransactions,
        double avgAmount,
        double currentAmount,
        double currentRisk
    ) {
        // For new users, heavily weight current transaction
        if (totalTransactions <= 2) {
            return currentRisk;
        }

        // Amount deviation risk
        double amountDeviation = Math.abs(currentAmount - avgAmount) / avgAmount;
        double deviationRisk = Math.min(1.0, amountDeviation);

        // Weighted average: 70% current risk, 30% deviation
        return (0.7 * currentRisk) + (0.3 * deviationRisk);
    }

    /**
     * Calculate behavioral risk score based on user patterns.
     */
    private double calculateBehavioralScore(
        double currentScore,
        int totalTransactions,
        int internationalTransactions,
        boolean isCurrentInternational
    ) {
        // International transaction ratio
        double intlRatio = (double) internationalTransactions / totalTransactions;

        // Sudden international activity is risky
        if (isCurrentInternational && intlRatio < 0.1) {
            return Math.min(1.0, currentScore + 0.15);
        }

        // Gradual decay towards neutral if no issues
        return currentScore * 0.98;
    }

    /**
     * Extract double from event data, with fallback.
     */
    private double extractDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return 0.0;
    }

    /**
     * Extract boolean from event data, with fallback.
     */
    private boolean extractBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return false;
    }
}