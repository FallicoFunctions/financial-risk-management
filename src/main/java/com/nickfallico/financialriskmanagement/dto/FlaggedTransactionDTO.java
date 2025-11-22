package com.nickfallico.financialriskmanagement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaggedTransactionDTO {
    private UUID transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String merchantCategory;
    private String merchantName;
    private Instant flaggedAt;
    private Double fraudProbability;
    private String riskLevel;
    private List<String> fraudReasons;
    private String reviewStatus;  // PENDING, APPROVED, REJECTED
    private Integer daysSinceFlagged;
}