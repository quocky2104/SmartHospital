package com.example.SmartHospital.config.exceptions;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}