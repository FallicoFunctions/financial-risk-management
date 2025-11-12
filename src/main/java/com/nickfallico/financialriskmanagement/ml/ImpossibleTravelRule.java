package com.nickfallico.financialriskmanagement.ml;

import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

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
    public Optional<FraudViolation> evaluate(FraudEvaluationContext context) {
        var currentTransaction = context.transaction();
        String userId = currentTransaction.getUserId();
        
        // Skip if current transaction has no location data
        if (!currentTransaction.hasGeographicData()) {
            return Optional.empty();
        }
        
        // Find previous transaction with location data
        Transactions previousTransaction = transactionRepository
            .findMostRecentTransactionWithLocation(userId)
            .block();
        
        // Skip if no previous transaction with location
        if (previousTransaction == null) {
            return Optional.empty();
        }
        
        // Skip if it's the same transaction (comparing to itself)
        if (previousTransaction.getId().equals(currentTransaction.getId())) {
            return Optional.empty();
        }
        
        // Calculate distance between locations
        double distanceKm = currentTransaction.distanceInKmTo(
            previousTransaction.getLatitude(),
            previousTransaction.getLongitude()
        );
        
        // Skip if distance is small (local movements are normal)
        if (distanceKm < MIN_DISTANCE_KM) {
            return Optional.empty();
        }
        
        // Calculate time difference in hours
        Duration timeDiff = Duration.between(
            previousTransaction.getCreatedAt(),
            currentTransaction.getCreatedAt()
        );
        
        double hoursElapsed = timeDiff.toMinutes() / 60.0;
        
        // Avoid division by zero
        if (hoursElapsed <= 0) {
            return Optional.empty();
        }
        
        // Calculate required travel speed
        double requiredSpeedKmh = distanceKm / hoursElapsed;
        
        // Check if travel speed is impossible
        if (requiredSpeedKmh > MAX_REALISTIC_SPEED_KMH) {
            log.warn("Impossible travel detected for user {}: {:.0f} km in {:.1f} hours (speed: {:.0f} km/h)",
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
            
            return Optional.of(new FraudViolation(
                "IMPOSSIBLE_TRAVEL",
                String.format("Impossible travel: %.0f km in %.1f hours (%.0f km/h required, max realistic: %.0f km/h)",
                    distanceKm, hoursElapsed, requiredSpeedKmh, MAX_REALISTIC_SPEED_KMH),
                riskScore
            ));
        }
        
        // Travel speed is realistic
        return Optional.empty();
    }
}