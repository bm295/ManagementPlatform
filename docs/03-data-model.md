# Data Model

This repo stores its application data in SQL Server through EF Core. The database schema is defined in `ApplicationDbContext` and managed with EF Core migrations.

## Tables

`Tenants`

- `Id`
- `Name`
- `Email`
- `CreatedAt`

`Orders`

- `Id`
- `TenantId`
- `Name`
- `Amount`
- `Currency`
- `Status`
- `CreatedAt`
- `PaidAt`

`CheckoutAttempts`

- `Id`
- `OrderId`
- `IdempotencyKey`
- `Status`
- `FailureReason`
- `CreatedAt`
- `CompletedAt`

`PaymentTransactions`

- `Id`
- `CheckoutAttemptId`
- `Status`
- `AttemptCount`
- `Amount`
- `Currency`
- `ProviderTransactionId`
- `FailureReason`
- `CreatedAt`

`Invoices`

- `Id`
- `CheckoutAttemptId`
- `Status`
- `FailureReason`
- `CreatedAt`
- `CompletedAt`

`OutboxMessages`

- `Id`
- `CheckoutAttemptId`
- `Type`
- `Status`
- `PayloadJson`
- `Attempts`
- `LastError`
- `CreatedAt`
- `NextAttemptAt`
- `LockedAt`
- `ProcessedAt`

`DeadLetterMessages`

- `Id`
- `OutboxMessageId`
- `CheckoutAttemptId`
- `Type`
- `PayloadJson`
- `AttemptCount`
- `FailureReason`
- `FailedAt`

## Relationships

- one `Tenants` row to many `Orders` rows
- one `Orders` row to many `CheckoutAttempts` rows
- one `CheckoutAttempts` row to one `PaymentTransactions` row
- one `CheckoutAttempts` row to one `Invoices` row
- one `CheckoutAttempts` row to many `OutboxMessages` rows

## Constraints

- `Tenants.Email` is unique
- `CheckoutAttempts(OrderId, IdempotencyKey)` is unique
- `PaymentTransactions.ProviderTransactionId` is unique
- `DeadLetterMessages.OutboxMessageId` is unique
- `Orders.Name` is indexed for search
- `OutboxMessages(Status, NextAttemptAt)` is indexed for polling

## Flow Notes

- a checkout attempt is created before payment is charged
- a failed payment stores a `PaymentTransactions` row and the order returns to `Draft`
- a successful payment stores the payment result, creates one `Invoices` row, and creates two `OutboxMessages` rows
- the outbox worker updates message state as it processes email and production work
- if an outbox message reaches its retry limit, it stays marked as `Failed` and a matching row is inserted into `DeadLetterMessages`
