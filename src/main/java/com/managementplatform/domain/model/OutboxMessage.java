package com.managementplatform.domain.model;

import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;
import java.time.Instant;

public record OutboxMessage(
    long id,
    long checkoutAttemptId,
    OutboxMessageType type,
    OutboxStatus status,
    String payloadJson,
    int attemptCount,
    String lastError,
    Instant createdAt,
    Instant nextAttemptAt,
    Instant lockedAt,
    Instant processedAt,
    DeadLetterMessage deadLetterMessage
) {
}
