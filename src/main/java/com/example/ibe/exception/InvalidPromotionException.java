package com.example.ibe.exception;

public class InvalidPromotionException extends RuntimeException {
    public InvalidPromotionException(String message) {
        super(message);
    }
}
