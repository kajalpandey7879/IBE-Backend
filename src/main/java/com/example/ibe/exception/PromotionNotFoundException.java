package com.example.ibe.exception;

public class PromotionNotFoundException extends RuntimeException {
    public PromotionNotFoundException(String message) {
        super(message);
    }
}
