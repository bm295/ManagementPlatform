package com.managementplatform.domain.model;

import com.managementplatform.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentTransaction(
    long checkoutAttemptId,
    PaymentStatus status,
    int attemptCount,
    BigDecimal amount,
    String currency,
    String providerTransactionId,
    String failureReason,
    Instant createdAt
) {
}
