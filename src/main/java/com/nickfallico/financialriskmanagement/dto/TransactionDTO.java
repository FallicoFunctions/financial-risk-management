package com.nickfallico.financialriskmanagement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.nickfallico.financialriskmanagement.model.Transaction.TransactionType;
import com.nickfallico.financialriskmanagement.validation.*;

@Data
@Builder
public class TransactionDTO {
    private UUID id;

    @ValidUserId
    private String userId;

    @NotNull(message = "Amount must not be null")
    @ValidTransactionAmount(
        message = "Transaction amount must be between $0.01 and $1,000,000",
        min = 0.01, 
        max = 1000000.0
    )
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

    @ValidMerchantName
    private String merchantName;

    private Boolean isInternational;
}