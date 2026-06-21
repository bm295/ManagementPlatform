# Java rewrite todo list

Tài liệu này là checklist nhỏ và rõ để tiếp tục rewrite Management Platform từ C# sang Java. Mục tiêu hiện tại là hoàn thiện Java demo app giữ cùng hành vi API chính: tìm đơn hàng, checkout, tra cứu checkout, và xem dead letters.

## 1. Mục tiêu và phạm vi

- [x] Xác định runtime Java 21 cho bản rewrite.
- [x] Dùng Maven để build, test, và package ứng dụng.
- [ ] Giữ response JSON của Java API tương thích với API C# hiện tại.
- [ ] Giữ các flow nghiệp vụ chính:
  - [ ] Tìm kiếm danh sách order.
  - [ ] Xem chi tiết một order.
  - [ ] Checkout một order.
  - [ ] Xem trạng thái checkout.
  - [ ] Xem danh sách dead letters.
- [ ] Không thay thế code C# hiện tại cho tới khi bản Java đủ chức năng tương đương.

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
- [ ] Kiểm tra lại `mainClass` trong manifest sau khi tạo lớp application thật.
- [ ] Chạy build Maven sau mỗi nhóm thay đổi lớn:

  ```bash
  mvn test
  ```

## 4. Cấu trúc thư mục Java

- [x] Tạo thư mục `src/main/java/com/managementplatform`.
- [x] Tạo package `domain.model`.
- [x] Tạo package enum cho domain status.
- [ ] Đổi tên thư mục `src/main/java/com/managementplatform/domain/enum` thành `src/main/java/com/managementplatform/domain/enums` để khớp với package `com.managementplatform.domain.enums`.
- [ ] Tạo package `application.port`.
- [ ] Tạo package `application.usecase`.
- [ ] Tạo package `application.dto`.
- [ ] Tạo package `infrastructure.repository`.
- [ ] Tạo package `infrastructure.gateway`.
- [ ] Tạo package `infrastructure.outbox`.
- [ ] Tạo package `infrastructure.config`.
- [ ] Tạo package `presentation.http`.
- [ ] Tạo package `presentation.controller` hoặc `presentation.handler`.
- [ ] Tạo package `shared.exception`.
- [ ] Tạo package `shared.util` nếu cần helper chung.
- [ ] Tạo thư mục `src/test/java/com/managementplatform` cho test Java.

## 5. Domain model

- [x] Tạo `Tenant`.
- [x] Tạo `Order`.
- [x] Tạo `CheckoutAttempt`.
- [x] Tạo `PaymentTransaction`.
- [x] Tạo `OutboxMessage`.
- [ ] Tạo `DeadLetterMessage` nếu Java API cần trả dead letters riêng thay vì chỉ đọc outbox failed.
- [ ] So sánh field của domain Java với domain C# hiện tại.
- [ ] Bổ sung field còn thiếu để response API không bị lệch.
- [ ] Đảm bảo `Order` có đủ thông tin tenant, amount, currency, status, created time, paid time.
- [ ] Đảm bảo `CheckoutAttempt` có đủ id, order id, idempotency key, status, payment transaction, failure reason, created/completed time.
- [ ] Đảm bảo model không phụ thuộc HTTP hoặc framework.

## 6. Domain enum và status

- [x] Tạo `OrderStatus`.
- [x] Tạo `CheckoutStatus`.
- [x] Tạo `PaymentStatus`.
- [x] Tạo `OutboxStatus`.
- [x] Tạo `OutboxMessageType`.
- [ ] Kiểm tra tên enum trả về JSON có tương thích với API hiện tại không.
- [ ] Giữ các trạng thái order tối thiểu:
  - [x] `DRAFT`.
  - [x] `CHECKOUT_PROCESSING`.
  - [x] `PAID`.
- [ ] Giữ các trạng thái checkout tối thiểu:
  - [x] `PAYMENT_PENDING`.
  - [x] `PAYMENT_FAILED`.
  - [x] `PAYMENT_SUCCEEDED`.

## 7. Application ports

- [ ] Tạo `OrderRepository` port.
- [ ] Tạo method tìm order theo id.
- [ ] Tạo method tìm kiếm order theo page, page size, và name.
- [ ] Tạo method đếm tổng số order theo filter.
- [ ] Tạo `CheckoutRepository` port.
- [ ] Tạo method tìm checkout theo id.
- [ ] Tạo method tìm checkout theo `orderId + idempotencyKey`.
- [ ] Tạo method lưu checkout attempt.
- [ ] Tạo `DeadLetterRepository` hoặc `OutboxRepository` port.
- [ ] Tạo `PaymentGateway` port.
- [ ] Tạo DTO kết quả payment gồm status, attempt count, provider transaction id, và failure reason.
- [ ] Tạo `Clock` hoặc time provider port nếu cần test deterministic.

## 8. Infrastructure in-memory repository

- [ ] Tạo in-memory repository dùng `ConcurrentHashMap`.
- [ ] Seed dữ liệu demo order giống hoặc tương đương bản C#.
- [ ] Implement tìm order theo id.
- [ ] Implement search order theo tên.
- [ ] Implement phân trang order.
- [ ] Implement count order theo filter.
- [ ] Implement lưu checkout attempt.
- [ ] Implement tìm checkout theo id.
- [ ] Implement tìm checkout theo `orderId + idempotencyKey`.
- [ ] Implement lưu outbox/dead-letter message.
- [ ] Đảm bảo thao tác checkout đủ an toàn khi có request song song cho cùng order.
- [ ] Viết test cho idempotency ở repository hoặc service level.

## 9. Mock payment gateway

- [ ] Tạo implementation `MockPaymentGateway`.
- [ ] Nếu payment token chứa `decline` thì trả thất bại.
- [ ] Nếu payment token chứa `fail` thì trả thất bại.
- [ ] Nếu payment token chứa `retry` thì trả thành công với `attemptCount > 1`.
- [ ] Nếu token bình thường thì trả thành công với `attemptCount = 1`.
- [ ] Trả `providerTransactionId` ổn định đủ để debug.
- [ ] Trả `failureReason` rõ ràng khi thất bại.

## 10. Checkout use case

- [ ] Tạo request DTO cho checkout gồm `idempotencyKey` và `paymentMethodToken`.
- [ ] Validate `idempotencyKey` không rỗng.
- [ ] Validate `paymentMethodToken` không rỗng.
- [ ] Tìm order theo `orderId`.
- [ ] Nếu không có order, trả lỗi tương ứng HTTP 404 ở layer API.
- [ ] Kiểm tra checkout cũ theo `orderId + idempotencyKey` trước khi charge.
- [ ] Nếu checkout cũ đã tồn tại, trả lại kết quả cũ và không gọi payment gateway lần nữa.
- [ ] Nếu order không ở trạng thái `DRAFT`, trả lỗi conflict HTTP 409 ở layer API.
- [ ] Đánh dấu order sang `CHECKOUT_PROCESSING` trước khi charge.
- [ ] Gọi mock payment gateway.
- [ ] Khi payment thất bại:
  - [ ] Lưu checkout status `PAYMENT_FAILED`.
  - [ ] Lưu payment transaction failed.
  - [ ] Rollback order về `DRAFT`.
  - [ ] Tạo dead-letter hoặc outbox failed record.
  - [ ] Trả response lỗi/failed tương thích API hiện tại.
- [ ] Khi payment thành công:
  - [ ] Lưu checkout status `PAYMENT_SUCCEEDED`.
  - [ ] Lưu payment transaction succeeded.
  - [ ] Đánh dấu order `PAID`.
  - [ ] Set `paidAt`.
  - [ ] Tạo outbox pending message.
  - [ ] Trả response success tương thích API hiện tại.

## 11. HTTP API

- [ ] Tạo `ManagementPlatformApplication` làm entry point.
- [ ] Tạo HTTP server bằng `com.sun.net.httpserver.HttpServer`.
- [ ] Đọc port từ environment hoặc dùng default rõ ràng.
- [ ] Tạo route `GET /api/orders`.
- [ ] Parse query `page`.
- [ ] Parse query `pageSize`.
- [ ] Parse query `name`.
- [ ] Tạo route `GET /api/orders/{id}`.
- [ ] Tạo route `POST /api/orders/{id}/checkout`.
- [ ] Parse JSON body checkout.
- [ ] Tạo route `GET /api/checkouts/{id}`.
- [ ] Tạo route `GET /api/dead-letters`.
- [ ] Trả `Content-Type: application/json` cho JSON response.
- [ ] Map validation error sang HTTP 400.
- [ ] Map not found sang HTTP 404.
- [ ] Map order conflict sang HTTP 409.
- [ ] Map lỗi không mong muốn sang HTTP 500.
- [ ] Chuẩn hóa error response body.

## 12. JSON mapping

- [ ] Chọn cách serialize JSON không làm phức tạp demo.
- [ ] Nếu không dùng thư viện ngoài, tạo helper escape string đúng cách.
- [ ] Serialize số tiền không mất precision.
- [ ] Serialize timestamp theo ISO-8601.
- [ ] Serialize enum theo đúng tên API đang dùng.
- [ ] Viết test hoặc snapshot nhỏ cho JSON response chính.

## 13. Dead letters và outbox

- [ ] Quyết định Java dùng model `DeadLetterMessage` riêng hay derive từ failed outbox.
- [ ] Tạo message khi checkout thất bại.
- [ ] Lưu reason của failure.
- [ ] Lưu payload đủ thông tin order/checkout để debug.
- [ ] Implement list dead letters.
- [ ] Đảm bảo `GET /api/dead-letters` trả dữ liệu tương thích docs API.
- [ ] Với checkout thành công, tạo outbox message status `PENDING`.

## 14. Test cần có

- [ ] Tạo Java test project dưới `src/test/java`.
- [ ] Thêm dependency test nếu cần.
- [ ] Test checkout success.
- [ ] Test checkout failure với token chứa `decline`.
- [ ] Test checkout failure với token chứa `fail`.
- [ ] Test checkout retry token trả thành công và attempt count > 1.
- [ ] Test idempotent checkout không double charge.
- [ ] Test checkout order đã paid trả conflict.
- [ ] Test search orders có phân trang.
- [ ] Test get order not found.
- [ ] Test get checkout not found.
- [ ] Test dead-letter listing sau payment failed.
- [ ] Chạy toàn bộ test:

  ```bash
  mvn test
  ```

## 15. Docker và chạy local

- [ ] Kiểm tra `Dockerfile` có build được Java app hay vẫn đang phục vụ bản C#.
- [ ] Cập nhật Dockerfile cho Java nếu mục tiêu là chạy Java app bằng container.
- [ ] Kiểm tra `docker-compose.yml` có đúng service Java không.
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

## 16. Kiểm tra tương thích với bản C#

- [ ] Đọc `docs/04-api.md` để lấy contract API hiện tại.
- [ ] So sánh route Java với route trong C# controllers.
- [ ] So sánh status code Java với middleware/exception C#.
- [ ] So sánh response của `GET /api/orders`.
- [ ] So sánh response của `GET /api/orders/{id}`.
- [ ] So sánh response của `POST /api/orders/{id}/checkout`.
- [ ] So sánh response của `GET /api/checkouts/{id}`.
- [ ] So sánh response của `GET /api/dead-letters`.
- [ ] Ghi lại khác biệt cố ý nếu có.

## 17. Dọn dẹp trước khi xem là hoàn thành

- [ ] Chạy format hoặc tự kiểm tra style Java.
- [ ] Xóa code dead hoặc helper không dùng.
- [ ] Đảm bảo README hoặc docs chỉ rõ cách chạy bản Java.
- [ ] Đảm bảo `mvn test` pass.
- [ ] Đảm bảo `mvn package` tạo được jar chạy được.
- [ ] Chạy thử các endpoint chính bằng curl hoặc HTTP client.
- [ ] Cập nhật checklist này: task nào đã làm thì đổi sang `[x]`.

## 18. Việc có thể làm sau khi Java demo hoàn chỉnh

- [ ] Tách HTTP handlers thành controller riêng nếu file quá lớn.
- [ ] Tách `CheckoutService` thành interface để mock dễ hơn.
- [ ] Thêm repository JDBC hoặc JPA nếu cần production persistence.
- [ ] Chuyển sang Spring Boot nếu cần framework đầy đủ.
- [ ] Thêm migration database nếu bỏ in-memory repository.
- [ ] Thêm observability: structured logs, metrics, tracing.
