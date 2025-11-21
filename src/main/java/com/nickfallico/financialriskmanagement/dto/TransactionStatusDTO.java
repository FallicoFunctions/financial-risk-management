package com.nickfallico.financialriskmanagement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nickfallico.financialriskmanagement.model.Transactions.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatusDTO {

    // Transaction Details
    private UUID transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;
    private TransactionType transactionType;
    private String merchantCategory;
    private String merchantName;
    private Boolean isInternational;

    // Geographic Data
    private Double latitude;
    private Double longitude;
    private String country;
    private String city;
    private String ipAddress;

    // Fraud Detection Results
    private String fraudStatus;  // CLEAN, FLAGGED, BLOCKED, UNDER_REVIEW
    private Double fraudProbability;
    private String riskLevel;  // LOW, MEDIUM, HIGH
    private List<String> fraudReasons;
    private Integer violationCount;

    // Processing State
    private String processingStatus;  // PENDING, APPROVED, DECLINED, BLOCKED
    private Instant lastUpdated;
    private String reviewedBy;

    // Related Events
    private List<FraudEventDTO> fraudEvents;
}