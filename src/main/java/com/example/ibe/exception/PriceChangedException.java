package com.example.ibe.exception;

public class PriceChangedException extends RuntimeException {
    public PriceChangedException(String message) {
        super(message);
    }
}
