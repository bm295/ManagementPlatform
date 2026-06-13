package com.example.managementplatform;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

enum OrderStatus { DRAFT, CHECKOUT_PROCESSING, PAID }
enum CheckoutStatus { PAYMENT_PENDING, PAYMENT_FAILED, PAYMENT_SUCCEEDED }
enum PaymentStatus { FAILED, SUCCEEDED }
enum OutboxStatus { PENDING, PROCESSING, SUCCEEDED, FAILED }
enum OutboxMessageType { SEND_CHECKOUT_EMAIL, PUSH_TO_PRODUCTION, PAYMENT_CHARGE }

record Tenant(long id, String name, String email) { }

final class Order {
    private final long id;
    private final Tenant tenant;
    private final String name;
    private final BigDecimal amount;
    private final String currency;
    private final Instant createdAt;
    private OrderStatus status = OrderStatus.DRAFT;
    private Instant paidAt;
    private final List<CheckoutAttempt> checkoutAttempts = new ArrayList<>();

    Order(long id, Tenant tenant, String name, BigDecimal amount, String currency, Instant createdAt) {
        this.id = id;
        this.tenant = tenant;
        this.name = name;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    long id() { return id; }
    Tenant tenant() { return tenant; }
    String name() { return name; }
    BigDecimal amount() { return amount; }
    String currency() { return currency; }
    Instant createdAt() { return createdAt; }
    OrderStatus status() { return status; }
    Instant paidAt() { return paidAt; }
    List<CheckoutAttempt> checkoutAttempts() { return checkoutAttempts; }
    void markProcessing() { status = OrderStatus.CHECKOUT_PROCESSING; }
    void markDraft() { status = OrderStatus.DRAFT; }
    void markPaid(Instant when) { status = OrderStatus.PAID; paidAt = when; }
}

final class CheckoutAttempt {
    private final long id;
    private final long orderId;
    private final String idempotencyKey;
    private final Instant createdAt;
    private CheckoutStatus status = CheckoutStatus.PAYMENT_PENDING;
    private Instant completedAt;
    private String failureReason;
    private PaymentTransaction paymentTransaction;
    private final List<OutboxMessage> outboxMessages = new ArrayList<>();

    CheckoutAttempt(long id, long orderId, String idempotencyKey, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    long id() { return id; }
    long orderId() { return orderId; }
    String idempotencyKey() { return idempotencyKey; }
    Instant createdAt() { return createdAt; }
    CheckoutStatus status() { return status; }
    Instant completedAt() { return completedAt; }
    String failureReason() { return failureReason; }
    PaymentTransaction paymentTransaction() { return paymentTransaction; }
    List<OutboxMessage> outboxMessages() { return outboxMessages; }
    void fail(String reason, Instant when) { status = CheckoutStatus.PAYMENT_FAILED; failureReason = reason; completedAt = when; }
    void succeed(Instant when) { status = CheckoutStatus.PAYMENT_SUCCEEDED; completedAt = when; }
    void paymentTransaction(PaymentTransaction paymentTransaction) { this.paymentTransaction = paymentTransaction; }
}

record PaymentTransaction(long checkoutAttemptId, PaymentStatus status, int attemptCount, BigDecimal amount,
                          String currency, String providerTransactionId, String failureReason, Instant createdAt) { }
record OutboxMessage(long checkoutAttemptId, OutboxMessageType type, OutboxStatus status, String payload,
                     int attemptCount, String lastError, Instant createdAt) { }
