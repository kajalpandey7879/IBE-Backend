package com.example.ibe.exception;

public class BookingPersistenceException extends RuntimeException {
    public BookingPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
