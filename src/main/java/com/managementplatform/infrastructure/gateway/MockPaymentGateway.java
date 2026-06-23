package com.managementplatform.infrastructure.gateway;

import com.managementplatform.application.port.PaymentGateway;
import com.managementplatform.application.port.PaymentGatewayRequest;
import com.managementplatform.application.port.PaymentGatewayResult;
import com.managementplatform.domain.enums.PaymentStatus;
import java.util.Locale;

/**
 * Deterministic payment gateway implementation for local development and tests.
 */
public final class MockPaymentGateway implements PaymentGateway {
    private static final int NORMAL_ATTEMPT_COUNT = 1;
    private static final int RETRY_ATTEMPT_COUNT = 2;

    @Override
    public PaymentGatewayResult charge(PaymentGatewayRequest request) {
        String token = normalize(request.paymentMethodToken());

        if (token.contains("decline")) {
            return failed(request, "Payment was declined by the mock gateway.");
        }

        if (token.contains("fail")) {
            return failed(request, "Payment failed in the mock gateway.");
        }

        int attemptCount = token.contains("retry") ? RETRY_ATTEMPT_COUNT : NORMAL_ATTEMPT_COUNT;
        return succeeded(request, attemptCount);
    }

    private static PaymentGatewayResult succeeded(PaymentGatewayRequest request, int attemptCount) {
        return new PaymentGatewayResult(
            PaymentStatus.SUCCEEDED,
            attemptCount,
            providerTransactionId(request),
            null
        );
    }

    private static PaymentGatewayResult failed(PaymentGatewayRequest request, String failureReason) {
        return new PaymentGatewayResult(
            PaymentStatus.FAILED,
            NORMAL_ATTEMPT_COUNT,
            providerTransactionId(request),
            failureReason
        );
    }

    private static String providerTransactionId(PaymentGatewayRequest request) {
        return "mock_pay_%d_%d".formatted(request.orderId(), request.checkoutAttemptId());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
