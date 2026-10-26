package com.managementplatform.application.usecase;

import com.managementplatform.application.dto.CheckoutRequest;
import com.managementplatform.application.port.out.CheckoutRepository;
import com.managementplatform.application.port.out.DeadLetterRepository;
import com.managementplatform.application.port.out.OrderRepository;
import com.managementplatform.application.port.out.PaymentGateway;
import com.managementplatform.application.port.out.TimeProvider;
import com.managementplatform.shared.exception.ValidationException;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class CheckoutEarlyValidationTest {
    private final CheckoutUseCase useCase = new CheckoutUseCase(
        unused(OrderRepository.class), unused(CheckoutRepository.class),
        unused(DeadLetterRepository.class), unused(PaymentGateway.class), unused(TimeProvider.class)
    );

    @Test
    void rejectsInvalidIdentifiersBeforeCallingDependencies() {
        CheckoutRequest request = new CheckoutRequest("key", "tok_success");
        for (long id : new long[] {Long.MIN_VALUE, -1, 0}) {
            assertValidation(() -> useCase.checkout(id, request), "orderId must be greater than zero.");
            assertValidation(() -> useCase.retryPayment(id, request), "checkoutId must be greater than zero.");
        }
    }

    @Test
    void rejectsInvalidBodiesBeforeCallingDependencies() {
        for (boolean retry : new boolean[] {false, true}) {
            assertValidation(() -> submit(retry, null), "Checkout request is required.");
            assertValidation(() -> submit(retry, new CheckoutRequest(" ", "tok_success")),
                "idempotencyKey is required.");
            assertValidation(() -> submit(retry, new CheckoutRequest("key", " ")),
                "paymentMethodToken is required.");
        }
    }

    private void submit(boolean retry, CheckoutRequest request) {
        if (retry) useCase.retryPayment(1, request);
        else useCase.checkout(1, request);
    }

    private static void assertValidation(Runnable action, String message) {
        assertEquals(message, assertThrows(ValidationException.class, action::run).getMessage());
    }

    private static <T> T unused(Class<T> port) {
        return port.cast(Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[] {port},
            (proxy, method, args) -> {
                throw new AssertionError("Invalid input must not call " + port.getSimpleName() + "." + method.getName());
            }));
    }
}
