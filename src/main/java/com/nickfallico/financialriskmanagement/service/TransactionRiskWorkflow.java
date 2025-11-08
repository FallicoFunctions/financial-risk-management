package com.nickfallico.financialriskmanagement.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.exception.FraudDetectionException;
import com.nickfallico.financialriskmanagement.kafka.event.FraudClearedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.FraudDetectedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionBlockedEvent;
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
                            // Publish fraud detected event
                            FraudDetectedEvent fraudEvent = FraudDetectedEvent.create(
                                transaction.getId(),
                                transaction.getUserId(),
                                transaction.getAmount(),
                                transaction.getCurrency(),
                                transaction.getMerchantCategory(),
                                transaction.getIsInternational(),
                                assessment.fraudProbability(),
                                assessment.violations().stream()
                                    .map(v -> v.ruleId())
                                    .toList(),
                                assessment.fraudProbability() >= 0.8 ? "CRITICAL" : "HIGH",
                                "BLOCK"
                            );
                            
                            // Publish transaction blocked event
                            TransactionBlockedEvent blockedEvent = TransactionBlockedEvent.create(
                                transaction.getId(),
                                transaction.getUserId(),
                                transaction.getAmount(),
                                transaction.getCurrency(),
                                transaction.getMerchantCategory(),
                                transaction.getIsInternational(),
                                assessment.getViolationSummary(),
                                assessment.violations().stream()
                                    .map(v -> v.ruleId())
                                    .toList(),
                                assessment.fraudProbability(),
                                assessment.fraudProbability() >= 0.8 ? "CRITICAL" : "HIGH"
                            );
                            
                            // Publish both events, then throw exception
                            return eventProducer.publishFraudDetected(fraudEvent)
                                .then(eventProducer.publishTransactionBlocked(blockedEvent))
                                .then(Mono.error(new FraudDetectionException(
                                    "Fraud detected: " + assessment.getViolationSummary()
                                )));
                        }
                        
                        // Fraud check passed - publish cleared event
                        FraudClearedEvent clearedEvent = FraudClearedEvent.create(
                            transaction.getId(),
                            transaction.getUserId(),
                            transaction.getAmount(),
                            transaction.getCurrency(),
                            transaction.getMerchantCategory(),
                            assessment.fraudProbability(),
                            assessment.violations().size()
                        );
                        
                        return eventProducer.publishFraudCleared(clearedEvent)
                            .thenReturn(transaction);
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
