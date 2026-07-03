package com.managementplatform.application.port.out;

import com.managementplatform.domain.model.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(long id);
    List<Order> search(String name, int page, int pageSize);
    long count(String name);
    Order save(Order order);
}
