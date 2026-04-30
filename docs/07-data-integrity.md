# Data Integrity

This file describes data integrity behavior in this demo only.

- Checkout is idempotent per `(OrderId, IdempotencyKey)`, enforced by a unique constraint and by application lookup behavior.
- After successful payment, checkout state, payment transaction, invoice creation, and outbox message creation are saved together in one unit of work.
- If payment fails, checkout is marked failed and the order is returned to `Draft`; no outbox follow-up messages are created.
- Order state flow is restricted in code: only `Draft` orders can start checkout, and successful payment moves the order to `Paid`.
- Retry is bounded:
  - payment retry uses configured max attempts/delay
  - outbox retry uses configured polling, batch size, and max attempts
- Dead-letter handling is explicit: when an outbox message reaches retry limit, it is marked `Failed` and copied to `DeadLetterMessages`.
- Key uniqueness and linkage used by this demo:
  - unique `(OrderId, IdempotencyKey)` on `CheckoutAttempts`
  - unique nullable `ProviderTransactionId` on `PaymentTransactions`
  - unique `OutboxMessageId` on `DeadLetterMessages`
  - foreign keys between tenant/order/checkout/payment/invoice/outbox tables
  - `DeadLetterMessages` is linked by `OutboxMessageId` convention and uniqueness (no database foreign key constraint)
