package com.nickfallico.financialriskmanagement.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.nickfallico.financialriskmanagement.model.Transaction.TransactionType;

@Data
@Builder
public class TransactionDTO {
    private UUID id;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;
    private TransactionType transactionType;
    private String merchantCategory;
    private String merchantName;
    private Boolean isInternational;
}