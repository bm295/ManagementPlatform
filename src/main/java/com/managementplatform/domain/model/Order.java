package com.managementplatform.domain.model;

import com.managementplatform.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Order {
    private final long id;
    private final long tenantId;
    private final Tenant tenant;
    private final String name;
    private final BigDecimal amount;
    private final String currency;
    private final Instant createdAt;
    private final String productSku;
    private final int quantity;
    private OrderStatus status = OrderStatus.DRAFT;
    private Instant paidAt;
    private Instant confirmedAt;
    private final List<CheckoutAttempt> checkoutAttempts = new ArrayList<>();

    public Order(long id, long tenantId, Tenant tenant, String name, BigDecimal amount, String currency, Instant createdAt) {
        this(id, tenantId, tenant, name, amount, currency, createdAt, null, 0);
    }

    public Order(long id, long tenantId, Tenant tenant, String name, BigDecimal amount, String currency, Instant createdAt, String productSku, int quantity) {
        this.id = id;
        this.tenantId = tenantId;
        this.tenant = tenant;
        this.name = name;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.productSku = productSku;
        this.quantity = quantity;
    }

    public long id() {
        return id;
    }

    public long tenantId() {
        return tenantId;
    }

    public Tenant tenant() {
        return tenant;
    }

    public String name() {
        return name;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String productSku() { return productSku; }

    public int quantity() { return quantity; }

    public Instant confirmedAt() { return confirmedAt; }

    public OrderStatus status() {
        return status;
    }

    public Instant paidAt() {
        return paidAt;
    }

    public List<CheckoutAttempt> checkoutAttempts() {
        return Collections.unmodifiableList(checkoutAttempts);
    }

    public void addCheckoutAttempt(CheckoutAttempt attempt) {
        checkoutAttempts.add(attempt);
    }

    public void markProcessing() {
        status = OrderStatus.CHECKOUT_PROCESSING;
    }

    public void confirm(Instant when) {
        if (status != OrderStatus.DRAFT) throw new IllegalStateException("Only draft orders can be confirmed.");
        status = OrderStatus.CONFIRMED;
        confirmedAt = when;
    }

    public void markDraft() {
        status = OrderStatus.DRAFT;
    }

    public void markFailed() {
        status = OrderStatus.FAILED;
    }

    public void markPaid(Instant when) {
        status = OrderStatus.PAID;
        paidAt = when;
    }
}
