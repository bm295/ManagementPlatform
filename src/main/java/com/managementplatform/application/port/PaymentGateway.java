package com.managementplatform.application.port;

/**
 * Application port for charging a checkout through an external payment provider.
 */
public interface PaymentGateway {
    /**
     * Charges the payment method described by the request.
     *
     * @param request payment request created by the checkout use case
     * @return provider charge result normalized for application logic
     */
    PaymentGatewayResult charge(PaymentGatewayRequest request);
}
