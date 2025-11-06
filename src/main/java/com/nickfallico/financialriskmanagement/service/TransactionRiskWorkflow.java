package com.nickfallico.financialriskmanagement.service;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.exception.FraudDetectionException;
import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionRiskWorkflow {
    private final FraudDetectionService fraudService;
    private final UserProfileService profileService;
    private final TransactionRepository txRepository;
    
    public Mono<Transaction> processTransaction(Transaction transaction) {
        return profileService.getUserProfile(transaction.getUserId())
            .zipWith(profileService.getMerchantFrequency(transaction.getUserId()))
            .flatMap(tuple -> {
                var profile = tuple.getT1();
                var frequency = tuple.getT2();
                
                return fraudService.assessFraud(transaction, profile, frequency)
                    .flatMap(assessment -> {
                        if (assessment.shouldBlock()) {
                            return Mono.error(new FraudDetectionException(
                                "Fraud detected: " + assessment.getViolationSummary()
                            ));
                        }
                        return Mono.just(transaction);
                    });
            })
            .flatMap(txRepository::save)
            .flatMap(savedTx -> profileService.updateProfileAfterTransaction(savedTx)
                .thenReturn(savedTx)
            );
    }
}
