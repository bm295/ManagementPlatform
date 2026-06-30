package com.managementplatform.application.usecase;

import com.managementplatform.application.dto.CheckoutRequest;
import com.managementplatform.application.dto.CheckoutResponse;
import com.managementplatform.application.dto.IntegrationStatusDto;
import com.managementplatform.application.port.CheckoutRepository;
import com.managementplatform.application.port.DeadLetterRepository;
import com.managementplatform.application.port.OrderRepository;
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
import com.managementplatform.domain.model.DeadLetterMessage;
import com.managementplatform.domain.model.Order;
import com.managementplatform.domain.model.OutboxMessage;
import com.managementplatform.domain.model.PaymentTransaction;
import com.managementplatform.shared.exception.ConflictException;
import com.managementplatform.shared.exception.ResourceNotFoundException;
import com.managementplatform.shared.exception.ValidationException;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates the checkout flow at the application layer.
 */
public final class CheckoutUseCase {
    private final OrderRepository orderRepository;
    private final CheckoutRepository checkoutRepository;
    private final DeadLetterRepository deadLetterRepository;
    private final PaymentGateway paymentGateway;
    private final TimeProvider timeProvider;
    private final AtomicLong checkoutIds = new AtomicLong(1);
    private final AtomicLong paymentTransactionIds = new AtomicLong(1);
    private final AtomicLong outboxMessageIds = new AtomicLong(1);
    private final AtomicLong deadLetterIds = new AtomicLong(1);

    public CheckoutUseCase(
        OrderRepository orderRepository,
        CheckoutRepository checkoutRepository,
        DeadLetterRepository deadLetterRepository,
        PaymentGateway paymentGateway,
        TimeProvider timeProvider
    ) {
        this.orderRepository = orderRepository;
        this.checkoutRepository = checkoutRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.paymentGateway = paymentGateway;
        this.timeProvider = timeProvider;
    }

    /**
     * Runs a checkout request, returning a response compatible with the current API shape.
     *
     * @param orderId order identifier from the API route
     * @param request checkout request body
     * @return checkout response for a new or existing checkout attempt
     * @throws ValidationException when required checkout fields are blank
     * @throws ResourceNotFoundException when the order does not exist; HTTP adapters should return 404
     * @throws ConflictException when the order is not in DRAFT state; HTTP adapters should return 409
     */
    public CheckoutResponse checkout(long orderId, CheckoutRequest request) {
        validate(request);

        String idempotencyKey = request.idempotencyKey().trim();
        return chargeNewCheckoutWithIdempotency(orderId, idempotencyKey, request.paymentMethodToken());
    }

    /**
     * Retries payment for a failed checkout attempt using a fresh idempotency key.
     *
     * @param checkoutId failed checkout attempt identifier
     * @param request retry payment request body
     * @return checkout response for the retry attempt
     * @throws ValidationException when the request is invalid or the checkout is not failed
     * @throws ResourceNotFoundException when the checkout does not exist
     * @throws ConflictException when the order is no longer available for payment
     */
    public CheckoutResponse retryPayment(long checkoutId, CheckoutRequest request) {
        validate(request);

        CheckoutAttempt failedAttempt = checkoutRepository.findById(checkoutId)
            .orElseThrow(() -> new ResourceNotFoundException("Checkout %d was not found.".formatted(checkoutId)));

        if (failedAttempt.status() != CheckoutStatus.PAYMENT_FAILED) {
            throw new ValidationException("Checkout %d is not eligible for payment retry.".formatted(checkoutId));
        }

        String idempotencyKey = request.idempotencyKey().trim();
        if (idempotencyKey.equals(failedAttempt.idempotencyKey())) {
            throw new ValidationException("Retry idempotencyKey must be different from the failed checkout idempotencyKey.");
        }

        return chargeNewCheckoutWithIdempotency(failedAttempt.orderId(), idempotencyKey, request.paymentMethodToken());
    }

    private CheckoutResponse chargeNewCheckoutWithIdempotency(long orderId, String idempotencyKey, String paymentMethodToken) {
        return checkoutRepository.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey)
            .map(CheckoutUseCase::toResponse)
            .orElseGet(() -> createAndChargeCheckoutAttempt(orderId, idempotencyKey, paymentMethodToken));
    }

    private CheckoutResponse createAndChargeCheckoutAttempt(long orderId, String idempotencyKey, String paymentMethodToken) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order %d was not found.".formatted(orderId)));

        if (order.status() != OrderStatus.DRAFT) {
            throw new ConflictException("Order %d is not available for checkout.".formatted(orderId));
        }

        order.markProcessing();
        CheckoutAttempt attempt = new CheckoutAttempt(nextCheckoutId(), order.id(), idempotencyKey, now());
        order.addCheckoutAttempt(attempt);
        checkoutRepository.save(attempt);

        PaymentGatewayResult paymentResult = paymentGateway.charge(new PaymentGatewayRequest(
            order.id(),
            attempt.id(),
            order.amount(),
            order.currency(),
            paymentMethodToken
        ));
        recordPaymentTransaction(order, attempt, paymentResult);

        if (paymentResult.status() == PaymentStatus.FAILED) {
            handlePaymentFailure(order, attempt, paymentResult);
        } else {
            handlePaymentSuccess(order, attempt);
        }

        checkoutRepository.save(attempt);
        return toResponse(attempt);
    }

    private void recordPaymentTransaction(Order order, CheckoutAttempt attempt, PaymentGatewayResult paymentResult) {
        attempt.paymentTransaction(new PaymentTransaction(
            paymentTransactionIds.getAndIncrement(),
            attempt.id(),
            paymentResult.status(),
            paymentResult.attemptCount(),
            order.amount(),
            order.currency(),
            paymentResult.providerTransactionId(),
            paymentResult.failureReason(),
            now()
        ));
    }

    private void handlePaymentSuccess(Order order, CheckoutAttempt attempt) {
        Instant completedAt = now();
        attempt.succeed(completedAt);
        order.markPaid(completedAt);

        OutboxMessage pendingCheckoutMessage = new OutboxMessage(
            outboxMessageIds.getAndIncrement(),
            attempt.id(),
            OutboxMessageType.SEND_CHECKOUT_EMAIL,
            OutboxStatus.PENDING,
            checkoutSuccessPayload(order, attempt),
            0,
            null,
            now(),
            null,
            null,
            null,
            null
        );
        attempt.addOutboxMessage(pendingCheckoutMessage);
    }

    private void handlePaymentFailure(Order order, CheckoutAttempt attempt, PaymentGatewayResult paymentResult) {
        String failureReason = paymentResult.failureReason() == null ? "Payment was declined." : paymentResult.failureReason();
        attempt.fail(failureReason, now());
        order.markDraft();

        OutboxMessage failedPaymentMessage = new OutboxMessage(
            outboxMessageIds.getAndIncrement(),
            attempt.id(),
            OutboxMessageType.PAYMENT_CHARGE,
            OutboxStatus.FAILED,
            paymentFailurePayload(order, attempt, paymentResult),
            paymentResult.attemptCount(),
            failureReason,
            now(),
            null,
            null,
            now(),
            null
        );
        attempt.addOutboxMessage(failedPaymentMessage);

        deadLetterRepository.save(new DeadLetterMessage(
            deadLetterIds.getAndIncrement(),
            failedPaymentMessage.id(),
            attempt.id(),
            OutboxMessageType.PAYMENT_CHARGE,
            failedPaymentMessage.payloadJson(),
            paymentResult.attemptCount(),
            failureReason,
            now()
        ));
    }

    private String checkoutSuccessPayload(Order order, CheckoutAttempt attempt) {
        return "{\"orderId\":%d,\"checkoutAttemptId\":%d,\"orderName\":\"%s\",\"tenantEmail\":\"%s\",\"amount\":%s,\"currency\":\"%s\"}".formatted(
            order.id(),
            attempt.id(),
            escapeJson(order.name()),
            escapeJson(order.tenant().email()),
            order.amount(),
            escapeJson(order.currency())
        );
    }

    private String paymentFailurePayload(Order order, CheckoutAttempt attempt, PaymentGatewayResult paymentResult) {
        return "{\"order\":{\"id\":%d,\"tenantId\":%d,\"tenantName\":\"%s\",\"tenantEmail\":\"%s\",\"name\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\"},\"checkout\":{\"id\":%d,\"idempotencyKey\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\",\"completedAt\":%s},\"payment\":{\"status\":\"%s\",\"attemptCount\":%d,\"providerTransactionId\":%s,\"failureReason\":%s}}".formatted(
            order.id(),
            order.tenantId(),
            escapeJson(order.tenant().name()),
            escapeJson(order.tenant().email()),
            escapeJson(order.name()),
            order.amount(),
            escapeJson(order.currency()),
            order.status().apiName(),
            order.createdAt(),
            attempt.id(),
            escapeJson(attempt.idempotencyKey()),
            attempt.status().apiName(),
            attempt.createdAt(),
            nullableInstantJson(attempt.completedAt()),
            paymentResult.status().apiName(),
            paymentResult.attemptCount(),
            nullableStringJson(paymentResult.providerTransactionId()),
            nullableStringJson(paymentResult.failureReason())
        );
    }

    private static String nullableInstantJson(Instant value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String nullableStringJson(String value) {
        return value == null ? "null" : "\"" + escapeJson(value) + "\"";
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private long nextCheckoutId() {
        return checkoutIds.getAndIncrement();
    }

    private Instant now() {
        return timeProvider.now();
    }

    private static void validate(CheckoutRequest request) {
        if (request == null) {
            throw new ValidationException("Checkout request is required.");
        }

        if (isBlank(request.idempotencyKey())) {
            throw new ValidationException("idempotencyKey is required.");
        }

        if (isBlank(request.paymentMethodToken())) {
            throw new ValidationException("paymentMethodToken is required.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static CheckoutResponse toResponse(CheckoutAttempt attempt) {
        return new CheckoutResponse(
            attempt.id(),
            attempt.orderId(),
            attempt.status(),
            attempt.paymentTransaction() == null ? null : attempt.paymentTransaction().status(),
            attempt.failureReason() != null ? attempt.failureReason() : paymentFailureReason(attempt),
            attempt.outboxMessages()
                .stream()
                .sorted(Comparator.comparing(OutboxMessage::type))
                .map(message -> new IntegrationStatusDto(
                    message.type(),
                    message.status(),
                    message.attemptCount(),
                    message.lastError()
                ))
                .toList()
        );
    }

    private static String paymentFailureReason(CheckoutAttempt attempt) {
        return attempt.paymentTransaction() == null ? null : attempt.paymentTransaction().failureReason();
    }
}
