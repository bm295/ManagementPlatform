package com.managementplatform.infrastructure.repository;

import com.managementplatform.domain.model.Order;
import com.managementplatform.domain.model.Tenant;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryOrderRepositoryGlobalQueryFilterTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void defaultFilterAppliesToIdSearchCountAndPagination() {
        Tenant active = tenant(1, true);
        Tenant inactive = tenant(2, false);
        InMemoryOrderRepository repository = new InMemoryOrderRepository(List.of(
            order(1, active, "visible one"),
            order(2, inactive, "hidden order"),
            order(3, active, "visible two")
        ));

        assertTrue(repository.findById(1).isPresent());
        assertTrue(repository.findById(2).isEmpty());
        assertEquals(List.of(1L), repository.search(null, 1, 1).stream().map(Order::id).toList());
        assertEquals(List.of(3L), repository.search(null, 2, 1).stream().map(Order::id).toList());
        assertEquals(2, repository.count(null));
        assertEquals(1, repository.count("two"));
        assertEquals(0, repository.count("hidden"));
        assertTrue(repository.search("hidden", 1, 10).isEmpty());
    }

    @Test
    void privilegedBypassIsExplicitAndDoesNotChangeDefaultVisibility() {
        Tenant inactive = tenant(2, false);
        InMemoryOrderRepository repository = new InMemoryOrderRepository(List.of(order(2, inactive, "hidden")));
        assertTrue(repository.findByIdIgnoringFilters(2).isPresent());
        assertTrue(repository.findById(2).isEmpty());
        assertEquals(0, repository.count(null));
        assertTrue(repository.findTenantById(2).isPresent());
    }

    @Test
    void trustedAdditionalPredicateIsCombinedWithDefaultPolicy() {
        Tenant active = tenant(1, true);
        InMemoryOrderRepository repository = new InMemoryOrderRepository(
            List.of(order(1, active, "alpha"), order(2, active, "beta")),
            OrderQueryFilter.activeTenantsOnly().and(order -> order.id() == 1)
        );
        assertEquals(List.of(1L), repository.search(null, 1, 10).stream().map(Order::id).toList());
        assertEquals(1, repository.count(null));
        assertTrue(repository.findById(2).isEmpty());
        assertThrows(NullPointerException.class, () -> new InMemoryOrderRepository(List.of(), null));
    }

    private static Tenant tenant(long id, boolean active) {
        return new Tenant(id, "Tenant " + id, "tenant@example.com", NOW, active, new BigDecimal("1000"));
    }

    private static Order order(long id, Tenant tenant, String name) {
        return new Order(id, tenant.id(), tenant, name, new BigDecimal("10"), "VND", NOW);
    }
}
