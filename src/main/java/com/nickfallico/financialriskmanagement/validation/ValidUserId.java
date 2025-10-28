package com.nickfallico.financialriskmanagement.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserIdValidator.class)
public @interface ValidUserId {
    String message() default "Invalid user ID format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

class UserIdValidator implements ConstraintValidator<ValidUserId, String> {
    // Regex for alphanumeric user IDs, optional prefixes
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^(user_|acct_)?[a-zA-Z0-9]{5,50}$");

    @Override
    public boolean isValid(String userId, ConstraintValidatorContext context) {
        if (userId == null) return false;
        return USER_ID_PATTERN.matcher(userId).matches();
    }
}