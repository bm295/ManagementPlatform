package com.managementplatform.domain.model;

import com.managementplatform.domain.enums.InvoiceStatus;
import java.time.Instant;

public record Invoice(
    long id,
    long checkoutAttemptId,
    InvoiceStatus status,
    String failureReason,
    Instant createdAt,
    Instant completedAt
) {
}
