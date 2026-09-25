package com.managementplatform.infrastructure.repository;

import com.managementplatform.application.port.out.OrderFulfillmentRepository;
import com.managementplatform.domain.model.ExpectedReceivable;
import com.managementplatform.domain.model.ShipmentRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryOrderFulfillmentRepository implements OrderFulfillmentRepository {
    private final java.util.Map<StockKey, Integer> available = new java.util.HashMap<>();
    private final List<ShipmentRequest> shipmentRequests = new ArrayList<>();
    private final List<ExpectedReceivable> expectedReceivables = new ArrayList<>();
    private final AtomicLong shipmentIds = new AtomicLong(1);

    public InMemoryOrderFulfillmentRepository() {
        available.put(new StockKey(normalize("Hồ Chí Minh"), normalize("ERP-PRO")), 10);
    }

    @Override
    public synchronized int available(String warehouse, String productSku) {
        return available.getOrDefault(new StockKey(normalize(warehouse), normalize(productSku)), 0);
    }

    @Override
    public synchronized void reserve(String warehouse, String productSku, int quantity) {
        StockKey key = new StockKey(normalize(warehouse), normalize(productSku));
        int current = available.getOrDefault(key, 0);
        if (quantity > current) throw new IllegalStateException("Insufficient stock for reservation.");
        available.put(key, current - quantity);
    }

    @Override
    public synchronized ExpectedReceivable recordExpectedReceivable(long orderId, long tenantId, BigDecimal amount, String currency, Instant createdAt) {
        ExpectedReceivable entry = new ExpectedReceivable(orderId, tenantId, amount, currency, createdAt);
        expectedReceivables.add(entry);
        return entry;
    }

    @Override
    public synchronized ShipmentRequest createShipmentRequest(long orderId, String warehouse, String productSku, int quantity, Instant createdAt) {
        ShipmentRequest request = new ShipmentRequest(shipmentIds.getAndIncrement(), orderId, warehouse, productSku, quantity, createdAt);
        shipmentRequests.add(request);
        return request;
    }

    @Override
    public synchronized List<ShipmentRequest> shipmentRequests() { return List.copyOf(shipmentRequests); }

    @Override
    public synchronized List<ExpectedReceivable> expectedReceivables() { return List.copyOf(expectedReceivables); }

    private static String normalize(String value) { return value.trim().toUpperCase(java.util.Locale.ROOT); }

    private record StockKey(String warehouse, String productSku) { }
}
