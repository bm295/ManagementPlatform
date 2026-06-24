package com.managementplatform.shared.exception;

/**
 * Raised when a requested resource cannot be found; HTTP adapters should map this to 404.
 */
public final class ResourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
