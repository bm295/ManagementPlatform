package com.managementplatform.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;

public final class Tenant {
    private final long id;
    private final String name;
    private final String email;
    private final Instant createdAt;
    private final boolean active;
    private final BigDecimal creditLimit;
    private final String creditLimitCurrency;
    private BigDecimal receivableBalance = BigDecimal.ZERO;
    private final List<Order> orders = new ArrayList<>();

    public Tenant(long id, String name, String email, Instant createdAt) {
        this(id, name, email, createdAt, true, new BigDecimal("100000000"));
    }

    public Tenant(long id, String name, String email, Instant createdAt, boolean active, BigDecimal creditLimit) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
        this.active = active;
        this.creditLimit = creditLimit;
        this.creditLimitCurrency = "VND";
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean active() { return active; }

    public BigDecimal creditLimit() { return creditLimit; }

    public String creditLimitCurrency() { return creditLimitCurrency; }

    public BigDecimal receivableBalance() { return receivableBalance; }

    public BigDecimal availableCredit() { return creditLimit.subtract(receivableBalance); }

    public void recordReceivable(BigDecimal amount) { receivableBalance = receivableBalance.add(amount); }

    public List<Order> orders() {
        return Collections.unmodifiableList(orders);
    }

    public void addOrder(Order order) {
        orders.add(order);
    }
}
