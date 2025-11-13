package com.nickfallico.financialriskmanagement.ml;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;

@Component
public class HighRiskMerchantCategoryRule implements FraudRule {
    private static final Set<String> HIGH_RISK_CATEGORIES = Set.of(
        "GAMBLING", "CRYPTO", "ADULT_ENTERTAINMENT"
    );
    
    @Override
    public Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext ctx) {
        String category = ctx.transaction().getMerchantCategory();
        if (category != null && 
            HIGH_RISK_CATEGORIES.contains(category.toUpperCase())) {
            return Mono.just(Optional.of(new FraudViolation(
                "HIGH_RISK_CATEGORY",
                "Transaction in high-risk merchant category: " + category,
                0.9
            )));
        }
        return Mono.just(Optional.empty());
    }
}