package com.managementplatform.infrastructure.gateway;

import com.managementplatform.application.port.PaymentGatewayRequest;
import com.managementplatform.application.port.PaymentGatewayResult;
import com.managementplatform.domain.enums.PaymentStatus;
import java.math.BigDecimal;

public final class MockPaymentGatewayCheck {
    private MockPaymentGatewayCheck() {
    }

    public static void main(String[] args) {
        normalTokenSucceedsOnFirstAttempt();
        retryTokenSucceedsAfterMultipleAttempts();
        declineAndFailTokensReturnClearFailures();
    }

    private static void normalTokenSucceedsOnFirstAttempt() {
        PaymentGatewayResult result = new MockPaymentGateway().charge(request("tok_ok"));

        require(result.status() == PaymentStatus.SUCCEEDED, "normal token should succeed");
        require(result.attemptCount() == 1, "normal token should use one attempt");
        require(result.providerTransactionId().equals("mock_pay_10_20"), "provider transaction id should be stable");
        require(result.failureReason() == null, "successful payment should not include a failure reason");
    }

    private static void retryTokenSucceedsAfterMultipleAttempts() {
        PaymentGatewayResult result = new MockPaymentGateway().charge(request("tok_retry"));

        require(result.status() == PaymentStatus.SUCCEEDED, "retry token should eventually succeed");
        require(result.attemptCount() > 1, "retry token should report multiple attempts");
    }

    private static void declineAndFailTokensReturnClearFailures() {
        PaymentGatewayResult decline = new MockPaymentGateway().charge(request("tok_DECLINE"));
        PaymentGatewayResult fail = new MockPaymentGateway().charge(request("tok_fail"));

        require(decline.status() == PaymentStatus.FAILED, "decline token should fail");
        require(decline.failureReason().contains("declined"), "decline failure should have a clear reason");
        require(fail.status() == PaymentStatus.FAILED, "fail token should fail");
        require(fail.failureReason().contains("failed"), "fail token should have a clear reason");
    }

    private static PaymentGatewayRequest request(String token) {
        return new PaymentGatewayRequest(10, 20, new BigDecimal("99.00"), "USD", token);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
