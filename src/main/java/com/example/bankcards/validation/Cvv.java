package com.example.bankcards.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CvvValidator.class)
public @interface Cvv {
    String message() default "Invalid card cvv";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
