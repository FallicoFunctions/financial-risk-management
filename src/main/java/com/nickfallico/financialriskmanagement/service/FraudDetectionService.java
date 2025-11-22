package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.ml.FraudRule;
import com.nickfallico.financialriskmanagement.ml.FraudRuleEngine;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;
import com.nickfallico.financialriskmanagement.model.Transactions;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Fraud detection service.
 * Uses functional rule engine; no imperative if-chains.
 * All business logic moved to pure functions in FraudRuleEngine.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudRuleEngine fraudRuleEngine;
    private final MeterRegistry meterRegistry;
    private final MetricsService metricsService;
    
    /**
     * Assess fraud probability for transaction (reactive).
     * Immutable design: profile and frequency are read-only.
     */
    public Mono<FraudAssessment> assessFraud(
        Transactions transaction,
        ImmutableUserRiskProfile profile,
        MerchantCategoryFrequency merchantFrequency) {

        Timer.Sample sample = Timer.start(meterRegistry);

        // Build immutable evaluation context
        var context = new FraudRule.FraudEvaluationContext(
            transaction,
            profile,
            merchantFrequency
        );

        // Evaluate all rules (reactive functional composition)
        return fraudRuleEngine.evaluateTransaction(context)
            .map(violations -> {
                // Calculate fraud probability
                double fraudProbability = fraudRuleEngine.calculateFraudProbability(violations);

                // Determine action
                FraudRuleEngine.FraudAction action =
                    fraudRuleEngine.determineAction(fraudProbability);

                // Record metrics
                recordMetrics(sample, transaction, violations, fraudProbability, action);

                // Log assessment
                logAssessment(transaction, violations, fraudProbability, action);

                return new FraudAssessment(
                    transaction.getId(),
                    fraudProbability,
                    violations,
                    action,
                    System.currentTimeMillis()
                );
            });
    }
    
    /**
     * Immutable fraud assessment result.
     */
    public record FraudAssessment(
        java.util.UUID transactionId,
        double fraudProbability,
        List<FraudRule.FraudViolation> violations,
        FraudRuleEngine.FraudAction action,
        long assessmentTime
    ) {
        public boolean shouldBlock() {
            return action.isBlocking();
        }
        
        public boolean needsReview() {
            return action.needsReview();
        }
        
        public String getViolationSummary() {
            return violations.stream()
                .map(v -> v.ruleId() + "(" + v.riskScore() + ")")
                .collect(Collectors.joining(", "));
        }
    }
    
    private void recordMetrics(
        Timer.Sample sample,
        Transactions tx,
        List<FraudRule.FraudViolation> violations,
        double fraudProbability,
        FraudRuleEngine.FraudAction action) {

        // Record fraud detection duration
        Timer timer = sample.stop(meterRegistry.timer(
            "fraud_assessment_time",
            "merchant_category", String.valueOf(tx.getMerchantCategory()),
            "action", action.name()
        ));
        metricsService.recordFraudDetectionDuration((long) timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));

        // Record violation counts
        meterRegistry.counter(
            "fraud_violations_total",
            "count", String.valueOf(violations.size()),
            "action", action.name()
        ).increment();

        // Record fraud probability
        meterRegistry.gauge(
            "fraud_probability",
            fraudProbability
        );

        // Record business metrics using MetricsService
        if (!violations.isEmpty()) {
            metricsService.recordFraudDetected(action.name());

            // Record each rule violation
            violations.forEach(v -> metricsService.recordRuleViolation(v.ruleId()));
        }

        // Record risk score
        metricsService.recordRiskScore(fraudProbability);

        // Record action-specific metrics
        switch (action) {
            case BLOCK -> metricsService.recordTransactionBlocked();
            case REVIEW -> {
                metricsService.recordHighRiskTransaction();
                metricsService.recordFraudDetected();
            }
            case APPROVE -> metricsService.recordTransactionApproved();
        }
    }
    
    private void logAssessment(
        Transactions tx,
        List<FraudRule.FraudViolation> violations,
        double fraudProbability,
        FraudRuleEngine.FraudAction action) {
        
        String violationStr = violations.isEmpty() 
            ? "none" 
            : violations.stream()
                .map(FraudRule.FraudViolation::ruleId)
                .collect(Collectors.joining(", "));
        
        String logMessage = String.format(
            "Fraud Assessment - TX: %s | Probability: %.2f | Action: %s | Violations: %s",
            tx.getId(),
            fraudProbability,
            action.name(),
            violationStr
        );
        
        if (action.isBlocking()) {
            log.warn(logMessage);
        } else if (action.needsReview()) {
            log.info(logMessage);
        } else {
            log.debug(logMessage);
        }
    }
}