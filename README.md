# Management Platform

This repository is now a Java demo app for searching orders and checking them out.

It uses:

- Java 21
- Java built-in HTTP server
- an in-memory repository seeded with demo orders
- mock payment behavior
- idempotent checkout handling
- outbox-style integration records and dead-letter reporting

## Run the Java app locally

This repo includes a Java 21 demo API under `src/main/java/com/managementplatform`.
Build and run it with Maven and Java:

```bash
mvn package
java -jar target/management-platform-0.1.0.jar
```

The app listens on port `8080` by default. To choose another port, set `PORT` before starting the jar:

```bash
PORT=9090 java -jar target/management-platform-0.1.0.jar
```

Open the browser user interface at:

```text
http://localhost:8080/
```

The UI lets you search demo orders, view order details, submit checkouts, look up checkout status, and refresh dead-letter messages.

Or go directly to the health check:

```text
http://localhost:8080/health
```

Or run with Docker Compose:

```bash
docker compose up --build
```

The compose file maps the container to:

```text
http://localhost:5247
```

## API

Search orders:

```bash
curl "http://localhost:8080/api/orders?name=catalog"
```

Get an order:

```bash
curl "http://localhost:8080/api/orders/1"
```

Checkout an order:

```bash
curl -X POST "http://localhost:8080/api/orders/1/checkout" \
  -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"demo-1","paymentMethodToken":"tok_success"}'
```

Create a draft sales order with a product line, then confirm it. Confirmation checks the customer's active status and available credit, reserves stock, records expected receivables, and creates a shipment request:

```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":3,"tenantName":"Công ty An Phát","tenantEmail":"ops@anphat.example","name":"SO-2026-0001","amount":20000000,"currency":"VND","productSku":"ERP-PRO","quantity":2}'

curl -X POST "http://localhost:8080/api/orders/1000/confirm" \
  -H "Content-Type: application/json" \
  -d '{"warehouse":"Hồ Chí Minh"}'
```

The demo seeds 10 units of `ERP-PRO` at the `Hồ Chí Minh` warehouse. New order tenants default to active with a 100,000,000 credit limit; data remains in memory and resets when the app restarts.

Use a token containing `decline` or `fail` to simulate terminal payment failure. Use a token containing `retry` to simulate a successful retry.

Get checkout status:

```bash
curl "http://localhost:8080/api/checkouts/1001"
```

List dead letters:

```bash
curl "http://localhost:8080/api/dead-letters"
```

## Test

```bash
mvn test
```

## Project Layout

```text
src/main/java/com/managementplatform             Java HTTP API application
Dockerfile                                       Java runtime image
docker-compose.yml                               API service definition
```
