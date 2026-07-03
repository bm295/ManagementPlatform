package com.managementplatform.application.port.in;

import com.managementplatform.application.dto.CreateOrderRequest;
import com.managementplatform.application.dto.OrderResponse;

public interface CreateOrderInputPort {
    OrderResponse create(CreateOrderRequest request);
}
