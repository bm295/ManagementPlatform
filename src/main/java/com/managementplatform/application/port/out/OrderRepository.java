package com.managementplatform.application.port.out;

import com.managementplatform.domain.model.Order;
import com.managementplatform.domain.model.Tenant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(long id);
    default Optional<Tenant> findTenantById(long tenantId) { return Optional.empty(); }
    List<Order> search(String name, int page, int pageSize);
    long count(String name);
    Order save(Order order);
}
