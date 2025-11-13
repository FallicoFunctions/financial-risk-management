package com.nickfallico.financialriskmanagement.ml;

import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * Pure functional interface for fraud detection rules.
 * Each rule is a stateless function: Transaction + Profile → Mono<Optional<Violation>>
 * No side effects; fully testable in isolation.
 * Reactive to support non-blocking database queries.
 */
public interface FraudRule {

    /**
     * Evaluate transaction against this rule (reactive version).
     *
     * @param context Immutable evaluation context
     * @return Mono containing empty if rule passes; violation if triggered
     */
    Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext context);
    
    /**
     * Immutable context containing all data needed for fraud evaluation.
     */
    record FraudEvaluationContext(
        com.nickfallico.financialriskmanagement.model.Transactions transaction,
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