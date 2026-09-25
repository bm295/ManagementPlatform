package com.managementplatform.application.usecase.order.create;

import com.managementplatform.application.usecase.order.create.dto.CreateOrderRequest;
import com.managementplatform.application.usecase.order.create.dto.OrderResponse;
import com.managementplatform.application.port.in.order.create.CreateOrderInputPort;
import com.managementplatform.application.port.out.OrderRepository;
import com.managementplatform.application.port.out.TimeProvider;
import com.managementplatform.domain.enums.OrderStatus;
import com.managementplatform.domain.model.Order;
import com.managementplatform.domain.model.Tenant;
import com.managementplatform.shared.exception.ValidationException;
import com.managementplatform.shared.exception.ConflictException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class CreateOrderUseCase implements CreateOrderInputPort {
    private final OrderRepository orderRepository;
    private final TimeProvider timeProvider;
    private final AtomicLong orderIds = new AtomicLong(1000);

    public CreateOrderUseCase(OrderRepository orderRepository, TimeProvider timeProvider) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository is required.");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider is required.");
    }

    @Override
    public synchronized OrderResponse create(CreateOrderRequest request) {
        validate(request);
        Tenant tenant = orderRepository.findTenantById(request.tenantId()).orElse(null);
        if (tenant != null && (!tenant.name().equals(request.tenantName().trim()) || !tenant.email().equals(request.tenantEmail().trim()))) {
            throw new ConflictException("Customer details do not match tenantId %d.".formatted(request.tenantId()));
        }
        if (tenant == null) tenant = new Tenant(request.tenantId(), request.tenantName().trim(), request.tenantEmail().trim(), now());
        Order order = new Order(nextOrderId(), tenant.id(), tenant, request.name().trim(), request.amount(), request.currency().trim().toUpperCase(), now(), request.productSku() == null ? null : request.productSku().trim().toUpperCase(), request.quantity() == null ? 0 : request.quantity());
        tenant.addOrder(order);
        Order saved = orderRepository.save(order);
        return new OrderResponse(saved.id(), saved.tenantId(), tenant.name(), tenant.email(), saved.name(), saved.amount(), saved.currency(), OrderStatus.DRAFT, saved.createdAt(), saved.paidAt(), saved.productSku(), saved.quantity());
    }

    private static void validate(CreateOrderRequest request) {
        if (request == null) throw new ValidationException("Create order request is required.");
        if (request.tenantId() < 1) throw new ValidationException("tenantId must be greater than zero.");
        if (isBlank(request.tenantName())) throw new ValidationException("tenantName is required.");
        if (isBlank(request.tenantEmail())) throw new ValidationException("tenantEmail is required.");
        if (isBlank(request.name())) throw new ValidationException("name is required.");
        if (request.amount() == null) throw new ValidationException("amount is required.");
        if (request.amount().signum() < 0) throw new ValidationException("amount must be zero or greater.");
        if (isBlank(request.currency())) throw new ValidationException("currency is required.");
        if ((request.productSku() == null) != (request.quantity() == null)) throw new ValidationException("productSku and quantity must be provided together.");
        if (request.productSku() != null && (isBlank(request.productSku()) || request.quantity() < 1)) throw new ValidationException("Order line requires a productSku and a quantity greater than zero.");
    }

    private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private long nextOrderId() { return orderIds.getAndIncrement(); }
    private java.time.Instant now() { return timeProvider.now(); }
}
