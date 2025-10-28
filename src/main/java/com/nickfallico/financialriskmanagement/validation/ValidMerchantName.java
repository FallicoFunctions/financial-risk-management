package com.nickfallico.financialriskmanagement.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MerchantNameValidator.class)
public @interface ValidMerchantName {
    String message() default "Invalid merchant name";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}