package com.nickfallico.financialriskmanagement.repository;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import com.nickfallico.financialriskmanagement.model.ImmutableUserRiskProfile;

import reactor.core.publisher.Mono;

@Repository
public interface ImmutableUserRiskProfileRepository 
    extends R2dbcRepository<ImmutableUserRiskProfile, String> {
        @Query("INSERT INTO user_risk_profiles " +
           "(user_id, average_transaction_amount, total_transactions, total_transaction_value, " +
           "high_risk_transactions, international_transactions, behavioral_risk_score, " +
           "transaction_risk_score, overall_risk_score, first_transaction_date, last_transaction_date) " +
           "VALUES (:#{#profile.userId}, :#{#profile.averageTransactionAmount}, :#{#profile.totalTransactions}, " +
           ":#{#profile.totalTransactionValue}, :#{#profile.highRiskTransactions}, " +
           ":#{#profile.internationalTransactions}, :#{#profile.behavioralRiskScore}, " +
           ":#{#profile.transactionRiskScore}, :#{#profile.overallRiskScore}, " +
           ":#{#profile.firstTransactionDate}, :#{#profile.lastTransactionDate}) " +
           "ON CONFLICT (user_id) DO UPDATE SET " +
           "average_transaction_amount = :#{#profile.averageTransactionAmount}, " +
           "total_transactions = :#{#profile.totalTransactions}, " +
           "total_transaction_value = :#{#profile.totalTransactionValue}, " +
           "high_risk_transactions = :#{#profile.highRiskTransactions}, " +
           "international_transactions = :#{#profile.internationalTransactions}, " +
           "behavioral_risk_score = :#{#profile.behavioralRiskScore}, " +
           "transaction_risk_score = :#{#profile.transactionRiskScore}, " +
           "overall_risk_score = :#{#profile.overallRiskScore}, " +
           "last_transaction_date = :#{#profile.lastTransactionDate}")
        Mono<Void> upsert(ImmutableUserRiskProfile profile);
}