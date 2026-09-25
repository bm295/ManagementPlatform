# Data Model

The Java demo stores data in memory for the lifetime of the process. Repositories are seeded when the HTTP server starts.

## Domain objects

- `Tenant`: customer organization for an order, including active status, credit limit, and expected receivable balance.
- `Order`: searchable sales order with amount, currency, optional product line, status, tenant details, and checkout attempts.
- `ExpectedReceivable`: projected accounts-receivable entry created when a sales order is confirmed.
- `ShipmentRequest`: warehouse fulfillment request created when a sales order is confirmed.
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
- one confirmed `Order` to one `ExpectedReceivable` and one `ShipmentRequest`

## Storage notes

The repositories under `src/main/java/com/managementplatform/infrastructure/repository` use in-memory collections. The demo seeds 10 `ERP-PRO` units at `Hồ Chí Minh`; newly created order tenants default to active with a 100,000,000 credit limit. Data is reset whenever the app restarts.
