package com.nickfallico.financialriskmanagement.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "transactions", 
    indexes = {
        @Index(name = "idx_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_merchant", columnList = "merchant_category"),
        @Index(name = "idx_user_country", columnList = "user_id, country"),
        @Index(name = "idx_created_at", columnList = "created_at")
    })
public class Transactions {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "User ID must not be blank")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotBlank(message = "Currency must not be blank")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    @Column(name = "merchant_category")
    private String merchantCategory;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "is_international")
    private Boolean isInternational;

    // ========== Geographic Location Fields ==========
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "country", length = 2)
    private String country; // ISO 3166-1 alpha-2 country code (e.g., "US", "GB")

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "ip_address", length = 45) // IPv6 max length
    private String ipAddress;

    // Enum for transaction types
    public enum TransactionType {
        PURCHASE,
        TRANSFER,
        WITHDRAWAL,
        DEPOSIT,
        REFUND
    }

    // Pre-persist method to set creation time
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
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
}