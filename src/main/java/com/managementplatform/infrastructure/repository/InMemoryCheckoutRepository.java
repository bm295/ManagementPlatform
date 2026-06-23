package com.managementplatform.infrastructure.repository;

import com.managementplatform.application.port.CheckoutRepository;
import com.managementplatform.domain.model.CheckoutAttempt;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Thread-safe in-memory checkout repository with an idempotency lookup index.
 */
public final class InMemoryCheckoutRepository implements CheckoutRepository {
    private final ConcurrentMap<Long, CheckoutAttempt> attemptsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<IdempotencyKey, Long> attemptIdsByIdempotencyKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Object> orderLocks = new ConcurrentHashMap<>();

    @Override
    public Optional<CheckoutAttempt> findById(long checkoutId) {
        return Optional.ofNullable(attemptsById.get(checkoutId));
    }

    @Override
    public Optional<CheckoutAttempt> findByOrderIdAndIdempotencyKey(long orderId, String idempotencyKey) {
        synchronized (lockForOrder(orderId)) {
            Long checkoutId = attemptIdsByIdempotencyKey.get(new IdempotencyKey(orderId, idempotencyKey));
            return checkoutId == null ? Optional.empty() : findById(checkoutId);
        }
    }

    @Override
    public void save(CheckoutAttempt attempt) {
        saveIfAbsentByIdempotencyKey(attempt);
    }

    /**
     * Runs a checkout operation while holding the lock for one order.
     *
     * <p>Use this around the complete checkout check-and-create sequence (load order, check status, create attempt,
     * and update order state) so concurrent requests for the same order cannot both observe the order as available.
     * Requests for different orders still proceed independently.</p>
     *
     * @param orderId order whose checkout flow must be serialized
     * @param operation checkout operation to run
     * @param <T> operation result type
     * @return result from the operation
     */
    public <T> T runWithOrderLock(long orderId, Supplier<T> operation) {
        synchronized (lockForOrder(orderId)) {
            return operation.get();
        }
    }

    /**
     * Atomically saves the attempt for its order/idempotency-key pair if no attempt already owns the pair.
     *
     * <p>If another request already created an attempt for the same idempotency key, this method returns that
     * existing attempt and leaves both maps unchanged.</p>
     *
     * @param attempt checkout attempt to reserve
     * @return the saved attempt, or the existing attempt for the same order/idempotency-key pair
     */
    public CheckoutAttempt saveIfAbsentByIdempotencyKey(CheckoutAttempt attempt) {
        return runWithOrderLock(attempt.orderId(), () -> {
            IdempotencyKey key = new IdempotencyKey(attempt.orderId(), attempt.idempotencyKey());
            Long existingAttemptId = attemptIdsByIdempotencyKey.get(key);
            if (existingAttemptId != null) {
                return attemptsById.get(existingAttemptId);
            }

            attemptsById.put(attempt.id(), attempt);
            attemptIdsByIdempotencyKey.put(key, attempt.id());
            return attempt;
        });
    }

    private Object lockForOrder(long orderId) {
        return orderLocks.computeIfAbsent(orderId, ignored -> new Object());
    }

    private record IdempotencyKey(long orderId, String idempotencyKey) {
    }
}
