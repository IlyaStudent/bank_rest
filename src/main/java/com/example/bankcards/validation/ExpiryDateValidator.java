package com.example.bankcards.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.YearMonth;

public class ExpiryDateValidator implements ConstraintValidator<ExpiryDate, String> {
    @Override
    public boolean isValid(
            String expiryDate,
            ConstraintValidatorContext constraintValidatorContext
    ) {
        if (expiryDate == null || !expiryDate.matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
            return false;
        }

        String[] parts = expiryDate.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]) + 2000;

        YearMonth expiry = YearMonth.of(year, month);
        YearMonth now = YearMonth.now();

        return expiry.isAfter(now);
    }
}
