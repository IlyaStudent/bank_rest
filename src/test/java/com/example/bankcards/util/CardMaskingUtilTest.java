package com.example.bankcards.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CardMaskingUtil unit tests")
class CardMaskingUtilTest {

    @Nested
    @DisplayName("maskCardNumber")
    class MaskCardNumber {

        @Test
        @DisplayName("Should return masked number with last four digits visible for valid card number")
        void shouldReturnMasked_whenValidNumber() {
            String cardNumber = "1111222233334444";
            String expected = "**** **** **** 4444";

            String result = CardMaskingUtil.maskCardNumber(cardNumber);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should ignore spaces in input and mask correctly")
        void shouldIgnoreSpaces() {
            String cardNumber = "1111 2222 3333 4444";
            String expected = "**** **** **** 4444";

            String result = CardMaskingUtil.maskCardNumber(cardNumber);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle card number with exactly four digits")
        void shouldHandleExactlyFourDigits() {
            String cardNumber = "1111";
            String expected = "**** **** **** 1111";

            String result = CardMaskingUtil.maskCardNumber(cardNumber);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle card number with leading/trailing spaces and multiple spaces")
        void shouldHandleNumberWithMultipleSpaces() {
            String cardNumber = "   1111 2222 3333 44 44   ";
            String expected = "**** **** **** 4444";

            String result = CardMaskingUtil.maskCardNumber(cardNumber);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return four stars when input is null")
        void shouldReturnFourStars_whenInputIsNull() {
            String cardNumber = null;

            String result = CardMaskingUtil.maskCardNumber(cardNumber);

            assertThat(result).isEqualTo("****");
        }

        @Test
        @DisplayName("Should return four stars when input is empty")
        void shouldReturnFourStars_whenInputIsEmpty() {
            String cardNumber = "";

            String result = CardMaskingUtil.maskCardNumber(cardNumber);

            assertThat(result).isEqualTo("****");
        }

        @Test
        @DisplayName("Should return four stars when input contains only spaces")
        void shouldReturnFourStars_whenInputContainsOnlySpaces() {
            String cardNumber = "   ";

            String result = CardMaskingUtil.maskCardNumber(cardNumber);

            assertThat(result).isEqualTo("****");
        }

        @Test
        @DisplayName("Should return four stars when input has less than four digits after removing spaces")
        void shouldReturnFourStars_whenLessThanFourDigits() {
            String cardNumber = "11 1";

            String result = CardMaskingUtil.maskCardNumber(cardNumber);

            assertThat(result).isEqualTo("****");
        }

        @Test
        @DisplayName("Should not remove non‑digit characters (only spaces are removed)")
        void shouldNotRemoveNonDigitCharacters() {
            String cardNumber = "abcd efgh 3333";
            String expected = "**** **** **** 3333";

            String result = CardMaskingUtil.maskCardNumber(cardNumber);

            assertThat(result).isEqualTo(expected);
        }
    }
}