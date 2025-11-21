package com.nickfallico.financialriskmanagement.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table("merchant_category_frequency")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantCategoryFrequency {
    
    @Id
    private String frequencyId;
    
    private String userId;
    
    private Map<String, Integer> categoryFrequencies;
    
    private Instant lastUpdated;
    
    public static MerchantCategoryFrequency createNew(String userId) {
        return MerchantCategoryFrequency.builder()
            .frequencyId(UUID.randomUUID().toString())
            .userId(userId)
            .categoryFrequencies(new HashMap<>())
            .lastUpdated(Instant.now())
            .build();
    }
    
    public MerchantCategoryFrequency incrementCategory(String category) {
        Map<String, Integer> updated = new HashMap<>(this.categoryFrequencies);
        updated.merge(category, 1, Integer::sum);
        
        return this.toBuilder()
            .categoryFrequencies(updated)
            .lastUpdated(Instant.now())
            .build();
    }
    
    public int getFrequency(String category) {
        return categoryFrequencies.getOrDefault(category, 0);
    }
    
    @JsonIgnore
    public int getUniqueCategoryCount() {
        return categoryFrequencies.size();
    }
    
    public boolean isCategoryCommon(String category) {
        return getFrequency(category) >= 3;
    }
    
    @JsonIgnore
    public Map<String, Integer> getFrequenciesImmutable() {
        return Collections.unmodifiableMap(categoryFrequencies);
    }
}