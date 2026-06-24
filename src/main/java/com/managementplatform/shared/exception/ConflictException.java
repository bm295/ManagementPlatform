package com.managementplatform.shared.exception;

/**
 * Raised when a valid request conflicts with current resource state; HTTP adapters should map this to 409.
 */
public final class ConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
