package com.nickfallico.financialriskmanagement.ml;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class HighAmountRule implements FraudRule {
    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(10000);

    @Override
    public Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext ctx) {
        if (ctx.transaction().getAmount().compareTo(THRESHOLD) >= 0) {
            return Mono.just(Optional.of(new FraudViolation(
                "HIGH_AMOUNT",
                "Transaction amount exceeds $10,000 threshold",
                0.8
            )));
        }
        return Mono.just(Optional.empty());
    }
}