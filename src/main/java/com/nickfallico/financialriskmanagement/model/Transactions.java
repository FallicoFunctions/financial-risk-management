package com.nickfallico.financialriskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    public boolean hasGeographicData() {
        return latitude != null && longitude != null;
    }

    /**
     * Calculate distance in kilometers between this transaction and another location
     * Uses Haversine formula for great-circle distance
     */
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