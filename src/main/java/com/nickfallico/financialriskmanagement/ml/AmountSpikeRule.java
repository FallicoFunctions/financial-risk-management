package com.nickfallico.financialriskmanagement.ml;

import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Amount spike fraud rule.
 * Detects when transaction amount is significantly higher than user's normal behavior.
 * Uses statistical analysis: compares to average and standard deviation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AmountSpikeRule implements FraudRule {
    
    private final TransactionRepository transactionRepository;
    
    // Look back 30 days for baseline calculation
    private static final Duration BASELINE_PERIOD = Duration.ofDays(30);
    
    // Multipliers for spike detection
    private static final double EXTREME_SPIKE_MULTIPLIER = 5.0; // 5x average
    private static final double HIGH_SPIKE_MULTIPLIER = 3.0;    // 3x average
    
    @Override
    public Optional<FraudViolation> evaluate(FraudEvaluationContext context) {
        var transaction = context.transaction();
        var profile = context.profile();
        String userId = transaction.getUserId();
        BigDecimal currentAmount = transaction.getAmount();
        
        // Skip for new users - no baseline to compare against
        if (profile.isNewUser()) {
            return Optional.empty();
        }
        
        // Calculate user's average transaction amount over last 30 days
        Instant baselineStart = transaction.getCreatedAt().minus(BASELINE_PERIOD);
        
        Double avgAmount = transactionRepository
            .calculateAverageAmountSince(userId, baselineStart)
            .block();
        
        // Skip if no historical data
        if (avgAmount == null || avgAmount <= 0) {
            return Optional.empty();
        }
        
        double currentAmountDouble = currentAmount.doubleValue();
        
        // Check for extreme spike (5x average)
        if (currentAmountDouble > avgAmount * EXTREME_SPIKE_MULTIPLIER) {
            log.warn("Extreme amount spike detected for user {}: ${:.2f} vs avg ${:.2f} ({}x multiplier)",
                userId, currentAmountDouble, avgAmount, EXTREME_SPIKE_MULTIPLIER);
            
            double actualMultiplier = currentAmountDouble / avgAmount;
            
            return Optional.of(new FraudViolation(
                "AMOUNT_EXTREME_SPIKE",
                String.format("Extreme amount spike: $%.2f vs 30-day average $%.2f (%.1fx higher)",
                    currentAmountDouble, avgAmount, actualMultiplier),
                0.85 // Very high risk
            ));
        }
        
        // Check for high spike (3x average)
        if (currentAmountDouble > avgAmount * HIGH_SPIKE_MULTIPLIER) {
            log.info("High amount spike detected for user {}: ${:.2f} vs avg ${:.2f} ({}x multiplier)",
                userId, currentAmountDouble, avgAmount, HIGH_SPIKE_MULTIPLIER);
            
            double actualMultiplier = currentAmountDouble / avgAmount;
            
            return Optional.of(new FraudViolation(
                "AMOUNT_HIGH_SPIKE",
                String.format("High amount spike: $%.2f vs 30-day average $%.2f (%.1fx higher)",
                    currentAmountDouble, avgAmount, actualMultiplier),
                0.65 // Moderate-high risk
            ));
        }
        
        // Check standard deviation for more sophisticated detection
        Double stdDev = transactionRepository
            .calculateStdDevAmountSince(userId, baselineStart)
            .block();
        
        if (stdDev != null && stdDev > 0) {
            // Check if amount is more than 3 standard deviations above mean
            // This catches outliers in statistical sense
            double zScore = (currentAmountDouble - avgAmount) / stdDev;
            
            if (zScore > 3.0) {
                log.info("Statistical amount anomaly for user {}: z-score {:.2f} (amount ${:.2f})",
                    userId, zScore, currentAmountDouble);
                
                return Optional.of(new FraudViolation(
                    "AMOUNT_STATISTICAL_ANOMALY",
                    String.format("Statistical anomaly: $%.2f is %.1f standard deviations above mean",
                        currentAmountDouble, zScore),
                    0.55 // Moderate risk
                ));
            }
        }
        
        // Amount is within normal range
        return Optional.empty();
    }
}