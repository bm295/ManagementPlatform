# Flow Sequence

See [05-flow-sequence.png](</abs/path/d:/Code/ManagementPlatform/docs/05-flow-sequence.png>).

Use this flow for one end-to-end diagram:

1. Browser demo searches orders through the API.
2. API reads matching orders from SQL Server and returns the paged result.
3. User selects one order.
4. Browser demo requests order details through the API.
5. API reads the order and tenant data from SQL Server and returns the order details.
6. User submits checkout with `idempotencyKey` and `paymentMethodToken`.
7. API loads the order for checkout and checks whether the order is still in `Draft`.
8. API creates a `CheckoutAttempt` row and updates the order status to `CheckoutProcessing`.
9. API calls the payment gateway and retries only when the payment error is marked retryable.

If payment fails:

1. API stores a `PaymentTransactions` row with failed status.
2. API updates the `CheckoutAttempt` to `PaymentFailed`.
3. API resets the order status back to `Draft`.
4. API returns the failed checkout response.
5. No outbox messages are created.

If payment succeeds:

1. API stores a `PaymentTransactions` row with succeeded status.
2. API updates the `CheckoutAttempt` to `PaymentSucceeded`.
3. API updates the order status to `Paid`.
4. API creates one `Invoices` row with pending status.
5. API creates two `OutboxMessages` rows:
   one for checkout email
   one for production push
6. API returns the successful checkout response.
7. Browser demo can request checkout status through `GET /api/checkouts/{checkoutId}`.
8. API reads the checkout result and outbox status from SQL Server and returns it.

Background worker flow after successful payment:

1. Outbox worker polls SQL Server for pending `OutboxMessages`.
2. Worker processes the email message by calling the mock email service.
3. Worker processes the production message by calling the mock production service.
4. Worker updates the existing `Invoices` row based on production processing result:
   set `Succeeded` on success, or `Failed` with failure reason on error.
5. Worker marks each outbox message as succeeded, or schedules retry / dead-letter if it fails.
