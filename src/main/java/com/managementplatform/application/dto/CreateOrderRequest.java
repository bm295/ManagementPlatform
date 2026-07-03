package com.managementplatform.application.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(
    long tenantId,
    String tenantName,
    String tenantEmail,
    String name,
    BigDecimal amount,
    String currency
) {}
