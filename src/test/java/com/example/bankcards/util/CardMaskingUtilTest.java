package com.example.bankcards.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CardMaskingUtil unit tests")
class CardMaskingUtilTest {

    @Nested
    @DisplayName("maskCardNumber")
    class MaskCardNumber {

        @ParameterizedTest(name = "input: \"{0}\" -> expected: \"{1}\"")
        @MethodSource("com.example.bankcards.util.CardMaskingUtilTest#validInputs")
        @DisplayName("Should return masked card number with visible last 4 numbers when valid input")
        void shouldReturnMaskedWhenValidInput(String input, String expected) {
            String result = CardMaskingUtil.maskCardNumber(input);
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest(name = "input: \"{0}\" -> expected: \"****\"")
        @MethodSource("com.example.bankcards.util.CardMaskingUtilTest#invalidInputs")
        @DisplayName("Should return four stars when invalid input")
        void shouldReturnFourStarsWhenInvalidInput(String input) {
            String result = CardMaskingUtil.maskCardNumber(input);
            assertThat(result).isEqualTo("****");
        }
    }

    static Stream<Arguments> validInputs() {
        return Stream.of(
                Arguments.of("1111222233334444", "**** **** **** 4444"),
                Arguments.of("1111 2222 3333 4444", "**** **** **** 4444"),
                Arguments.of("1111", "**** **** **** 1111"),
                Arguments.of("   1111 2222 3333 44 44   ", "**** **** **** 4444"),
                Arguments.of("abcd efgh 3333", "**** **** **** 3333")
        );
    }

    static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(""),
                Arguments.of("   "),
                Arguments.of("11 1")
        );
    }
}