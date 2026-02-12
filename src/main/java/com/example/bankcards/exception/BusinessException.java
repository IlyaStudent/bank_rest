package com.example.bankcards.exception;

import com.example.bankcards.util.constants.ApiErrorMessage;

import java.math.BigDecimal;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public static BusinessException insufficientFunds(BigDecimal required, BigDecimal available) {
        return new BusinessException(ApiErrorMessage.INSUFFICIENT_FUNDS.getMessage(required, available));
    }

    public static BusinessException cardBlocked(Long cardId) {
        return new BusinessException(ApiErrorMessage.CARD_BLOCKED.getMessage(cardId));
    }

    public static BusinessException cardExpired(Long cardId) {
        return new BusinessException(ApiErrorMessage.CARD_EXPIRED.getMessage(cardId));
    }

    public static BusinessException invalidCardStatus(String status) {
        return new BusinessException(ApiErrorMessage.INVALID_CARD_STATUS.getMessage(status));
    }

    public static BusinessException sameCardTransfer() {
        return new BusinessException(ApiErrorMessage.SAME_CARD_TRANSFER.getMessage());
    }

    public static BusinessException invalidTransferAmount() {
        return new BusinessException(ApiErrorMessage.INVALID_TRANSFER_AMOUNT.getMessage());
    }

    public static BusinessException passwordsDoNotMatch() {
        return new BusinessException(ApiErrorMessage.PASSWORDS_DO_NOT_MATCH.getMessage());
    }

    public static BusinessException invalidRole(String roleName) {
        return new BusinessException(ApiErrorMessage.INVALID_ROLE.getMessage(roleName));
    }

    public static BusinessException invalidExpiryDate(String expiryDate) {
        return new BusinessException(ApiErrorMessage.INVALID_EXPIRY_DATE.getMessage(expiryDate));
    }
}
