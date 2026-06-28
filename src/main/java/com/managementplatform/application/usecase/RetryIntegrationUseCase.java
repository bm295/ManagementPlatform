package com.managementplatform.application.usecase;

import com.managementplatform.application.dto.RetryIntegrationRequest;
import com.managementplatform.application.dto.RetryIntegrationResponse;
import com.managementplatform.application.port.IntegrationRetryExecutor;
import com.managementplatform.application.port.IntegrationRetryExecutor.IntegrationRetryExecutionResult;
import com.managementplatform.application.port.IntegrationRetryRepository;
import com.managementplatform.domain.enums.OutboxStatus;
import com.managementplatform.domain.model.OutboxMessage;
import com.managementplatform.domain.model.RetryIdempotencyRecord;
import com.managementplatform.shared.exception.ConflictException;
import com.managementplatform.shared.exception.ResourceNotFoundException;
import com.managementplatform.shared.exception.ValidationException;
import java.util.Objects;

/**
 * Retries one eligible integration follow-up action while enforcing retry idempotency.
 */
public final class RetryIntegrationUseCase {
    private final IntegrationRetryRepository integrationRetryRepository;
    private final IntegrationRetryExecutor integrationRetryExecutor;

    public RetryIntegrationUseCase(
        IntegrationRetryRepository integrationRetryRepository,
        IntegrationRetryExecutor integrationRetryExecutor
    ) {
        this.integrationRetryRepository = Objects.requireNonNull(integrationRetryRepository, "integrationRetryRepository is required.");
        this.integrationRetryExecutor = Objects.requireNonNull(integrationRetryExecutor, "integrationRetryExecutor is required.");
    }

    /**
     * Retries one selected integration follow-up action and returns its latest retry state.
     *
     * @param outboxMessageId identifier of the selected outbox message
     * @param request retry command request
     * @return latest retry result for the selected integration follow-up action
     * @throws ValidationException when the retry command is invalid or the integration is not retryable
     * @throws ResourceNotFoundException when the selected outbox message does not exist
     * @throws ConflictException when the idempotency key has already been used for a different message
     */
    public RetryIntegrationResponse retry(long outboxMessageId, RetryIntegrationRequest request) {
        validate(outboxMessageId, request);

        String idempotencyKey = request.idempotencyKey().trim();
        return integrationRetryRepository.findRetryIdempotencyRecord(idempotencyKey)
            .map(record -> responseForExistingIdempotencyRecord(record, outboxMessageId))
            .orElseGet(() -> executeRetry(outboxMessageId, idempotencyKey));
    }

    private RetryIntegrationResponse responseForExistingIdempotencyRecord(
        RetryIdempotencyRecord record,
        long requestedOutboxMessageId
    ) {
        if (record.outboxMessageId() != requestedOutboxMessageId) {
            throw new ConflictException(
                "idempotencyKey was already used to retry outbox message %d.".formatted(record.outboxMessageId())
            );
        }

        return toResponse(record);
    }

    private RetryIntegrationResponse executeRetry(long outboxMessageId, String idempotencyKey) {
        OutboxMessage message = integrationRetryRepository.findByOutboxMessageId(outboxMessageId)
            .orElseThrow(() -> new ResourceNotFoundException("Integration %d was not found.".formatted(outboxMessageId)));

        if (message.status() != OutboxStatus.FAILED) {
            throw new ValidationException("Integration %d is not eligible for retry.".formatted(outboxMessageId));
        }

        IntegrationRetryExecutionResult executionResult = integrationRetryExecutor.execute(message);
        OutboxMessage retriedMessage = withExecutionResult(message, executionResult);
        integrationRetryRepository.save(retriedMessage);
        RetryIdempotencyRecord record = toIdempotencyRecord(idempotencyKey, retriedMessage);
        integrationRetryRepository.saveRetryIdempotencyRecord(record);
        return toResponse(record);
    }

    private static void validate(long outboxMessageId, RetryIntegrationRequest request) {
        if (outboxMessageId < 1) {
            throw new ValidationException("outboxMessageId must be greater than zero.");
        }

        if (request == null) {
            throw new ValidationException("Retry integration request is required.");
        }

        if (isBlank(request.idempotencyKey())) {
            throw new ValidationException("idempotencyKey is required.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static OutboxMessage withExecutionResult(
        OutboxMessage message,
        IntegrationRetryExecutionResult executionResult
    ) {
        return new OutboxMessage(
            message.id(),
            message.checkoutAttemptId(),
            message.type(),
            executionResult.status(),
            message.payloadJson(),
            executionResult.attemptCount(),
            executionResult.failureReason(),
            message.createdAt(),
            message.nextAttemptAt(),
            message.lockedAt(),
            message.processedAt(),
            message.deadLetterMessage()
        );
    }

    private static RetryIdempotencyRecord toIdempotencyRecord(String idempotencyKey, OutboxMessage message) {
        return new RetryIdempotencyRecord(
            idempotencyKey,
            message.id(),
            message.checkoutAttemptId(),
            message.type(),
            message.status(),
            message.attemptCount(),
            message.lastError()
        );
    }

    private static RetryIntegrationResponse toResponse(RetryIdempotencyRecord record) {
        return new RetryIntegrationResponse(
            record.outboxMessageId(),
            record.checkoutAttemptId(),
            record.type(),
            record.status(),
            record.attemptCount(),
            record.failureReason()
        );
    }
}
