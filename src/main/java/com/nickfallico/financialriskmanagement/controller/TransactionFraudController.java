package com.nickfallico.financialriskmanagement.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nickfallico.financialriskmanagement.dto.FraudEventDTO;
import com.nickfallico.financialriskmanagement.dto.TransactionStatusDTO;
import com.nickfallico.financialriskmanagement.eventstore.model.EventLog;
import com.nickfallico.financialriskmanagement.eventstore.repository.EventLogRepository;
import com.nickfallico.financialriskmanagement.model.Transactions;
import com.nickfallico.financialriskmanagement.repository.TransactionRepository;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionFraudController {

    private final EventLogRepository eventLogRepository;
    private final TransactionRepository transactionRepository;
    private final MeterRegistry meterRegistry;

    @GetMapping("/{transactionId}")
    public Mono<ResponseEntity<TransactionStatusDTO>> getTransactionStatus(
        @PathVariable UUID transactionId
    ) {
        log.info("Fetching transaction status for: {}", transactionId);
        meterRegistry.counter("api.transaction_status.requests").increment();

        Mono<Transactions> transactionMono = transactionRepository.findById(transactionId)
            .switchIfEmpty(Mono.error(new RuntimeException("Transaction not found: " + transactionId)));

        Mono<List<FraudEventDTO>> fraudEventsMono = eventLogRepository
            .findByAggregateIdAndAggregateTypeOrderBySequenceNumberAsc(
                transactionId.toString(), "TRANSACTION")
            .filter(this::isFraudRelatedEvent)
            .map(this::convertToDTO)
            .collectList();

        return Mono.zip(transactionMono, fraudEventsMono)
            .map(tuple -> {
                Transactions transaction = tuple.getT1();
                List<FraudEventDTO> fraudEvents = tuple.getT2();

                TransactionStatusDTO status = buildTransactionStatus(transaction, fraudEvents);

                log.info("Retrieved transaction status for {}: fraudStatus={}, probability={}",
                    transactionId, status.getFraudStatus(), status.getFraudProbability());

                return ResponseEntity.ok(status);
            })
            .doOnError(error -> {
                log.error("Failed to retrieve transaction status for: {}", transactionId, error);
                meterRegistry.counter("api.transaction_status.errors").increment();
            });
    }

    @GetMapping("/{transactionId}/fraud-events")
    public Mono<ResponseEntity<List<FraudEventDTO>>> getTransactionFraudEvents(
        @PathVariable String transactionId
    ) {
        log.info("Fetching fraud events for transaction: {}", transactionId);
        meterRegistry.counter("api.transaction_fraud_events.requests").increment();

        return eventLogRepository.findByAggregateIdAndAggregateTypeOrderBySequenceNumberAsc(
                transactionId, "TRANSACTION")
            .filter(this::isFraudRelatedEvent)
            .map(this::convertToDTO)
            .collectList()
            .doOnError(error -> {
                log.error("Failed to retrieve fraud events for transaction: {}", transactionId, error);
                meterRegistry.counter("api.transaction_fraud_events.errors").increment();
            })
            .map(events -> {
                log.info("Found {} fraud events for transaction: {}", events.size(), transactionId);
                return ResponseEntity.ok(events);
            });
    }

    private boolean isFraudRelatedEvent(EventLog event) {
        String type = event.getEventType();
        return "FRAUD_DETECTED".equals(type)
            || "FRAUD_CLEARED".equals(type)
            || "TRANSACTION_BLOCKED".equals(type)
            || "HIGH_RISK_ALERT".equals(type);
    }

    private FraudEventDTO convertToDTO(EventLog event) {
        Map<String, Object> data = event.getEventData();

        FraudEventDTO dto = FraudEventDTO.builder()
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .sequenceNumber(event.getSequenceNumber())
            .aggregateId(event.getAggregateId())
            .aggregateType(event.getAggregateType())
            .eventData(data)
            .createdAt(event.getCreatedAt())
            .build();

        if (data != null) {
            if (data.containsKey("transactionId")) {
                dto.setTransactionId(UUID.fromString(data.get("transactionId").toString()));
            }
            if (data.containsKey("amount")) {
                dto.setAmount(data.get("amount").toString());
            }
            if (data.containsKey("currency")) {
                dto.setCurrency(data.get("currency").toString());
            }
            if (data.containsKey("fraudProbability")) {
                dto.setFraudProbability(Double.parseDouble(data.get("fraudProbability").toString()));
            }
            if (data.containsKey("riskLevel")) {
                dto.setRiskLevel(data.get("riskLevel").toString());
            }
            if (data.containsKey("reason")) {
                dto.setReason(data.get("reason").toString());
            }
            if (data.containsKey("merchantCategory")) {
                dto.setMerchantCategory(data.get("merchantCategory").toString());
            }
        }

        return dto;
    }

    private TransactionStatusDTO buildTransactionStatus(
        Transactions transaction,
        List<FraudEventDTO> fraudEvents
    ) {
        String fraudStatus = "CLEAN";
        Double fraudProbability = 0.0;
        String riskLevel = "LOW";
        List<String> fraudReasons = new ArrayList<>();
        String processingStatus = "APPROVED";

        if (!fraudEvents.isEmpty()) {
            FraudEventDTO latestEvent = fraudEvents.get(fraudEvents.size() - 1);

            if (latestEvent.getFraudProbability() != null) {
                fraudProbability = latestEvent.getFraudProbability();
            }

            if (latestEvent.getRiskLevel() != null) {
                riskLevel = latestEvent.getRiskLevel();
            }

            boolean hasBlockedEvent = fraudEvents.stream()
                .anyMatch(e -> "TRANSACTION_BLOCKED".equals(e.getEventType()));
            boolean hasFraudDetected = fraudEvents.stream()
                .anyMatch(e -> "FRAUD_DETECTED".equals(e.getEventType()));
            boolean hasFraudCleared = fraudEvents.stream()
                .anyMatch(e -> "FRAUD_CLEARED".equals(e.getEventType()));

            if (hasBlockedEvent) {
                fraudStatus = "BLOCKED";
                processingStatus = "BLOCKED";
            } else if (hasFraudDetected && !hasFraudCleared) {
                fraudStatus = "FLAGGED";
                processingStatus = "UNDER_REVIEW";
            } else if (hasFraudCleared) {
                fraudStatus = "CLEARED";
                processingStatus = "APPROVED";
            }

            fraudReasons = fraudEvents.stream()
                .map(e -> {
                    if (e.getEventData() != null && e.getEventData().containsKey("reason")) {
                        return e.getEventData().get("reason").toString();
                    } else if (e.getReason() != null) {
                        return e.getReason();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        }

        Instant lastUpdated = fraudEvents.isEmpty()
            ? transaction.getCreatedAt()
            : fraudEvents.get(fraudEvents.size() - 1).getCreatedAt();

        return TransactionStatusDTO.builder()
            .transactionId(transaction.getId())
            .userId(transaction.getUserId())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .createdAt(transaction.getCreatedAt())
            .transactionType(transaction.getTransactionType())
            .merchantCategory(transaction.getMerchantCategory())
            .merchantName(transaction.getMerchantName())
            .isInternational(transaction.getIsInternational())
            .latitude(transaction.getLatitude())
            .longitude(transaction.getLongitude())
            .country(transaction.getCountry())
            .city(transaction.getCity())
            .ipAddress(transaction.getIpAddress())
            .fraudStatus(fraudStatus)
            .fraudProbability(fraudProbability)
            .riskLevel(riskLevel)
            .fraudReasons(fraudReasons)
            .violationCount(fraudReasons.size())
            .processingStatus(processingStatus)
            .lastUpdated(lastUpdated)
            .reviewedBy(null)
            .fraudEvents(fraudEvents)
            .build();
    }
}