package com.nickfallico.financialriskmanagement.ml;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class NewUserOnboardingRule implements FraudRule {
    private static final BigDecimal NEW_USER_LIMIT = BigDecimal.valueOf(500);
    
    /**
     * NEW USER LOGIC (FIXED): New users can transact, but with reduced thresholds.
     * This rule is permissive; other rules (HIGH_AMOUNT, etc.) catch real issues.
     */
    @Override
    public Optional<FraudViolation> evaluate(FraudEvaluationContext ctx) {
        if (ctx.profile().getTotalTransactions() == 0) {
            // First transaction: only flag if amount is excessive
            if (ctx.transaction().getAmount().compareTo(NEW_USER_LIMIT) > 0) {
                return Optional.of(new FraudViolation(
                    "FIRST_TX_HIGH_AMOUNT",
                    "First transaction exceeds $500 new user limit",
                    0.5
                ));
            }
        }
        return Optional.empty();
    }
}