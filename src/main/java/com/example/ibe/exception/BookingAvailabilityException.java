package com.example.ibe.exception;

public class BookingAvailabilityException extends RuntimeException {
    public BookingAvailabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
