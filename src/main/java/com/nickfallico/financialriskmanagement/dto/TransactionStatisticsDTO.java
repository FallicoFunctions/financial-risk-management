package com.nickfallico.financialriskmanagement.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for detailed transaction statistics.
 * Provides granular breakdown of user transaction patterns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatisticsDTO {
    
    // Overall statistics
    private Integer totalTransactions;
    private BigDecimal totalValue;
    private BigDecimal averageAmount;
    private BigDecimal medianAmount;
    
    // Transaction breakdown by type
    private Map<String, Integer> transactionsByType;
    
    // Merchant category breakdown
    private Map<String, Integer> transactionsByMerchantCategory;
    
    // Geographic breakdown
    private Integer domesticTransactions;
    private Integer internationalTransactions;
    private Map<String, Integer> transactionsByCountry;
    
    // Risk metrics
    private Integer highRiskTransactions;
    private Double highRiskPercentage;
    
    // Recent activity
    private Integer transactionsLast7Days;
    private Integer transactionsLast30Days;
    private BigDecimal valueLast7Days;
    private BigDecimal valueLast30Days;
}