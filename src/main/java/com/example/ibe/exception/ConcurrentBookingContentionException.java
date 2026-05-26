package com.example.ibe.exception;

public class ConcurrentBookingContentionException extends RuntimeException {
    public ConcurrentBookingContentionException(String message) {
        super(message);
    }
}
