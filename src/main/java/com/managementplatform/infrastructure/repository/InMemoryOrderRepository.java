package com.managementplatform.infrastructure.repository;

import com.managementplatform.application.port.OrderRepository;
import com.managementplatform.domain.model.Order;
import com.managementplatform.domain.model.Tenant;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory order repository backed by a {@link ConcurrentHashMap}.
 */
public final class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentMap<Long, Order> ordersById = new ConcurrentHashMap<>();

    public InMemoryOrderRepository() {
        seedDemoOrders();
    }

    public InMemoryOrderRepository(Collection<Order> seedOrders) {
        seedOrders.forEach(this::save);
    }

    @Override
    public Optional<Order> findById(long orderId) {
        return Optional.ofNullable(ordersById.get(orderId));
    }

    @Override
    public List<Order> search(String name, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        long skip = (long) (safePage - 1) * safePageSize;

        return filteredOrders(name)
            .stream()
            .skip(skip)
            .limit(safePageSize)
            .toList();
    }

    @Override
    public long count(String name) {
        return filteredOrders(name).size();
    }

    public void save(Order order) {
        ordersById.put(order.id(), order);
    }

    private List<Order> filteredOrders(String name) {
        String normalizedName = normalize(name);
        return ordersById.values()
            .stream()
            .filter(order -> normalizedName.isEmpty() || normalize(order.name()).contains(normalizedName))
            .sorted(Comparator.comparingLong(Order::id))
            .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void seedDemoOrders() {
        Instant baseTime = Instant.parse("2026-04-28T08:00:00Z");
        Tenant acme = new Tenant(1, "Acme Corp", "ops@acme.example", baseTime);
        Tenant globex = new Tenant(2, "Globex", "billing@globex.example", baseTime.plusSeconds(60));

        save(new Order(1, acme.id(), acme, "Acme onboarding package", new BigDecimal("199.00"), "USD", baseTime.plusSeconds(120)));
        save(new Order(2, acme.id(), acme, "Acme renewal", new BigDecimal("499.00"), "USD", baseTime.plusSeconds(240)));
        save(new Order(3, globex.id(), globex, "Globex analytics subscription", new BigDecimal("299.00"), "USD", baseTime.plusSeconds(360)));
    }
}
