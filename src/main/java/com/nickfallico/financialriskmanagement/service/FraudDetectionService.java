package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.ml.FraudRule;
import com.nickfallico.financialriskmanagement.ml.FraudRuleEngine;
import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;
import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;
import com.nickfallico.financialriskmanagement.model.Transaction;
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
    
    /**
     * Assess fraud probability for transaction.
     * Immutable design: profile and frequency are read-only.
     */
    public Mono<FraudAssessment> assessFraud(
        Transaction transaction,
        ImmutableUserRiskProfile profile,
        MerchantCategoryFrequency merchantFrequency) {
        
        return Mono.fromCallable(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            // Build immutable evaluation context
            var context = new FraudRule.FraudEvaluationContext(
                transaction,
                profile,
                merchantFrequency
            );
            
            // Evaluate all rules (functional stream composition)
            List<FraudRule.FraudViolation> violations = 
                fraudRuleEngine.evaluateTransaction(context);
            
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
        Transaction tx,
        List<FraudRule.FraudViolation> violations,
        double fraudProbability,
        FraudRuleEngine.FraudAction action) {
        
        sample.stop(meterRegistry.timer(
            "fraud_assessment_time",
            "merchant_category", String.valueOf(tx.getMerchantCategory()),
            "action", action.name()
        ));
        
        meterRegistry.counter(
            "fraud_violations_total",
            "count", String.valueOf(violations.size()),
            "action", action.name()
        ).increment();
        
        meterRegistry.gauge(
            "fraud_probability",
            fraudProbability
        );
    }
    
    private void logAssessment(
        Transaction tx,
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