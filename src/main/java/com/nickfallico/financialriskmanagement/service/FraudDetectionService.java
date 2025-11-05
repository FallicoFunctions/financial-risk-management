package com.nickfallico.financialriskmanagement.service;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.ml.FraudFeatureExtractor;
import com.nickfallico.financialriskmanagement.ml.ProbabilisticFraudModel;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {
    private static final java.util.Set<String> HIGH_RISK =
        java.util.Set.of("GAMBLING","CRYPTO","ADULT_ENTERTAINMENT");

    private final FraudFeatureExtractor fraudFeatureExtractor;
    private final ProbabilisticFraudModel fraudModel;
    private final MeterRegistry meterRegistry;
    private static final java.math.BigDecimal HIGH_AMOUNT = new java.math.BigDecimal("10000");

    public Mono<Boolean> isPotentialFraud(Transaction transaction, UserRiskProfile profile) {
        return Mono.fromCallable(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);

            // --- Deterministic rules the tests expect ---
            String cat = transaction.getMerchantCategory() == null
                    ? "" : transaction.getMerchantCategory().toUpperCase();

            // 1) High-risk merchant category → flag
            if (HIGH_RISK.contains(cat)) {
                record(sample, transaction, true);
                log.warn("Potential Fraud (rule): high-risk category [{}] for tx {}", cat, transaction.getId());
                return true;
            }

            // 2) high amount → flag
            if (transaction.getAmount() != null && transaction.getAmount().compareTo(HIGH_AMOUNT) >= 0) {
                record(sample, transaction, true);
                log.warn("Potential Fraud (rule): high amount [{}] for tx {}", transaction.getAmount(), transaction.getId());
                return true;
            }

            // 3) International + thin history → flag
            if (Boolean.TRUE.equals(transaction.getIsInternational())
                    && (profile == null || profile.getTotalTransactions() <= 5)) {
                record(sample, transaction, true);
                log.warn("Potential Fraud (rule): international with low history for tx {}", transaction.getId());
                return true;
            }

            // 4) Odd-hour spending with low history → flag (midnight–4:59 UTC)
            if (transaction.getCreatedAt() != null) {
                int hourUtc = java.time.ZonedDateTime.ofInstant(
                        transaction.getCreatedAt(), java.time.ZoneOffset.UTC).getHour();
                if (hourUtc < 5 && (profile == null || profile.getTotalTransactions() < 20)) {
                    record(sample, transaction, true);
                    log.warn("Potential Fraud (rule): odd-hour activity for tx {}", transaction.getId());
                    return true;
                }
            }

            // 5) Very low activity overall → flag
            if (profile == null || profile.getTotalTransactions() <= 2) {
                record(sample, transaction, true);
                log.warn("Potential Fraud (rule): very low transaction frequency for tx {}", transaction.getId());
                return true;
            }

            // --- Probabilistic model as a fallback ---
            var features = fraudFeatureExtractor.extractFeatures(transaction, profile);
            double fraudProbability = fraudModel.calculateFraudProbability(features);
            boolean isFraudulent = fraudModel.isFraudulent(fraudProbability);

            record(sample, transaction, isFraudulent);
            logFraudDetectionResult(transaction, fraudProbability, isFraudulent);

            return isFraudulent;
        });
    }

    private void record(Timer.Sample sample, Transaction tx, boolean isFraudulent) {
        sample.stop(meterRegistry.timer("fraud_detection_time",
                "merchant_category", String.valueOf(tx.getMerchantCategory()),
                "is_fraudulent", String.valueOf(isFraudulent)));
        meterRegistry.counter("fraud_detection_attempts",
                "merchant_category", String.valueOf(tx.getMerchantCategory()),
                "is_fraudulent", String.valueOf(isFraudulent)).increment();
    }

    private void logFraudDetectionResult(Transaction transaction, double p, boolean fraud) {
        if (fraud) {
            log.warn("Potential Fraud Detected - Transaction Details: [ID: {}, Amount: {}, Merchant: {}, Fraud Probability: {}]",
                    transaction.getId(), transaction.getAmount(), transaction.getMerchantCategory(), p);
        } else {
            log.info("Transaction Approved - Transaction Details: [ID: {}, Amount: {}, Merchant: {}]",
                    transaction.getId(), transaction.getAmount(), transaction.getMerchantCategory());
        }
    }
}
