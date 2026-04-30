# Performance and Reliability

This demo includes a few simple performance and reliability choices.

- Order search is paged and indexed by name.
- Checkout does not wait for email or production calls to finish.
- Payment retry is bounded by configured max attempts and delay, so temporary failures can recover without creating long-running requests.
- The outbox worker polls pending messages in small batches and retries failures.
- Outbox retry uses exponential backoff (`2^attempt` seconds) capped at 60 seconds.
- Failed outbox messages are moved to a dead letter table after the retry limit is reached.
- Mock integrations can be configured to succeed or fail for payment, email, and production calls.
- The app can apply EF Core migrations and seed demo data on startup when `Database:ApplyMigrationsOnStartup` is enabled.
