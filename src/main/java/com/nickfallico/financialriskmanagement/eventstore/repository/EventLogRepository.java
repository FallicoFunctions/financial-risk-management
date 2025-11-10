package com.nickfallico.financialriskmanagement.eventstore.repository;

import com.nickfallico.financialriskmanagement.eventstore.model.EventLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository for event log storage.
 * All queries return events in chronological order (sequence_number).
 */
@Repository
public interface EventLogRepository extends R2dbcRepository<EventLog, UUID> {
    
    /**
     * Get all events for a specific aggregate (user or transaction)
     * Ordered by sequence number for replay.
     */
    Flux<EventLog> findByAggregateIdAndAggregateTypeOrderBySequenceNumberAsc(
        String aggregateId, 
        String aggregateType
    );
    
    /**
     * Get events by type (e.g., all FRAUD_DETECTED events)
     */
    Flux<EventLog> findByEventTypeOrderByCreatedAtDesc(String eventType);
    
    /**
     * Get events within a time range
     */
    Flux<EventLog> findByCreatedAtBetweenOrderBySequenceNumberAsc(
        Instant start, 
        Instant end
    );
    
    /**
     * Get all events for a user up to a specific timestamp (time-travel query)
     */
    @Query("""
        SELECT * FROM event_log 
        WHERE aggregate_id = :aggregateId 
          AND aggregate_type = :aggregateType 
          AND created_at <= :asOfTime 
        ORDER BY sequence_number ASC
    """)
    Flux<EventLog> findByAggregateAsOf(
        String aggregateId, 
        String aggregateType, 
        Instant asOfTime
    );
    
    /**
     * Get the latest sequence number (for generating next sequence)
     */
    @Query("SELECT COALESCE(MAX(sequence_number), 0) FROM event_log")
    Mono<Long> getMaxSequenceNumber();
    
    /**
     * Count events by type (useful for metrics)
     */
    Mono<Long> countByEventType(String eventType);
    
    /**
     * Get all events for replay (entire system rebuild)
     * WARNING: This can be expensive on large datasets
     */
    @Query("""
        SELECT * FROM event_log 
        ORDER BY sequence_number ASC 
        LIMIT :batchSize OFFSET :offset
    """)
    Flux<EventLog> findAllForReplay(int batchSize, long offset);
}