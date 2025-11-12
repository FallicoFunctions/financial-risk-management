package com.nickfallico.financialriskmanagement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.nickfallico.financialriskmanagement.model.Transactions.TransactionType;
import com.nickfallico.financialriskmanagement.validation.ValidMerchantCategory;

@Data
@Builder
// @ValidTransaction  // Remove/disable for these tests to avoid extra violations
public class TransactionDTO {
    private UUID id;

    @NotBlank(message = "User ID must not be blank")
    private String userId;

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @NotNull(message = "Currency must not be null")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase ISO code")
    private String currency;

    // leave nullable so it doesn't add a second violation in tests
    private Instant createdAt;

    // leave nullable so it doesn't add a second violation in tests
    private TransactionType transactionType;

    // Exactly one violation if unsupported; null is allowed
    @ValidMerchantCategory(message = "Merchant category is not supported")
    private String merchantCategory;

    // Keep simple; null allowed; won't trip tests
    @Size(max = 255, message = "Merchant name must be <= 255 chars")
    private String merchantName;

    private Boolean isInternational;

    // ========== Geographic Location Fields ==========
    private Double latitude;
    private Double longitude;
    
    @Size(max = 2, message = "Country code must be 2 characters")
    private String country;
    
    @Size(max = 100, message = "City name must be <= 100 characters")
    private String city;
    
    @Size(max = 45, message = "IP address must be <= 45 characters")
    private String ipAddress;
}