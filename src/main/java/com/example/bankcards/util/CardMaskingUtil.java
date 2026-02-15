package com.example.bankcards.util;

public final class CardMaskingUtil {
    private CardMaskingUtil() {
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return "****";
        }

        String digits = cardNumber.replaceAll("\\s", "");
        if (digits.length() < 4) {
            return "****";
        }

        String lastFour = digits.substring(digits.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
