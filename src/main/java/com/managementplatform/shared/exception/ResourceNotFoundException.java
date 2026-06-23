package com.managementplatform.shared.exception;

/**
 * Raised when a requested resource cannot be found; HTTP adapters should map this to 404.
 */
public final class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
