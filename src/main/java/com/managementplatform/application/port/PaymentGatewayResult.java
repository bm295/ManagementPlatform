package com.managementplatform.application.port;

import com.managementplatform.domain.enums.PaymentStatus;

/**
 * Normalized payment provider result returned to the checkout use case.
 */
public record PaymentGatewayResult(
    PaymentStatus status,
    int attemptCount,
    String providerTransactionId,
    String failureReason
) {
}
