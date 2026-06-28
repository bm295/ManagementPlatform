package com.managementplatform.application.port;

import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;
import com.managementplatform.domain.model.OutboxMessage;
import com.managementplatform.domain.model.RetryIdempotencyRecord;
import java.util.List;
import java.util.Optional;

/**
 * Application port for querying retryable integration work and persisting retry state.
 */
public interface IntegrationRetryRepository {
    /**
     * Finds one integration outbox message by its stable identifier.
     *
     * @param outboxMessageId outbox message identifier from the API route or use case input
     * @return the matching outbox message when present
     */
    Optional<OutboxMessage> findByOutboxMessageId(long outboxMessageId);

    /**
     * Finds retryable integration outbox messages for the operations recovery queue.
     *
     * @param page one-based page number
     * @param pageSize maximum number of items to return
     * @return retryable outbox messages in deterministic queue order
     */
    List<OutboxMessage> findRetryable(int page, int pageSize);

    /**
     * Counts retryable integration outbox messages available to operations.
     *
     * @return total number of retryable outbox messages
     */
    long countRetryable();

    /**
     * Persists retry state for an integration outbox message after a retry attempt.
     *
     * @param message outbox message with the latest retry state
     */
    void save(OutboxMessage message);

    /**
     * Finds a previously stored retry idempotency record by caller-provided key.
     *
     * @param idempotencyKey caller-provided key used to make retry commands idempotent
     * @return the matching retry idempotency record when present
     */
    Optional<RetryIdempotencyRecord> findRetryIdempotencyRecord(String idempotencyKey);

    /**
     * Stores the result associated with a retry idempotency key.
     *
     * @param record retry idempotency record to persist
     */
    void saveRetryIdempotencyRecord(RetryIdempotencyRecord record);

}
