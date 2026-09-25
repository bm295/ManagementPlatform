package com.managementplatform.application.usecase.order.create.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(
    long tenantId,
    String tenantName,
    String tenantEmail,
    String name,
    BigDecimal amount,
    String currency,
    String productSku,
    Integer quantity
) {
    public CreateOrderRequest(long tenantId, String tenantName, String tenantEmail, String name, BigDecimal amount, String currency) {
        this(tenantId, tenantName, tenantEmail, name, amount, currency, null, null);
    }
}
