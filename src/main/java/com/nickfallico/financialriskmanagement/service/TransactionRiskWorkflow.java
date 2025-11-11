package com.nickfallico.financialriskmanagement.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nickfallico.financialriskmanagement.kafka.event.FraudClearedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.FraudDetectedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionBlockedEvent;
import com.nickfallico.financialriskmanagement.kafka.event.TransactionCreatedEvent;
import com.nickfallico.financialriskmanagement.kafka.producer.TransactionEventProducer;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;
import com.nickfallico.financialriskmanagement.service.FraudDetectionService.FraudAssessment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionRiskWorkflow {
    private final FraudDetectionService fraudService;
    private final UserProfileService profileService;
    private final TransactionRepository txRepository;
    private final TransactionEventProducer eventProducer;
    
    /**
     * Process transaction with fire-and-forget fraud detection pattern.
     * Saves transaction immediately and returns fast response to user.
     * Fraud detection runs asynchronously in background.
     */
    public Mono<Transactions> processTransaction(Transactions transaction) {
        // Set the ID if it's not already set
        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID());
        }
        
        // STEP 1: Save transaction immediately (fast response to user)
        return txRepository.saveTransaction(
            transaction.getId(),
            transaction.getUserId(), 
            transaction.getAmount(), 
            transaction.getCurrency(), 
            transaction.getCreatedAt(), 
            transaction.getTransactionType(), 
            transaction.getMerchantCategory(), 
            transaction.getIsInternational(),
            transaction.getMerchantName()
        )
        .flatMap(savedTx -> {
            // STEP 2: Publish TransactionCreated event
            TransactionCreatedEvent event = TransactionCreatedEvent.fromTransaction(savedTx);
            
            return eventProducer.publishTransactionCreated(event)
                .thenReturn(savedTx);
        })
        .doOnSuccess(savedTx -> {
            // STEP 3: Trigger async fraud detection (fire-and-forget)
            // Uses subscribeOn to run on separate thread pool
            processFraudDetectionAsync(savedTx)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    result -> log.info("Fraud detection completed for tx: {}", savedTx.getId()),
                    error -> log.error("Fraud detection failed for tx: {}", savedTx.getId(), error)
                );
        });
    }

    /**
     * Async fraud detection - runs in background after API returns.
     * Publishes fraud events and updates user profile.
     * Does not block the main transaction flow.
     */
    private Mono<FraudAssessment> processFraudDetectionAsync(Transactions transaction) {
        return profileService.getUserProfile(transaction.getUserId())
            .zipWith(profileService.getMerchantFrequency(transaction.getUserId()))
            .flatMap(tuple -> {
                var profile = tuple.getT1();
                var frequency = tuple.getT2();
                
                return fraudService.assessFraud(transaction, profile, frequency);
            })
            .flatMap(assessment -> {
                if (assessment.shouldBlock()) {
                    // Create fraud events
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
                    
                    // Publish events (timeout/error handling is in producer methods)
                    return eventProducer.publishFraudDetected(fraudEvent)
                        .then(eventProducer.publishTransactionBlocked(blockedEvent))
                        .then(profileService.updateProfileAfterTransaction(transaction))
                        .thenReturn(assessment);
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
                    .then(profileService.updateProfileAfterTransaction(transaction))
                    .thenReturn(assessment);
            });
    }
}