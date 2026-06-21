package com.managementplatform.domain.model;

import com.managementplatform.domain.enums.OutboxMessageType;
import java.time.Instant;

public record DeadLetterMessage(
    long id,
    long outboxMessageId,
    long checkoutAttemptId,
    OutboxMessageType type,
    String payloadJson,
    int attemptCount,
    String failureReason,
    Instant failedAt
) {
}
