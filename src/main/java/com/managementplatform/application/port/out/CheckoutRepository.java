package com.managementplatform.application.port.out;

import com.managementplatform.domain.model.CheckoutAttempt;
import java.util.Optional;

public interface CheckoutRepository {
    Optional<CheckoutAttempt> findById(long id);
    Optional<CheckoutAttempt> findByOrderIdAndIdempotencyKey(long orderId, String idempotencyKey);
    void save(CheckoutAttempt attempt);
}
