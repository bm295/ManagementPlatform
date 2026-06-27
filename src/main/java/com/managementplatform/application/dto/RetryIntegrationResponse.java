package com.managementplatform.application.dto;

import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;

/**
 * Response DTO returned after retrying one integration follow-up command.
 *
 * @param outboxMessageId identifier of the retried outbox message
 * @param checkoutAttemptId identifier of the checkout attempt that owns the message
 * @param type integration follow-up action type
 * @param status latest outbox message status after retry handling
 * @param attemptCount number of attempts recorded for the integration message
 * @param failureReason latest failure reason, if the retry did not complete successfully
 */
public record RetryIntegrationResponse(
    long outboxMessageId,
    long checkoutAttemptId,
    OutboxMessageType type,
    OutboxStatus status,
    int attemptCount,
    String failureReason
) {
}
