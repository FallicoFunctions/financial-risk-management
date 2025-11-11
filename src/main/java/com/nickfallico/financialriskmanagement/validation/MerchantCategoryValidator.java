package com.nickfallico.financialriskmanagement.validation;

import java.util.List;
import java.util.Locale;

import com.nickfallico.financialriskmanagement.constants.MerchantCategories;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MerchantCategoryValidator implements ConstraintValidator<ValidMerchantCategory, String> {
    private static final List<String> VALID_CATEGORIES = MerchantCategories.VALID_CATEGORIES;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // allow null/blank
        return VALID_CATEGORIES.contains(value.toUpperCase(Locale.ROOT));
    }
}