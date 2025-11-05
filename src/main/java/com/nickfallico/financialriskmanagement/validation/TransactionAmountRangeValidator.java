package com.nickfallico.financialriskmanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class TransactionAmountRangeValidator
        implements ConstraintValidator<ValidTransactionAmount, BigDecimal> {

    private BigDecimal min;
    private BigDecimal max;

    @Override
    public void initialize(ValidTransactionAmount constraintAnnotation) {
        this.min = BigDecimal.valueOf(constraintAnnotation.min());
        this.max = BigDecimal.valueOf(constraintAnnotation.max());
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Defer null handling to @NotNull so you get exactly one violation
        if (value == null) return true;

        // Inclusive range check: min ≤ value ≤ max
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
