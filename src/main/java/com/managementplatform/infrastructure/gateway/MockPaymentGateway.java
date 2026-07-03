package com.managementplatform.infrastructure.gateway;

import com.managementplatform.application.port.out.PaymentGateway;
import com.managementplatform.application.port.out.PaymentGatewayRequest;
import com.managementplatform.application.port.out.PaymentGatewayResult;
import com.managementplatform.domain.enums.PaymentStatus;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

/**
 * Mock payment gateway implementation for local development and tests.
 */
public final class MockPaymentGateway implements PaymentGateway {
    private static final int NORMAL_ATTEMPT_COUNT = 1;
    private static final int RETRY_ATTEMPT_COUNT = 2;
    private static final int UNSTABLE_FAILURE_PERCENT = 50;
    private final IntSupplier failureRollSupplier;

    /**
     * Creates a mock gateway with realistic unstable-token randomness.
     */
    public MockPaymentGateway() {
        this(() -> ThreadLocalRandom.current().nextInt(100));
    }

    /**
     * Creates a mock gateway with an injectable roll supplier for deterministic checks.
     *
     * @param failureRollSupplier supplies a value from 0 through 99 for chance-based failures
     */
    public MockPaymentGateway(IntSupplier failureRollSupplier) {
        this.failureRollSupplier = failureRollSupplier;
    }

    @Override
    public PaymentGatewayResult charge(PaymentGatewayRequest request) {
        String token = normalize(request.paymentMethodToken());

        if (token.contains("decline")) {
            return failed(request, "Payment was declined by the mock gateway.");
        }

        if (token.contains("fail")) {
            if (token.contains("retry")) {
                return failed(request, RETRY_ATTEMPT_COUNT, "Payment failed after retry limit in the mock gateway.");
            }
            return failed(request, "Payment failed in the mock gateway.");
        }

        if (token.contains("unstable") && failureRollSupplier.getAsInt() < UNSTABLE_FAILURE_PERCENT) {
            return failed(request, "Payment failed by chance in the mock gateway.");
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
        return failed(request, NORMAL_ATTEMPT_COUNT, failureReason);
    }

    private static PaymentGatewayResult failed(PaymentGatewayRequest request, int attemptCount, String failureReason) {
        return new PaymentGatewayResult(
            PaymentStatus.FAILED,
            attemptCount,
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
