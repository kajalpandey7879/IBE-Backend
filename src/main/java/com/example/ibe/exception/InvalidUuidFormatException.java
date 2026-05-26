package com.example.ibe.exception;

public class InvalidUuidFormatException extends RuntimeException {

    public InvalidUuidFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}