package com.example.bankcards.exception;

import com.example.bankcards.util.constants.ApiErrorMessage;

public class EncryptionException extends RuntimeException {

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static EncryptionException encryptionFailed(Throwable cause) {
        return new EncryptionException(ApiErrorMessage.ENCRYPTION_FAILED.getMessage(), cause);
    }

    public static EncryptionException decryptionFailed(Throwable cause) {
        return new EncryptionException(ApiErrorMessage.DECRYPTION_FAILED.getMessage(), cause);
    }
}
