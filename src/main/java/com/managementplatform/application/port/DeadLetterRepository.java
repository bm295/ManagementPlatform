package com.managementplatform.application.port;

import com.managementplatform.domain.model.DeadLetterMessage;
import java.util.List;

/**
 * Application port for storing and querying checkout failure dead-letter messages.
 */
public interface DeadLetterRepository {
    /**
     * Returns recent dead-letter messages for operational diagnostics.
     *
     * @return recent dead-letter messages in deterministic order, typically newest first
     */
    List<DeadLetterMessage> findRecent();

    /**
     * Persists a dead-letter message created when checkout processing cannot complete.
     *
     * @param message dead-letter message to save
     */
    void save(DeadLetterMessage message);
}
