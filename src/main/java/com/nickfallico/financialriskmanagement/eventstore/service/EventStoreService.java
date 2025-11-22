package com.nickfallico.financialriskmanagement.eventstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickfallico.financialriskmanagement.eventstore.model.EventLog;
import com.nickfallico.financialriskmanagement.eventstore.model.EventType;
import com.nickfallico.financialriskmanagement.eventstore.repository.EventLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event Store Service - Core of Event Sourcing architecture.
 * 
 * Responsibilities:
 * - Store all domain events immutably
 * - Retrieve event history for aggregates
 * - Provide time-travel queries
 * - Generate sequence numbers for strict ordering
 * 
 * Thread-safe and reactive.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventStoreService {
    
    private final EventLogRepository eventLogRepository;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    
    /**
     * Store a domain event in the event log.
     * Returns the stored event with sequence number assigned.
     */
    public Mono<EventLog> storeEvent(
        EventType eventType,
        String aggregateId,
        String aggregateType,
        Object eventPayload,
        Map<String, Object> metadata
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return getNextSequenceNumber()
            .flatMap(sequenceNumber -> {
                // Convert event payload to Map
                Map<String, Object> eventData = objectMapper.convertValue(
                    eventPayload, 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
                );
                
                EventLog eventLog = EventLog.builder()
                    .eventId(UUID.randomUUID())
                    .eventType(eventType.name())
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventData(eventData)
                    .metadata(metadata != null ? metadata : new HashMap<>())
                    .createdAt(Instant.now())
                    .sequenceNumber(sequenceNumber)
                    .version(1)
                    .build();
                
                return eventLogRepository.insertEvent(eventLog);
            })
            .doOnSuccess(savedEvent -> {
                sample.stop(meterRegistry.timer("event_store_write_time", 
                    "event_type", eventType.name()));
                
                meterRegistry.counter("events_stored_total", 
                    "event_type", eventType.name(),
                    "aggregate_type", aggregateType)
                    .increment();
                
                log.debug("Stored event: type={}, aggregateId={}, sequenceNumber={}", 
                    eventType.name(), aggregateId, savedEvent.getSequenceNumber());
            })
            .doOnError(error -> {
                log.error("Failed to store event: type={}, aggregateId={}", 
                    eventType.name(), aggregateId, error);
                
                meterRegistry.counter("event_store_errors_total",
                    "event_type", eventType.name())
                    .increment();
            });
    }

    /**
     * Convenience wrapper to publish an event using simple string parameters.
     * Resolves the {@link EventType} from its name and delegates to {@link #storeEvent}.
     */
    public Mono<EventLog> publishEvent(
        String aggregateId,
        String aggregateType,
        String eventTypeName,
        Map<String, Object> eventData,
        Map<String, Object> metadata
    ) {
        EventType eventType;
        try {
            eventType = EventType.valueOf(eventTypeName);
        } catch (IllegalArgumentException ex) {
            log.error("Attempted to publish unknown event type: {}", eventTypeName, ex);
            return Mono.error(ex);
        }

        return storeEvent(
            eventType,
            aggregateId,
            aggregateType,
            eventData,
            metadata != null ? metadata : new HashMap<>()
        );
    }
    
    /**
     * Get complete event history for an aggregate (user or transaction).
     * Returns events in chronological order.
     */
    public Flux<EventLog> getAggregateHistory(String aggregateId, String aggregateType) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return eventLogRepository.findByAggregateIdAndAggregateTypeOrderBySequenceNumberAsc(
            aggregateId, 
            aggregateType
        )
        .doFinally(signalType -> {
            sample.stop(meterRegistry.timer("event_store_read_time",
                "aggregate_type", aggregateType));
            
            meterRegistry.counter("event_history_queries_total",
                "aggregate_type", aggregateType)
                .increment();
        });
    }
    
    /**
     * Get event history for a user.
     * Convenience method for common use case.
     */
    public Flux<EventLog> getUserEventHistory(String userId) {
        return getAggregateHistory(userId, "USER");
    }
    
    /**
     * Get event history for a transaction.
     */
    public Flux<EventLog> getTransactionEventHistory(UUID transactionId) {
        return getAggregateHistory(transactionId.toString(), "TRANSACTION");
    }
    
    /**
     * Time-travel query: Get aggregate state as of a specific timestamp.
     * Returns all events up to that point in time.
     */
    public Flux<EventLog> getAggregateHistoryAsOf(
        String aggregateId, 
        String aggregateType, 
        Instant asOfTime
    ) {
        return eventLogRepository.findByAggregateAsOf(aggregateId, aggregateType, asOfTime)
            .doOnSubscribe(sub -> 
                log.debug("Time-travel query: aggregateId={}, asOf={}", aggregateId, asOfTime)
            );
    }
    
    /**
     * Get all events of a specific type.
     * Useful for analytics and reporting.
     */
    public Flux<EventLog> getEventsByType(EventType eventType) {
        return eventLogRepository.findByEventTypeOrderByCreatedAtDesc(eventType.name());
    }
    
    /**
     * Get events within a time range.
     * Useful for auditing and compliance reporting.
     */
    public Flux<EventLog> getEventsInTimeRange(Instant start, Instant end) {
        return eventLogRepository.findByCreatedAtBetweenOrderBySequenceNumberAsc(start, end)
            .doOnSubscribe(sub -> 
                log.debug("Querying events in range: {} to {}", start, end)
            );
    }
    
    /**
     * Count events by type.
     * Useful for metrics and dashboards.
     */
    public Mono<Long> countEventsByType(EventType eventType) {
        return eventLogRepository.countByEventType(eventType.name());
    }
    
    /**
     * Get all events for full system replay.
     * Returns events in batches to avoid memory issues.
     * WARNING: Can be expensive on large datasets.
     */
    public Flux<EventLog> getAllEventsForReplay(int batchSize, long offset) {
        return eventLogRepository.findAllForReplay(batchSize, offset)
            .doOnSubscribe(sub -> 
                log.info("Starting event replay: batchSize={}, offset={}", batchSize, offset)
            );
    }
    
    /**
     * Generate next sequence number atomically.
     * Ensures strict ordering of events.
     */
    private Mono<Long> getNextSequenceNumber() {
        return eventLogRepository.getMaxSequenceNumber()
            .map(maxSeq -> maxSeq + 1)
            .defaultIfEmpty(1L);
    }
    
    /**
     * Helper: Create Kafka metadata map
     */
    public static Map<String, Object> createKafkaMetadata(
        String topic,
        int partition,
        long offset
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("kafka_topic", topic);
        metadata.put("kafka_partition", partition);
        metadata.put("kafka_offset", offset);
        metadata.put("source", "kafka");
        return metadata;
    }
}
