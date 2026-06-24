# Java API compatibility check

This note records the compatibility review between the Java demo API, the current API contract in `docs/04-api.md`, and the C# ASP.NET controllers/middleware.

## Contract source

The public API contract is `docs/04-api.md`.

## Routes

The Java routes match the C# controller routes:

| API | C# controller | Java route | Result |
| --- | --- | --- | --- |
| `GET /api/orders?name=&page=&pageSize=` | `OrdersController.Search` | `/api/orders` with `GET` | Match |
| `GET /api/orders/{orderId}` | `OrdersController.Get` | `/api/orders/{id}` with `GET` | Match |
| `POST /api/orders/{orderId}/checkout` | `OrdersController.Checkout` | `/api/orders/{id}/checkout` with `POST` | Match |
| `GET /api/checkouts/{checkoutId}` | `CheckoutsController.Get` | `/api/checkouts/{id}` with `GET` | Match |
| `GET /api/dead-letters` | `DeadLettersController.Get` | `/api/dead-letters` with `GET` | Match |

## Status codes and errors

The Java API returns the same status-code classes as the C# middleware for application errors:

| Scenario | C# middleware/controller status | Java status | Result |
| --- | --- | --- | --- |
| Validation error | `400 Bad Request` | `400 Bad Request` | Match |
| Missing order/checkout | `404 Not Found` | `404 Not Found` | Match |
| Checkout state conflict | `409 Conflict` | `409 Conflict` | Match |
| Unexpected server error | `500 Internal Server Error` | `500 Internal Server Error` | Match |
| Successful checkout result | `200 OK` | `200 OK` | Match |
| Failed payment checkout result | `200 OK` | `200 OK` | Match |

Intentional difference: the Java problem JSON uses simpler titles such as `Bad Request`, `Not Found`, and `Conflict`, while the C# middleware uses titles such as `Request validation failed`, `Resource not found`, and `Request conflicts with current state`. The status code, content type, detail, and route behavior are kept compatible.

## Response comparison

| API | C# response shape | Java response shape | Result |
| --- | --- | --- | --- |
| `GET /api/orders` | `items`, `page`, `pageSize`, `totalCount`; each item has `id`, `name`, `tenantName`, `amount`, `currency`, `status`, `createdAt` | Same | Match |
| `GET /api/orders/{id}` | `id`, `name`, `tenantName`, `tenantEmail`, `amount`, `currency`, `status`, `createdAt`, `paidAt` | Same | Match |
| `POST /api/orders/{id}/checkout` | `checkoutId`, `orderId`, `status`, `paymentStatus`, `failureReason`, `integrations[]` | Same | Match |
| `GET /api/checkouts/{id}` | Same as checkout response | Same | Match |
| `GET /api/dead-letters` | Array items with `id`, `checkoutAttemptId`, `outboxMessageId`, `type`, `attemptCount`, `failureReason`, `failedAt` | Same | Match |

## Intentional differences

- Java uses a lightweight in-memory implementation and manually builds JSON for the rewrite/demo path; C# uses ASP.NET Core controllers, DTO serialization, EF-backed repositories, and `ProblemDetails` middleware.
- Java stores the full failed checkout debug payload internally on outbox/dead-letter records, but does not expose that payload from `GET /api/dead-letters` because the current public API contract does not include a `payload` field.
