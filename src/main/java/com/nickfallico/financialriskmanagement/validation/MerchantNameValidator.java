package com.nickfallico.financialriskmanagement.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MerchantNameValidator implements ConstraintValidator<ValidMerchantName, String> {
    private static final int MAX_LENGTH = 100;
    private static final Pattern MERCHANT_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N} .,&'()#\\-+/]+$");

    @Override
    public boolean isValid(String merchantName, ConstraintValidatorContext context) {
        if (merchantName == null) return false;
        return merchantName.length() <= MAX_LENGTH && 
               MERCHANT_NAME_PATTERN.matcher(merchantName).matches();
    }
}