package com.nickfallico.financialriskmanagement.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.exception.FraudDetectionException;
import com.nickfallico.financialriskmanagement.exception.TransactionValidationException;
import com.nickfallico.financialriskmanagement.model.Transaction;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionRiskWorkflow {
    private final FraudDetectionService fraudDetectionService;
    private final UserRiskProfileService userRiskProfileService;
    private final MeterRegistry meterRegistry;

    public Mono<Transaction> processTransaction(Transaction transaction) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        log.info("Processing transaction: {}", transaction.getId());
        
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid transaction amount for transaction: {}", transaction.getId());
            meterRegistry.counter("transaction_validation_failures", 
                "reason", "negative_amount").increment();
            return Mono.error(new TransactionValidationException("Transaction amount must be positive"));
        }

        return userRiskProfileService.getUserProfile(transaction.getUserId())
            .flatMap(userProfile -> 
                fraudDetectionService.isPotentialFraud(transaction, userProfile)
                    .flatMap(isPotentialFraud -> {
                        if (isPotentialFraud) {
                            log.warn("Potential fraud detected for transaction: {}", transaction.getId());
                            meterRegistry.counter("fraud_transactions", 
                                "merchant_category", transaction.getMerchantCategory()).increment();
                            return Mono.error(new FraudDetectionException("Potential fraud detected"));
                        }
                        
                        return userRiskProfileService.updateUserRiskProfile(transaction)
                            .doOnSuccess(v -> {
                                log.info("Transaction processed successfully: {}", transaction.getId());
                                meterRegistry.counter("successful_transactions", 
                                    "merchant_category", transaction.getMerchantCategory()).increment();
                                
                                // Stop and record transaction processing time
                                sample.stop(meterRegistry.timer("transaction_processing_time", 
                                    "merchant_category", transaction.getMerchantCategory()));
                            })
                            .thenReturn(transaction);
                    })
            );
    }
}