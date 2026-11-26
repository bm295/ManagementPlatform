package com.managementplatform.infrastructure.repository;

import com.managementplatform.application.port.out.OrderRepository;
import com.managementplatform.domain.model.Order;
import com.managementplatform.domain.model.Tenant;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory order repository backed by a {@link ConcurrentHashMap}.
 *
 * <p>All regular reads share one default visibility policy. Administrative reads
 * that intentionally bypass it are explicit and separate from the application port.</p>
 */
public final class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentMap<Long, Order> ordersById = new ConcurrentHashMap<>();
    private final OrderQueryFilter queryFilter;

    public InMemoryOrderRepository() {
        this.queryFilter = OrderQueryFilter.activeTenantsOnly();
        seedDemoOrders();
    }

    public InMemoryOrderRepository(Collection<Order> seedOrders) {
        this(seedOrders, OrderQueryFilter.activeTenantsOnly());
    }

    public InMemoryOrderRepository(Collection<Order> seedOrders, OrderQueryFilter queryFilter) {
        this.queryFilter = Objects.requireNonNull(queryFilter, "queryFilter is required.");
        Objects.requireNonNull(seedOrders, "seedOrders is required.").forEach(this::save);
    }

    @Override
    public Optional<Order> findById(long orderId) {
        return Optional.ofNullable(ordersById.get(orderId)).filter(queryFilter::allows);
    }

    /** Explicit trusted recovery/admin read; never expose this directly through public HTTP routes. */
    public Optional<Order> findByIdIgnoringFilters(long orderId) {
        return Optional.ofNullable(ordersById.get(orderId));
    }

    @Override
    public Optional<Tenant> findTenantById(long tenantId) {
        // Tenant identity lookup must not silently create a duplicate inactive tenant.
        return ordersById.values().stream()
            .filter(order -> order.tenantId() == tenantId)
            .map(Order::tenant)
            .findFirst();
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

    @Override
    public Order save(Order order) {
        Order required = Objects.requireNonNull(order, "order is required.");
        ordersById.put(required.id(), required);
        return required;
    }

    private List<Order> filteredOrders(String name) {
        String normalizedName = normalize(name);
        return ordersById.values()
            .stream()
            .filter(queryFilter::allows)
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
        save(new Order(4, globex.id(), globex, "Globex premium support", new BigDecimal("149.00"), "USD", baseTime.plusSeconds(480)));
        save(new Order(5, acme.id(), acme, "Acme migration services", new BigDecimal("899.00"), "USD", baseTime.plusSeconds(600)));
    }
}
