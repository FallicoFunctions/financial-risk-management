package com.nickfallico.financialriskmanagement.eventstore.model;

/**
 * All event types in the system.
 * Central registry for event sourcing.
 */
public enum EventType {
    // Transaction events
    TRANSACTION_CREATED("transaction.created"),
    TRANSACTION_BLOCKED("transaction.blocked"),
    
    // Fraud events
    FRAUD_DETECTED("fraud.detected"),
    FRAUD_CLEARED("fraud.cleared"),
    
    // User profile events
    USER_PROFILE_UPDATED("user.profile.updated"),
    HIGH_RISK_USER_IDENTIFIED("user.high-risk"),
    
    // System events
    EVENT_REPLAY_STARTED("system.replay.started"),
    EVENT_REPLAY_COMPLETED("system.replay.completed");
    
    private final String topicName;
    
    EventType(String topicName) {
        this.topicName = topicName;
    }
    
    public String getTopicName() {
        return topicName;
    }
    
    public static EventType fromTopicName(String topicName) {
        for (EventType type : values()) {
            if (type.topicName.equals(topicName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown topic: " + topicName);
    }

    /**
     * Parse event type from its string name (e.g., "TRANSACTION_CREATED").
     * Used when reading events from the event store.
     */
    public static EventType fromEventTypeName(String eventTypeName) {
        try {
            return EventType.valueOf(eventTypeName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown event type: " + eventTypeName);
        }
    }
}