package com.managementplatform.domain.enums;

public enum OutboxMessageType {
    SEND_CHECKOUT_EMAIL,
    PUSH_TO_PRODUCTION,
    PAYMENT_CHARGE
}
