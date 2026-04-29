# Data Model

This repo stores application data in SQL Server through EF Core.

## Full table schema

```sql
CREATE TABLE Tenants (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  Name nvarchar(200) NOT NULL,
  Email nvarchar(320) NOT NULL,
  CreatedAt datetimeoffset NOT NULL
);

CREATE TABLE Orders (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  TenantId bigint NOT NULL,
  Name nvarchar(240) NOT NULL,
  Amount decimal(18,2) NOT NULL,
  Currency nvarchar(3) NOT NULL,
  Status nvarchar(40) NOT NULL,
  CreatedAt datetimeoffset NOT NULL,
  PaidAt datetimeoffset NULL,
  CONSTRAINT FK_Orders_Tenants FOREIGN KEY (TenantId) REFERENCES Tenants(Id)
);

CREATE TABLE CheckoutAttempts (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  OrderId bigint NOT NULL,
  IdempotencyKey nvarchar(120) NOT NULL,
  Status nvarchar(40) NOT NULL,
  FailureReason nvarchar(500) NULL,
  CreatedAt datetimeoffset NOT NULL,
  CompletedAt datetimeoffset NULL,
  CONSTRAINT FK_CheckoutAttempts_Orders FOREIGN KEY (OrderId) REFERENCES Orders(Id)
);

CREATE TABLE PaymentTransactions (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  CheckoutAttemptId bigint NOT NULL,
  Status nvarchar(40) NOT NULL,
  AttemptCount int NOT NULL,
  Amount decimal(18,2) NOT NULL,
  Currency nvarchar(3) NOT NULL,
  ProviderTransactionId nvarchar(120) NULL,
  FailureReason nvarchar(500) NULL,
  CreatedAt datetimeoffset NOT NULL,
  CONSTRAINT FK_PaymentTransactions_CheckoutAttempts FOREIGN KEY (CheckoutAttemptId) REFERENCES CheckoutAttempts(Id)
);

CREATE TABLE Invoices (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  CheckoutAttemptId bigint NOT NULL,
  Status nvarchar(40) NOT NULL,
  FailureReason nvarchar(500) NULL,
  CreatedAt datetimeoffset NOT NULL,
  CompletedAt datetimeoffset NULL,
  CONSTRAINT FK_Invoices_CheckoutAttempts FOREIGN KEY (CheckoutAttemptId) REFERENCES CheckoutAttempts(Id)
);

CREATE TABLE OutboxMessages (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  CheckoutAttemptId bigint NOT NULL,
  Type nvarchar(60) NOT NULL,
  Status nvarchar(40) NOT NULL,
  PayloadJson nvarchar(max) NOT NULL,
  Attempts int NOT NULL,
  LastError nvarchar(1000) NULL,
  CreatedAt datetimeoffset NOT NULL,
  NextAttemptAt datetimeoffset NULL,
  LockedAt datetimeoffset NULL,
  ProcessedAt datetimeoffset NULL,
  CONSTRAINT FK_OutboxMessages_CheckoutAttempts FOREIGN KEY (CheckoutAttemptId) REFERENCES CheckoutAttempts(Id)
);

CREATE TABLE DeadLetterMessages (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  OutboxMessageId bigint NOT NULL,
  CheckoutAttemptId bigint NOT NULL,
  Type nvarchar(60) NOT NULL,
  PayloadJson nvarchar(max) NOT NULL,
  AttemptCount int NOT NULL,
  FailureReason nvarchar(max) NOT NULL,
  FailedAt datetimeoffset NOT NULL,
  CONSTRAINT FK_DeadLetterMessages_OutboxMessages FOREIGN KEY (OutboxMessageId) REFERENCES OutboxMessages(Id)
);
```

## Relationships

- one `Tenants` row to many `Orders` rows
- one `Orders` row to many `CheckoutAttempts` rows
- one `CheckoutAttempts` row to one `PaymentTransactions` row
- one `CheckoutAttempts` row to one `Invoices` row
- one `CheckoutAttempts` row to many `OutboxMessages` rows

## Constraints and indexes

- `Tenants.Email` is unique
- `CheckoutAttempts(OrderId, IdempotencyKey)` is unique
- `PaymentTransactions.ProviderTransactionId` is unique (when non-null)
- `DeadLetterMessages.OutboxMessageId` is unique
- `Orders.Name` is indexed for search
- `OutboxMessages(Status, NextAttemptAt)` is indexed for polling
