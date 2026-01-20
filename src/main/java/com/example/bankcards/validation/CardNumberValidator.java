package com.example.bankcards.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CardNumberValidator implements ConstraintValidator<CardNumber, String> {
    @Override
    public boolean isValid(
            String cardNumber,
            ConstraintValidatorContext constraintValidatorContext
    ) {
        if (cardNumber == null) {
            return false;
        }

        String digits = cardNumber.replaceAll("[\\s-]", "");

        if (!digits.matches("\\d{13,19}")) {
            return false;
        }

        return checkLuhn(digits);
    }

    private static boolean checkLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(digits.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;

    }
}
