package com.example.bankcards.exception;

import com.example.bankcards.util.constants.ApiErrorMessage;

public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public static AuthException invalidCredentials() {
        return new AuthException(ApiErrorMessage.INVALID_CREDENTIALS.getMessage());
    }

    public static AuthException tokenExpired() {
        return new AuthException(ApiErrorMessage.TOKEN_EXPIRED.getMessage());
    }

    public static AuthException invalidTokenSignature() {
        return new AuthException(ApiErrorMessage.INVALID_TOKEN_SIGNATURE.getMessage());
    }
}
