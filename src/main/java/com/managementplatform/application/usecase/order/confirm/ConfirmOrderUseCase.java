package com.managementplatform.application.usecase.order.confirm;

import com.managementplatform.application.dto.ConfirmOrderRequest;
import com.managementplatform.application.dto.ConfirmOrderResponse;
import com.managementplatform.application.port.in.order.confirm.ConfirmOrderInputPort;
import com.managementplatform.application.port.out.OrderFulfillmentRepository;
import com.managementplatform.application.port.out.OrderRepository;
import com.managementplatform.application.port.out.TimeProvider;
import com.managementplatform.domain.enums.OrderStatus;
import com.managementplatform.domain.model.Order;
import com.managementplatform.domain.model.ShipmentRequest;
import com.managementplatform.shared.exception.ConflictException;
import com.managementplatform.shared.exception.ResourceNotFoundException;
import com.managementplatform.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.util.Objects;

/** Confirms a draft sales order after checking customer credit and warehouse stock. */
public final class ConfirmOrderUseCase implements ConfirmOrderInputPort {
    private final OrderRepository orderRepository;
    private final OrderFulfillmentRepository fulfillmentRepository;
    private final TimeProvider timeProvider;

    public ConfirmOrderUseCase(OrderRepository orderRepository, OrderFulfillmentRepository fulfillmentRepository, TimeProvider timeProvider) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository is required.");
        this.fulfillmentRepository = Objects.requireNonNull(fulfillmentRepository, "fulfillmentRepository is required.");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider is required.");
    }

    @Override
    public synchronized ConfirmOrderResponse confirm(long orderId, ConfirmOrderRequest request) {
        if (orderId < 1) throw new ValidationException("orderId must be greater than zero.");
        if (request == null || isBlank(request.warehouse())) throw new ValidationException("warehouse is required.");

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order %d was not found.".formatted(orderId)));
        if (order.status() != OrderStatus.DRAFT) throw new ConflictException("Only draft orders can be confirmed.");
        if (!order.tenant().active()) throw new ConflictException("Inactive customers cannot place orders.");
        if (isBlank(order.productSku()) || order.quantity() < 1) throw new ValidationException("Order must contain a product and a positive quantity before confirmation.");
        if (!order.currency().equalsIgnoreCase(order.tenant().creditLimitCurrency())) throw new ConflictException("Order currency must match the customer's credit limit currency.");

        BigDecimal availableCredit = order.tenant().availableCredit();
        if (order.amount().compareTo(availableCredit) > 0) throw new ConflictException("Order amount exceeds the customer's available credit.");
        String warehouse = request.warehouse().trim();
        int availableStock = fulfillmentRepository.available(warehouse, order.productSku());
        if (availableStock < order.quantity()) throw new ConflictException("Insufficient stock in warehouse '%s'.".formatted(warehouse));

        var now = timeProvider.now();
        fulfillmentRepository.reserve(warehouse, order.productSku(), order.quantity());
        fulfillmentRepository.recordExpectedReceivable(order.id(), order.tenantId(), order.amount(), order.currency(), now);
        order.tenant().recordReceivable(order.amount());
        ShipmentRequest shipment = fulfillmentRepository.createShipmentRequest(order.id(), warehouse, order.productSku(), order.quantity(), now);
        order.confirm(now);
        orderRepository.save(order);

        return new ConfirmOrderResponse(order.id(), order.status(), order.productSku(), order.quantity(), warehouse,
            order.amount(), shipment.id(), order.confirmedAt());
    }

    private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
