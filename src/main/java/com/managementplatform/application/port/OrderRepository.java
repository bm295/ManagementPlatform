package com.managementplatform.application.port;

import com.managementplatform.domain.model.Order;
import java.util.List;
import java.util.Optional;

/**
 * Application port for reading and persisting orders without coupling use cases
 * to a concrete storage technology.
 */
public interface OrderRepository {
    /**
     * Finds an order by its stable identifier.
     *
     * @param orderId order identifier from the API route or use case input
     * @return the matching order when present
     */
    Optional<Order> findById(long orderId);

    /**
     * Searches orders by optional name text and returns a single page.
     *
     * @param name optional order name filter; implementations should treat null or blank as no filter
     * @param page one-based page number
     * @param pageSize maximum number of orders to return
     * @return orders for the requested page in deterministic order
     */
    List<Order> search(String name, int page, int pageSize);

    /**
     * Counts all orders matching the same filter used by {@link #search(String, int, int)}.
     *
     * @param name optional order name filter; implementations should treat null or blank as no filter
     * @return total number of matching orders before pagination
     */
    long count(String name);
}
