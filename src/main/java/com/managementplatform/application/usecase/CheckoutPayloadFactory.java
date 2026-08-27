package com.managementplatform.application.usecase;

import com.managementplatform.domain.model.Order;
import com.managementplatform.domain.model.PaymentTransaction;
import java.time.Instant;

final class CheckoutPayloadFactory {
    String successPayload(Order order, long checkoutAttemptId) {
        return "{\"orderId\":%d,\"checkoutAttemptId\":%d,\"orderName\":\"%s\",\"tenantEmail\":\"%s\",\"amount\":%s,\"currency\":\"%s\"}".formatted(
            order.id(),
            checkoutAttemptId,
            escapeJson(order.name()),
            escapeJson(order.tenant().email()),
            order.amount(),
            escapeJson(order.currency())
        );
    }

    String failurePayload(
        Order order,
        long checkoutAttemptId,
        Instant checkoutCreatedAt,
        String idempotencyKey,
        Instant completedAt,
        PaymentTransaction paymentTransaction
    ) {
        return new StringBuilder()
            .append("{\"order\":{")
            .append("\"id\":").append(order.id()).append(',')
            .append("\"tenantId\":").append(order.tenantId()).append(',')
            .append("\"tenantName\":\"").append(escapeJson(order.tenant().name())).append("\",")
            .append("\"tenantEmail\":\"").append(escapeJson(order.tenant().email())).append("\",")
            .append("\"name\":\"").append(escapeJson(order.name())).append("\",")
            .append("\"amount\":").append(order.amount()).append(',')
            .append("\"currency\":\"").append(escapeJson(order.currency())).append("\",")
            .append("\"status\":\"").append(order.status().apiName()).append("\",")
            .append("\"createdAt\":\"").append(order.createdAt()).append("\"},")
            .append("\"checkout\":{")
            .append("\"id\":").append(checkoutAttemptId).append(',')
            .append("\"idempotencyKey\":\"").append(escapeJson(idempotencyKey)).append("\",")
            .append("\"status\":\"").append(paymentTransaction.status().apiName()).append("\",")
            .append("\"createdAt\":\"").append(checkoutCreatedAt).append("\",")
            .append("\"completedAt\":").append(nullableInstantJson(completedAt)).append("},")
            .append("\"payment\":{")
            .append("\"status\":\"").append(paymentTransaction.status().apiName()).append("\",")
            .append("\"attemptCount\":").append(paymentTransaction.attemptCount()).append(',')
            .append("\"providerTransactionId\":").append(nullableStringJson(paymentTransaction.providerTransactionId())).append(',')
            .append("\"failureReason\":").append(nullableStringJson(paymentTransaction.failureReason()))
            .append("}}")
            .toString();
    }

    private static String nullableInstantJson(Instant value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String nullableStringJson(String value) {
        return value == null ? "null" : "\"" + escapeJson(value) + "\"";
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
