package com.nickfallico.financialriskmanagement.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.exception.FraudDetectionException;
import com.nickfallico.financialriskmanagement.exception.TransactionValidationException;
import com.nickfallico.financialriskmanagement.model.Transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionRiskWorkflow {
    private final FraudDetectionService fraudDetectionService;
    private final UserRiskProfileService userRiskProfileService;

    public Mono<Transaction> processTransaction(Transaction transaction) {
        log.info("Processing transaction: {}", transaction.getId());
        
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid transaction amount for transaction: {}", transaction.getId());
            return Mono.error(new TransactionValidationException("Transaction amount must be positive"));
        }
    
        return userRiskProfileService.getUserProfile(transaction.getUserId())
            .flatMap(userProfile -> 
                fraudDetectionService.isPotentialFraud(transaction, userProfile)
                    .flatMap(isPotentialFraud -> {
                        if (isPotentialFraud) {
                            log.warn("Potential fraud detected for transaction: {}", transaction.getId());
                            return Mono.error(new FraudDetectionException("Potential fraud detected"));
                        }
                        
                        return userRiskProfileService.updateUserRiskProfile(transaction)
                            .doOnSuccess(v -> log.info("Transaction processed successfully: {}", transaction.getId()))
                            .thenReturn(transaction);
                    })
            );
    }
}