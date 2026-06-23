package com.managementplatform.application.dto;

/**
 * Request DTO for starting a checkout flow.
 *
 * @param idempotencyKey caller-provided key used to make checkout retries idempotent
 * @param paymentMethodToken token representing the selected payment method
 */
public record CheckoutRequest(
    String idempotencyKey,
    String paymentMethodToken
) {
}
