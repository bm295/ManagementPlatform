package com.managementplatform.domain.enums;

import java.util.List;

public final class EnumApiNameCompatibilityCheck {
    private EnumApiNameCompatibilityCheck() {
    }

    public static void main(String[] args) {
        require(List.of(OrderStatus.values()).equals(List.of(
            OrderStatus.DRAFT,
            OrderStatus.CHECKOUT_PROCESSING,
            OrderStatus.PAID
        )), "OrderStatus must keep the documented API-compatible minimum states");
        require(OrderStatus.DRAFT.apiName().equals("Draft"), "DRAFT must serialize as Draft");
        require(OrderStatus.CHECKOUT_PROCESSING.apiName().equals("CheckoutProcessing"), "CHECKOUT_PROCESSING must serialize as CheckoutProcessing");
        require(OrderStatus.PAID.apiName().equals("Paid"), "PAID must serialize as Paid");

        require(List.of(CheckoutStatus.values()).equals(List.of(
            CheckoutStatus.PAYMENT_PENDING,
            CheckoutStatus.PAYMENT_FAILED,
            CheckoutStatus.PAYMENT_SUCCEEDED
        )), "CheckoutStatus must keep the documented API-compatible minimum states");
        require(CheckoutStatus.PAYMENT_PENDING.apiName().equals("PaymentPending"), "PAYMENT_PENDING must serialize as PaymentPending");
        require(CheckoutStatus.PAYMENT_FAILED.apiName().equals("PaymentFailed"), "PAYMENT_FAILED must serialize as PaymentFailed");
        require(CheckoutStatus.PAYMENT_SUCCEEDED.apiName().equals("PaymentSucceeded"), "PAYMENT_SUCCEEDED must serialize as PaymentSucceeded");

        require(PaymentStatus.FAILED.apiName().equals("Failed"), "FAILED must serialize as Failed");
        require(PaymentStatus.SUCCEEDED.apiName().equals("Succeeded"), "SUCCEEDED must serialize as Succeeded");
        require(InvoiceStatus.PENDING.apiName().equals("Pending"), "PENDING must serialize as Pending");
        require(InvoiceStatus.SUCCEEDED.apiName().equals("Succeeded"), "SUCCEEDED must serialize as Succeeded");
        require(InvoiceStatus.FAILED.apiName().equals("Failed"), "FAILED must serialize as Failed");
        require(OutboxStatus.PENDING.apiName().equals("Pending"), "PENDING must serialize as Pending");
        require(OutboxStatus.PROCESSING.apiName().equals("Processing"), "PROCESSING must serialize as Processing");
        require(OutboxStatus.SUCCEEDED.apiName().equals("Succeeded"), "SUCCEEDED must serialize as Succeeded");
        require(OutboxStatus.FAILED.apiName().equals("Failed"), "FAILED must serialize as Failed");
        require(OutboxMessageType.SEND_CHECKOUT_EMAIL.apiName().equals("SendCheckoutEmail"), "SEND_CHECKOUT_EMAIL must serialize as SendCheckoutEmail");
        require(OutboxMessageType.PUSH_TO_PRODUCTION.apiName().equals("PushToProduction"), "PUSH_TO_PRODUCTION must serialize as PushToProduction");
        require(OutboxMessageType.PAYMENT_CHARGE.apiName().equals("PaymentCharge"), "PAYMENT_CHARGE must serialize as PaymentCharge");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
