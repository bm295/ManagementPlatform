package com.managementplatform.presentation.http;

import com.managementplatform.application.dto.CheckoutRequest;
import com.managementplatform.application.dto.CheckoutResponse;
import com.managementplatform.application.dto.CreateOrderRequest;
import com.managementplatform.application.dto.IntegrationStatusDto;
import com.managementplatform.application.dto.OrderResponse;
import com.managementplatform.application.port.in.CreateOrderInputPort;
import com.managementplatform.application.port.in.CheckoutInputPort;
import com.managementplatform.application.port.out.CheckoutRepository;
import com.managementplatform.application.port.out.DeadLetterRepository;
import com.managementplatform.application.port.out.OrderRepository;
import com.managementplatform.domain.model.CheckoutAttempt;
import com.managementplatform.domain.model.DeadLetterMessage;
import com.managementplatform.domain.model.Order;
import com.managementplatform.shared.exception.ConflictException;
import com.managementplatform.shared.exception.ResourceNotFoundException;
import com.managementplatform.shared.exception.ValidationException;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManagementPlatformRouteHandlers {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private final OrderRepository orderRepository;
    private final CheckoutRepository checkoutRepository;
    private final DeadLetterRepository deadLetterRepository;
    private final CreateOrderInputPort createOrderUseCase;
    private final CheckoutInputPort checkoutUseCase;

    public ManagementPlatformRouteHandlers(OrderRepository orderRepository,
                                           CheckoutRepository checkoutRepository,
                                           DeadLetterRepository deadLetterRepository,
                                           CreateOrderInputPort createOrderUseCase,
                                           CheckoutInputPort checkoutUseCase) {
        this.orderRepository = orderRepository;
        this.checkoutRepository = checkoutRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.createOrderUseCase = createOrderUseCase;
        this.checkoutUseCase = checkoutUseCase;
    }

    public void handleOrders(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            List<String> segments = pathSegments(path);
            if (segments.size() < 2 || !"api".equals(segments.get(0)) || !"orders".equals(segments.get(1))) {
                sendProblem(exchange, 404, "Not Found", "Route was not found.");
                return;
            }
            if (segments.size() == 2 && "GET".equals(method)) { handleSearchOrders(exchange); return; }
            if (segments.size() == 2 && "POST".equals(method)) { handleCreateOrder(exchange); return; }
            if (segments.size() == 3 && "GET".equals(method)) { handleGetOrder(exchange, parseId(segments.get(2), "orderId")); return; }
            if (segments.size() == 4 && "POST".equals(method) && "checkout".equals(segments.get(3))) { handleCheckout(exchange, parseId(segments.get(2), "orderId")); return; }
            if (segments.size() == 2 || segments.size() == 3 || segments.size() == 4) { sendMethodNotAllowed(exchange, allowedMethodsFor(segments)); return; }
            sendProblem(exchange, 404, "Not Found", "Route was not found.");
        } catch (ValidationException exception) { sendProblem(exchange, 400, "Bad Request", exception.getMessage()); } catch (ResourceNotFoundException exception) { sendProblem(exchange, 404, "Not Found", exception.getMessage()); } catch (ConflictException exception) { sendProblem(exchange, 409, "Conflict", exception.getMessage()); } catch (RuntimeException exception) { sendProblem(exchange, 500, "Internal Server Error", "An unexpected error occurred."); }
    }

    public void handleCheckouts(HttpExchange exchange) throws IOException { try { String method = exchange.getRequestMethod(); List<String> segments = pathSegments(exchange.getRequestURI().getPath()); if (segments.size() < 2 || !"api".equals(segments.get(0)) || !"checkouts".equals(segments.get(1))) { sendProblem(exchange, 404, "Not Found", "Route was not found."); return; } if (segments.size() == 3 && "GET".equals(method)) { long checkoutId = parseId(segments.get(2), "checkoutId"); CheckoutAttempt attempt = checkoutRepository.findById(checkoutId).orElseThrow(() -> new ResourceNotFoundException("Checkout %d was not found.".formatted(checkoutId))); sendJson(exchange, 200, checkoutAttemptJson(attempt)); return; } if (segments.size() == 4 && "POST".equals(method) && "retry".equals(segments.get(3))) { handleRetryPayment(exchange, parseId(segments.get(2), "checkoutId")); return; } if (segments.size() == 3 || segments.size() == 4) { sendMethodNotAllowed(exchange, allowedCheckoutMethodsFor(segments)); return; } sendProblem(exchange, 404, "Not Found", "Route was not found."); } catch (ValidationException exception) { sendProblem(exchange, 400, "Bad Request", exception.getMessage()); } catch (ResourceNotFoundException exception) { sendProblem(exchange, 404, "Not Found", exception.getMessage()); } catch (ConflictException exception) { sendProblem(exchange, 409, "Conflict", exception.getMessage()); } catch (RuntimeException exception) { sendProblem(exchange, 500, "Internal Server Error", "An unexpected error occurred."); } }

    public void handleDeadLetters(HttpExchange exchange) throws IOException { try { String method = exchange.getRequestMethod(); List<String> segments = pathSegments(exchange.getRequestURI().getPath()); if (segments.size() != 2 || !"api".equals(segments.get(0)) || !"dead-letters".equals(segments.get(1))) { sendProblem(exchange, 404, "Not Found", "Route was not found."); return; } if (!"GET".equals(method)) { sendMethodNotAllowed(exchange, "GET"); return; } StringBuilder json = new StringBuilder(); json.append('['); List<DeadLetterMessage> messages = deadLetterRepository.findRecent(); for (int index = 0; index < messages.size(); index++) { if (index > 0) { json.append(','); } json.append(deadLetterJson(messages.get(index))); } json.append(']'); sendJson(exchange, 200, json.toString()); } catch (RuntimeException exception) { sendProblem(exchange, 500, "Internal Server Error", "An unexpected error occurred."); } }

    private void handleSearchOrders(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        int page = parsePositiveInt(query.get("page"), DEFAULT_PAGE, "page");
        int pageSize = parsePositiveInt(query.get("pageSize"), DEFAULT_PAGE_SIZE, "pageSize");
        String name = query.get("name");
        List<Order> orders = orderRepository.search(name, page, pageSize);
        long total = orderRepository.count(name);
        StringBuilder json = new StringBuilder();
        json.append("{\"items\":[");
        for (int index = 0; index < orders.size(); index++) { if (index > 0) { json.append(','); } json.append(orderSummaryJson(orders.get(index))); }
        json.append("],\"page\":").append(page).append(",\"pageSize\":").append(pageSize).append(",\"totalCount\":").append(total).append('}');
        sendJson(exchange, 200, json.toString());
    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException { CreateOrderRequest request = createOrderRequestFromJson(readRequestBody(exchange)); OrderResponse response = createOrderUseCase.create(request); sendJson(exchange, 201, orderResponseJson(response)); }
    private void handleGetOrder(HttpExchange exchange, long orderId) throws IOException { Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order %d was not found.".formatted(orderId))); sendJson(exchange, 200, orderDetailsJson(order)); }
    private void handleCheckout(HttpExchange exchange, long orderId) throws IOException { CheckoutRequest request = checkoutRequestFromJson(readRequestBody(exchange)); CheckoutResponse response = checkoutUseCase.checkout(orderId, request); sendJson(exchange, 200, checkoutResponseJson(response)); }
    private void handleRetryPayment(HttpExchange exchange, long checkoutId) throws IOException { CheckoutRequest request = checkoutRequestFromJson(readRequestBody(exchange)); CheckoutResponse response = checkoutUseCase.retryPayment(checkoutId, request); sendJson(exchange, 200, checkoutResponseJson(response)); }

    public static CheckoutRequest checkoutRequestFromJson(String json) { if (json == null || json.trim().isEmpty()) { throw new ValidationException("Checkout request body is required."); } return new CheckoutRequest(requiredJsonString(json, "idempotencyKey"), requiredJsonString(json, "paymentMethodToken")); }
    public static CreateOrderRequest createOrderRequestFromJson(String json) {
        if (json == null || json.trim().isEmpty()) { throw new ValidationException("Create order request body is required."); }
        return new CreateOrderRequest(
            requiredJsonLong(json, "tenantId"),
            requiredJsonString(json, "tenantName"),
            requiredJsonString(json, "tenantEmail"),
            requiredJsonString(json, "name"),
            requiredJsonBigDecimal(json, "amount"),
            requiredJsonString(json, "currency")
        );
    }
    private static String requiredJsonString(String json, String fieldName) { String value = jsonStringValue(json, fieldName); if (value == null) { throw new ValidationException("%s is required.".formatted(fieldName)); } return value; }
    private static long requiredJsonLong(String json, String fieldName) { String value = jsonNumberValue(json, fieldName); if (value == null) { throw new ValidationException("%s is required.".formatted(fieldName)); } try { long parsed = Long.parseLong(value.trim()); if (parsed < 1) { throw new ValidationException("%s must be greater than zero.".formatted(fieldName)); } return parsed; } catch (NumberFormatException exception) { throw new ValidationException("%s must be a valid integer.".formatted(fieldName)); } }
    private static BigDecimal requiredJsonBigDecimal(String json, String fieldName) { String value = jsonNumberValue(json, fieldName); if (value == null) { throw new ValidationException("%s is required.".formatted(fieldName)); } try { BigDecimal parsed = new BigDecimal(value.trim()); if (parsed.signum() < 0) { throw new ValidationException("%s must be zero or greater.".formatted(fieldName)); } return parsed; } catch (NumberFormatException exception) { throw new ValidationException("%s must be a valid decimal number.".formatted(fieldName)); } }
    private static String jsonStringValue(String json, String fieldName) { String quotedField = "\"" + fieldName + "\""; int fieldStart = json.indexOf(quotedField); if (fieldStart < 0) { return null; } int colon = json.indexOf(':', fieldStart + quotedField.length()); if (colon < 0) { return null; } int quoteStart = nextNonWhitespaceIndex(json, colon + 1); if (quoteStart >= json.length() || json.charAt(quoteStart) != '"') { return null; } StringBuilder value = new StringBuilder(); boolean escaping = false; for (int index = quoteStart + 1; index < json.length(); index++) { char current = json.charAt(index); if (escaping) { value.append(unescapeJsonCharacter(current)); escaping = false; } else if (current == '\\') { escaping = true; } else if (current == '"') { return value.toString(); } else { value.append(current); } } return null; }
    private static String jsonNumberValue(String json, String fieldName) { String quotedField = "\"" + fieldName + "\""; int fieldStart = json.indexOf(quotedField); if (fieldStart < 0) { return null; } int colon = json.indexOf(':', fieldStart + quotedField.length()); if (colon < 0) { return null; } int numberStart = nextNonWhitespaceIndex(json, colon + 1); int index = numberStart; while (index < json.length()) { char current = json.charAt(index); if (Character.isDigit(current) || current == '-' || current == '+' || current == '.' || current == 'e' || current == 'E') { index++; } else { break; } } return index == numberStart ? null : json.substring(numberStart, index); }
    private static int nextNonWhitespaceIndex(String value, int start) { int index = start; while (index < value.length() && Character.isWhitespace(value.charAt(index))) { index++; } return index; }
    private static char unescapeJsonCharacter(char current) { return switch (current) { case '"' -> '"'; case '\\' -> '\\'; case '/' -> '/'; case 'b' -> '\b'; case 'f' -> '\f'; case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t'; default -> current; }; }
    private static String readRequestBody(HttpExchange exchange) throws IOException { return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); }
    private static Map<String, String> parseQuery(URI uri) { Map<String, String> values = new LinkedHashMap<>(); String query = uri.getRawQuery(); if (query == null || query.isBlank()) { return values; } for (String pair : query.split("&")) { int equals = pair.indexOf('='); String key = equals < 0 ? pair : pair.substring(0, equals); String value = equals < 0 ? "" : pair.substring(equals + 1); values.put(urlDecode(key), urlDecode(value)); } return values; }
    private static String urlDecode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private static int parsePositiveInt(String value, int defaultValue, String fieldName) { if (value == null || value.isBlank()) { return defaultValue; } try { int parsed = Integer.parseInt(value.trim()); if (parsed < 1) { throw new ValidationException("%s must be greater than zero.".formatted(fieldName)); } return parsed; } catch (NumberFormatException exception) { throw new ValidationException("%s must be a valid integer.".formatted(fieldName)); } }
    private static long parseId(String value, String fieldName) { try { long id = Long.parseLong(value); if (id < 1) { throw new ValidationException("%s must be greater than zero.".formatted(fieldName)); } return id; } catch (NumberFormatException exception) { throw new ValidationException("%s must be a valid integer.".formatted(fieldName)); } }
    private static List<String> pathSegments(String path) { return List.of(path.split("/")).stream().filter(segment -> !segment.isBlank()).toList(); }
    private static String allowedMethodsFor(List<String> segments) { if (segments.size() == 2) { return "GET, POST"; } if (segments.size() == 3) { return "GET"; } if (segments.size() == 4 && "checkout".equals(segments.get(3))) { return "POST"; } return "GET, POST"; }
    private static String allowedCheckoutMethodsFor(List<String> segments) { if (segments.size() == 3) { return "GET"; } if (segments.size() == 4 && "retry".equals(segments.get(3))) { return "POST"; } return "GET, POST"; }
    private static String orderSummaryJson(Order order) { return "{\"id\":%d,\"name\":\"%s\",\"tenantName\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\"}".formatted(order.id(), escapeJson(order.name()), escapeJson(order.tenant().name()), decimalJson(order.amount()), escapeJson(order.currency()), order.status().apiName(), order.createdAt()); }
    private static String orderDetailsJson(Order order) { return "{\"id\":%d,\"name\":\"%s\",\"tenantName\":\"%s\",\"tenantEmail\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\",\"paidAt\":%s}".formatted(order.id(), escapeJson(order.name()), escapeJson(order.tenant().name()), escapeJson(order.tenant().email()), decimalJson(order.amount()), escapeJson(order.currency()), order.status().apiName(), order.createdAt(), order.paidAt() == null ? "null" : "\"" + order.paidAt() + "\""); }
    private static String orderResponseJson(OrderResponse response) { return "{\"id\":%d,\"tenantId\":%d,\"tenantName\":\"%s\",\"tenantEmail\":\"%s\",\"name\":\"%s\",\"amount\":%s,\"currency\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\",\"paidAt\":%s}".formatted(response.id(), response.tenantId(), escapeJson(response.tenantName()), escapeJson(response.tenantEmail()), escapeJson(response.name()), decimalJson(response.amount()), escapeJson(response.currency()), response.status().apiName(), response.createdAt(), response.paidAt() == null ? "null" : "\"" + response.paidAt() + "\""); }
    private static String checkoutAttemptJson(CheckoutAttempt attempt) { return "{\"checkoutId\":%d,\"orderId\":%d,\"status\":\"%s\",\"paymentStatus\":%s,\"failureReason\":%s,\"integrations\":[%s]}".formatted(attempt.id(), attempt.orderId(), attempt.status().apiName(), attempt.paymentTransaction() == null ? "null" : "\"" + attempt.paymentTransaction().status().apiName() + "\"", nullableStringJson(attempt.failureReason() != null ? attempt.failureReason() : paymentFailureReason(attempt)), integrationsJson(attempt.outboxMessages().stream().map(message -> new IntegrationStatusDto(message.type(), message.status(), message.attemptCount(), message.lastError())).toList())); }
    private static String integrationsJson(List<IntegrationStatusDto> integrations) { StringBuilder json = new StringBuilder(); for (int index = 0; index < integrations.size(); index++) { if (index > 0) { json.append(','); } json.append(integrationJson(integrations.get(index))); } return json.toString(); }
    private static String paymentFailureReason(CheckoutAttempt attempt) { return attempt.paymentTransaction() == null ? null : attempt.paymentTransaction().failureReason(); }
    private static String deadLetterJson(DeadLetterMessage message) { return "{\"id\":%d,\"checkoutAttemptId\":%d,\"outboxMessageId\":%d,\"type\":\"%s\",\"attemptCount\":%d,\"failureReason\":%s,\"failedAt\":\"%s\"}".formatted(message.id(), message.checkoutAttemptId(), message.outboxMessageId(), message.type().apiName(), message.attemptCount(), nullableStringJson(message.failureReason()), message.failedAt()); }
    private static String checkoutResponseJson(CheckoutResponse response) { StringBuilder json = new StringBuilder(); json.append("{\"checkoutId\":").append(response.checkoutId()).append(",\"orderId\":").append(response.orderId()).append(",\"status\":\"").append(response.status().apiName()).append('"').append(",\"paymentStatus\":").append(response.paymentStatus() == null ? "null" : "\"" + response.paymentStatus().apiName() + "\"").append(",\"failureReason\":").append(nullableStringJson(response.failureReason())).append(",\"integrations\":["); for (int index = 0; index < response.integrations().size(); index++) { if (index > 0) { json.append(','); } json.append(integrationJson(response.integrations().get(index))); } json.append("]}"); return json.toString(); }
    private static String integrationJson(IntegrationStatusDto integration) { return "{\"type\":\"%s\",\"status\":\"%s\",\"attemptCount\":%d,\"lastError\":%s}".formatted(integration.type().apiName(), integration.status().apiName(), integration.attemptCount(), nullableStringJson(integration.lastError())); }
    private static String decimalJson(BigDecimal value) { return value.toPlainString(); }
    private static String nullableStringJson(String value) { return value == null ? "null" : "\"" + escapeJson(value) + "\""; }
    private static String escapeJson(String value) { StringBuilder escaped = new StringBuilder(); for (int index = 0; index < value.length(); index++) { char current = value.charAt(index); switch (current) { case '"' -> escaped.append("\\\""); case '\\' -> escaped.append("\\\\"); case '\b' -> escaped.append("\\b"); case '\f' -> escaped.append("\\f"); case '\n' -> escaped.append("\\n"); case '\r' -> escaped.append("\\r"); case '\t' -> escaped.append("\\t"); default -> { if (current < 0x20) { escaped.append("\\u").append("%04x".formatted((int) current)); } else { escaped.append(current); } } } } return escaped.toString(); }
    private static void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException { exchange.getResponseHeaders().set("Allow", allowedMethods); sendProblem(exchange, 405, "Method Not Allowed", "HTTP method is not allowed for this route."); }
    private static void sendProblem(HttpExchange exchange, int statusCode, String title, String detail) throws IOException { exchange.getResponseHeaders().set("Content-Type", "application/problem+json; charset=utf-8"); byte[] body = "{\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"}".formatted(escapeJson(title), statusCode, escapeJson(detail)).getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(statusCode, body.length); try (OutputStream responseBody = exchange.getResponseBody()) { responseBody.write(body); } }
    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException { exchange.getResponseHeaders().set("Content-Type", JSON_CONTENT_TYPE); byte[] body = json.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(statusCode, body.length); try (OutputStream responseBody = exchange.getResponseBody()) { responseBody.write(body); } }
}
