package com.managementplatform.application.port.out;

import com.managementplatform.domain.model.ExpectedReceivable;
import com.managementplatform.domain.model.ShipmentRequest;
import java.util.List;

public interface OrderFulfillmentRepository {
    int available(String warehouse, String productSku);
    void reserve(String warehouse, String productSku, int quantity);
    ExpectedReceivable recordExpectedReceivable(long orderId, long tenantId, java.math.BigDecimal amount, String currency, java.time.Instant createdAt);
    ShipmentRequest createShipmentRequest(long orderId, String warehouse, String productSku, int quantity, java.time.Instant createdAt);
    List<ShipmentRequest> shipmentRequests();
    List<ExpectedReceivable> expectedReceivables();
}
