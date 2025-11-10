package com.nickfallico.financialriskmanagement.eventstore.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable event log entry for event sourcing.
 * Every domain event (transaction created, fraud detected, etc.) is stored here.
 * Enables complete audit trail, event replay, and compliance reporting.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("event_log")
public class EventLog {
    
    @Id
    @Column("event_id")
    private UUID eventId;
    
    @Column("event_type")
    private String eventType;  // TRANSACTION_CREATED, FRAUD_DETECTED, etc.
    
    @Column("aggregate_id")
    private String aggregateId;  // user_id or transaction_id
    
    @Column("aggregate_type")
    private String aggregateType;  // USER, TRANSACTION
    
    @Column("event_data")
    private Map<String, Object> eventData;  // Full event payload as JSONB
    
    @Column("metadata")
    private Map<String, Object> metadata;  // Kafka metadata (topic, partition, offset)
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("sequence_number")
    private Long sequenceNumber;  // For strict ordering
    
    @Column("version")
    private Integer version;  // Event schema version
    
    /**
     * Factory method: Create event log from domain event
     */
    public static EventLog fromDomainEvent(
        String eventType,
        String aggregateId,
        String aggregateType,
        Map<String, Object> eventData,
        Map<String, Object> metadata
    ) {
        return EventLog.builder()
            .eventId(UUID.randomUUID())
            .eventType(eventType)
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .eventData(eventData)
            .metadata(metadata)
            .createdAt(Instant.now())
            .version(1)
            .build();
    }
}