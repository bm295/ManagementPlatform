package com.managementplatform.application.usecase;

import com.managementplatform.application.dto.RetryIntegrationRequest;
import com.managementplatform.application.dto.RetryIntegrationResponse;
import com.managementplatform.application.port.out.IntegrationRetryExecutor;
import com.managementplatform.application.port.out.IntegrationRetryRepository;
import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;
import com.managementplatform.domain.model.OutboxMessage;
import com.managementplatform.domain.model.RetryIdempotencyRecord;
import com.managementplatform.shared.exception.ConflictException;
import com.managementplatform.shared.exception.ResourceNotFoundException;
import com.managementplatform.shared.exception.ValidationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RetryIntegrationUseCaseCheck {
    private RetryIntegrationUseCaseCheck() {
    }

    public static void main(String[] args) {
        validatesRequestAndIdempotencyKey();
        throwsNotFoundWhenIntegrationDoesNotExist();
        rejectsCompletedIntegration();
        retriesEligibleIntegrationAndStoresIdempotencyResult();
        replaysExistingResultForSameIdempotencyKeyWithoutExecutingAgain();
        detectsIdempotencyConflictForDifferentIntegration();
    }

    private static void validatesRequestAndIdempotencyKey() {
        RetryIntegrationUseCase useCase = useCaseWith(new StubIntegrationRetryRepository(), new StubIntegrationRetryExecutor());

        expectValidation(() -> useCase.retry(0, new RetryIntegrationRequest("retry-1")), "outboxMessageId must be greater than zero.");
        expectValidation(() -> useCase.retry(1, null), "Retry integration request is required.");
        expectValidation(() -> useCase.retry(1, new RetryIntegrationRequest(" ")), "idempotencyKey is required.");
        expectValidation(() -> useCase.retry(1, new RetryIntegrationRequest(null)), "idempotencyKey is required.");
    }

    private static void throwsNotFoundWhenIntegrationDoesNotExist() {
        RetryIntegrationUseCase useCase = useCaseWith(new StubIntegrationRetryRepository(), new StubIntegrationRetryExecutor());

        try {
            useCase.retry(404, new RetryIntegrationRequest("retry-404"));
            throw new AssertionError("missing integration should throw ResourceNotFoundException");
        } catch (ResourceNotFoundException exception) {
            require(exception.getMessage().equals("Integration 404 was not found."), "not found message should include integration id");
        }
    }

    private static void rejectsCompletedIntegration() {
        StubIntegrationRetryRepository repository = new StubIntegrationRetryRepository();
        repository.save(message(101, OutboxStatus.SUCCEEDED, 1, null));
        StubIntegrationRetryExecutor executor = new StubIntegrationRetryExecutor();
        RetryIntegrationUseCase useCase = useCaseWith(repository, executor);

        expectValidation(() -> useCase.retry(101, new RetryIntegrationRequest("retry-completed")), "Integration 101 is not eligible for retry.");
        require(executor.callCount == 0, "completed integration should not be executed again");
        require(repository.retryIdempotencyRecords.isEmpty(), "completed integration rejection should not store idempotency result");
    }

    private static void retriesEligibleIntegrationAndStoresIdempotencyResult() {
        StubIntegrationRetryRepository repository = new StubIntegrationRetryRepository();
        repository.save(message(201, OutboxStatus.FAILED, 2, "Email provider unavailable."));
        StubIntegrationRetryExecutor executor = new StubIntegrationRetryExecutor(new IntegrationRetryExecutor.IntegrationRetryExecutionResult(
            OutboxStatus.SUCCEEDED,
            3,
            null
        ));
        RetryIntegrationUseCase useCase = useCaseWith(repository, executor);

        RetryIntegrationResponse response = useCase.retry(201, new RetryIntegrationRequest(" retry-success "));
        OutboxMessage saved = repository.messagesById.get(201L);
        RetryIdempotencyRecord record = repository.retryIdempotencyRecords.get("retry-success");

        require(executor.callCount == 1, "eligible integration should be executed once");
        require(executor.executedMessages.getFirst().id() == 201, "executor should receive selected integration message");
        require(saved.status() == OutboxStatus.SUCCEEDED, "saved integration should use executor status");
        require(saved.attemptCount() == 3, "saved integration should use executor attempt count");
        require(saved.lastError() == null, "saved integration should use executor failure reason");
        require(record != null, "retry idempotency record should be stored");
        require(record.idempotencyKey().equals("retry-success"), "stored idempotency key should be trimmed");
        require(response.outboxMessageId() == 201, "response should include outbox message id");
        require(response.checkoutAttemptId() == 301, "response should include checkout attempt id");
        require(response.type() == OutboxMessageType.SEND_CHECKOUT_EMAIL, "response should include integration type");
        require(response.status() == OutboxStatus.SUCCEEDED, "response should include latest status");
        require(response.attemptCount() == 3, "response should include latest attempt count");
        require(response.failureReason() == null, "response should include latest failure reason");
    }

    private static void replaysExistingResultForSameIdempotencyKeyWithoutExecutingAgain() {
        StubIntegrationRetryRepository repository = new StubIntegrationRetryRepository();
        repository.retryIdempotencyRecords.put("retry-same", new RetryIdempotencyRecord(
            "retry-same",
            301,
            401,
            OutboxMessageType.SEND_CHECKOUT_EMAIL,
            OutboxStatus.FAILED,
            4,
            "Still unavailable."
        ));
        StubIntegrationRetryExecutor executor = new StubIntegrationRetryExecutor();
        RetryIntegrationUseCase useCase = useCaseWith(repository, executor);

        RetryIntegrationResponse response = useCase.retry(301, new RetryIntegrationRequest("retry-same"));

        require(executor.callCount == 0, "idempotent replay should not execute integration again");
        require(response.outboxMessageId() == 301, "replay should return stored outbox message id");
        require(response.checkoutAttemptId() == 401, "replay should return stored checkout attempt id");
        require(response.status() == OutboxStatus.FAILED, "replay should return stored status");
        require(response.attemptCount() == 4, "replay should return stored attempt count");
        require(response.failureReason().equals("Still unavailable."), "replay should return stored failure reason");
    }

    private static void detectsIdempotencyConflictForDifferentIntegration() {
        StubIntegrationRetryRepository repository = new StubIntegrationRetryRepository();
        repository.retryIdempotencyRecords.put("retry-conflict", new RetryIdempotencyRecord(
            "retry-conflict",
            401,
            501,
            OutboxMessageType.SEND_CHECKOUT_EMAIL,
            OutboxStatus.SUCCEEDED,
            2,
            null
        ));
        StubIntegrationRetryExecutor executor = new StubIntegrationRetryExecutor();
        RetryIntegrationUseCase useCase = useCaseWith(repository, executor);

        try {
            useCase.retry(402, new RetryIntegrationRequest("retry-conflict"));
            throw new AssertionError("reusing idempotency key for a different integration should throw ConflictException");
        } catch (ConflictException exception) {
            require(exception.getMessage().equals("idempotencyKey was already used to retry outbox message 401."),
                "conflict message should identify the original outbox message");
        }
        require(executor.callCount == 0, "idempotency conflict should not execute integration");
    }

    private static RetryIntegrationUseCase useCaseWith(
        IntegrationRetryRepository repository,
        IntegrationRetryExecutor executor
    ) {
        return new RetryIntegrationUseCase(repository, executor);
    }

    private static OutboxMessage message(long id, OutboxStatus status, int attemptCount, String lastError) {
        return new OutboxMessage(
            id,
            id + 100,
            OutboxMessageType.SEND_CHECKOUT_EMAIL,
            status,
            "{}",
            attemptCount,
            lastError,
            Instant.parse("2026-04-28T08:00:00Z"),
            null,
            null,
            null,
            null
        );
    }

    private static void expectValidation(Runnable operation, String expectedMessage) {
        try {
            operation.run();
            throw new AssertionError("operation should throw ValidationException");
        } catch (ValidationException exception) {
            require(exception.getMessage().equals(expectedMessage), "validation message should be " + expectedMessage);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class StubIntegrationRetryRepository implements IntegrationRetryRepository {
        private final Map<Long, OutboxMessage> messagesById = new HashMap<>();
        private final Map<String, RetryIdempotencyRecord> retryIdempotencyRecords = new HashMap<>();

        @Override
        public Optional<OutboxMessage> findByOutboxMessageId(long outboxMessageId) {
            return Optional.ofNullable(messagesById.get(outboxMessageId));
        }

        @Override
        public List<OutboxMessage> findRetryable(int page, int pageSize) {
            throw new UnsupportedOperationException("findRetryable is not used by this check");
        }

        @Override
        public long countRetryable() {
            throw new UnsupportedOperationException("countRetryable is not used by this check");
        }

        @Override
        public void save(OutboxMessage message) {
            messagesById.put(message.id(), message);
        }

        @Override
        public Optional<RetryIdempotencyRecord> findRetryIdempotencyRecord(String idempotencyKey) {
            return Optional.ofNullable(retryIdempotencyRecords.get(idempotencyKey));
        }

        @Override
        public void saveRetryIdempotencyRecord(RetryIdempotencyRecord record) {
            retryIdempotencyRecords.put(record.idempotencyKey(), record);
        }
    }

    private static final class StubIntegrationRetryExecutor implements IntegrationRetryExecutor {
        private final IntegrationRetryExecutionResult result;
        private final List<OutboxMessage> executedMessages = new java.util.ArrayList<>();
        private int callCount;

        private StubIntegrationRetryExecutor() {
            this(new IntegrationRetryExecutionResult(OutboxStatus.SUCCEEDED, 1, null));
        }

        private StubIntegrationRetryExecutor(IntegrationRetryExecutionResult result) {
            this.result = result;
        }

        @Override
        public IntegrationRetryExecutionResult execute(OutboxMessage message) {
            callCount++;
            executedMessages.add(message);
            return result;
        }
    }
}
