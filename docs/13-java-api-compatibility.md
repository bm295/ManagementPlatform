# Java API compatibility check

This note records the compatibility review for the current Java demo API against the public API contract in `docs/04-api.md`.

## Routes

| API | Java route | Result |
| --- | --- | --- |
| `GET /api/orders?name=&page=&pageSize=` | `/api/orders` with `GET` | Match |
| `GET /api/orders/{orderId}` | `/api/orders/{id}` with `GET` | Match |
| `POST /api/orders/{orderId}/checkout` | `/api/orders/{id}/checkout` with `POST` | Match |
| `GET /api/checkouts/{checkoutId}` | `/api/checkouts/{id}` with `GET` | Match |
| `GET /api/dead-letters` | `/api/dead-letters` with `GET` | Match |

## Response comparison

The Java API keeps the response shapes documented in `docs/04-api.md` for order search, order detail, checkout, checkout lookup, and dead-letter listing.

## Implementation notes

Java uses a lightweight in-memory implementation and manually builds JSON for the demo path. The public route behavior, status-code classes, and response field names are kept aligned with the documented API contract.
