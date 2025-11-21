package com.nickfallico.financialriskmanagement.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEventDTO {
    private UUID eventId;
    private String eventType;
    private Long sequenceNumber;
    private String aggregateId;
    private String aggregateType;
    private Map<String, Object> eventData;
    private Instant createdAt;
    private UUID transactionId;
    private String amount;
    private String currency;
    private Double fraudProbability;
    private String riskLevel;
    private String reason;
    private String merchantCategory;
}