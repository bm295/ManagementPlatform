package com.managementplatform.shared.exception;

/**
 * Raised when an application request fails validation before domain work begins.
 */
public final class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
