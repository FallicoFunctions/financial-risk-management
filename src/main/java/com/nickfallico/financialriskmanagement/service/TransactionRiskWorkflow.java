package com.nickfallico.financialriskmanagement.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.exception.FraudDetectionException;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;
import com.nickfallico.financialriskmanagement.kafka.producer.TransactionEventProducer;
import com.nickfallico.financialriskmanagement.model.Transactions;
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
    private final TransactionEventProducer eventProducer;
    
    public Mono<Transactions> processTransaction(Transactions transaction) {
        // Set the ID if it's not already set
        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID());
        }
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
            .flatMap(tx -> txRepository.saveTransaction(
                tx.getId(),
                tx.getUserId(), 
                tx.getAmount(), 
                tx.getCurrency(), 
                tx.getCreatedAt(), 
                tx.getTransactionType(), 
                tx.getMerchantCategory(), 
                tx.getIsInternational(),
                tx.getMerchantName()
            ))
            .flatMap(savedTx -> {
                // Publish event to Kafka
                TransactionCreatedEvent event = TransactionCreatedEvent.fromTransaction(savedTx);
                
                return eventProducer.publishTransactionCreated(event)
                    .then(profileService.updateProfileAfterTransaction(savedTx))
                    .thenReturn(savedTx);
            });
    }
}
