package com.nickfallico.financialriskmanagement.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "transactions")
public class Transactions implements Persistable<UUID> {

    @Id
    private UUID id;

    @NotBlank(message = "User ID must not be blank")
    @Column("user_id")
    private String userId;

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Column("amount")
    private BigDecimal amount;

    @NotBlank(message = "Currency must not be blank")
    @Column("currency")
    private String currency;

    @Column("created_at")
    private Instant createdAt;

    @Column("transaction_type")
    private TransactionType transactionType;

    @Column("merchant_category")
    private String merchantCategory;

    @Column("merchant_name")
    private String merchantName;

    @Column("is_international")
    private Boolean isInternational;

    // ========== Geographic Location Fields ==========
    @Column("latitude")
    private Double latitude;

    @Column("longitude")
    private Double longitude;

    @Column("country")
    private String country; // ISO 3166-1 alpha-2 country code (e.g., "US", "GB")

    @Column("city")
    private String city;

    @Column("ip_address")
    private String ipAddress;

    // Enum for transaction types
    public enum TransactionType {
        PURCHASE,
        TRANSFER,
        WITHDRAWAL,
        DEPOSIT,
        REFUND
    }

    /**
     * Check if transaction has valid geographic coordinates
     */
    @JsonIgnore
    public boolean hasGeographicData() {
        return latitude != null && longitude != null;
    }

    /**
     * Calculate distance in kilometers between this transaction and another location
     * Uses Haversine formula for great-circle distance
     */
    @JsonIgnore
    public double distanceInKmTo(Double otherLat, Double otherLon) {
        if (!hasGeographicData() || otherLat == null || otherLon == null) {
            return 0.0;
        }

        final int EARTH_RADIUS_KM = 6371;

        double lat1Rad = Math.toRadians(latitude);
        double lat2Rad = Math.toRadians(otherLat);
        double deltaLat = Math.toRadians(otherLat - latitude);
        double deltaLon = Math.toRadians(otherLon - longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    // ========== Persistable Implementation ==========
    
    @Override
    @JsonIgnore
    public UUID getId() {
        return id;
    }

    @Override
    @JsonIgnore
    @Transient
    public boolean isNew() {
        return id == null;
    }
}