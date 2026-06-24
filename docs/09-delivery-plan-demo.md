# Delivery Plan for Demo Build

This plan is for the current demo scope only. The target is 4 days, or 32 hours total.

## Day 1 - 8 hours

- Create the solution and project structure.
- Add the main domain model with tenants, orders, checkouts, payments, invoices, outbox messages, and dead letters.
- Set up in-memory repositories with in-memory demo storage.
- Add seed data for demo tenants and orders.

## Day 2 - 8 hours

- Build order search and order detail APIs.
- Build the checkout flow with idempotency and conflict handling.
- Handle successful and failed payments with the mock payment gateway.
- Save payment records, invoice rows, and outbox messages.

## Day 3 - 8 hours

- Add the mock email and production integrations.
- Let the production integration also create the invoice as part of the production step.
- Add the outbox worker with retry and dead letter support.
- Add the demo web page for searching orders and running checkout.

## Day 4 - 8 hours

- Add tests for the main API and service flows.
- Add Docker support for the API and in-memory repositories.
- Write the README and the design docs.
- Run the demo from a clean start and fix the issues that block the walkthrough.

## Estimated Total

- 32 hours

## Scope for Demo

- Mock external services only.
- Focus on the main checkout flow from order search to production handoff.
- Keep the setup simple enough to run locally with Docker.
- Keep payment retry small and safe. Retry only temporary payment failures.
- Keep dead letter support limited to failed outbox messages.
- Do not add production-grade auth, secret management, or full monitoring.

## Done Criteria

- The app starts with Docker.
- Demo tenants and orders are available after startup.
- Orders can be searched by name and viewed in the demo page.
- Checkout can show both payment success and payment failure.
- Checkout can show retryable payment failure that later succeeds.
- Successful checkout creates email and production follow-up work.
- The production step marks the invoice as succeeded, and the production outbox status shows whether handoff succeeded.
- Failed outbox messages can be reviewed in the dead letter list.
- The demo page is enough to explain the flow in a live walkthrough.
