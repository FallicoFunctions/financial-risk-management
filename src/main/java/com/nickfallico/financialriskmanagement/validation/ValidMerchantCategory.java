package com.nickfallico.financialriskmanagement.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MerchantCategoryValidator.class)
public @interface ValidMerchantCategory {
    String message() default "Merchant category is not supported";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}