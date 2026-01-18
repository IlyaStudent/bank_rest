package com.example.bankcards.util.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiErrorMessage {
    // Resource not found (404)
    USER_NOT_FOUND("User with id %s not found"),
    USER_NOT_FOUND_BY_USERNAME("User with username '%s' not found"),
    ROLE_NOT_FOUND("Role %s not found"),
    CARD_NOT_FOUND("Card with id %s not found"),
    TRANSFER_NOT_FOUND("Transfer with id %s not found"),

    // Resource exists (409)
    USERNAME_ALREADY_EXISTS("Username '%s' already exists"),
    EMAIL_ALREADY_EXISTS("Email '%s' already exists"),
    CARD_ALREADY_EXISTS("Card already exists"),

    // Business errors (422)
    INSUFFICIENT_FUNDS("Insufficient funds. Required: %s, available: %s"),
    CARD_BLOCKED("Card with id %s is blocked"),
    CARD_EXPIRED("Card with id %s has expired"),
    INVALID_CARD_STATUS("Invalid card status: %s"),
    SAME_CARD_TRANSFER("Cannot transfer to the same card"),
    INVALID_TRANSFER_AMOUNT("Transfer amount must be positive"),

    // Authentication errors (401)
    INVALID_CREDENTIALS("Invalid username or password"),
    TOKEN_EXPIRED("Token has expired"),
    INVALID_TOKEN_SIGNATURE("Invalid token signature"),

    // General errors (500)
    ENCRYPTION_FAILED("Encryption failed"),
    DECRYPTION_FAILED("Decryption failed"),
    ERROR_DURING_JWT_PROCESSING("An error occurred during JWT processing"),
    UNEXPECTED_ERROR("An unexpected error occurred. Please try again later"),
    ;

    private final String message;

    public String getMessage(Object... args) {
        return String.format(message, args);
    }
}
