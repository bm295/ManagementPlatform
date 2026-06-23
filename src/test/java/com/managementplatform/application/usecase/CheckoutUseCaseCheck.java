package com.managementplatform.application.usecase;

import com.managementplatform.application.dto.CheckoutRequest;
import com.managementplatform.application.dto.CheckoutResponse;
import com.managementplatform.application.port.PaymentGateway;
import com.managementplatform.application.port.PaymentGatewayRequest;
import com.managementplatform.application.port.PaymentGatewayResult;
import com.managementplatform.application.port.TimeProvider;
import com.managementplatform.domain.enums.CheckoutStatus;
import com.managementplatform.domain.enums.OrderStatus;
import com.managementplatform.domain.enums.OutboxMessageType;
import com.managementplatform.domain.enums.OutboxStatus;
import com.managementplatform.domain.enums.PaymentStatus;
import com.managementplatform.domain.model.CheckoutAttempt;
import com.managementplatform.domain.model.Order;
import com.managementplatform.domain.model.Tenant;
import com.managementplatform.infrastructure.gateway.MockPaymentGateway;
import com.managementplatform.infrastructure.repository.InMemoryCheckoutRepository;
import com.managementplatform.infrastructure.repository.InMemoryDeadLetterRepository;
import com.managementplatform.infrastructure.repository.InMemoryOrderRepository;
import com.managementplatform.shared.exception.ConflictException;
import com.managementplatform.shared.exception.ResourceNotFoundException;
import com.managementplatform.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CheckoutUseCaseCheck {
    private static final long ORDER_ID = 99L;
    private static final Instant NOW = Instant.parse("2026-04-28T08:00:00Z");
    private static final TimeProvider FIXED_TIME = () -> NOW;

    private CheckoutUseCaseCheck() {
    }

    public static void main(String[] args) {
        validatesIdempotencyKeyIsRequired();
        validatesPaymentMethodTokenIsRequired();
        findsOrderByOrderIdAndCreatesCheckoutAttempt();
        returnsExistingCheckoutForSameOrderAndIdempotencyKeyWithoutChargingAgain();
        throwsNotFoundWhenOrderDoesNotExist();
        throwsConflictWhenOrderIsNotDraft();
        marksOrderProcessingBeforePaymentChargeStep();
        recordsPaymentFailureAndDeadLetter();
    }

    private static void validatesIdempotencyKeyIsRequired() {
        CheckoutUseCase useCase = useCaseWithOrder(order());

        expectValidation(() -> useCase.checkout(ORDER_ID, new CheckoutRequest(" ", "tok_success")),
            "idempotencyKey is required.");
        expectValidation(() -> useCase.checkout(ORDER_ID, new CheckoutRequest(null, "tok_success")),
            "idempotencyKey is required.");
    }

    private static void validatesPaymentMethodTokenIsRequired() {
        CheckoutUseCase useCase = useCaseWithOrder(order());

        expectValidation(() -> useCase.checkout(ORDER_ID, new CheckoutRequest("checkout-1", " ")),
            "paymentMethodToken is required.");
        expectValidation(() -> useCase.checkout(ORDER_ID, new CheckoutRequest("checkout-1", null)),
            "paymentMethodToken is required.");
    }

    private static void findsOrderByOrderIdAndCreatesCheckoutAttempt() {
        Order order = order();
        InMemoryCheckoutRepository checkoutRepository = new InMemoryCheckoutRepository();
        CheckoutUseCase useCase = useCaseWith(order, checkoutRepository, new InMemoryDeadLetterRepository(), new MockPaymentGateway());

        CheckoutResponse response = useCase.checkout(ORDER_ID, new CheckoutRequest("checkout-1", "tok_success"));
        CheckoutAttempt attempt = checkoutRepository.findById(response.checkoutId()).orElseThrow();

        require(response.orderId() == ORDER_ID, "checkout response should belong to the requested order");
        require(attempt.idempotencyKey().equals("checkout-1"), "checkout attempt should keep the request idempotency key");
        require(attempt.createdAt().equals(NOW), "checkout attempt should use the application clock");
        require(response.status() == CheckoutStatus.PAYMENT_SUCCEEDED, "successful mock payment should return succeeded checkout response");
    }

    private static void returnsExistingCheckoutForSameOrderAndIdempotencyKeyWithoutChargingAgain() {
        Order order = order();
        InMemoryCheckoutRepository checkoutRepository = new InMemoryCheckoutRepository();
        CountingPaymentGateway paymentGateway = new CountingPaymentGateway(new MockPaymentGateway());
        CheckoutUseCase useCase = useCaseWith(order, checkoutRepository, new InMemoryDeadLetterRepository(), paymentGateway);

        CheckoutResponse first = useCase.checkout(ORDER_ID, new CheckoutRequest("same-key", "tok_success"));
        CheckoutResponse duplicate = useCase.checkout(ORDER_ID, new CheckoutRequest("same-key", "tok_retry_success"));

        require(duplicate.checkoutId() == first.checkoutId(), "duplicate checkout should return existing attempt response");
        require(order.checkoutAttempts().size() == 1, "duplicate checkout should not create a second attempt");
        require(paymentGateway.callCount == 1, "duplicate checkout should not call payment gateway again");
    }

    private static void throwsNotFoundWhenOrderDoesNotExist() {
        CheckoutUseCase useCase = new CheckoutUseCase(
            new InMemoryOrderRepository(List.of()),
            new InMemoryCheckoutRepository(),
            new InMemoryDeadLetterRepository(),
            new MockPaymentGateway(),
            FIXED_TIME
        );

        try {
            useCase.checkout(ORDER_ID, new CheckoutRequest("checkout-1", "tok_success"));
            throw new AssertionError("missing order should throw ResourceNotFoundException");
        } catch (ResourceNotFoundException exception) {
            require(exception.getMessage().equals("Order 99 was not found."), "not found message should include order id");
        }
    }

    private static void throwsConflictWhenOrderIsNotDraft() {
        Order order = order();
        order.markPaid(NOW);
        CheckoutUseCase useCase = useCaseWithOrder(order);

        try {
            useCase.checkout(ORDER_ID, new CheckoutRequest("checkout-1", "tok_success"));
            throw new AssertionError("non-DRAFT order should throw ConflictException");
        } catch (ConflictException exception) {
            require(exception.getMessage().equals("Order 99 is not available for checkout."),
                "conflict message should include order id");
        }
    }

    private static void marksOrderProcessingBeforePaymentChargeStep() {
        Order order = order();
        ProcessingStateAssertingGateway paymentGateway = new ProcessingStateAssertingGateway(order);
        CheckoutUseCase useCase = useCaseWith(order, new InMemoryCheckoutRepository(), new InMemoryDeadLetterRepository(), paymentGateway);

        useCase.checkout(ORDER_ID, new CheckoutRequest("checkout-1", "tok_success"));

        require(paymentGateway.wasCalled, "payment gateway should be called");
        require(paymentGateway.observedProcessingState, "order should be CHECKOUT_PROCESSING when the payment gateway is called");
    }

    private static void recordsPaymentFailureAndDeadLetter() {
        Order order = order();
        InMemoryCheckoutRepository checkoutRepository = new InMemoryCheckoutRepository();
        InMemoryDeadLetterRepository deadLetterRepository = new InMemoryDeadLetterRepository();
        CheckoutUseCase useCase = useCaseWith(order, checkoutRepository, deadLetterRepository, new MockPaymentGateway());

        CheckoutResponse response = useCase.checkout(ORDER_ID, new CheckoutRequest("failed-key", "tok_fail"));
        CheckoutAttempt attempt = checkoutRepository.findById(response.checkoutId()).orElseThrow();

        require(response.status() == CheckoutStatus.PAYMENT_FAILED, "failed payment should return failed checkout response");
        require(response.paymentStatus() == PaymentStatus.FAILED, "failed payment should return failed payment status");
        require(response.failureReason().contains("failed"), "failed checkout response should include failure reason");
        require(order.status() == OrderStatus.DRAFT, "failed payment should roll order back to DRAFT");
        require(attempt.status() == CheckoutStatus.PAYMENT_FAILED, "checkout attempt should be marked failed");
        require(attempt.paymentTransaction().status() == PaymentStatus.FAILED, "payment transaction should be marked failed");
        require(attempt.outboxMessages().size() == 1, "failed payment should create one failed outbox message");
        require(attempt.outboxMessages().getFirst().type() == OutboxMessageType.PAYMENT_CHARGE, "failed outbox should represent payment charge");
        require(attempt.outboxMessages().getFirst().status() == OutboxStatus.FAILED, "failed outbox should be marked failed");
        require(deadLetterRepository.findRecent().size() == 1, "failed payment should create a dead-letter message");
        require(response.integrations().size() == 1, "failed response should include failed integration status");
        require(response.integrations().getFirst().status() == OutboxStatus.FAILED, "failed response integration should be failed");
    }

    private static CheckoutUseCase useCaseWithOrder(Order order) {
        return useCaseWith(order, new InMemoryCheckoutRepository(), new InMemoryDeadLetterRepository(), new MockPaymentGateway());
    }

    private static CheckoutUseCase useCaseWith(
        Order order,
        InMemoryCheckoutRepository checkoutRepository,
        InMemoryDeadLetterRepository deadLetterRepository,
        PaymentGateway paymentGateway
    ) {
        return new CheckoutUseCase(
            new InMemoryOrderRepository(List.of(order)),
            checkoutRepository,
            deadLetterRepository,
            paymentGateway,
            FIXED_TIME
        );
    }

    private static Order order() {
        Tenant tenant = new Tenant(1, "Acme", "ops@acme.example", NOW);
        return new Order(ORDER_ID, tenant.id(), tenant, "Acme onboarding", new BigDecimal("199.00"), "USD", NOW);
    }

    private static void expectValidation(Runnable action, String expectedMessage) {
        try {
            action.run();
            throw new AssertionError("invalid checkout request should throw ValidationException");
        } catch (ValidationException exception) {
            require(exception.getMessage().equals(expectedMessage), "validation message should be precise");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class CountingPaymentGateway implements PaymentGateway {
        private final PaymentGateway inner;
        private int callCount;

        private CountingPaymentGateway(PaymentGateway inner) {
            this.inner = inner;
        }

        @Override
        public PaymentGatewayResult charge(PaymentGatewayRequest request) {
            callCount++;
            return inner.charge(request);
        }
    }

    private static final class ProcessingStateAssertingGateway implements PaymentGateway {
        private final Order order;
        private boolean wasCalled;
        private boolean observedProcessingState;

        private ProcessingStateAssertingGateway(Order order) {
            this.order = order;
        }

        @Override
        public PaymentGatewayResult charge(PaymentGatewayRequest request) {
            wasCalled = true;
            observedProcessingState = order.status() == OrderStatus.CHECKOUT_PROCESSING;
            return new PaymentGatewayResult(PaymentStatus.SUCCEEDED, 1, "mock_pay", null);
        }
    }
}
