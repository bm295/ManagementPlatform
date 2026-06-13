package com.managementplatform.domain.model;

import com.managementplatform.domain.enums.CheckoutStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CheckoutAttempt {
    private final long id;
    private final long orderId;
    private final String idempotencyKey;
    private final Instant createdAt;
    private CheckoutStatus status = CheckoutStatus.PAYMENT_PENDING;
    private Instant completedAt;
    private String failureReason;
    private PaymentTransaction paymentTransaction;
    private final List<OutboxMessage> outboxMessages = new ArrayList<>();

    public CheckoutAttempt(long id, long orderId, String idempotencyKey, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public long id() {
        return id;
    }

    public long orderId() {
        return orderId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public CheckoutStatus status() {
        return status;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public String failureReason() {
        return failureReason;
    }

    public PaymentTransaction paymentTransaction() {
        return paymentTransaction;
    }

    public List<OutboxMessage> outboxMessages() {
        return Collections.unmodifiableList(outboxMessages);
    }

    public void addOutboxMessage(OutboxMessage message) {
        outboxMessages.add(message);
    }

    public void fail(String reason, Instant when) {
        status = CheckoutStatus.PAYMENT_FAILED;
        failureReason = reason;
        completedAt = when;
    }

    public void succeed(Instant when) {
        status = CheckoutStatus.PAYMENT_SUCCEEDED;
        completedAt = when;
    }

    public void paymentTransaction(PaymentTransaction paymentTransaction) {
        this.paymentTransaction = paymentTransaction;
    }
}
