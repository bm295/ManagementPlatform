package com.managementplatform.infrastructure.repository;

import com.managementplatform.domain.model.Order;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Default read visibility for orders, analogous to an EF Core global query filter.
 *
 * <p>All public repository reads use this policy. The predicate is intentionally
 * stateless and application-wide: a tenant-scoped filter requires authenticated,
 * request-bound identity and must never be driven by a caller-supplied tenant id.</p>
 */
public final class OrderQueryFilter {
    private final Predicate<Order> visibility;

    private OrderQueryFilter(Predicate<Order> visibility) {
        this.visibility = Objects.requireNonNull(visibility, "visibility is required.");
    }

    /** Hide orders belonging to inactive customers by default. */
    public static OrderQueryFilter activeTenantsOnly() {
        return new OrderQueryFilter(order -> order.tenant() != null && order.tenant().active());
    }

    /** Combine a baseline filter with another trusted, application-defined predicate. */
    public OrderQueryFilter and(Predicate<Order> additionalPredicate) {
        Objects.requireNonNull(additionalPredicate, "additionalPredicate is required.");
        return new OrderQueryFilter(visibility.and(additionalPredicate));
    }

    boolean allows(Order order) {
        return order != null && visibility.test(order);
    }
}
