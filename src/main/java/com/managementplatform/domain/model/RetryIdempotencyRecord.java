package com.managementplatform.domain.model;

import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;

/**
 * Domain state for a retry command idempotency result.
 *
 * @param idempotencyKey caller-provided key used to make retry commands idempotent
 * @param outboxMessageId identifier of the retried outbox message
 * @param checkoutAttemptId identifier of the checkout attempt that owns the message
 * @param type integration follow-up action type
 * @param status latest outbox message status for the stored retry result
 * @param attemptCount number of attempts recorded for the integration message
 * @param failureReason latest failure reason, if the retry did not complete successfully
 */
public record RetryIdempotencyRecord(
    String idempotencyKey,
    long outboxMessageId,
    long checkoutAttemptId,
    OutboxMessageType type,
    OutboxStatus status,
    int attemptCount,
    String failureReason
) {
}
