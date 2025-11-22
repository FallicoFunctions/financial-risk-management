package com.nickfallico.financialriskmanagement.ml;

import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Geographic anomaly fraud rule.
 * Detects unusual country usage patterns.
 * Indicators: first-time countries, excessive country hopping.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GeographicAnomalyRule implements FraudRule {
    
    private final TransactionRepository transactionRepository;
    
    // Threshold: more than this many countries indicates possible account compromise
    private static final long MAX_DISTINCT_COUNTRIES = 5;
    
    @Override
    public Mono<Optional<FraudViolation>> evaluate(FraudEvaluationContext context) {
        var transaction = context.transaction();
        var profile = context.profile();
        String userId = transaction.getUserId();
        String country = transaction.getCountry();

        // Skip if no country data available
        if (country == null || country.isBlank()) {
            return Mono.just(Optional.empty());
        }

        return transactionRepository.countDistinctCountries(userId)
            .defaultIfEmpty(0L)
            .flatMap(distinctCountries -> {
                if (distinctCountries > MAX_DISTINCT_COUNTRIES) {
                    log.warn("Geographic anomaly: User {} has used {} distinct countries (max: {})",
                        userId, distinctCountries, MAX_DISTINCT_COUNTRIES);

                    return Mono.just(Optional.of(new FraudViolation(
                        "GEOGRAPHIC_COUNTRY_HOPPING",
                        String.format("Excessive country usage: %d distinct countries (max: %d)",
                            distinctCountries, MAX_DISTINCT_COUNTRIES),
                        0.65 // Moderate-high risk
                    )));
                }

                return transactionRepository.hasUserTransactedInCountry(userId, country)
                    .defaultIfEmpty(false)
                    .map(hasTransactedInCountry -> {
                        if (!hasTransactedInCountry) {
                            if (profile.isNewUser()) {
                                log.warn("Geographic anomaly: New user {} transacting from country {} for first time",
                                    userId, country);

                                return Optional.of(new FraudViolation(
                                    "GEOGRAPHIC_NEW_USER_NEW_COUNTRY",
                                    String.format("New user transacting from new country: %s", country),
                                    0.75 // High risk - common account takeover pattern
                                ));
                            }

                            if (profile.isEstablished()) {
                                log.info("Geographic anomaly: Established user {} transacting from new country {}",
                                    userId, country);

                                return Optional.of(new FraudViolation(
                                    "GEOGRAPHIC_NEW_COUNTRY",
                                    String.format("First transaction from country: %s", country),
                                    0.5 // Moderate risk - could be legitimate travel
                                ));
                            }
                        }
                        return Optional.empty();
                    });
            });
    }
}
