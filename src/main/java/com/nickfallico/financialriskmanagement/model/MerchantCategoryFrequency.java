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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable aggregate for tracking merchant category frequencies per user.
 * Persisted separately from ImmutableUserRiskProfile to avoid @Transient fields.
 * All state is immutable; updates create new instances.
 */
@Entity
@jakarta.persistence.Table(name = "merchant_category_frequency")
@Table("merchant_category_frequency")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantCategoryFrequency {
    
    @jakarta.persistence.Id
    @Id
    private String frequencyId;
    
    @Column(nullable = false)
    private String userId;
    
    // Serialized as JSONB in DB; immutable Map
    @Column(columnDefinition = "JSONB")
    private Map<String, Integer> categoryFrequencies;
    
    @Column(nullable = false)
    private Instant lastUpdated;
    
    /**
     * Factory method: Create new frequency aggregate for user
     */
    public static MerchantCategoryFrequency createNew(String userId) {
        return MerchantCategoryFrequency.builder()
            .frequencyId(UUID.randomUUID().toString())
            .userId(userId)
            .categoryFrequencies(new HashMap<>())
            .lastUpdated(Instant.now())
            .build();
    }
    
    /**
     * Functional update: Increment category count (returns new instance)
     */
    public MerchantCategoryFrequency incrementCategory(String category) {
        Map<String, Integer> updated = new HashMap<>(this.categoryFrequencies);
        updated.merge(category, 1, Integer::sum);
        
        return this.toBuilder()
            .categoryFrequencies(updated)
            .lastUpdated(Instant.now())
            .build();
    }
    
    /**
     * Get frequency for a specific category
     */
    public int getFrequency(String category) {
        return categoryFrequencies.getOrDefault(category, 0);
    }
    
    /**
     * Get count of unique categories used
     */
    @JsonIgnore
    public int getUniqueCategoryCount() {
        return categoryFrequencies.size();
    }
    
    /**
     * Check if category is commonly used by this user
     */
    public boolean isCategoryCommon(String category) {
        return getFrequency(category) >= 3;
    }
    
    /**
     * Get immutable view of frequencies
     */
    @JsonIgnore
    public Map<String, Integer> getFrequenciesImmutable() {
        return Collections.unmodifiableMap(categoryFrequencies);
    }
}