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
@Constraint(validatedBy = MerchantNameValidator.class)
public @interface ValidMerchantName {
    String message() default "Invalid merchant name";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

class MerchantNameValidator implements ConstraintValidator<ValidMerchantName, String> {
    private static final int MAX_LENGTH = 100;
    private static final Pattern MERCHANT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 &'-]+$");

    @Override
    public boolean isValid(String merchantName, ConstraintValidatorContext context) {
        if (merchantName == null) return false;
        return merchantName.length() <= MAX_LENGTH && 
               MERCHANT_NAME_PATTERN.matcher(merchantName).matches();
    }
}