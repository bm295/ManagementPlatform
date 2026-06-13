package com.example.managementplatform;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

record PagedResult<T>(List<T> items, int page, int pageSize, long totalCount) { }
record OrderSummaryDto(long id, String name, BigDecimal amount, String currency, OrderStatus status, Instant createdAt) { }
record OrderDetailsDto(long id, String tenantName, String name, BigDecimal amount, String currency, OrderStatus status,
                       Instant createdAt, Instant paidAt, List<CheckoutResponse> checkouts) { }
record CheckoutRequest(String idempotencyKey, String paymentMethodToken) { }
record CheckoutResponse(long checkoutId, long orderId, CheckoutStatus status, PaymentStatus paymentStatus,
                        String failureReason, List<IntegrationStatusDto> integrations) { }
record IntegrationStatusDto(OutboxMessageType type, OutboxStatus status, int attemptCount, String lastError) { }
record DeadLetterMessageDto(long checkoutAttemptId, OutboxMessageType type, String failureReason, int attemptCount) { }
