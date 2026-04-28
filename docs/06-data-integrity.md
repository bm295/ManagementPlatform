# Data Integrity

This demo already has a few basic rules to keep the data correct.

- Checkout is idempotent. The same order and `idempotencyKey` cannot create duplicate checkout records.
- The database has unique constraints for important fields such as tenant email, payment provider transaction id, dead letter outbox message id, and `(OrderId, IdempotencyKey)`.
- After a successful payment, the system saves the payment result and the outbox messages in the same save operation.
- If payment fails, the order goes back to `Draft` and no follow-up work is created.
- Temporary payment errors can be retried, but only up to the configured limit.
- A paid or processing order cannot be checked out again with a different key.
- Foreign keys keep the links between tenant, order, checkout, payment, invoice, and outbox data valid.
- If an outbox message reaches its retry limit, it is moved to the dead letter table instead of being lost.

For a real project, I would keep the same ideas and make them stricter:

- Keep a clear state flow for orders and checkouts. For example: `Draft -> CheckoutProcessing -> Paid`.
- Let the database block bad writes where possible. Unique indexes and foreign keys are safer than relying only on application code.
- Keep checkout idempotent for every client request. Client retries should reuse the same `idempotencyKey`.
- Save payment success and outbox work together, so the system does not mark an order as paid and then forget the next steps.
- Treat payment as the source of truth for whether the order is paid. Later email or production failures should not change payment history.
- Use concurrency protection in a bigger system, so two checkout requests cannot update the same order without detection.
- Keep a simple audit trail for important actions such as checkout started, payment succeeded, payment failed, invoice succeeded, and production push failed.
- Retry only safe operations. Payment should retry only temporary errors. Outbox messages can use automatic retry.
- Keep failed outbox messages visible in a dead letter state so someone can review them later.
- Add simple reports or alerts for stuck data, such as orders left in `CheckoutProcessing` too long or outbox messages left in `Pending` too long.
