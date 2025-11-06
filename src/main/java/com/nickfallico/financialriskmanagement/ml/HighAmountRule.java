package com.nickfallico.financialriskmanagement.ml;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class HighAmountRule implements FraudRule {
    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(10000);
    
    @Override
    public Optional<FraudViolation> evaluate(FraudEvaluationContext ctx) {
        if (ctx.transaction().getAmount().compareTo(THRESHOLD) >= 0) {
            return Optional.of(new FraudViolation(
                "HIGH_AMOUNT",
                "Transaction amount exceeds $10,000 threshold",
                0.8
            ));
        }
        return Optional.empty();
    }
}