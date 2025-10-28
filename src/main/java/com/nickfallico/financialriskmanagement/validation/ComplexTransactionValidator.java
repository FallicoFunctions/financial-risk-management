package com.nickfallico.financialriskmanagement.validation;

import com.nickfallico.financialriskmanagement.dto.TransactionDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class ComplexTransactionValidator implements ConstraintValidator<ValidTransaction, TransactionDTO> {

    private static final List<String> HIGH_RISK_CATEGORIES = Arrays.asList(
        "GAMBLING", "CRYPTO", "ADULT_ENTERTAINMENT"
    );

    private static final BigDecimal HIGH_RISK_MAX_AMOUNT = BigDecimal.valueOf(5000);
    private static final BigDecimal STANDARD_MAX_AMOUNT = BigDecimal.valueOf(25000);
    private static final BigDecimal INTERNATIONAL_MAX_AMOUNT = BigDecimal.valueOf(10000);

    @Override
    public boolean isValid(TransactionDTO dto, ConstraintValidatorContext context) {
        // If there's no DTO at all, don't complain here.
        if (dto == null) {
            return true;
        }

        BigDecimal amount = dto.getAmount();
        String category = dto.getMerchantCategory();
        Boolean isInternational = dto.getIsInternational();

        // If amount is missing or otherwise already invalid,
        // let the field-level annotations (@NotNull, @ValidTransactionAmount)
        // generate the violations. We stay quiet.
        if (amount == null) {
            return true;
        }

        // ----- Rule 1: cap by merchant risk -----
        // If category is high-risk, amount must be <= 5000.
        // Otherwise amount must be <= 25000.
        if (category != null && HIGH_RISK_CATEGORIES.contains(category.toUpperCase())) {
            if (amount.compareTo(HIGH_RISK_MAX_AMOUNT) > 0) {
                return false;
            }
        } else {
            if (amount.compareTo(STANDARD_MAX_AMOUNT) > 0) {
                return false;
            }
        }

        // ----- Rule 2: international cap -----
        // If it's marked international, cap at 10000.
        if (Boolean.TRUE.equals(isInternational)) {
            if (amount.compareTo(INTERNATIONAL_MAX_AMOUNT) > 0) {
                return false;
            }
        }

        // If we didn't trip any custom business rule, it's valid.
        return true;
    }
}
