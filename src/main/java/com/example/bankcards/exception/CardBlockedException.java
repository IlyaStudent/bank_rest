package com.example.bankcards.exception;

public class CardBlockedException extends RuntimeException {
    public CardBlockedException() {
        super("Card blocked");
    }
}
