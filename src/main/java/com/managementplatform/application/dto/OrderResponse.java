package com.managementplatform.application.dto;

import com.managementplatform.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    long id,
    long tenantId,
    String tenantName,
    String tenantEmail,
    String name,
    BigDecimal amount,
    String currency,
    OrderStatus status,
    Instant createdAt,
    Instant paidAt
) {}
