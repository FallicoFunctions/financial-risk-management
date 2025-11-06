package com.nickfallico.financialriskmanagement.ml;

import java.util.Optional;

/**
 * Pure functional interface for fraud detection rules.
 * Each rule is a stateless function: Transaction + Profile → Optional<Violation>
 * No side effects; fully testable in isolation.
 */
public interface FraudRule {
    
    /**
     * Evaluate transaction against this rule.
     * 
     * @param context Immutable evaluation context
     * @return Empty if rule passes; violation if triggered
     */
    Optional<FraudViolation> evaluate(FraudEvaluationContext context);
    
    /**
     * Immutable context containing all data needed for fraud evaluation.
     */
    record FraudEvaluationContext(
        com.nickfallico.financialriskmanagement.model.Transaction transaction,
        com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile profile,
        com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency merchantFrequency
    ) {}
    
    /**
     * Immutable fraud violation record.
     */
    record FraudViolation(
        String ruleId,
        String description,
        double riskScore,
        long timestamp
    ) {
        public FraudViolation(String ruleId, String description, double riskScore) {
            this(ruleId, description, riskScore, System.currentTimeMillis());
        }
    }
}