package com.nickfallico.financialriskmanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MerchantCategoryValidator implements ConstraintValidator<ValidMerchantCategory, String> {
    private static final List<String> VALID_CATEGORIES = Arrays.asList(
        "GROCERIES", "ELECTRONICS", "TRAVEL", "DINING", 
        "ENTERTAINMENT", "UTILITIES", "TRANSPORTATION", 
        "GAMBLING", "CRYPTO", "ONLINE_SHOPPING"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // allow null/blank
        return VALID_CATEGORIES.contains(value.toUpperCase(Locale.ROOT));
    }
}