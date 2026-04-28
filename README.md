# Management Platform

This is an ASP.NET Core Web API for searching orders and checking them out.

It uses:

- SQL Server for data storage
- EF Core for database access and migrations
- mock services for payment, email, invoice, and production system calls
- an outbox worker for retrying work that happens after payment
- a hexagonal architecture style inside a modular monolith
- a small browser demo for the checkout flow

## Run Locally

Start the app:

```powershell
docker compose up -d
```

Open:

```text
http://localhost:5247
```

Use this only after changing API code or the Dockerfile:

```powershell
docker compose up --build -d
```

Stop the API and SQL Server:

```powershell
docker compose stop
```

## Fresh Demo Data

Use this when you want to reset the database and demo from the beginning.

```powershell
.\scripts\reset-demo.ps1
```

This removes the SQL Server Docker volume, rebuilds the API image, starts the containers, runs migrations, and adds the demo orders again.

## Demo Flow

1. Open `http://localhost:5247`.
2. Search for an order by name, for example `catalog`.
3. Select an order.
4. Choose `Successful payment` or `Failed payment`.
5. Click `Checkout selected order`.
6. Use `Reload status` to see email, invoice, and production work update.
7. Use `Retryable failure, then success` to demo payment retry.
8. Use `Retryable failure, then final failure` to demo payment retry reaching the limit.
9. Use the `Dead Letters` section to see outbox work that failed too many times.

Use a new idempotency key when you want to run a new checkout attempt. Reusing the same key returns the same checkout result.

## Test

```powershell
dotnet test
```

Some API tests use SQL Server in Docker. The service tests do not need Docker.

## Project Layout

```text
src/ManagementPlatform.Domain          Main business objects and enums
src/ManagementPlatform.Application     Use cases and ports
src/ManagementPlatform.Infrastructure  Outbound adapters: SQL Server, mocks, outbox worker
src/ManagementPlatform.Api             Inbound adapter: REST API
tests/ManagementPlatform.Tests         Unit and API tests
docs/                                  Design notes and delivery plans
```

The core of the system is `Domain` plus `Application`. The API and Infrastructure projects sit outside that core.
