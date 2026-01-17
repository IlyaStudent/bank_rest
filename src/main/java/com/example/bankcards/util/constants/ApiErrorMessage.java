package com.example.bankcards.util.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiErrorMessage {
    INVALID_TOKEN_SIGNATURE("Invalid token signature"),

    ERROR_DURING_JWT_PROCESSING("An unexpected error occurred during JWT processing"),
    TOKEN_EXPIRED("Token expired."),
    UNEXPECTED_ERROR_OCCURRED("An unexpected error occurred. Please try again later."),
    ;

    private final String message;

    public String getMessage(Object... args) {
        return String.format(message, args);
    }
}
