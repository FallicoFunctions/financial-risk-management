package com.nickfallico.financialriskmanagement.controller;

import com.nickfallico.financialriskmanagement.dto.FraudEventDTO;
import com.nickfallico.financialriskmanagement.eventstore.model.EventLog;
import com.nickfallico.financialriskmanagement.eventstore.repository.EventLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionFraudController {

    private final EventLogRepository eventLogRepository;
    private final MeterRegistry meterRegistry;

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
}