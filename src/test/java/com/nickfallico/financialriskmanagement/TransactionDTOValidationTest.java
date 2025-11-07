package com.nickfallico.financialriskmanagement;

import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import com.nickfallico.financialriskmanagement.model.Transactions.TransactionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validTransaction() {
        TransactionDTO transaction = TransactionDTO.builder()
            .userId("user123")
            .amount(BigDecimal.valueOf(100.00))
            .currency("USD")
            .transactionType(TransactionType.PURCHASE)
            .merchantCategory("GROCERIES")
            .build();

        Set<ConstraintViolation<TransactionDTO>> violations = validator.validate(transaction);
        assertTrue(violations.isEmpty(), "Valid transaction should have no violations");
    }

    @Test
    void invalidUserId() {
        TransactionDTO transaction = TransactionDTO.builder()
            .userId("")
            .amount(BigDecimal.valueOf(100.00))
            .currency("USD")
            .transactionType(TransactionType.PURCHASE)
            .merchantCategory("GROCERIES")
            .build();

        Set<ConstraintViolation<TransactionDTO>> violations = validator.validate(transaction);
        assertEquals(1, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("User ID must not be blank")));
    }

    @Test
    void invalidAmount() {
        TransactionDTO transaction = TransactionDTO.builder()
            .userId("user123")
            .amount(BigDecimal.valueOf(0))
            .currency("USD")
            .transactionType(TransactionType.PURCHASE)
            .merchantCategory("GROCERIES")
            .build();

        Set<ConstraintViolation<TransactionDTO>> violations = validator.validate(transaction);
        assertEquals(1, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Amount must be at least 0.01")));
    }

    @Test
    void invalidMerchantCategory() {
        TransactionDTO transaction = TransactionDTO.builder()
            .userId("user123")
            .amount(BigDecimal.valueOf(100.00))
            .currency("USD")
            .transactionType(TransactionType.PURCHASE)
            .merchantCategory("INVALID_CATEGORY")
            .build();

        Set<ConstraintViolation<TransactionDTO>> violations = validator.validate(transaction);
        assertEquals(1, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Merchant category is not supported")));
    }
}