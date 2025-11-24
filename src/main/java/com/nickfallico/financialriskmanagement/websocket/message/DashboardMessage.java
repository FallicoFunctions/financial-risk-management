package com.nickfallico.financialriskmanagement.websocket.message;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all WebSocket dashboard messages.
 * Provides common fields for message identification and timing.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class DashboardMessage {

    /**
     * Message types for WebSocket streaming.
     */
    public enum MessageType {
        // Fraud alert types
        FRAUD_DETECTED,
        FRAUD_CLEARED,
        TRANSACTION_BLOCKED,

        // Transaction types
        TRANSACTION_CREATED,

        // Risk score types
        HIGH_RISK_USER_IDENTIFIED,
        USER_PROFILE_UPDATED,

        // Metrics types
        METRICS_SNAPSHOT
    }

    /**
     * The type of message being sent.
     */
    private MessageType messageType;

    /**
     * Timestamp when the original event occurred.
     */
    private Instant eventTimestamp;

    /**
     * Timestamp when this message was published to WebSocket.
     */
    private Instant publishedAt;
}
