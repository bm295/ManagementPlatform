package com.managementplatform.application.usecase;

import com.managementplatform.application.dto.RetryableIntegrationPageResponse;
import com.managementplatform.application.port.IntegrationRetryRepository;
import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;
import com.managementplatform.domain.model.OutboxMessage;
import com.managementplatform.shared.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class ListRetryableIntegrationsUseCaseCheck {
    private ListRetryableIntegrationsUseCaseCheck() {
    }

    public static void main(String[] args) {
        validatesPageIsPositive();
        validatesPageSizeIsPositive();
        returnsRecoveryWorkQueuePage();
    }

    private static void validatesPageIsPositive() {
        ListRetryableIntegrationsUseCase useCase = new ListRetryableIntegrationsUseCase(new StubIntegrationRetryRepository(List.of(), 0));

        expectValidation(() -> useCase.listRetryable(0, 20), "page must be greater than zero.");
        expectValidation(() -> useCase.listRetryable(-1, 20), "page must be greater than zero.");
    }

    private static void validatesPageSizeIsPositive() {
        ListRetryableIntegrationsUseCase useCase = new ListRetryableIntegrationsUseCase(new StubIntegrationRetryRepository(List.of(), 0));

        expectValidation(() -> useCase.listRetryable(1, 0), "pageSize must be greater than zero.");
        expectValidation(() -> useCase.listRetryable(1, -1), "pageSize must be greater than zero.");
    }

    private static void returnsRecoveryWorkQueuePage() {
        OutboxMessage message = new OutboxMessage(
            101,
            202,
            OutboxMessageType.SEND_CHECKOUT_EMAIL,
            OutboxStatus.FAILED,
            "{}",
            3,
            "Email provider unavailable.",
            Instant.parse("2026-04-28T08:00:00Z"),
            null,
            null,
            null,
            null
        );
        StubIntegrationRetryRepository repository = new StubIntegrationRetryRepository(List.of(message), 7);
        ListRetryableIntegrationsUseCase useCase = new ListRetryableIntegrationsUseCase(repository);

        RetryableIntegrationPageResponse response = useCase.listRetryable(2, 5);

        require(repository.lastPage == 2, "use case should pass the requested page to the repository");
        require(repository.lastPageSize == 5, "use case should pass the requested pageSize to the repository");
        require(response.page() == 2, "response should include requested page");
        require(response.pageSize() == 5, "response should include requested pageSize");
        require(response.totalCount() == 7, "response should include total retryable count");
        require(response.items().size() == 1, "response should include repository retryable items");
        require(response.items().getFirst().outboxMessageId() == 101, "item should include outbox message id");
        require(response.items().getFirst().checkoutAttemptId() == 202, "item should include checkout attempt id");
        require(response.items().getFirst().type() == OutboxMessageType.SEND_CHECKOUT_EMAIL, "item should include integration type");
        require(response.items().getFirst().status() == OutboxStatus.FAILED, "item should include integration status");
        require(response.items().getFirst().attemptCount() == 3, "item should include attempt count");
        require(response.items().getFirst().failureReason().equals("Email provider unavailable."), "item should include failure reason");
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
        private final List<OutboxMessage> retryableMessages;
        private final long totalCount;
        private int lastPage;
        private int lastPageSize;

        private StubIntegrationRetryRepository(List<OutboxMessage> retryableMessages, long totalCount) {
            this.retryableMessages = retryableMessages;
            this.totalCount = totalCount;
        }

        @Override
        public Optional<OutboxMessage> findByOutboxMessageId(long outboxMessageId) {
            throw new UnsupportedOperationException("findByOutboxMessageId is not used by this check");
        }

        @Override
        public List<OutboxMessage> findRetryable(int page, int pageSize) {
            lastPage = page;
            lastPageSize = pageSize;
            return retryableMessages;
        }

        @Override
        public long countRetryable() {
            return totalCount;
        }

        @Override
        public void save(OutboxMessage message) {
            throw new UnsupportedOperationException("save is not used by this check");
        }

        @Override
        public Optional<RetryIdempotencyRecord> findRetryIdempotencyRecord(String idempotencyKey) {
            throw new UnsupportedOperationException("findRetryIdempotencyRecord is not used by this check");
        }

        @Override
        public void saveRetryIdempotencyRecord(RetryIdempotencyRecord record) {
            throw new UnsupportedOperationException("saveRetryIdempotencyRecord is not used by this check");
        }
    }
}
