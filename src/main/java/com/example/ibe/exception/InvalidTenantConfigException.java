package com.example.ibe.exception;

public class InvalidTenantConfigException extends RuntimeException {

    public InvalidTenantConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}