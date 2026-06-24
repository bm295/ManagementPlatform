# Java rewrite todo list

Tài liệu này là checklist nhỏ cho bản Java demo app. Mục tiêu hiện tại là giữ cùng hành vi API chính: tìm đơn hàng, checkout, tra cứu checkout, và xem dead letters.

## 1. Mục tiêu và phạm vi

- [x] Xác định runtime Java 21 cho bản rewrite.
- [x] Dùng Maven để build, test, và package ứng dụng.
- [x] Giữ response JSON của Java API tương thích với API contract.
- [ ] Giữ các flow nghiệp vụ chính:
  - [ ] Tìm kiếm danh sách order.
  - [ ] Xem chi tiết một order.
  - [ ] Checkout một order.
  - [ ] Xem trạng thái checkout.
  - [ ] Xem danh sách dead letters.
- [x] Xóa implementation .NET cũ sau khi bản Java đã có compatibility check.

## 2. Chuẩn bị môi trường

- [ ] Cài Java 21 JDK.
- [ ] Cài Maven 3.9+.
- [ ] Cài Git.
- [ ] Cài Docker và Docker Compose nếu cần chạy bằng container.
- [ ] Kiểm tra Java:

  ```bash
  java -version
  ```

- [ ] Kiểm tra Maven:

  ```bash
  mvn -version
  ```

- [ ] Kiểm tra Git:

  ```bash
  git --version
  ```

- [ ] Kiểm tra Docker nếu dùng container:

  ```bash
  docker --version
  docker compose version
  ```

## 3. Maven project

- [x] Tạo `pom.xml` ở repo root.
- [x] Đặt artifact Java là `management-platform`.
- [x] Đặt version hiện tại là `0.1.0`.
- [x] Đặt packaging là `jar`.
- [x] Cấu hình Java release 21.
- [x] Cấu hình `maven-compiler-plugin`.
- [x] Cấu hình `maven-surefire-plugin`.
- [x] Cấu hình `maven-jar-plugin`.
- [x] Kiểm tra lại `mainClass` trong manifest sau khi tạo lớp application thật.
- [ ] Chạy build Maven sau mỗi nhóm thay đổi lớn:

  ```bash
  mvn test
  ```

## 4. Cấu trúc thư mục Java

- [x] Tạo thư mục `src/main/java/com/managementplatform`.
- [x] Tạo package `domain.model`.
- [x] Tạo package `domain.enums` cho domain status.
- [x] Đổi tên thư mục `src/main/java/com/managementplatform/domain/enum` thành `src/main/java/com/managementplatform/domain/enums` để khớp với package `com.managementplatform.domain.enums`.
- [x] Tạo package `application.port`.
- [x] Tạo package `application.usecase`.
- [x] Tạo package `application.dto`.
- [x] Tạo package `infrastructure.repository`.
- [x] Tạo package `infrastructure.gateway`.
- [x] Tạo package `infrastructure.outbox`.
- [x] Tạo package `infrastructure.config`.
- [x] Tạo package `presentation.http`.
- [x] Tạo package `presentation.controller` hoặc `presentation.handler`.
- [x] Tạo package `shared.exception`.
- [x] Tạo package `shared.util` nếu cần helper chung.
- [x] Tạo thư mục `src/test/java/com/managementplatform` cho test Java.

## 5. Domain model

- [x] Tạo `Tenant`.
- [x] Tạo `Order`.
- [x] Tạo `CheckoutAttempt`.
- [x] Tạo `PaymentTransaction`.
- [x] Tạo `OutboxMessage`.
- [x] Tạo `DeadLetterMessage` nếu Java API cần trả dead letters riêng thay vì chỉ đọc outbox failed.
- [x] So sánh field của domain Java với domain API contract.
- [x] Bổ sung field còn thiếu để response API không bị lệch.
- [x] Đảm bảo `Order` có đủ thông tin tenant, amount, currency, status, created time, paid time.
- [x] Đảm bảo `CheckoutAttempt` có đủ id, order id, idempotency key, status, payment transaction, failure reason, created/completed time.
- [x] Đảm bảo model không phụ thuộc HTTP hoặc framework.

## 6. Domain enum và status

- [x] Tạo `OrderStatus`.
- [x] Tạo `CheckoutStatus`.
- [x] Tạo `PaymentStatus`.
- [x] Tạo `OutboxStatus`.
- [x] Tạo `OutboxMessageType`.
- [x] Kiểm tra tên enum trả về JSON có tương thích với API hiện tại không.
- [x] Giữ các trạng thái order tối thiểu:
  - [x] `DRAFT`.
  - [x] `CHECKOUT_PROCESSING`.
  - [x] `PAID`.
- [x] Giữ các trạng thái checkout tối thiểu:
  - [x] `PAYMENT_PENDING`.
  - [x] `PAYMENT_FAILED`.
  - [x] `PAYMENT_SUCCEEDED`.

## 7. Application ports

- [x] Tạo `OrderRepository` port.
- [x] Tạo method tìm order theo id.
- [x] Tạo method tìm kiếm order theo page, page size, và name.
- [x] Tạo method đếm tổng số order theo filter.
- [x] Tạo `CheckoutRepository` port.
- [x] Tạo method tìm checkout theo id.
- [x] Tạo method tìm checkout theo `orderId + idempotencyKey`.
- [x] Tạo method lưu checkout attempt.
- [x] Tạo `DeadLetterRepository` hoặc `OutboxRepository` port.
- [x] Tạo `PaymentGateway` port.
- [x] Tạo DTO kết quả payment gồm status, attempt count, provider transaction id, và failure reason.
- [x] Tạo `Clock` hoặc time provider port nếu cần test deterministic.

## 8. Infrastructure in-memory repository

- [x] Tạo in-memory repository dùng `ConcurrentHashMap`.
- [x] Seed dữ liệu demo order giống hoặc tương đương API contract.
- [x] Implement tìm order theo id.
- [x] Implement search order theo tên.
- [x] Implement phân trang order.
- [x] Implement count order theo filter.
- [x] Implement lưu checkout attempt.
- [x] Implement tìm checkout theo id.
- [x] Implement tìm checkout theo `orderId + idempotencyKey`.
- [x] Implement lưu outbox/dead-letter message.
- [x] Đảm bảo thao tác checkout đủ an toàn khi có request song song cho cùng order.
- [x] Viết test cho idempotency ở repository hoặc service level.

## 9. Mock payment gateway

- [x] Tạo implementation `MockPaymentGateway`.
- [x] Nếu payment token chứa `decline` thì trả thất bại.
- [x] Nếu payment token chứa `fail` thì trả thất bại.
- [x] Nếu payment token chứa `retry` thì trả thành công với `attemptCount > 1`.
- [x] Nếu token bình thường thì trả thành công với `attemptCount = 1`.
- [x] Trả `providerTransactionId` ổn định đủ để debug.
- [x] Trả `failureReason` rõ ràng khi thất bại.

## 10. Checkout use case

- [x] Tạo request DTO cho checkout gồm `idempotencyKey` và `paymentMethodToken`.
- [x] Validate `idempotencyKey` không rỗng.
- [x] Validate `paymentMethodToken` không rỗng.
- [x] Tìm order theo `orderId`.
- [x] Nếu không có order, trả lỗi tương ứng HTTP 404 ở layer API.
- [x] Kiểm tra checkout cũ theo `orderId + idempotencyKey` trước khi charge.
- [x] Nếu checkout cũ đã tồn tại, trả lại kết quả cũ và không gọi payment gateway lần nữa.
- [x] Nếu order không ở trạng thái `DRAFT`, trả lỗi conflict HTTP 409 ở layer API.
- [x] Đánh dấu order sang `CHECKOUT_PROCESSING` trước khi charge.
- [x] Gọi mock payment gateway.
- [ ] Khi payment thất bại:
  - [x] Lưu checkout status `PAYMENT_FAILED`.
  - [x] Lưu payment transaction failed.
  - [x] Rollback order về `DRAFT`.
  - [x] Tạo dead-letter hoặc outbox failed record.
  - [x] Trả response lỗi/failed tương thích API hiện tại.
- [ ] Khi payment thành công:
  - [x] Lưu checkout status `PAYMENT_SUCCEEDED`.
  - [x] Lưu payment transaction succeeded.
  - [x] Đánh dấu order `PAID`.
  - [x] Set `paidAt`.
  - [x] Tạo outbox pending message.
  - [x] Trả response success tương thích API hiện tại.

## 11. HTTP API

- [x] Tạo `ManagementPlatformApplication` làm entry point.
- [x] Tạo HTTP server bằng `com.sun.net.httpserver.HttpServer`.
- [x] Đọc port từ environment hoặc dùng default rõ ràng.
- [x] Tạo route `GET /api/orders`.
- [x] Parse query `page`.
- [x] Parse query `pageSize`.
- [x] Parse query `name`.
- [x] Tạo route `GET /api/orders/{id}`.
- [x] Tạo route `POST /api/orders/{id}/checkout`.
- [x] Parse JSON body checkout.
- [x] Tạo route `GET /api/checkouts/{id}`.
- [x] Tạo route `GET /api/dead-letters`.
- [x] Trả `Content-Type: application/json` cho JSON response.
- [x] Map validation error sang HTTP 400.
- [x] Map not found sang HTTP 404.
- [x] Map order conflict sang HTTP 409.
- [x] Map lỗi không mong muốn sang HTTP 500.
- [x] Chuẩn hóa error response body.

## 12. JSON mapping

- [x] Chọn cách serialize JSON không làm phức tạp demo.
- [x] Nếu không dùng thư viện ngoài, tạo helper escape string đúng cách.
- [x] Serialize số tiền không mất precision.
- [x] Serialize timestamp theo ISO-8601.
- [x] Serialize enum theo đúng tên API đang dùng.
- [x] Viết test hoặc snapshot nhỏ cho JSON response chính.

## 13. Dead letters và outbox

- [x] Quyết định Java dùng model `DeadLetterMessage` riêng hay derive từ failed outbox.
- [x] Tạo message khi checkout thất bại.
- [x] Lưu reason của failure.
- [x] Lưu payload đủ thông tin order/checkout để debug.
- [x] Implement list dead letters.
- [x] Đảm bảo `GET /api/dead-letters` trả dữ liệu tương thích docs API.
- [x] Với checkout thành công, tạo outbox message status `PENDING`.

## 14. Test cần có

- [x] Tạo Java test project dưới `src/test/java`.
- [x] Thêm dependency test nếu cần.
- [x] Test checkout success.
- [x] Test checkout failure với token chứa `decline`.
- [x] Test checkout failure với token chứa `fail`.
- [x] Test checkout retry token trả thành công và attempt count > 1.
- [x] Test idempotent checkout không double charge.
- [x] Test checkout order đã paid trả conflict.
- [x] Test search orders có phân trang.
- [x] Test get order not found.
- [x] Test get checkout not found.
- [x] Test dead-letter listing sau payment failed.
- [ ] Chạy toàn bộ test:

  ```bash
  mvn test
  ```

## 15. Docker và chạy local

- [x] Kiểm tra `Dockerfile` build Java app.
- [x] Cập nhật Dockerfile cho Java app.
- [x] Kiểm tra `docker-compose.yml` có đúng service Java.
- [ ] Build jar:

  ```bash
  mvn package
  ```

- [ ] Chạy app bằng jar:

  ```bash
  java -jar target/management-platform-0.1.0.jar
  ```

- [ ] Chạy app bằng Docker nếu đã cập nhật Dockerfile:

  ```bash
  docker compose up --build
  ```

## 16. Kiểm tra tương thích với API contract

- [x] Đọc `docs/04-api.md` để lấy contract API hiện tại.
- [x] So sánh route Java với route trong API contract.
- [x] So sánh status code Java với error behavior trong API contract.
- [x] So sánh response của `GET /api/orders`.
- [x] So sánh response của `GET /api/orders/{id}`.
- [x] So sánh response của `POST /api/orders/{id}/checkout`.
- [x] So sánh response của `GET /api/checkouts/{id}`.
- [x] So sánh response của `GET /api/dead-letters`.
- [x] Ghi lại khác biệt cố ý nếu có.

## 17. Dọn dẹp trước khi xem là hoàn thành

- [x] Chạy format hoặc tự kiểm tra style Java.
- [x] Xóa code dead hoặc helper không dùng.
- [x] Đảm bảo README hoặc docs chỉ rõ cách chạy bản Java.
- [ ] Đảm bảo `mvn test` pass.
- [ ] Đảm bảo `mvn package` tạo được jar chạy được.
- [ ] Chạy thử các endpoint chính bằng curl hoặc HTTP client.
- [x] Cập nhật checklist này: task nào đã làm thì đổi sang `[x]`.

## 18. Việc có thể làm sau khi Java demo hoàn chỉnh

- [ ] Tách HTTP handlers thành controller riêng nếu file quá lớn.
- [ ] Tách `CheckoutService` thành interface để mock dễ hơn.
- [ ] Thêm repository JDBC hoặc JPA nếu cần production persistence.
- [ ] Chuyển sang Spring Boot nếu cần framework đầy đủ.
- [ ] Thêm migration database nếu bỏ in-memory repository.
- [ ] Thêm observability: structured logs, metrics, tracing.
