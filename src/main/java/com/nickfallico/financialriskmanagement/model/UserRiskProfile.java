package com.nickfallico.financialriskmanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@jakarta.persistence.Table(name = "user_risk_profiles")
@Table("user_risk_profiles")
public class UserRiskProfile {

    @jakarta.persistence.Id
    @Id
    private String userId;

    private double averageTransactionAmount;
    private int totalTransactions;
    private double totalTransactionValue;
    private int highRiskTransactions;
    private int internationalTransactions;

    private double behavioralRiskScore;
    private double transactionRiskScore;
    private double overallRiskScore;

    private Instant firstTransactionDate;
    private Instant lastTransactionDate;

    @ElementCollection
    @CollectionTable(name = "merchant_category_frequency", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "merchant_category")
    @Column(name = "frequency")
    @Builder.Default
    @Transient // <-- Spring Data should ignore this field
    private Map<String, Integer> merchantCategoryFrequency = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "merchant_category_risk_scores", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "merchant_category")
    @Column(name = "risk_score")
    @Builder.Default
    @Transient // <-- Spring Data should ignore this field
    private Map<String, Double> merchantCategoryRiskScores = new HashMap<>();
}
