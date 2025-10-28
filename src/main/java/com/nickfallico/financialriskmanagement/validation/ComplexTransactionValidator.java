package com.nickfallico.financialriskmanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class ComplexTransactionValidator implements ConstraintValidator<ValidTransaction, Object> {
    private static final List<String> HIGH_RISK_CATEGORIES = Arrays.asList(
        "GAMBLING", "CRYPTO", "ADULT_ENTERTAINMENT"
    );

    private static final BigDecimal HIGH_RISK_MAX_AMOUNT = BigDecimal.valueOf(5000);
    private static final BigDecimal STANDARD_MAX_AMOUNT = BigDecimal.valueOf(25000);

    @Override
    public boolean isValid(Object transactionDto, ConstraintValidatorContext context) {
        try {
            String merchantCategory = (String) transactionDto.getClass()
                .getMethod("getMerchantCategory")
                .invoke(transactionDto);
            
            BigDecimal amount = (BigDecimal) transactionDto.getClass()
                .getMethod("getAmount")
                .invoke(transactionDto);
            
            Boolean isInternational = (Boolean) transactionDto.getClass()
                .getMethod("getIsInternational")
                .invoke(transactionDto);

            if (HIGH_RISK_CATEGORIES.contains(merchantCategory)) {
                if (amount.compareTo(HIGH_RISK_MAX_AMOUNT) > 0) {
                    return false;
                }
            } else {
                if (amount.compareTo(STANDARD_MAX_AMOUNT) > 0) {
                    return false;
                }
            }

            if (Boolean.TRUE.equals(isInternational)) {
                return amount.compareTo(BigDecimal.valueOf(10000)) <= 0;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}