# Performance and Reliability

This demo includes a few simple performance and reliability choices.

- Order search is paged and runs against seeded in-memory data.
- Checkout records an outbox-style integration status without requiring an external service call in the request path.
- Mock payment behavior supports success, terminal failure, and retry-like outcomes for demo coverage.
- Failed payment attempts create dead-letter records so failures can be inspected through the API.
- Data is process-local and resets when the Java app restarts.
