package com.nickfallico.financialriskmanagement.ml;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Impossible travel fraud rule.
 * Detects when user appears to travel impossibly fast between locations.
 * Example: Transaction in NYC, then Paris 2 hours later (requires supersonic flight).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ImpossibleTravelRule implements FraudRule {
    
    private final TransactionRepository transactionRepository;
    
    // Maximum realistic travel speed in km/h
    // Commercial jets: ~900 km/h
    // Adding buffer for time zones, processing delays, etc.
    private static final double MAX_REALISTIC_SPEED_KMH = 1000.0;
    
    // Minimum distance to check (skip local movements)
    private static final double MIN_DISTANCE_KM = 50.0;
    
    @Override
    public Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext context) {
        var currentTransaction = context.transaction();
        String userId = currentTransaction.getUserId();
        
        // Skip if current transaction has no location data
        if (!currentTransaction.hasGeographicData()) {
            log.debug("ImpossibleTravelRule skipped for tx {} - missing geographic data (lat={}, lon={})",
                currentTransaction.getId(),
                currentTransaction.getLatitude(),
                currentTransaction.getLongitude());
            return Mono.just(Optional.empty());
        }
        
        log.debug("Evaluating ImpossibleTravelRule for user {} tx {} at ({}, {}) createdAt={}",
            userId,
            currentTransaction.getId(),
            currentTransaction.getLatitude(),
            currentTransaction.getLongitude(),
            currentTransaction.getCreatedAt());

        Mono<Transactions> previousTransactionMono;

        if (currentTransaction.getCreatedAt() == null) {
            log.debug("Current transaction {} missing createdAt; falling back to latest previous transaction lookup",
                currentTransaction.getId());
            previousTransactionMono = transactionRepository
                .findLatestOtherTransactionWithLocation(userId, currentTransaction.getId());
        } else {
            previousTransactionMono = transactionRepository
                .findPreviousTransactionWithLocation(
                    userId,
                    currentTransaction.getId(),
                    currentTransaction.getCreatedAt()
                );
        }

        // Find previous transaction with location data (reactive)
        return previousTransactionMono
            .doOnSuccess(previous -> {
                if (previous == null) {
                    log.debug("No previous transaction with location found for user {}", userId);
                } else {
                    log.debug("Found previous transaction {} at ({}, {}) createdAt={}",
                        previous.getId(),
                        previous.getLatitude(),
                        previous.getLongitude(),
                        previous.getCreatedAt());
                }
            })
            .flatMap(previousTransaction -> {
                // Skip if no previous transaction with location
                if (previousTransaction == null) {
                    return Mono.just(Optional.<FraudViolation>empty());
                }

                // Skip if it's the same transaction (comparing to itself)
                if (previousTransaction.getId().equals(currentTransaction.getId())) {
                    return Mono.just(Optional.<FraudViolation>empty());
                }
                
                // Calculate distance between locations
                double distanceKm = currentTransaction.distanceInKmTo(
                    previousTransaction.getLatitude(),
                    previousTransaction.getLongitude()
                );
                
                // Skip if distance is small (local movements are normal)
                if (distanceKm < MIN_DISTANCE_KM) {
                    log.debug("ImpossibleTravelRule skipped (distance {} km below threshold {} km)",
                        distanceKm,
                        MIN_DISTANCE_KM);
                    return Mono.just(Optional.<FraudViolation>empty());
                }
                
                // Calculate time difference in hours (high precision using millis to avoid truncation)
                Duration timeDiff = Duration.between(
                    previousTransaction.getCreatedAt(),
                    currentTransaction.getCreatedAt()
                );
                
                double hoursElapsed = timeDiff.toMillis() / 3_600_000.0;
                
                // Avoid division by zero
                if (hoursElapsed <= 0) {
                    log.debug("ImpossibleTravelRule skipped due to non-positive time difference ({} hours) between tx {} and {}",
                        hoursElapsed,
                        currentTransaction.getId(),
                        previousTransaction.getId());
                    return Mono.just(Optional.<FraudViolation>empty());
                }
                
                // Calculate required travel speed
                double requiredSpeedKmh = distanceKm / hoursElapsed;
                
                log.debug("ImpossibleTravelRule speed calculation: distance={} km, hoursElapsed={}, requiredSpeed={} km/h",
                    distanceKm,
                    hoursElapsed,
                    requiredSpeedKmh);
                
                // Check if travel speed is impossible
                if (requiredSpeedKmh > MAX_REALISTIC_SPEED_KMH) {
                    log.warn("Impossible travel detected for user {}: {} km in {} hours (speed: {} km/h)",
                        userId, distanceKm, hoursElapsed, requiredSpeedKmh);
                    
                    log.warn("  Previous location: ({}, {}) at {}",
                        previousTransaction.getLatitude(),
                        previousTransaction.getLongitude(),
                        previousTransaction.getCreatedAt());
                    
                    log.warn("  Current location: ({}, {}) at {}",
                        currentTransaction.getLatitude(),
                        currentTransaction.getLongitude(),
                        currentTransaction.getCreatedAt());
                    
                    // Calculate risk score based on how impossible it is
                    // More impossible = higher risk
                    double impossibilityFactor = requiredSpeedKmh / MAX_REALISTIC_SPEED_KMH;
                    double riskScore = Math.min(0.5 + (impossibilityFactor * 0.3), 0.95);

                    return Mono.just(Optional.of(new FraudViolation(
                        "IMPOSSIBLE_TRAVEL",
                        String.format("Impossible travel: %.0f km in %.1f hours (%.0f km/h required, max realistic: %.0f km/h)",
                            distanceKm, hoursElapsed, requiredSpeedKmh, MAX_REALISTIC_SPEED_KMH),
                        riskScore
                    )));
                }

                // Travel speed is realistic
                return Mono.just(Optional.<FraudViolation>empty());
            })
            .defaultIfEmpty(Optional.empty());
    }
}
