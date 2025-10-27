package com.nickfallico.financialriskmanagement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.nickfallico.financialriskmanagement.model.Transaction.TransactionType;
import com.nickfallico.financialriskmanagement.validation.ValidMerchantCategory;

@Data
@Builder
public class TransactionDTO {
    private UUID id;

    @NotBlank(message = "User ID must not be blank")
    private String userId;

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "1000000", message = "Amount exceeds maximum limit")
    private BigDecimal amount;

    @NotBlank(message = "Currency must not be blank")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;

    private Instant createdAt;

    @NotNull(message = "Transaction type must be specified")
    private TransactionType transactionType;

    @Size(max = 100, message = "Merchant category must be less than 100 characters")
    @ValidMerchantCategory(message = "Merchant category is not supported")
    private String merchantCategory;

    @Size(max = 200, message = "Merchant name must be less than 200 characters")
    private String merchantName;

    private Boolean isInternational;
}