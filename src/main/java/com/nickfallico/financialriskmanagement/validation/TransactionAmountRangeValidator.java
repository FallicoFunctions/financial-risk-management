package com.nickfallico.financialriskmanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class TransactionAmountRangeValidator implements ConstraintValidator<ValidTransactionAmount, BigDecimal> {
    private double min;
    private double max;

    @Override
    public void initialize(ValidTransactionAmount constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return value.doubleValue() >= min && value.doubleValue() <= max;
    }
}