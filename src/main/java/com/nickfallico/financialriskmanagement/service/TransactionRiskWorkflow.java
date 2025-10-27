package com.nickfallico.financialriskmanagement.service;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.exception.FraudDetectionException;
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
        return userRiskProfileService.getUserProfile(transaction.getUserId())
            .flatMap(userProfile -> {
                boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(transaction, userProfile);
                
                if (isPotentialFraud) {
                    log.warn("Potential fraud detected for transaction: {}", transaction.getId());
                    return Mono.error(new FraudDetectionException("Potential fraud detected"));
                }
                
                // Update user risk profile
                return userRiskProfileService.updateUserRiskProfile(transaction)
                    .thenReturn(transaction);
            });
    }
}