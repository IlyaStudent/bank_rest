package com.example.bankcards.exception;

import com.example.bankcards.util.constants.ApiErrorMessage;

public class ResourceExistsException extends RuntimeException {

    public ResourceExistsException(String message) {
        super(message);
    }

    public static ResourceExistsException username(String username) {
        return new ResourceExistsException(ApiErrorMessage.USERNAME_ALREADY_EXISTS.getMessage(username));
    }

    public static ResourceExistsException email(String email) {
        return new ResourceExistsException(ApiErrorMessage.EMAIL_ALREADY_EXISTS.getMessage(email));
    }

    public static ResourceExistsException card() {
        return new ResourceExistsException(ApiErrorMessage.CARD_ALREADY_EXISTS.getMessage());
    }
}
