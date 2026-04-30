# Time Log

| Activity | Time |
| --- | ---: |
| Modular monolith scaffold, SQL Server persistence and migrations, checkout/payment retry and dead-letter flow, outbox worker and mock integrations, REST API and demo UI, automated tests, Docker setup, and supporting documentation. | 2.0h |
| Replace Customer with Tenant across domain, persistence, tests, and migrations; simplify invoice handling and align the schema and related code. | 3.0h |
| Replace GUID-based entities with numeric ID domain models (Order, Tenant, CheckoutAttempt, PaymentTransaction, Invoice, OutboxMessage, DeadLetterMessage), refactor app ports/services/controllers/repos and seeding for long IDs, update mock integrations and EF migrations/schema docs to bigint identity columns. | 1.0h |
| Convert status storage to numeric tinyint with migration/index updates, rename `OutboxMessage.Attempts` to `AttemptCount` across layers and UI, switch outbox settings to `IConfiguration` defaults, rename DI/session abstractions (`*ServiceCollectionExtensions`, `IAppDbSession`), and update dev config plus README/docs for enums and outbox behavior. | 2.0h |
| Implement dead-letter queue support end-to-end: add `IAppDbContext`, introduce `DeadLetterMessage` and FK alignment with `OutboxMessage`, extend checkout/payment-failure and outbox handling, update repositories/DI extensions (`ApplicationExtensions`, `InfrastructureExtensions`), add/adjust EF migrations, and expand integration tests. | 3.0h |
| **Total** | **11.0h** |
