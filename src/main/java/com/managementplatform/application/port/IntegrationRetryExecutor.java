package com.managementplatform.application.port;

import com.managementplatform.domain.enums.OutboxStatus;
import com.managementplatform.domain.model.OutboxMessage;

/**
 * Application port for executing a selected integration follow-up action during retry handling.
 */
public interface IntegrationRetryExecutor {
    /**
     * Executes the follow-up action represented by the selected outbox message.
     *
     * @param message outbox message selected for retry by the application use case
     * @return normalized retry execution result for application-level state updates
     */
    IntegrationRetryExecutionResult execute(OutboxMessage message);

    /**
     * Result of retrying one integration follow-up action.
     *
     * @param status resulting outbox status after executing the retry
     * @param attemptCount updated attempt count for the integration message
     * @param failureReason latest failure reason when the retry did not complete successfully
     */
    record IntegrationRetryExecutionResult(
        OutboxStatus status,
        int attemptCount,
        String failureReason
    ) {
    }
}
