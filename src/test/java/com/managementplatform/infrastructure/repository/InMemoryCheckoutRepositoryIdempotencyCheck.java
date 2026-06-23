package com.managementplatform.infrastructure.repository;

import com.managementplatform.domain.model.CheckoutAttempt;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class InMemoryCheckoutRepositoryIdempotencyCheck {
    private static final long ORDER_ID = 42L;
    private static final String IDEMPOTENCY_KEY = "same-request";

    private InMemoryCheckoutRepositoryIdempotencyCheck() {
    }

    public static void main(String[] args) throws Exception {
        returnsExistingAttemptForSameOrderAndIdempotencyKey();
        concurrentSavesForSameOrderAndIdempotencyKeyKeepOneAttempt();
    }

    private static void returnsExistingAttemptForSameOrderAndIdempotencyKey() {
        InMemoryCheckoutRepository repository = new InMemoryCheckoutRepository();
        CheckoutAttempt first = attempt(1);
        CheckoutAttempt duplicate = attempt(2);

        CheckoutAttempt savedFirst = repository.saveIfAbsentByIdempotencyKey(first);
        CheckoutAttempt savedDuplicate = repository.saveIfAbsentByIdempotencyKey(duplicate);

        require(savedFirst == first, "first attempt should be saved");
        require(savedDuplicate == first, "duplicate idempotency key should return existing attempt");
        require(repository.findById(1).orElseThrow() == first, "first attempt should be stored by id");
        require(repository.findById(2).isEmpty(), "duplicate attempt should not be stored by id");
        require(repository.findByOrderIdAndIdempotencyKey(ORDER_ID, IDEMPOTENCY_KEY).orElseThrow() == first,
            "idempotency lookup should point to first attempt");
    }

    private static void concurrentSavesForSameOrderAndIdempotencyKeyKeepOneAttempt() throws Exception {
        InMemoryCheckoutRepository repository = new InMemoryCheckoutRepository();
        int requestCount = 16;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CheckoutAttempt>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                long attemptId = index + 1L;
                futures.add(executor.submit(concurrentSave(repository, ready, start, attemptId)));
            }

            require(ready.await(5, TimeUnit.SECONDS), "workers should be ready before the concurrent start");
            start.countDown();

            CheckoutAttempt winner = null;
            for (Future<CheckoutAttempt> future : futures) {
                CheckoutAttempt saved = future.get(5, TimeUnit.SECONDS);
                if (winner == null) {
                    winner = saved;
                }
                require(saved == winner, "all concurrent saves should return the same saved attempt");
            }

            require(winner != null, "one attempt should win the idempotency reservation");
            Optional<CheckoutAttempt> lookup = repository.findByOrderIdAndIdempotencyKey(ORDER_ID, IDEMPOTENCY_KEY);
            require(lookup.orElseThrow() == winner, "idempotency lookup should return the winning attempt");

            long storedAttempts = 0;
            for (long attemptId = 1; attemptId <= requestCount; attemptId++) {
                if (repository.findById(attemptId).isPresent()) {
                    storedAttempts++;
                }
            }
            require(storedAttempts == 1, "only one attempt should be stored for concurrent duplicate requests");
        } finally {
            executor.shutdownNow();
        }
    }

    private static Callable<CheckoutAttempt> concurrentSave(
        InMemoryCheckoutRepository repository,
        CountDownLatch ready,
        CountDownLatch start,
        long attemptId
    ) {
        return () -> {
            ready.countDown();
            require(start.await(5, TimeUnit.SECONDS), "worker should be released by the start latch");
            return repository.saveIfAbsentByIdempotencyKey(attempt(attemptId));
        };
    }

    private static CheckoutAttempt attempt(long id) {
        return new CheckoutAttempt(id, ORDER_ID, IDEMPOTENCY_KEY, Instant.parse("2026-04-28T08:00:00Z"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
