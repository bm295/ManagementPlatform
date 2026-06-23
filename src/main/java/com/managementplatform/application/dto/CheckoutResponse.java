package com.managementplatform.application.dto;

import com.managementplatform.domain.enums.CheckoutStatus;
import com.managementplatform.domain.enums.PaymentStatus;
import java.util.List;

/**
 * Response DTO for checkout commands and checkout status queries.
 */
public record CheckoutResponse(
    long checkoutId,
    long orderId,
    CheckoutStatus status,
    PaymentStatus paymentStatus,
    String failureReason,
    List<IntegrationStatusDto> integrations
) {
}
