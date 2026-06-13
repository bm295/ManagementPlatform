package com.example.managementplatform;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class ManagementRepository {
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final Map<Long, CheckoutAttempt> checkouts = new ConcurrentHashMap<>();
    private final AtomicLong checkoutIds = new AtomicLong(1000);

    ManagementRepository() {
        var tenant = new Tenant(1, "Demo Tenant", "ops@example.com");
        save(new Order(1, tenant, "Catalog redesign", new BigDecimal("1250.00"), "USD", Instant.now().minusSeconds(7200)));
        save(new Order(2, tenant, "Warehouse setup", new BigDecimal("799.99"), "USD", Instant.now().minusSeconds(3600)));
        save(new Order(3, tenant, "Annual support", new BigDecimal("5000.00"), "USD", Instant.now().minusSeconds(1800)));
    }

    List<Order> search(String name, int page, int pageSize) {
        var normalized = name == null ? "" : name.trim().toLowerCase();
        return orders.values().stream()
            .filter(order -> normalized.isBlank() || order.name().toLowerCase().contains(normalized))
            .sorted(Comparator.comparing(Order::createdAt).reversed())
            .skip((long) Math.max(page - 1, 0) * Math.max(pageSize, 1))
            .limit(Math.max(pageSize, 1))
            .toList();
    }

    long count(String name) {
        var normalized = name == null ? "" : name.trim().toLowerCase();
        return orders.values().stream()
            .filter(order -> normalized.isBlank() || order.name().toLowerCase().contains(normalized))
            .count();
    }

    Optional<Order> findOrder(long id) { return Optional.ofNullable(orders.get(id)); }
    Optional<CheckoutAttempt> findCheckout(long id) { return Optional.ofNullable(checkouts.get(id)); }

    Optional<CheckoutAttempt> findCheckout(long orderId, String idempotencyKey) {
        return checkouts.values().stream()
            .filter(attempt -> attempt.orderId() == orderId && attempt.idempotencyKey().equals(idempotencyKey))
            .findFirst();
    }

    CheckoutAttempt createCheckout(long orderId, String idempotencyKey) {
        var attempt = new CheckoutAttempt(checkoutIds.incrementAndGet(), orderId, idempotencyKey, Instant.now());
        checkouts.put(attempt.id(), attempt);
        orders.get(orderId).checkoutAttempts().add(attempt);
        return attempt;
    }

    void save(Order order) { orders.put(order.id(), order); }
    List<CheckoutAttempt> allCheckouts() { return new ArrayList<>(checkouts.values()); }
}
