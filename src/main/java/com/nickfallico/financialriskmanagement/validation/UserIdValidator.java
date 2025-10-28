package com.nickfallico.financialriskmanagement.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UserIdValidator implements ConstraintValidator<ValidUserId, String> {
    // Regex for alphanumeric user IDs, optional prefixes
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^(user_|acct_)?[a-zA-Z0-9]{5,50}$");

    @Override
    public boolean isValid(String userId, ConstraintValidatorContext context) {
        if (userId == null) return false;
        return USER_ID_PATTERN.matcher(userId).matches();
    }
}