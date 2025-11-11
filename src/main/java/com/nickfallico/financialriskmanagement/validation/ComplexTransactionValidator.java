package com.nickfallico.financialriskmanagement.validation;

import java.math.BigDecimal;
import java.util.List;

import com.nickfallico.financialriskmanagement.constants.MerchantCategories;
import com.nickfallico.financialriskmanagement.dto.TransactionDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ComplexTransactionValidator implements ConstraintValidator<ValidTransaction, TransactionDTO> {

    private static final List<String> HIGH_RISK_CATEGORIES = MerchantCategories.HIGH_RISK_CATEGORIES;

    // mirror the allowed list used by @ValidMerchantCategory (keep in sync)
    private static final List<String> VALID_CATEGORIES = MerchantCategories.VALID_CATEGORIES;

    private static final BigDecimal BASIC_MIN = new BigDecimal("0.01");
    private static final BigDecimal BASIC_MAX = new BigDecimal("1000000");
    private static final BigDecimal HIGH_RISK_MAX_AMOUNT = new BigDecimal("5000");
    private static final BigDecimal STANDARD_MAX_AMOUNT = new BigDecimal("25000");
    private static final BigDecimal INTERNATIONAL_MAX_AMOUNT = new BigDecimal("10000");

    @Override
    public boolean isValid(TransactionDTO dto, ConstraintValidatorContext context) {
        if (dto == null) return true;

        BigDecimal amount = dto.getAmount();
        if (amount == null) return true;

        // If amount violates the field-level range, let that annotation report it
        if (amount.compareTo(BASIC_MIN) < 0 || amount.compareTo(BASIC_MAX) > 0) {
            return true;
        }

        String category = dto.getMerchantCategory();
        String catUpper = category == null ? null : category.toUpperCase();

        // If the category itself is invalid, defer to @ValidMerchantCategory (avoid dup)
        if (catUpper != null && !VALID_CATEGORIES.contains(catUpper)) {
            return true;
        }

        // Business caps by risk category
        if (catUpper != null && HIGH_RISK_CATEGORIES.contains(catUpper)) {
            if (amount.compareTo(HIGH_RISK_MAX_AMOUNT) > 0) return false;
        } else {
            if (amount.compareTo(STANDARD_MAX_AMOUNT) > 0) return false;
        }

        // International cap
        if (Boolean.TRUE.equals(dto.getIsInternational())) {
            if (amount.compareTo(INTERNATIONAL_MAX_AMOUNT) > 0) return false;
        }

        return true;
    }

}
