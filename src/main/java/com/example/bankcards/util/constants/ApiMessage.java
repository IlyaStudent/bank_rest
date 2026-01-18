package com.example.bankcards.util.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiMessage {
    TOKEN_CREATED_OR_UPDATED("User token has been created or updated"),
    ;

    private final String message;
}