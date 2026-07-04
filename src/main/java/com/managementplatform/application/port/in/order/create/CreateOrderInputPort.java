package com.managementplatform.application.port.in.order.create;

import com.managementplatform.application.usecase.order.create.dto.CreateOrderRequest;
import com.managementplatform.application.usecase.order.create.dto.OrderResponse;

public interface CreateOrderInputPort {
    OrderResponse create(CreateOrderRequest request);
}
