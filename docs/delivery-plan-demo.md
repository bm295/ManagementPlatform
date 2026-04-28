# Delivery Plan for a Demo Build

This plan is for a short demo build that shows the main idea in 3-4 days.

## Day 1 - 6 to 8 hours

- Create the solution and project structure.
- Add the main domain model.
- Set up SQL Server with Docker.
- Add seed data for demo orders.

## Day 2 - 6 to 8 hours

- Build order search and order detail APIs.
- Build checkout flow with idempotency.
- Handle successful and failed payments with mock services.
- Save payment records and outbox messages.

## Day 3 - 6 to 8 hours

- Add mock email, invoice, and production services.
- Add the outbox worker.
- Add the demo web page for searching orders and running checkout.
- Add simple tests for the main flow.
- If time allows, add bounded retry for temporary payment failures and a dead letter list for failed outbox messages.

## Day 4 - 4 to 6 hours

- Clean up the demo flow and fix issues.
- Add Docker support for the API and database.
- Write the README, system design, and time log.
- Check that the demo can run from a clean start.
- If time is left, pick 1-3 small improvements from [demo-extra-work.md](D:/Code/ManagementPlatform/docs/demo-extra-work.md).

## Estimated Total

- About 22 to 30 hours

## Scope for Demo

- Mock external services only.
- Focus on the main checkout flow.
- Focus on simple setup and clear demo steps.
- Extra demo work is optional and should stay small.
- Payment retry in the demo should be small and safe. Retry only temporary payment failures and keep idempotency in place.
- Dead letter queue support should stay limited to failed outbox messages.
- No production-grade auth, secrets management, or advanced monitoring.

## Done Criteria

- The app starts with Docker.
- Demo orders are available.
- Orders can be searched by name.
- Checkout can show both success and failure.
- Checkout can show retryable payment failure that later succeeds.
- Success creates email, invoice, and production follow-up work.
- Failed outbox messages can be reviewed in a dead letter list.
- The demo page is enough to explain the flow in a live walkthrough.
