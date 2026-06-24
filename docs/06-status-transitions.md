# Status Transitions in the Demo

This document describes how status values move for each object in this demo.

All status enums are defined under `src/main/java/com/managementplatform/domain/enums` and are serialized through their API names.

## 1) Order (`OrderStatus`)

Values:

- `Draft` (0)
- `CheckoutProcessing` (1)
- `Paid` (2)

Transitions used by the code:

- `Draft -> CheckoutProcessing`
  - when checkout starts.
- `CheckoutProcessing -> Draft`
  - when payment fails.
- `CheckoutProcessing -> Paid`
  - when payment succeeds.

Guard rule:

- Checkout can start only when current order status is `Draft`.

## 2) CheckoutAttempt (`CheckoutStatus`)

Values:

- `PaymentPending` (0)
- `PaymentFailed` (1)
- `PaymentSucceeded` (2)

Transitions used by the code:

- (new attempt) `PaymentPending`
- `PaymentPending -> PaymentFailed`
  - when payment retries end in failure.
- `PaymentPending -> PaymentSucceeded`
  - when payment succeeds.

## 3) PaymentTransaction (`PaymentStatus`)

Values:

- `Failed` (0)
- `Succeeded` (1)

Behavior:

- A payment transaction is created after payment execution.
- Its status is written once from payment result (`Succeeded` or `Failed`).

## 4) Invoice (`InvoiceStatus`)

Values:

- `Pending` (0)
- `Succeeded` (1)
- `Failed` (2)

Transitions used by the code:

- (on payment success) create invoice as `Pending`.
- `Pending -> Succeeded`
  - when `PushToProduction` outbox message is dispatched successfully.
- `Pending -> Failed`
  - when production push/outbox dispatch fails.

## 5) OutboxMessage (`OutboxStatus`)

Values:

- `Pending` (0)
- `Processing` (1)
- `Succeeded` (2)
- `Failed` (3)

Transitions used by the code:

- (new message) `Pending`
- successful checkout creates a `Pending` checkout-email message.
- terminal payment failure creates a `Failed` payment-charge message and a dead-letter record.

Retry details:

- retry count is tracked by `AttemptCount` on payment and outbox-style records.
- the current Java demo does not run a background outbox dispatcher.

## 6) DeadLetterMessage

`DeadLetterMessage` has no status enum.

A record is created when terminal payment failure creates a failed payment-charge outbox-style message.

It stores:

- outbox message id
- checkout attempt id
- message type and payload
- final `AttemptCount`
- failure reason and failure time

## Status transition source locations

- Enums: `src/main/java/com/managementplatform/domain/enums`
- Checkout state changes: `src/main/java/com/managementplatform/application/usecase/CheckoutUseCase.java`
