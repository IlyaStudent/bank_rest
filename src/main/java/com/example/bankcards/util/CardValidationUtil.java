package com.example.bankcards.util;

import java.time.YearMonth;

public final class CardValidationUtil {

    private CardValidationUtil() {
    }

    public static boolean isValidCardNumber(String cardNumber) {
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

    public static boolean isValidExpiryDate(String expiryDate) {
        if (expiryDate == null || !expiryDate.matches("^(0[1-9]|1[0-2])/[0-9]{2}$")) {
            return false;
        }

        String[] parts = expiryDate.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]) + 2000;

        YearMonth expiry = YearMonth.of(year, month);
        YearMonth now = YearMonth.now();

        return expiry.isAfter(now);

    }

    public static boolean isValidCVV(String cvv) {
        if (cvv == null) {
            return false;
        }
        return cvv.matches("^\\d{3}$");
    }
}
