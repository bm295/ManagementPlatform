package com.managementplatform.application.port;

import com.managementplatform.domain.model.CheckoutAttempt;
import java.util.Optional;

/**
 * Application port for checkout attempt persistence and idempotency lookups.
 */
public interface CheckoutRepository {
    /**
     * Finds a checkout attempt by its stable identifier.
     *
     * @param checkoutId checkout attempt identifier from the API route or use case input
     * @return the matching checkout attempt when present
     */
    Optional<CheckoutAttempt> findById(long checkoutId);

    /**
     * Finds a checkout attempt for an order and idempotency key pair.
     *
     * @param orderId order identifier associated with the checkout attempt
     * @param idempotencyKey caller-provided key used to make checkout retries idempotent
     * @return the matching checkout attempt when present
     */
    Optional<CheckoutAttempt> findByOrderIdAndIdempotencyKey(long orderId, String idempotencyKey);

    /**
     * Persists a checkout attempt created or updated by the checkout use case.
     *
     * @param attempt checkout attempt to save
     */
    void save(CheckoutAttempt attempt);
}
