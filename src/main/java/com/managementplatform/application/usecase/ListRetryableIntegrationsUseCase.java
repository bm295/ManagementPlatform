package com.managementplatform.application.usecase;

import com.managementplatform.application.dto.RetryableIntegrationItemDto;
import com.managementplatform.application.dto.RetryableIntegrationPageResponse;
import com.managementplatform.application.port.in.ListRetryableIntegrationsInputPort;
import com.managementplatform.application.port.out.IntegrationRetryRepository;
import com.managementplatform.domain.model.OutboxMessage;
import com.managementplatform.shared.exception.ValidationException;
import java.util.Objects;

/**
 * Lists retryable integration follow-up actions for the operations recovery queue.
 */
public final class ListRetryableIntegrationsUseCase implements ListRetryableIntegrationsInputPort {
    private final IntegrationRetryRepository integrationRetryRepository;

    public ListRetryableIntegrationsUseCase(IntegrationRetryRepository integrationRetryRepository) {
        this.integrationRetryRepository = Objects.requireNonNull(integrationRetryRepository, "integrationRetryRepository is required.");
    }

    /**
     * Returns one page of retryable integration work after validating pagination input.
     *
     * @param page one-based page number
     * @param pageSize maximum number of retryable integration items to return
     * @return recovery work queue page for operations users
     * @throws ValidationException when page or pageSize is less than one
     */
    public RetryableIntegrationPageResponse listRetryable(int page, int pageSize) {
        validatePagination(page, pageSize);

        return new RetryableIntegrationPageResponse(
            integrationRetryRepository.findRetryable(page, pageSize)
                .stream()
                .map(ListRetryableIntegrationsUseCase::toItem)
                .toList(),
            page,
            pageSize,
            integrationRetryRepository.countRetryable()
        );
    }

    private static void validatePagination(int page, int pageSize) {
        if (page < 1) {
            throw new ValidationException("page must be greater than zero.");
        }

        if (pageSize < 1) {
            throw new ValidationException("pageSize must be greater than zero.");
        }
    }

    private static RetryableIntegrationItemDto toItem(OutboxMessage message) {
        return new RetryableIntegrationItemDto(
            message.id(),
            message.checkoutAttemptId(),
            message.type(),
            message.status(),
            message.attemptCount(),
            message.lastError()
        );
    }
}
