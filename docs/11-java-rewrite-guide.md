# Hướng dẫn code lại repo này bằng Java

Tài liệu này mô tả cách triển khai lại repo Management Platform theo kiến trúc Java hiện tại, dựa trên cấu trúc đã có trong thư mục `src/main/java`.

## 1. Mục tiêu

- Chuyển toàn bộ logic nghiệp vụ từ phiên bản C# sang Java.
- Giữ nguyên hành vi API hiện tại: tìm đơn hàng, checkout, tra cứu trạng thái checkout, xem dead letters.
- Dùng Maven + Java 21 để build và chạy.

## 2. Cần cài gì

Để build và chạy project này, máy cần có:

- Java 21 JDK
- Maven 3.9+
- Git
- Docker và Docker Compose, nếu muốn chạy bằng container

Kiểm tra nhanh:

```bash
java -version
mvn -version
git --version
docker --version
docker compose version
```

Nếu chưa có Java 21, có thể cài một trong các bản sau:

- Eclipse Temurin 21
- Oracle JDK 21
- Microsoft Build of OpenJDK 21

## 3. Công nghệ nên dùng

- Java 21
- Java built-in HTTP server (`com.sun.net.httpserver.HttpServer`)
- Maven cho build/test/package
- In-memory repository cho demo, không cần database

## 4. Cấu trúc gợi ý

Trong giai đoạn rewrite, nên tách phần Java sang một folder riêng để không đè lên code C# hiện tại. Gợi ý:

```text
rewrite/java
  pom.xml
  src/main/java/com/managementplatform
    ManagementPlatformApplication.java
    domain/
      model/
      enum/
      service/
    application/
      port/
      usecase/
      dto/
    infrastructure/
      repository/
      gateway/
      outbox/
      config/
    presentation/
      http/
      controller/
      handler/
    shared/
      exception/
      util/
  src/test/java/com/managementplatform
```

Ý nghĩa các layer:

- `domain`: core business, không phụ thuộc framework.
- `application`: use case, port, DTO, orchestration.
- `infrastructure`: repository in-memory, mock payment gateway, outbox, persistence tạm thời.
- `presentation`: HTTP server, routing, request/response mapping.
- `shared`: exception, helper, các phần dùng chung.

Nếu muốn phân tách rõ hơn giữa hai ngôn ngữ, có thể tổ chức:

```text
rewrite/
  csharp/   # code gốc hiện tại
  java/     # bản rewrite
```

## 5. Các bước rewrite

### Bước 1: Khởi tạo Maven project

- Dùng `pom.xml` hiện có với Java 21.
- Đảm bảo plugin `maven-compiler-plugin`, `maven-surefire-plugin`, `maven-jar-plugin` được cấu hình đúng.

### Bước 2: Tái tạo các domain model

Các lớp chính cần giữ nguyên logic:

- `Order`
- `CheckoutAttempt`
- `PaymentTransaction`
- `OutboxMessage`
- `Tenant`

Các enum quan trọng:

- `OrderStatus`
- `CheckoutStatus`
- `PaymentStatus`
- `OutboxStatus`
- `OutboxMessageType`

### Bước 3: Triển khai repository in-memory

Repository nên:

- lưu các order và checkout attempt trong `ConcurrentHashMap`
- hỗ trợ `search`, `count`, `findOrder`, `findCheckout`
- tạo checkout idempotent bằng `idempotencyKey`

Điểm quan trọng: checkout phải trả về cùng kết quả nếu gọi lại với cùng `orderId + idempotencyKey`.

### Bước 4: Triển khai checkout flow

Logic hiện tại cần giữ nguyên:

1. Kiểm tra `idempotencyKey` và `paymentMethodToken`.
2. Tìm order theo `orderId`.
3. Nếu order chưa ở trạng thái `DRAFT`, trả lỗi `409`.
4. Đánh dấu order đang xử lý.
5. Gọi mock payment gateway.
6. Nếu thất bại: lưu `FAILED` checkout, rollback order về `DRAFT`, tạo dead-letter/outbox record.
7. Nếu thành công: đánh dấu checkout `SUCCEEDED`, order `PAID`, tạo outbox pending message.

### Bước 5: Triển khai HTTP API

Các endpoint cần có:

- `GET /api/orders?page=1&pageSize=20&name=...`
- `GET /api/orders/{id}`
- `POST /api/orders/{id}/checkout`
- `GET /api/checkouts/{id}`
- `GET /api/dead-letters`

Các handler nên:

- parse query string
- parse JSON body đơn giản
- trả về JSON chuẩn
- map exception sang HTTP status code phù hợp

### Bước 6: Viết test để bảo vệ hành vi

Nên có ít nhất:

- test checkout success
- test checkout failure
- test idempotent checkout
- test dead-letter listing

## 6. Các quy tắc cần giữ khi rewrite

- Giữ API response JSON tương thích với phiên bản hiện tại.
- Không dùng database để demo; dùng in-memory repository.
- Luôn xử lý idempotency để tránh double charge.
- Bảo toàn logic mock payment token:
  - token chứa `decline` hoặc `fail` => payment thất bại
  - token chứa `retry` => payment thành công nhưng retry count > 1

## 7. Chạy local

```bash
mvn package
java -jar target/management-platform-0.1.0.jar
```

Hoặc dùng Docker:

```bash
docker compose up --build
```

## 8. Gợi ý tối ưu khi tiếp tục phát triển

- Tách `HttpApiServer` thành `OrderController`, `CheckoutController` nếu sau này chuyển sang Spring Boot.
- Tách `CheckoutService` thành interface để mock dễ hơn trong test.
- Có thể thay repository in-memory bằng JDBC hoặc JPA khi cần production.

## 9. Kết luận

Repo này đã được chuyển về Java demo app hoàn chỉnh. Nếu muốn tiếp tục mở rộng, nên giữ logic nghiệp vụ hiện tại và chỉ thay lớp infrastructure khi cần nâng cấp lên Spring Boot hoặc database thật.
