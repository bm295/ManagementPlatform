package com.managementplatform.application.dto;

import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;

/**
 * One retryable integration item in the operations recovery queue.
 *
 * @param outboxMessageId identifier of the retryable outbox message
 * @param checkoutAttemptId identifier of the checkout attempt that owns the message
 * @param type integration follow-up action type
 * @param status current outbox message status
 * @param attemptCount number of attempts already recorded for the integration message
 * @param failureReason latest failure reason for the retryable integration
 */
public record RetryableIntegrationItemDto(
    long outboxMessageId,
    long checkoutAttemptId,
    OutboxMessageType type,
    OutboxStatus status,
    int attemptCount,
    String failureReason
) {
}
