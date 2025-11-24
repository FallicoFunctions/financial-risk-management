package com.nickfallico.financialriskmanagement.websocket.message;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * WebSocket message for transaction events.
 * Used for real-time transaction feed on dashboard.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransactionEventMessage extends DashboardMessage {

    // Transaction identification
    private UUID transactionId;
    private String userId;

    // Transaction details
    private BigDecimal amount;
    private String currency;
    private String transactionType;

    // Merchant information
    private String merchantCategory;
    private String merchantName;

    // Geographic information
    private Boolean isInternational;
    private String country;
    private String city;
}
