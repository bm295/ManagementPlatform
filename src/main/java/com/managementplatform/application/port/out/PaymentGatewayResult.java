package com.managementplatform.application.port.out;

public record PaymentGatewayResult(com.managementplatform.domain.enums.PaymentStatus status, int attemptCount, String providerTransactionId, String failureReason) {}
