package com.managementplatform.application.port;

import java.math.BigDecimal;

/**
 * Payment charge request sent from the checkout use case to a payment gateway.
 */
public record PaymentGatewayRequest(
    long orderId,
    long checkoutAttemptId,
    BigDecimal amount,
    String currency,
    String paymentMethodToken
) {
}
