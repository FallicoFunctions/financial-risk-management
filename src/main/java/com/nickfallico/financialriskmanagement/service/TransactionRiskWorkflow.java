package com.nickfallico.financialriskmanagement.service;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.exception.FraudDetectionException;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;

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
        return Mono.fromSupplier(() -> {
            UserRiskProfile userProfile = userRiskProfileService.getUserProfile(transaction.getUserId());
            
            boolean isPotentialFraud = fraudDetectionService.isPotentialFraud(transaction, userProfile);
            
            if (isPotentialFraud) {
                log.warn("Potential fraud detected for transaction: {}", transaction.getId());
                throw new FraudDetectionException("Potential fraud detected");
            }
            
            // Update user risk profile
            userRiskProfileService.updateUserRiskProfile(transaction);
            
            return transaction;
        });
    }
}