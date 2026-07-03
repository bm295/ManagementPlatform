package com.managementplatform.application.port.in;

import com.managementplatform.application.dto.CheckoutRequest;
import com.managementplatform.application.dto.CheckoutResponse;

public interface CheckoutInputPort {
    CheckoutResponse checkout(long orderId, CheckoutRequest request);

    CheckoutResponse retryPayment(long checkoutId, CheckoutRequest request);
}
