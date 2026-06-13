# Management Platform

This repository is now a Java demo app for searching orders and checking them out.

It uses:

- Java 21
- Java built-in HTTP server
- an in-memory repository seeded with demo orders
- mock payment behavior
- idempotent checkout handling
- outbox-style integration records and dead-letter reporting

## Run Locally

Build and run the app with Maven and Java:

```bash
mvn package
java -jar target/management-platform-0.1.0.jar
```

Open the API at:

```text
http://localhost:8080
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
  -d '{"idempotencyKey":"demo-1","paymentMethodToken":"success"}'
```

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
src/main/java/com/example/managementplatform     Java HTTP API application
Dockerfile                                       Java runtime image
docker-compose.yml                               API service definition
```
