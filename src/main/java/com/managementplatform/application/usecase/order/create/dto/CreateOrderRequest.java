package com.managementplatform.application.usecase.order.create.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(
    long tenantId,
    String tenantName,
    String tenantEmail,
    String name,
    BigDecimal amount,
    String currency
) {}
