package com.managementplatform.domain.enums;

public enum OutboxMessageType {
    SEND_CHECKOUT_EMAIL("SendCheckoutEmail"),
    PUSH_TO_PRODUCTION("PushToProduction"),
    PAYMENT_CHARGE("PaymentCharge");

    private final String apiName;

    OutboxMessageType(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }
}
