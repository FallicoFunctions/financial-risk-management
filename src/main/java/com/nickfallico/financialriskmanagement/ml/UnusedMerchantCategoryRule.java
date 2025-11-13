package com.nickfallico.financialriskmanagement.ml;

import java.util.Optional;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class UnusedMerchantCategoryRule implements FraudRule {
    
    @Override
    public Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext ctx) {
        String category = ctx.transaction().getMerchantCategory();
        
        if (category == null || ctx.merchantFrequency() == null) {
            return Mono.just(Optional.empty());
        }
        
        boolean categoryIsUnused = !ctx.merchantFrequency().isCategoryCommon(category);
        boolean hasHistory = ctx.profile().getTotalTransactions() > 5;
        
        if (categoryIsUnused && hasHistory) {
            return Mono.just(Optional.of(new FraudViolation(
                "UNUSED_CATEGORY",
                "Transaction in merchant category never used by this user",
                0.3
            )));
        }
        return Mono.just(Optional.empty());
    }
}