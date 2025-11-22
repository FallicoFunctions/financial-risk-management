package com.nickfallico.financialriskmanagement.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nickfallico.financialriskmanagement.dto.FraudEventDTO;
import com.nickfallico.financialriskmanagement.dto.FraudHistoryResponseDTO;
import com.nickfallico.financialriskmanagement.eventstore.model.EventLog;
import com.nickfallico.financialriskmanagement.eventstore.repository.EventLogRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fraud Detection", description = "Fraud event history and monitoring")
public class FraudHistoryController {

    private final EventLogRepository eventLogRepository;
    private final MeterRegistry meterRegistry;

    @Operation(
        summary = "Get fraud history",
        description = "Retrieves paginated fraud event history for a user. Returns fraud detected events, cleared events, " +
            "blocked transactions, and high-risk alerts with aggregated statistics by event type and risk level."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fraud history retrieved successfully",
            content = @Content(schema = @Schema(implementation = FraudHistoryResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{userId}/fraud-history")
    public Mono<ResponseEntity<FraudHistoryResponseDTO>> getFraudHistory(
        @Parameter(description = "User ID", required = true) @PathVariable String userId,
        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "Filter by event type (FRAUD_DETECTED, FRAUD_CLEARED, TRANSACTION_BLOCKED, HIGH_RISK_ALERT)")
        @RequestParam(required = false) String eventType
    ) {
        log.info("Fetching fraud history for user: {}, page: {}, size: {}, eventType: {}",
                 userId, page, size, eventType);

        meterRegistry.counter("api.fraud_history.requests").increment();

        return eventLogRepository.findByAggregateIdAndAggregateTypeOrderBySequenceNumberAsc(userId, "USER")
            .filter(event -> isFraudRelatedEvent(event, eventType))
            .collectList()
            .doOnError(error -> {
                log.error("Failed to retrieve fraud history for user: {}", userId, error);
                meterRegistry.counter("api.fraud_history.errors").increment();
            })
            .map(allEvents -> {
                if (allEvents.isEmpty()) {
                    log.info("No fraud events found for user: {}", userId);
                    return ResponseEntity.ok(buildEmptyResponse(userId, page, size));
                }

                int totalEvents = allEvents.size();
                int totalPages = (int) Math.ceil((double) totalEvents / size);
                int startIndex = page * size;
                int endIndex = Math.min(startIndex + size, totalEvents);

                List<EventLog> pageEvents = (startIndex < totalEvents)
                    ? allEvents.subList(startIndex, endIndex)
                    : Collections.emptyList();

                List<FraudEventDTO> eventDTOs = pageEvents.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

                Map<String, Integer> eventCounts = calculateEventCounts(allEvents);
                Map<String, Integer> riskLevelCounts = calculateRiskLevelCounts(allEvents);

                FraudHistoryResponseDTO response = FraudHistoryResponseDTO.builder()
                    .userId(userId)
                    .totalFraudEvents(totalEvents)
                    .fraudDetectedCount(eventCounts.getOrDefault("FRAUD_DETECTED", 0))
                    .fraudClearedCount(eventCounts.getOrDefault("FRAUD_CLEARED", 0))
                    .transactionsBlockedCount(eventCounts.getOrDefault("TRANSACTION_BLOCKED", 0))
                    .eventsByRiskLevel(riskLevelCounts)
                    .events(eventDTOs)
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages(totalPages)
                    .totalElements((long) totalEvents)
                    .build();

                log.info("Retrieved {} fraud events for user: {} (page {}/{})",
                         eventDTOs.size(), userId, page, totalPages);

                return ResponseEntity.ok(response);
            });
    }

    private boolean isFraudRelatedEvent(EventLog event, String eventTypeFilter) {
        String type = event.getEventType();
        boolean isFraudEvent = "FRAUD_DETECTED".equals(type)
            || "FRAUD_CLEARED".equals(type)
            || "TRANSACTION_BLOCKED".equals(type)
            || "HIGH_RISK_ALERT".equals(type);

        if (!isFraudEvent) {
            return false;
        }

        if (eventTypeFilter != null && !eventTypeFilter.isEmpty()) {
            return type.equals(eventTypeFilter);
        }

        return true;
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

    private Map<String, Integer> calculateEventCounts(List<EventLog> events) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("FRAUD_DETECTED", 0);
        counts.put("FRAUD_CLEARED", 0);
        counts.put("TRANSACTION_BLOCKED", 0);
        counts.put("HIGH_RISK_ALERT", 0);

        for (EventLog event : events) {
            String type = event.getEventType();
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }

        return counts;
    }

    private Map<String, Integer> calculateRiskLevelCounts(List<EventLog> events) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("HIGH", 0);
        counts.put("MEDIUM", 0);
        counts.put("LOW", 0);

        for (EventLog event : events) {
            Map<String, Object> data = event.getEventData();
            if (data != null && data.containsKey("riskLevel")) {
                String riskLevel = data.get("riskLevel").toString();
                counts.put(riskLevel, counts.getOrDefault(riskLevel, 0) + 1);
            }
        }

        return counts;
    }

    private FraudHistoryResponseDTO buildEmptyResponse(String userId, int page, int size) {
        return FraudHistoryResponseDTO.builder()
            .userId(userId)
            .totalFraudEvents(0)
            .fraudDetectedCount(0)
            .fraudClearedCount(0)
            .transactionsBlockedCount(0)
            .eventsByRiskLevel(Map.of("HIGH", 0, "MEDIUM", 0, "LOW", 0))
            .events(Collections.emptyList())
            .currentPage(page)
            .pageSize(size)
            .totalPages(0)
            .totalElements(0L)
            .build();
    }
}