package com.managementplatform.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Tenant {
    private final long id;
    private final String name;
    private final String email;
    private final Instant createdAt;
    private final List<Order> orders = new ArrayList<>();

    public Tenant(long id, String name, String email, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
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

    public List<Order> orders() {
        return Collections.unmodifiableList(orders);
    }

    public void addOrder(Order order) {
        orders.add(order);
    }
}
