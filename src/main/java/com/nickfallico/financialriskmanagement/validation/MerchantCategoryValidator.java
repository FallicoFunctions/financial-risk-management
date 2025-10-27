package com.nickfallico.financialriskmanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class MerchantCategoryValidator implements ConstraintValidator<ValidMerchantCategory, String> {
    private static final List<String> VALID_CATEGORIES = Arrays.asList(
        "GROCERIES", "ELECTRONICS", "TRAVEL", "DINING", 
        "ENTERTAINMENT", "UTILITIES", "TRANSPORTATION", 
        "GAMBLING", "CRYPTO", "ONLINE_SHOPPING"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || VALID_CATEGORIES.contains(value);
    }
}