package com.nickfallico.financialriskmanagement.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ComplexTransactionValidator.class)
public @interface ValidTransaction {
    String message() default "Invalid transaction parameters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}