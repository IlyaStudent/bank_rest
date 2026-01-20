package com.example.bankcards.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CvvValidator implements ConstraintValidator<Cvv, String> {
    @Override
    public boolean isValid(
            String cvv,
            ConstraintValidatorContext constraintValidatorContext
    ) {
        if (cvv == null) {
            return false;
        }
        return cvv.matches("^\\d{3}$");
    }
}
