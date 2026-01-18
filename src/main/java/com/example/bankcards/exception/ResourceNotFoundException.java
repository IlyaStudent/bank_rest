package com.example.bankcards.exception;

import com.example.bankcards.entity.RoleType;
import com.example.bankcards.util.constants.ApiErrorMessage;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException user(Long id) {
        return new ResourceNotFoundException(ApiErrorMessage.USER_NOT_FOUND.getMessage(id));
    }

    public static ResourceNotFoundException userByUsername(String username) {
        return new ResourceNotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_USERNAME.getMessage(username));
    }

    public static ResourceNotFoundException role(RoleType roleType) {
        return new ResourceNotFoundException(ApiErrorMessage.ROLE_NOT_FOUND.getMessage(roleType));
    }

    public static ResourceNotFoundException card(Long id) {
        return new ResourceNotFoundException(ApiErrorMessage.CARD_NOT_FOUND.getMessage(id));
    }

    public static ResourceNotFoundException transfer(Long id) {
        return new ResourceNotFoundException(ApiErrorMessage.TRANSFER_NOT_FOUND.getMessage(id));
    }
}
