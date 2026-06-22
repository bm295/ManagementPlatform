package com.managementplatform.application.port;

import java.time.Instant;

/**
 * Application port for reading current time in deterministic use-case tests.
 */
public interface TimeProvider {
    /**
     * Returns the current UTC instant.
     *
     * @return current time as an {@link Instant}
     */
    Instant now();
}
