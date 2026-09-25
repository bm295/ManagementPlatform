package com.managementplatform.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpectedReceivable(long orderId, long tenantId, BigDecimal amount, String currency, Instant createdAt) {
}
