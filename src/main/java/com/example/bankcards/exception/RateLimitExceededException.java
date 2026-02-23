package com.example.bankcards.exception;

import com.example.bankcards.util.constants.ApiErrorMessage;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super(ApiErrorMessage.RATE_LIMIT_EXCEEDED.getMessage());
    }
}
