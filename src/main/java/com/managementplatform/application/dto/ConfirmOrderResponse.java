package com.managementplatform.application.dto;

import com.managementplatform.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ConfirmOrderResponse(long orderId, OrderStatus status, String productSku, int reservedQuantity,
                                   String warehouse, BigDecimal expectedReceivable, long shipmentRequestId,
                                   Instant confirmedAt) {
}
