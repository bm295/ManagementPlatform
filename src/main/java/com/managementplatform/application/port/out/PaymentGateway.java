package com.managementplatform.application.port.out;

public interface PaymentGateway {
    PaymentGatewayResult charge(PaymentGatewayRequest request);
}
