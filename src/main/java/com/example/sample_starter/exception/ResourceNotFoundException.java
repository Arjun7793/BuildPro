package com.example.sample_starter.exception;

/**
 * Available for services/controllers that would rather throw than return
 * Optional.empty() - GlobalExceptionHandler turns this into a 404 ApiError.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
