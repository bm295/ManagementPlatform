package com.managementplatform.domain.model;

import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;
import java.time.Instant;

public record OutboxMessage(
    long checkoutAttemptId,
    OutboxMessageType type,
    OutboxStatus status,
    String payload,
    int attemptCount,
    String lastError,
    Instant createdAt
) {
}
