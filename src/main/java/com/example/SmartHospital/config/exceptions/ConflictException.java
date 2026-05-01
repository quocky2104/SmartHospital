package com.example.SmartHospital.config.exceptions;

public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(message);
    }
}