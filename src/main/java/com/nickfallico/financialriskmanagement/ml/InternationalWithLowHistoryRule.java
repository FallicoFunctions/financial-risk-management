package com.nickfallico.financialriskmanagement.ml;

import java.util.Optional;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class InternationalWithLowHistoryRule implements FraudRule {
    private static final int NEW_USER_THRESHOLD = 5;
    
    @Override
    public Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext ctx) {
        boolean isInternational = Boolean.TRUE.equals(ctx.transaction().getIsInternational());
        boolean isNewUser = ctx.profile().getTotalTransactions() <= NEW_USER_THRESHOLD;
        
        if (isInternational && isNewUser) {
            return Mono.just(Optional.of(new FraudViolation(
                "INTERNATIONAL_NEW_USER",
                "International transaction by user with < 5 prior transactions",
                0.7
            )));
        }
        return Mono.just(Optional.empty());
    }
}