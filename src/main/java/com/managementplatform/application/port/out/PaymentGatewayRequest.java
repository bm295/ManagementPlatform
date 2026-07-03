package com.managementplatform.application.port.out;

import java.math.BigDecimal;

public record PaymentGatewayRequest(long orderId, long checkoutAttemptId, BigDecimal amount, String currency, String paymentMethodToken) {}
