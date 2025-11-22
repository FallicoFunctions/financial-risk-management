package com.nickfallico.financialriskmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRuleDTO {
    private String ruleId;
    private String ruleName;
    private String description;
    private Double riskWeight;
    private Boolean isActive;
    private String category;  // VELOCITY, AMOUNT, LOCATION, BEHAVIOR
    private Integer triggerCount;  // How many times this rule was triggered
}