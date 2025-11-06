package com.nickfallico.financialriskmanagement.ml;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class UnusedMerchantCategoryRule implements FraudRule {
    
    @Override
    public Optional<FraudViolation> evaluate(FraudEvaluationContext ctx) {
        String category = ctx.transaction().getMerchantCategory();
        
        if (category == null || ctx.merchantFrequency() == null) {
            return Optional.empty();
        }
        
        boolean categoryIsUnused = !ctx.merchantFrequency().isCategoryCommon(category);
        boolean hasHistory = ctx.profile().getTotalTransactions() > 5;
        
        if (categoryIsUnused && hasHistory) {
            return Optional.of(new FraudViolation(
                "UNUSED_CATEGORY",
                "Transaction in merchant category never used by this user",
                0.3
            ));
        }
        return Optional.empty();
    }
}