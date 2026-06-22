package com.managementplatform.domain.enums;

public enum CheckoutStatus {
    PAYMENT_PENDING("PaymentPending"),
    PAYMENT_FAILED("PaymentFailed"),
    PAYMENT_SUCCEEDED("PaymentSucceeded");

    private final String apiName;

    CheckoutStatus(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }
}
