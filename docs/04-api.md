# API

## `GET /api/orders?name=&page=&pageSize=`

Searches orders by name and returns a paged result.

- `name` is optional
- `page` defaults to `1`
- `pageSize` defaults to `20`

## `GET /api/orders/{orderId}`

Gets one order with tenant and payment state.

- returns `200 OK` when the order exists
- returns `404 Not Found` when the order does not exist

## `POST /api/orders/{orderId}/checkout`

Request:

```json
{
  "idempotencyKey": "client-generated-key",
  "paymentMethodToken": "tok_success"
}
```

The response includes:

- `checkoutId`
- `orderId`
- `status` (`PaymentPending`, `PaymentFailed`, `PaymentSucceeded`)
- `paymentStatus` (`Succeeded` or `Failed`, nullable)
- `failureReason` (nullable)
- `integrations[]` with `type`, `status`, `attemptCount`, and `lastError`

Use `tok_fail` to simulate a failed payment.

Use `tok_retry_success` to simulate a temporary payment failure that succeeds after retry.

Use `tok_retry_fail` to simulate a temporary payment failure that reaches the retry limit.

Status behavior:

- returns `200 OK` for both successful and failed checkout results
- returns `400 Bad Request` when the request body is invalid
- returns `404 Not Found` when the order does not exist
- returns `409 Conflict` when the order is already paid or being processed with a different idempotency key

## `GET /api/checkouts/{checkoutId}`

Gets a checkout result and the status of its follow-up work.

- returns `200 OK` when the checkout exists
- returns `404 Not Found` when the checkout does not exist

## `GET /api/dead-letters`

Gets recent outbox messages that failed too many times and were moved to the dead letter table.

Each item includes:

- `id`
- `checkoutAttemptId`
- `outboxMessageId`
- `type`
- `attemptCount`
- `failureReason`
- `failedAt`

## Error Format

Application errors are returned as `application/problem+json` using `ProblemDetails`.

- `400` for validation errors
- `404` for missing resources
- `409` for state conflicts
- `500` for unexpected server errors

## Demo Requests

The repo includes example requests in [ManagementPlatform.Api.http](/abs/path/d:/Code/ManagementPlatform/src/ManagementPlatform.Api/ManagementPlatform.Api.http).
