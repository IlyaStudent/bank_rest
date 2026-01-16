package com.example.bankcards.util;

public final class CardMaskingUtil {
    private CardMaskingUtil() {
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String digits = cardNumber.replaceAll("\\s", "");
        String lastFour = digits.substring(digits.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
