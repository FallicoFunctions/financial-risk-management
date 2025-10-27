package com.nickfallico.financialriskmanagement.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.risk")
@Data
public class RiskAssessmentConfig {
    private double highRiskThreshold = 10000.0;
    private List<String> highRiskMerchantCategories = 
        Arrays.asList("GAMBLING", "CRYPTO");
}