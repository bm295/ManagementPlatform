package com.example.managementplatform;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

class CheckoutService {
    private final ManagementRepository repository;

    CheckoutService(ManagementRepository repository) {
        this.repository = repository;
    }

    synchronized CheckoutResponse checkout(long orderId, CheckoutRequest request) {
        var idempotencyKey = requireText(request.idempotencyKey(), "An idempotency key is required.");
        var token = requireText(request.paymentMethodToken(), "A payment method token is required.");
        return repository.findCheckout(orderId, idempotencyKey).map(this::toResponse).orElseGet(() -> createCheckout(orderId, idempotencyKey, token));
    }

    CheckoutResponse get(long checkoutId) {
        return repository.findCheckout(checkoutId).map(this::toResponse)
            .orElseThrow(() -> new ApiException(404, "Checkout was not found."));
    }

    private CheckoutResponse createCheckout(long orderId, String idempotencyKey, String token) {
        var order = repository.findOrder(orderId)
            .orElseThrow(() -> new ApiException(404, "Order was not found."));
        if (order.status() != OrderStatus.DRAFT) {
            throw new ApiException(409, "Order is already paid or being processed.");
        }

        order.markProcessing();
        var attempt = repository.createCheckout(order.id(), idempotencyKey);
        var payment = charge(order, attempt, token);
        attempt.paymentTransaction(payment);

        if (payment.status() == PaymentStatus.FAILED) {
            var reason = payment.failureReason() == null ? "Payment was declined." : payment.failureReason();
            attempt.fail(reason, Instant.now());
            order.markDraft();
            attempt.outboxMessages().add(new OutboxMessage(attempt.id(), OutboxMessageType.PAYMENT_CHARGE, OutboxStatus.FAILED,
                "{\"orderId\":" + order.id() + ",\"checkoutAttemptId\":" + attempt.id() + "}", payment.attemptCount(), reason, Instant.now()));
            return toResponse(attempt);
        }

        attempt.succeed(Instant.now());
        order.markPaid(Instant.now());
        attempt.outboxMessages().add(new OutboxMessage(attempt.id(), OutboxMessageType.SEND_CHECKOUT_EMAIL, OutboxStatus.PENDING,
            "{\"orderId\":" + order.id() + ",\"email\":\"" + order.tenant().email() + "\"}", 0, null, Instant.now()));
        attempt.outboxMessages().add(new OutboxMessage(attempt.id(), OutboxMessageType.PUSH_TO_PRODUCTION, OutboxStatus.PENDING,
            "{\"orderId\":" + order.id() + ",\"name\":\"" + order.name() + "\"}", 0, null, Instant.now()));
        return toResponse(attempt);
    }

    private PaymentTransaction charge(Order order, CheckoutAttempt attempt, String token) {
        var normalized = token.toLowerCase();
        if (normalized.contains("fail") || normalized.contains("decline")) {
            return new PaymentTransaction(attempt.id(), PaymentStatus.FAILED, 1, order.amount(), order.currency(), null,
                "Mock payment gateway declined the payment.", Instant.now());
        }
        return new PaymentTransaction(attempt.id(), PaymentStatus.SUCCEEDED, normalized.contains("retry") ? 2 : 1,
            order.amount(), order.currency(), "txn_" + UUID.randomUUID(), null, Instant.now());
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(400, message);
        }
        return value.trim();
    }

    CheckoutResponse toResponse(CheckoutAttempt attempt) {
        var integrations = attempt.outboxMessages().stream()
            .sorted(Comparator.comparing(OutboxMessage::type))
            .map(message -> new IntegrationStatusDto(message.type(), message.status(), message.attemptCount(), message.lastError()))
            .toList();
        var payment = attempt.paymentTransaction();
        return new CheckoutResponse(attempt.id(), attempt.orderId(), attempt.status(), payment == null ? null : payment.status(),
            attempt.failureReason() == null && payment != null ? payment.failureReason() : attempt.failureReason(), integrations);
    }
}
