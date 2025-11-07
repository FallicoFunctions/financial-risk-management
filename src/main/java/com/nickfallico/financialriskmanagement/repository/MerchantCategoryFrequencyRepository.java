package com.nickfallico.financialriskmanagement.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import com.nickfallico.financialriskmanagement.model.MerchantCategoryFrequency;

import reactor.core.publisher.Mono;


/**
 * Repository for merchant category frequencies.
 * Separate aggregate; enables independent querying and caching.
 */
@Repository
public interface MerchantCategoryFrequencyRepository 
    extends R2dbcRepository<MerchantCategoryFrequency, String> {
    
    /**
     * Find frequency aggregate by user ID.
     */
    Mono<MerchantCategoryFrequency> findByUserId(String userId);
    
    /**
     * Delete all frequency data for user (useful for data cleanup).
     */
    Mono<Void> deleteByUserId(String userId);

    @Query("INSERT INTO merchant_category_frequency (frequency_id, user_id, category_frequencies, last_updated) " +
       "VALUES (:#{#freq.frequencyId}, :#{#freq.userId}, :#{#freq.categoryFrequencies}::jsonb, :#{#freq.lastUpdated}) " +
       "ON CONFLICT (user_id) DO UPDATE SET " +  // Changed from frequency_id
       "category_frequencies = :#{#freq.categoryFrequencies}::jsonb, " +
       "last_updated = :#{#freq.lastUpdated}")
    Mono<Void> upsert(MerchantCategoryFrequency freq);
}