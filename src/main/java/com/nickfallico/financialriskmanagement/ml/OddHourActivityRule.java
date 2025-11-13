package com.nickfallico.financialriskmanagement.ml;

import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OddHourActivityRule implements FraudRule {
    private static final int LOW_HISTORY_THRESHOLD = 20;
    
    @Override
    public Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext ctx) {
        int hourUtc = java.time.ZonedDateTime
            .ofInstant(ctx.transaction().getCreatedAt(), ZoneId.of("UTC"))
            .getHour();
        
        // Odd hours: 0-4 AM UTC
        boolean isOddHour = hourUtc < 5;
        boolean hasLowHistory = ctx.profile().getTotalTransactions() < LOW_HISTORY_THRESHOLD;

        if (isOddHour && hasLowHistory) {
            return Mono.just(Optional.of(new FraudViolation(
                "ODD_HOUR_LOW_HISTORY",
                "Late-night transaction by user with limited history",
                0.6
            )));
        }
        return Mono.just(Optional.empty());
    }
}