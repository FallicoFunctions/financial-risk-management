package com.nickfallico.financialriskmanagement.repository;

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
}