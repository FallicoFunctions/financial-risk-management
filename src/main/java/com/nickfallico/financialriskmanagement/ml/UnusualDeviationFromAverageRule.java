package com.nickfallico.financialriskmanagement.ml;

import java.util.Optional;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class UnusualDeviationFromAverageRule implements FraudRule {
    private static final double DEVIATION_THRESHOLD = 0.5; // 50% deviation
    
    @Override
    public Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext ctx) {
        double avgAmount = ctx.profile().getAverageTransactionAmount();
        if (avgAmount == 0) return Mono.just(Optional.empty()); // No history yet
        
        double currentAmount = ctx.transaction().getAmount().doubleValue();
        double deviation = Math.abs(currentAmount - avgAmount) / avgAmount;
        
        if (deviation > DEVIATION_THRESHOLD) {
            return Mono.just(Optional.of(new FraudViolation(
                "UNUSUAL_DEVIATION",
                String.format("Amount deviates %.1f%% from user average", deviation * 100),
                Math.min(deviation * 0.5, 0.7)
            )));
        }
        return Mono.just(Optional.empty());
    }
}