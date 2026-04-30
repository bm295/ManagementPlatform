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
  Status tinyint NOT NULL,
  CreatedAt datetimeoffset NOT NULL,
  PaidAt datetimeoffset NULL,
  CONSTRAINT FK_Orders_Tenants FOREIGN KEY (TenantId) REFERENCES Tenants(Id)
);
CREATE INDEX IX_Orders_Name ON Orders(Name);

CREATE TABLE CheckoutAttempts (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  OrderId bigint NOT NULL,
  IdempotencyKey nvarchar(120) NOT NULL,
  Status tinyint NOT NULL,
  FailureReason nvarchar(500) NULL,
  CreatedAt datetimeoffset NOT NULL,
  CompletedAt datetimeoffset NULL,
  CONSTRAINT FK_CheckoutAttempts_Orders FOREIGN KEY (OrderId) REFERENCES Orders(Id)
);
CREATE UNIQUE INDEX IX_CheckoutAttempts_OrderId_IdempotencyKey
  ON CheckoutAttempts(OrderId, IdempotencyKey);

CREATE TABLE PaymentTransactions (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  CheckoutAttemptId bigint NOT NULL,
  Status tinyint NOT NULL,
  AttemptCount int NOT NULL,
  Amount decimal(18,2) NOT NULL,
  Currency nvarchar(3) NOT NULL,
  ProviderTransactionId nvarchar(120) NULL,
  FailureReason nvarchar(500) NULL,
  CreatedAt datetimeoffset NOT NULL,
  CONSTRAINT FK_PaymentTransactions_CheckoutAttempts FOREIGN KEY (CheckoutAttemptId) REFERENCES CheckoutAttempts(Id)
);
CREATE UNIQUE INDEX IX_PaymentTransactions_ProviderTransactionId
  ON PaymentTransactions(ProviderTransactionId)
  WHERE ProviderTransactionId IS NOT NULL;

CREATE TABLE Invoices (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  CheckoutAttemptId bigint NOT NULL,
  Status tinyint NOT NULL,
  FailureReason nvarchar(500) NULL,
  CreatedAt datetimeoffset NOT NULL,
  CompletedAt datetimeoffset NULL,
  CONSTRAINT FK_Invoices_CheckoutAttempts FOREIGN KEY (CheckoutAttemptId) REFERENCES CheckoutAttempts(Id)
);
CREATE UNIQUE INDEX IX_Invoices_CheckoutAttemptId ON Invoices(CheckoutAttemptId);

CREATE TABLE OutboxMessages (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  CheckoutAttemptId bigint NOT NULL,
  Type nvarchar(60) NOT NULL,
  Status tinyint NOT NULL,
  PayloadJson nvarchar(max) NOT NULL,
  AttemptCount int NOT NULL,
  LastError nvarchar(1000) NULL,
  CreatedAt datetimeoffset NOT NULL,
  NextAttemptAt datetimeoffset NULL,
  LockedAt datetimeoffset NULL,
  ProcessedAt datetimeoffset NULL,
  CONSTRAINT FK_OutboxMessages_CheckoutAttempts FOREIGN KEY (CheckoutAttemptId) REFERENCES CheckoutAttempts(Id)
);
CREATE INDEX IX_OutboxMessages_Status_NextAttemptAt
  ON OutboxMessages(Status, NextAttemptAt);

CREATE TABLE DeadLetterMessages (
  Id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
  OutboxMessageId bigint NOT NULL,
  CheckoutAttemptId bigint NOT NULL,
  Type nvarchar(60) NOT NULL,
  PayloadJson nvarchar(max) NOT NULL,
  AttemptCount int NOT NULL,
  FailureReason nvarchar(1000) NOT NULL,
  FailedAt datetimeoffset NOT NULL,
  CONSTRAINT FK_DeadLetterMessages_OutboxMessages FOREIGN KEY (OutboxMessageId) REFERENCES OutboxMessages(Id)
);
CREATE UNIQUE INDEX IX_DeadLetterMessages_OutboxMessageId
  ON DeadLetterMessages(OutboxMessageId);
```

## Relationships

- one `Tenants` row to many `Orders` rows
- one `Orders` row to many `CheckoutAttempts` rows
- one `CheckoutAttempts` row to one `PaymentTransactions` row
- one `CheckoutAttempts` row to one `Invoices` row
- one `CheckoutAttempts` row to many `OutboxMessages` rows
