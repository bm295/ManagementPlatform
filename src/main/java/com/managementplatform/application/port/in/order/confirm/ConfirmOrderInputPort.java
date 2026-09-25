package com.managementplatform.application.port.in.order.confirm;

import com.managementplatform.application.dto.ConfirmOrderRequest;
import com.managementplatform.application.dto.ConfirmOrderResponse;

public interface ConfirmOrderInputPort {
    ConfirmOrderResponse confirm(long orderId, ConfirmOrderRequest request);
}
