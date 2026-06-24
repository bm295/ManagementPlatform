# Data Model

The Java demo stores data in memory for the lifetime of the process. Repositories are seeded when the HTTP server starts.

## Domain objects

- `Tenant`: customer organization for an order.
- `Order`: searchable order with amount, currency, status, tenant details, and checkout attempts.
- `CheckoutAttempt`: idempotent checkout record for an order.
- `PaymentTransaction`: payment result attached to a checkout attempt.
- `Invoice`: invoice state model kept for API compatibility and future extension.
- `OutboxMessage`: integration-style status record returned with checkout responses.
- `DeadLetterMessage`: failure record created when payment fails terminally.

## Relationships

- one `Tenant` to many `Order` objects
- one `Order` to many `CheckoutAttempt` objects
- one `CheckoutAttempt` to one `PaymentTransaction`
- one `CheckoutAttempt` to many `OutboxMessage` objects
- one failed `OutboxMessage` to one `DeadLetterMessage`

## Storage notes

The repositories under `src/main/java/com/managementplatform/infrastructure/repository` use in-memory collections. Data is reset whenever the app restarts.
