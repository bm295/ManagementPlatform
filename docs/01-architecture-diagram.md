# Architecture Diagram

```mermaid
flowchart LR
    Client[Browser / API client] --> Server[Java HTTP server]
    Server --> Root[Landing page / health]
    Server --> Orders[GET /api/orders]
    Server --> Checkouts["GET /api/checkouts/{id}"]
    Server --> DeadLetters[GET /api/dead-letters]

    Orders --> OrderRepo[In-memory order repository]
    Checkouts --> CheckoutRepo[In-memory checkout repository]
    DeadLetters --> DeadLetterRepo[In-memory dead-letter repository]

    Orders --> CheckoutUseCase[Checkout use case]
    Checkouts --> CheckoutUseCase
    CheckoutUseCase --> OrderRepo
    CheckoutUseCase --> CheckoutRepo
    CheckoutUseCase --> DeadLetterRepo
    CheckoutUseCase --> PaymentGateway[Mock payment gateway]
    CheckoutUseCase --> TimeProvider[System time]
```

## Notes

- The server is a JDK `HttpServer` entry point, not Spring Boot.
- The implementation uses in-memory repositories and a mock payment gateway for demo behavior.
- The landing page at `/` is a simple HTML overview, while the API lives under `/api/*`.
