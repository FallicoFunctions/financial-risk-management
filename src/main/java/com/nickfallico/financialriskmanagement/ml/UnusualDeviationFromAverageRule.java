package com.nickfallico.financialriskmanagement.ml;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class UnusualDeviationFromAverageRule implements FraudRule {
    private static final double DEVIATION_THRESHOLD = 0.5; // 50% deviation
    
    @Override
    public Optional<FraudViolation> evaluate(FraudEvaluationContext ctx) {
        double avgAmount = ctx.profile().getAverageTransactionAmount();
        if (avgAmount == 0) return Optional.empty(); // No history yet
        
        double currentAmount = ctx.transaction().getAmount().doubleValue();
        double deviation = Math.abs(currentAmount - avgAmount) / avgAmount;
        
        if (deviation > DEVIATION_THRESHOLD) {
            return Optional.of(new FraudViolation(
                "UNUSUAL_DEVIATION",
                String.format("Amount deviates %.1f%% from user average", deviation * 100),
                Math.min(deviation * 0.5, 0.7)
            ));
        }
        return Optional.empty();
    }
}