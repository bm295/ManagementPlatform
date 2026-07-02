package com.managementplatform.domain.enums;

public enum OrderStatus {
    DRAFT("Draft"),
    CHECKOUT_PROCESSING("CheckoutProcessing"),
    FAILED("Failed"),
    PAID("Paid");

    private final String apiName;

    OrderStatus(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }
}
