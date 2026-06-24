package com.managementplatform;

import com.managementplatform.application.dto.CheckoutRequest;
import com.managementplatform.application.dto.CheckoutResponse;
import com.managementplatform.application.dto.IntegrationStatusDto;
import com.managementplatform.application.port.TimeProvider;
import com.managementplatform.application.usecase.CheckoutUseCase;
import com.managementplatform.domain.model.CheckoutAttempt;
import com.managementplatform.domain.model.DeadLetterMessage;
import com.managementplatform.domain.model.Order;
import com.managementplatform.infrastructure.gateway.MockPaymentGateway;
import com.managementplatform.infrastructure.repository.InMemoryCheckoutRepository;
import com.managementplatform.infrastructure.repository.InMemoryDeadLetterRepository;
import com.managementplatform.infrastructure.repository.InMemoryOrderRepository;
import com.managementplatform.shared.exception.ConflictException;
import com.managementplatform.shared.exception.ResourceNotFoundException;
import com.managementplatform.shared.exception.ValidationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java application entry point for the Management Platform demo API.
 */
public final class ManagementPlatformApplication {
    public static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String PORT_ENVIRONMENT_VARIABLE = "PORT";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private ManagementPlatformApplication() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = createServer(portFromEnvironment(System.getenv()));
        server.start();
        System.out.printf("Management Platform HTTP server listening on port %d%n", server.getAddress().getPort());
    }

    /**
     * Creates the JDK HTTP server and wires the application dependencies.
     *
     * @param port TCP port to bind; pass {@code 0} in tests to let the OS choose a free port
     * @return configured, not-yet-started HTTP server
     * @throws IOException when the server socket cannot be created
     */
    public static HttpServer createServer(int port) throws IOException {
        AppDependencies dependencies = createDependencies();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", ManagementPlatformApplication::handleHealth);
        server.createContext("/api/orders", exchange -> handleOrders(exchange, dependencies));
        server.createContext("/api/checkouts", exchange -> handleCheckouts(exchange, dependencies));
        server.createContext("/api/dead-letters", exchange -> handleDeadLetters(exchange, dependencies));
        server.setExecutor(null);
        return server;
    }

    private static AppDependencies createDependencies() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryCheckoutRepository checkoutRepository = new InMemoryCheckoutRepository();
        InMemoryDeadLetterRepository deadLetterRepository = new InMemoryDeadLetterRepository();
        TimeProvider timeProvider = Instant::now;
        CheckoutUseCase checkoutUseCase = new CheckoutUseCase(
            orderRepository,
            checkoutRepository,
            deadLetterRepository,
            new MockPaymentGateway(),
            timeProvider
        );
        return new AppDependencies(orderRepository, checkoutRepository, deadLetterRepository, checkoutUseCase);
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, "GET");
            return;
        }

        sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private static void handleOrders(HttpExchange exchange, AppDependencies dependencies) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            List<String> segments = pathSegments(path);
            if (segments.size() < 2 || !"api".equals(segments.get(0)) || !"orders".equals(segments.get(1))) {
                sendProblem(exchange, 404, "Not Found", "Route was not found.");
                return;
            }

            if (segments.size() == 2 && "GET".equals(method)) {
                handleSearchOrders(exchange, dependencies.orderRepository());
                return;
            }

            if (segments.size() == 3 && "GET".equals(method)) {
                handleGetOrder(exchange, dependencies.orderRepository(), parseId(segments.get(2), "orderId"));
                return;
            }

            if (segments.size() == 4 && "POST".equals(method) && "checkout".equals(segments.get(3))) {
                handleCheckout(exchange, dependencies.checkoutUseCase(), parseId(segments.get(2), "orderId"));
                return;
            }

            if (segments.size() == 2 || segments.size() == 3 || segments.size() == 4) {
                sendMethodNotAllowed(exchange, allowedMethodsFor(segments));
                return;
            }

            sendProblem(exchange, 404, "Not Found", "Route was not found.");
        } catch (ValidationException exception) {
            sendProblem(exchange, 400, "Bad Request", exception.getMessage());
        } catch (ResourceNotFoundException exception) {
            sendProblem(exchange, 404, "Not Found", exception.getMessage());
        } catch (ConflictException exception) {
            sendProblem(exchange, 409, "Conflict", exception.getMessage());
        } catch (RuntimeException exception) {
            sendProblem(exchange, 500, "Internal Server Error", "An unexpected error occurred.");
        }
    }


    private static void handleCheckouts(HttpExchange exchange, AppDependencies dependencies) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            List<String> segments = pathSegments(exchange.getRequestURI().getPath());
            if (segments.size() < 2 || !"api".equals(segments.get(0)) || !"checkouts".equals(segments.get(1))) {
                sendProblem(exchange, 404, "Not Found", "Route was not found.");
                return;
            }

            if (segments.size() == 3 && "GET".equals(method)) {
                long checkoutId = parseId(segments.get(2), "checkoutId");
                CheckoutAttempt attempt = dependencies.checkoutRepository().findById(checkoutId)
                    .orElseThrow(() -> new ResourceNotFoundException("Checkout %d was not found.".formatted(checkoutId)));
                sendJson(exchange, 200, checkoutAttemptJson(attempt));
                return;
            }

            if (segments.size() == 3) {
                sendMethodNotAllowed(exchange, "GET");
                return;
            }

            sendProblem(exchange, 404, "Not Found", "Route was not found.");
        } catch (ValidationException exception) {
            sendProblem(exchange, 400, "Bad Request", exception.getMessage());
        } catch (ResourceNotFoundException exception) {
            sendProblem(exchange, 404, "Not Found", exception.getMessage());
        } catch (RuntimeException exception) {
            sendProblem(exchange, 500, "Internal Server Error", "An unexpected error occurred.");
        }
    }

    private static void handleDeadLetters(HttpExchange exchange, AppDependencies dependencies) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            List<String> segments = pathSegments(exchange.getRequestURI().getPath());
            if (segments.size() != 2 || !"api".equals(segments.get(0)) || !"dead-letters".equals(segments.get(1))) {
                sendProblem(exchange, 404, "Not Found", "Route was not found.");
                return;
            }

            if (!"GET".equals(method)) {
                sendMethodNotAllowed(exchange, "GET");
                return;
            }

            StringBuilder json = new StringBuilder();
            json.append('[');
            List<DeadLetterMessage> messages = dependencies.deadLetterRepository().findRecent();
            for (int index = 0; index < messages.size(); index++) {
                if (index > 0) {
                    json.append(',');
                }
                json.append(deadLetterJson(messages.get(index)));
            }
            json.append(']');
            sendJson(exchange, 200, json.toString());
        } catch (RuntimeException exception) {
            sendProblem(exchange, 500, "Internal Server Error", "An unexpected error occurred.");
        }
    }

    private static void handleSearchOrders(HttpExchange exchange, InMemoryOrderRepository orderRepository) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        int page = parsePositiveInt(query.get("page"), DEFAULT_PAGE, "page");
        int pageSize = parsePositiveInt(query.get("pageSize"), DEFAULT_PAGE_SIZE, "pageSize");
        String name = query.get("name");
        List<Order> orders = orderRepository.search(name, page, pageSize);
        long total = orderRepository.count(name);

        StringBuilder json = new StringBuilder();
        json.append("{\"items\":[");
        for (int index = 0; index < orders.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(orderSummaryJson(orders.get(index)));
        }
        json.append("],\"page\":").append(page)
            .append(",\"pageSize\":").append(pageSize)
            .append(",\"totalCount\":").append(total)
            .append('}');
        sendJson(exchange, 200, json.toString());
    }

    private static void handleGetOrder(HttpExchange exchange, InMemoryOrderRepository orderRepository, long orderId) throws IOException {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order %d was not found.".formatted(orderId)));
        sendJson(exchange, 200, orderDetailsJson(order));
    }

    private static void handleCheckout(HttpExchange exchange, CheckoutUseCase checkoutUseCase, long orderId) throws IOException {
        CheckoutRequest request = checkoutRequestFromJson(readRequestBody(exchange));
        CheckoutResponse response = checkoutUseCase.checkout(orderId, request);
        sendJson(exchange, 200, checkoutResponseJson(response));
    }

    static CheckoutRequest checkoutRequestFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new ValidationException("Checkout request body is required.");
        }

        return new CheckoutRequest(requiredJsonString(json, "idempotencyKey"), requiredJsonString(json, "paymentMethodToken"));
    }

    private static String requiredJsonString(String json, String fieldName) {
        String value = jsonStringValue(json, fieldName);
        if (value == null) {
            throw new ValidationException("%s is required.".formatted(fieldName));
        }
        return value;
    }

    private static String jsonStringValue(String json, String fieldName) {
        String quotedField = "\"" + fieldName + "\"";
        int fieldStart = json.indexOf(quotedField);
        if (fieldStart < 0) {
            return null;
        }

        int colon = json.indexOf(':', fieldStart + quotedField.length());
        if (colon < 0) {
            return null;
        }

        int quoteStart = nextNonWhitespaceIndex(json, colon + 1);
        if (quoteStart >= json.length() || json.charAt(quoteStart) != '"') {
            return null;
        }

        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int index = quoteStart + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (escaping) {
                value.append(unescapeJsonCharacter(current));
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else if (current == '"') {
                return value.toString();
            } else {
                value.append(current);
            }
        }

        return null;
    }

    private static int nextNonWhitespaceIndex(String value, int start) {
        int index = start;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static char unescapeJsonCharacter(char current) {
        return switch (current) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> current;
        };
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> values = new LinkedHashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return values;
        }

        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            String key = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            values.put(urlDecode(key), urlDecode(value));
        }
        return values;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static int parsePositiveInt(String value, int defaultValue, String fieldName) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1) {
                throw new ValidationException("%s must be greater than zero.".formatted(fieldName));
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new ValidationException("%s must be a valid integer.".formatted(fieldName));
        }
    }

    private static long parseId(String value, String fieldName) {
        try {
            long id = Long.parseLong(value);
            if (id < 1) {
                throw new ValidationException("%s must be greater than zero.".formatted(fieldName));
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new ValidationException("%s must be a valid integer.".formatted(fieldName));
        }
    }

    private static List<String> pathSegments(String path) {
        return List.of(path.split("/"))
            .stream()
            .filter(segment -> !segment.isBlank())
            .toList();
    }

    private static String allowedMethodsFor(List<String> segments) {
        if (segments.size() == 2 || segments.size() == 3) {
            return "GET";
        }
        if (segments.size() == 4 && "checkout".equals(segments.get(3))) {
            return "POST";
        }
        return "GET, POST";
    }

    private static String orderSummaryJson(Order order) {
        return "{\"id\":%d,\"name\":\"%s\",\"tenantName\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\"}".formatted(
            order.id(),
            escapeJson(order.name()),
            escapeJson(order.tenant().name()),
            decimalJson(order.amount()),
            escapeJson(order.currency()),
            order.status().apiName(),
            order.createdAt()
        );
    }

    private static String orderDetailsJson(Order order) {
        return "{\"id\":%d,\"name\":\"%s\",\"tenantName\":\"%s\",\"tenantEmail\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\",\"paidAt\":%s}".formatted(
            order.id(),
            escapeJson(order.name()),
            escapeJson(order.tenant().name()),
            escapeJson(order.tenant().email()),
            decimalJson(order.amount()),
            escapeJson(order.currency()),
            order.status().apiName(),
            order.createdAt(),
            order.paidAt() == null ? "null" : "\"" + order.paidAt() + "\""
        );
    }


    private static String checkoutAttemptJson(CheckoutAttempt attempt) {
        return "{\"checkoutId\":%d,\"orderId\":%d,\"status\":\"%s\",\"paymentStatus\":%s,\"failureReason\":%s,\"integrations\":[%s]}".formatted(
            attempt.id(),
            attempt.orderId(),
            attempt.status().apiName(),
            attempt.paymentTransaction() == null ? "null" : "\"" + attempt.paymentTransaction().status().apiName() + "\"",
            nullableStringJson(attempt.failureReason() != null ? attempt.failureReason() : paymentFailureReason(attempt)),
            integrationsJson(attempt.outboxMessages()
                .stream()
                .map(message -> new IntegrationStatusDto(message.type(), message.status(), message.attemptCount(), message.lastError()))
                .toList())
        );
    }

    private static String integrationsJson(List<IntegrationStatusDto> integrations) {
        StringBuilder json = new StringBuilder();
        for (int index = 0; index < integrations.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(integrationJson(integrations.get(index)));
        }
        return json.toString();
    }

    private static String paymentFailureReason(CheckoutAttempt attempt) {
        return attempt.paymentTransaction() == null ? null : attempt.paymentTransaction().failureReason();
    }

    private static String deadLetterJson(DeadLetterMessage message) {
        return "{\"id\":%d,\"checkoutAttemptId\":%d,\"outboxMessageId\":%d,\"type\":\"%s\",\"attemptCount\":%d,\"failureReason\":%s,\"failedAt\":\"%s\"}".formatted(
            message.id(),
            message.checkoutAttemptId(),
            message.outboxMessageId(),
            message.type().apiName(),
            message.attemptCount(),
            nullableStringJson(message.failureReason()),
            message.failedAt()
        );
    }

    private static String checkoutResponseJson(CheckoutResponse response) {
        StringBuilder json = new StringBuilder();
        json.append("{\"checkoutId\":").append(response.checkoutId())
            .append(",\"orderId\":").append(response.orderId())
            .append(",\"status\":\"").append(response.status().apiName()).append('"')
            .append(",\"paymentStatus\":").append(response.paymentStatus() == null ? "null" : "\"" + response.paymentStatus().apiName() + "\"")
            .append(",\"failureReason\":").append(nullableStringJson(response.failureReason()))
            .append(",\"integrations\":[");
        for (int index = 0; index < response.integrations().size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(integrationJson(response.integrations().get(index)));
        }
        json.append("]}");
        return json.toString();
    }

    private static String integrationJson(IntegrationStatusDto integration) {
        return "{\"type\":\"%s\",\"status\":\"%s\",\"attemptCount\":%d,\"lastError\":%s}".formatted(
            integration.type().apiName(),
            integration.status().apiName(),
            integration.attemptCount(),
            nullableStringJson(integration.lastError())
        );
    }

    private static String decimalJson(BigDecimal value) {
        return value.toPlainString();
    }

    private static String nullableStringJson(String value) {
        return value == null ? "null" : "\"" + escapeJson(value) + "\"";
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (current < 0x20) {
                        escaped.append("\\u").append("%04x".formatted((int) current));
                    } else {
                        escaped.append(current);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        sendProblem(exchange, 405, "Method Not Allowed", "HTTP method is not allowed for this route.");
    }

    private static void sendProblem(HttpExchange exchange, int statusCode, String title, String detail) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/problem+json; charset=utf-8");
        byte[] body = "{\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"}".formatted(
            escapeJson(title),
            statusCode,
            escapeJson(detail)
        ).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", JSON_CONTENT_TYPE);
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }

    static int portFromEnvironment(Map<String, String> environment) {
        String value = environment.get(PORT_ENVIRONMENT_VARIABLE);
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_PORT;
        }

        try {
            int port = Integer.parseInt(value.trim());
            if (port < 0 || port > 65_535) {
                throw new IllegalArgumentException("PORT must be between 0 and 65535.");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("PORT must be a valid integer.", exception);
        }
    }

    private record AppDependencies(
        InMemoryOrderRepository orderRepository,
        InMemoryCheckoutRepository checkoutRepository,
        InMemoryDeadLetterRepository deadLetterRepository,
        CheckoutUseCase checkoutUseCase
    ) {
    }
}
