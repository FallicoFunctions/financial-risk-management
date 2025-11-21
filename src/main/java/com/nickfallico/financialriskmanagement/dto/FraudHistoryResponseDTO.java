package com.nickfallico.financialriskmanagement.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudHistoryResponseDTO {
    private String userId;
    private Integer totalFraudEvents;
    private Integer fraudDetectedCount;
    private Integer fraudClearedCount;
    private Integer transactionsBlockedCount;
    private Map<String, Integer> eventsByRiskLevel;
    private List<FraudEventDTO> events;
    private Integer currentPage;
    private Integer pageSize;
    private Integer totalPages;
    private Long totalElements;
}