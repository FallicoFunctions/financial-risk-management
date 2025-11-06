package com.nickfallico.financialriskmanagement.ml;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Functional fraud rule engine.
 * Composes all fraud rules via stream operations (no loops).
 * Immutable; thread-safe by design.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FraudRuleEngine {
    
    private final HighAmountRule highAmountRule;
    private final HighRiskMerchantCategoryRule highRiskCategoryRule;
    private final InternationalWithLowHistoryRule internationalRule;
    private final OddHourActivityRule oddHourRule;
    private final NewUserOnboardingRule newUserRule;
    private final UnusualDeviationFromAverageRule deviationRule;
    private final UnusedMerchantCategoryRule unusedCategoryRule;
    
    /**
     * Evaluate transaction against all fraud rules.
     * Returns all violations found (can be multiple per transaction).
     * Uses functional stream composition; no mutable state.
     */
    public List<FraudRule.FraudViolation> evaluateTransaction(
        FraudRule.FraudEvaluationContext context) {
        
        return List.of(
            highAmountRule,
            highRiskCategoryRule,
            internationalRule,
            oddHourRule,
            newUserRule,
            deviationRule,
            unusedCategoryRule
        )
        .stream()
        .flatMap(rule -> rule.evaluate(context).stream())
        .peek(violation -> log.debug(
            "Fraud rule triggered: {} - {}",
            violation.ruleId(),
            violation.description()
        ))
        .collect(Collectors.toList());
    }
    
    /**
     * Calculate composite fraud probability from violations.
     * Uses weighted average of violation scores.
     */
    public double calculateFraudProbability(
        List<FraudRule.FraudViolation> violations) {
        
        if (violations.isEmpty()) {
            return 0.0;
        }
        
        double totalScore = violations.stream()
            .mapToDouble(FraudRule.FraudViolation::riskScore)
            .sum();
        
        double averageScore = totalScore / violations.size();
        
        // Boost if multiple violations (compound risk)
        double boostFactor = Math.min(violations.size() * 0.1, 0.3);
        
        return Math.min(averageScore + boostFactor, 1.0);
    }
    
    /**
     * Determine action based on fraud probability.
     */
    public FraudAction determineAction(double fraudProbability) {
        if (fraudProbability >= 0.8) {
            return FraudAction.BLOCK;
        } else if (fraudProbability >= 0.6) {
            return FraudAction.REVIEW;
        } else if (fraudProbability >= 0.3) {
            return FraudAction.MONITOR;
        }
        return FraudAction.APPROVE;
    }
    
    public enum FraudAction {
        APPROVE("Transaction approved"),
        MONITOR("Monitor transaction - low risk"),
        REVIEW("Review transaction - moderate risk"),
        BLOCK("Block transaction - high fraud probability");
        
        private final String description;
        
        FraudAction(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isBlocking() {
            return this == BLOCK;
        }
        
        public boolean needsReview() {
            return this == REVIEW;
        }
    }
}