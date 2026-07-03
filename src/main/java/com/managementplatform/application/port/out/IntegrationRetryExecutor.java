package com.managementplatform.application.port.out;

import com.managementplatform.domain.model.OutboxMessage;

public interface IntegrationRetryExecutor {
    IntegrationRetryExecutionResult execute(OutboxMessage message);

    record IntegrationRetryExecutionResult(
        com.managementplatform.domain.enums.OutboxStatus status,
        int attemptCount,
        String failureReason
    ) {}
}
