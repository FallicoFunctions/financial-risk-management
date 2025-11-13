package com.nickfallico.financialriskmanagement.ml;

import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Velocity-based fraud rule.
 * Detects transaction bursts (too many transactions in short time windows).
 * Common fraud pattern: automated attacks, card testing, account takeover.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VelocityRule implements FraudRule {
    
    private final TransactionRepository transactionRepository;
    
    // Thresholds: transactions allowed in each time window
    private static final int MAX_TRANSACTIONS_IN_5_MINUTES = 3;
    private static final int MAX_TRANSACTIONS_IN_15_MINUTES = 8;
    private static final int MAX_TRANSACTIONS_IN_1_HOUR = 20;
    
    @Override
    public Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext context) {
        String userId = context.transaction().getUserId();
        Instant now = context.transaction().getCreatedAt();

        // Check 5-minute window (most critical) - reactive version
        return transactionRepository
            .countRecentTransactions(userId, now.minus(Duration.ofMinutes(5)))
            .flatMap(count5min -> {
                if (count5min != null && count5min > MAX_TRANSACTIONS_IN_5_MINUTES) {
                    log.warn("Velocity violation: {} transactions in 5 minutes for user {}",
                        count5min, userId);

                    return Mono.just(Optional.of(new FraudViolation(
                        "VELOCITY_5MIN",
                        String.format("Excessive velocity: %d transactions in 5 minutes (max: %d)",
                            count5min, MAX_TRANSACTIONS_IN_5_MINUTES),
                        0.9 // Very high risk - likely automated attack
                    )));
                }

                // Check 15-minute window
                return transactionRepository
                    .countRecentTransactions(userId, now.minus(Duration.ofMinutes(15)))
                    .flatMap(count15min -> {
                        if (count15min != null && count15min > MAX_TRANSACTIONS_IN_15_MINUTES) {
                            log.warn("Velocity violation: {} transactions in 15 minutes for user {}",
                                count15min, userId);

                            return Mono.just(Optional.of(new FraudViolation(
                                "VELOCITY_15MIN",
                                String.format("High velocity: %d transactions in 15 minutes (max: %d)",
                                    count15min, MAX_TRANSACTIONS_IN_15_MINUTES),
                                0.75 // High risk
                            )));
                        }

                        // Check 1-hour window
                        return transactionRepository
                            .countRecentTransactions(userId, now.minus(Duration.ofHours(1)))
                            .map(count1hour -> {
                                if (count1hour != null && count1hour > MAX_TRANSACTIONS_IN_1_HOUR) {
                                    log.info("Velocity violation: {} transactions in 1 hour for user {}",
                                        count1hour, userId);

                                    return Optional.of(new FraudViolation(
                                        "VELOCITY_1HOUR",
                                        String.format("Elevated velocity: %d transactions in 1 hour (max: %d)",
                                            count1hour, MAX_TRANSACTIONS_IN_1_HOUR),
                                        0.6 // Moderate risk
                                    ));
                                }

                                // All velocity checks passed
                                return Optional.<FraudViolation>empty();
                            });
                    });
            })
            .defaultIfEmpty(Optional.empty());
    }
}