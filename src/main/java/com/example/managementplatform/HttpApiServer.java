package com.example.managementplatform;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

final class HttpApiServer {
    private static final Pattern ORDER_PATH = Pattern.compile("/api/orders/(\\d+)");
    private static final Pattern CHECKOUT_PATH = Pattern.compile("/api/checkouts/(\\d+)");
    private static final Pattern ORDER_CHECKOUT_PATH = Pattern.compile("/api/orders/(\\d+)/checkout");

    private HttpApiServer() { }

    static void start(String[] args) throws IOException {
        var port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
        var repository = new ManagementRepository();
        var checkoutService = new CheckoutService(repository);
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> handle(exchange, repository, checkoutService));
        server.start();
        System.out.println("Management Platform Java API listening on http://localhost:" + port);
    }

    private static void handle(HttpExchange exchange, ManagementRepository repository, CheckoutService checkoutService) throws IOException {
        try {
            var path = exchange.getRequestURI().getPath();
            var method = exchange.getRequestMethod();
            if (method.equals("GET") && path.equals("/api/orders")) {
                var query = query(exchange.getRequestURI());
                var page = Integer.parseInt(query.getOrDefault("page", "1"));
                var pageSize = Integer.parseInt(query.getOrDefault("pageSize", "20"));
                var name = query.get("name");
                var items = repository.search(name, page, pageSize).stream().map(HttpApiServer::orderSummaryJson).toList();
                send(exchange, 200, "{\"items\":[" + String.join(",", items) + "],\"page\":" + page + ",\"pageSize\":" + pageSize + ",\"totalCount\":" + repository.count(name) + "}");
                return;
            }
            var orderMatcher = ORDER_PATH.matcher(path);
            if (method.equals("GET") && orderMatcher.matches()) {
                var order = repository.findOrder(Long.parseLong(orderMatcher.group(1))).orElseThrow(() -> new ApiException(404, "Order was not found."));
                send(exchange, 200, orderDetailsJson(order, checkoutService));
                return;
            }
            var checkoutMatcher = ORDER_CHECKOUT_PATH.matcher(path);
            if (method.equals("POST") && checkoutMatcher.matches()) {
                var request = parseCheckoutRequest(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                send(exchange, 200, checkoutJson(checkoutService.checkout(Long.parseLong(checkoutMatcher.group(1)), request)));
                return;
            }
            var checkoutStatusMatcher = CHECKOUT_PATH.matcher(path);
            if (method.equals("GET") && checkoutStatusMatcher.matches()) {
                send(exchange, 200, checkoutJson(checkoutService.get(Long.parseLong(checkoutStatusMatcher.group(1)))));
                return;
            }
            if (method.equals("GET") && path.equals("/api/dead-letters")) {
                var json = repository.allCheckouts().stream().flatMap(a -> a.outboxMessages().stream())
                    .filter(m -> m.status() == OutboxStatus.FAILED)
                    .map(m -> "{\"checkoutAttemptId\":" + m.checkoutAttemptId() + ",\"type\":\"" + m.type() + "\",\"failureReason\":\"" + escape(m.lastError()) + "\",\"attemptCount\":" + m.attemptCount() + "}").toList();
                send(exchange, 200, "[" + String.join(",", json) + "]");
                return;
            }
            send(exchange, 404, "{\"error\":\"Not found\"}");
        } catch (ApiException ex) {
            send(exchange, ex.statusCode(), "{\"error\":\"" + escape(ex.getMessage()) + "\"}");
        } catch (Exception ex) {
            send(exchange, 500, "{\"error\":\"" + escape(ex.getMessage()) + "\"}");
        }
    }

    private static Map<String, String> query(URI uri) {
        var result = new LinkedHashMap<String, String>();
        if (uri.getRawQuery() == null) return result;
        for (var part : uri.getRawQuery().split("&")) {
            var pieces = part.split("=", 2);
            result.put(pieces[0], pieces.length == 2 ? pieces[1].replace("+", " ") : "");
        }
        return result;
    }

    private static CheckoutRequest parseCheckoutRequest(String body) {
        return new CheckoutRequest(extract(body, "idempotencyKey"), extract(body, "paymentMethodToken"));
    }

    private static String extract(String body, String key) {
        var marker = "\"" + key + "\"";
        var i = body.indexOf(marker);
        if (i < 0) return null;
        var colon = body.indexOf(':', i);
        var first = body.indexOf('"', colon + 1);
        var second = body.indexOf('"', first + 1);
        return first < 0 || second < 0 ? null : body.substring(first + 1, second);
    }

    private static String orderSummaryJson(Order o) {
        return "{\"id\":" + o.id() + ",\"name\":\"" + escape(o.name()) + "\",\"amount\":" + money(o.amount()) + ",\"currency\":\"" + o.currency() + "\",\"status\":\"" + o.status() + "\",\"createdAt\":\"" + o.createdAt() + "\"}";
    }

    private static String orderDetailsJson(Order o, CheckoutService checkoutService) {
        var checkouts = o.checkoutAttempts().stream().map(checkoutService::toResponse).map(HttpApiServer::checkoutJson).toList();
        return "{\"id\":" + o.id() + ",\"tenantName\":\"" + escape(o.tenant().name()) + "\",\"name\":\"" + escape(o.name()) + "\",\"amount\":" + money(o.amount()) + ",\"currency\":\"" + o.currency() + "\",\"status\":\"" + o.status() + "\",\"createdAt\":\"" + o.createdAt() + "\",\"paidAt\":" + (o.paidAt() == null ? "null" : "\"" + o.paidAt() + "\"") + ",\"checkouts\":[" + String.join(",", checkouts) + "]}";
    }

    private static String checkoutJson(CheckoutResponse response) {
        var integrations = response.integrations().stream().map(i -> "{\"type\":\"" + i.type() + "\",\"status\":\"" + i.status() + "\",\"attemptCount\":" + i.attemptCount() + ",\"lastError\":" + (i.lastError() == null ? "null" : "\"" + escape(i.lastError()) + "\"") + "}").toList();
        return "{\"checkoutId\":" + response.checkoutId() + ",\"orderId\":" + response.orderId() + ",\"status\":\"" + response.status() + "\",\"paymentStatus\":" + (response.paymentStatus() == null ? "null" : "\"" + response.paymentStatus() + "\"") + ",\"failureReason\":" + (response.failureReason() == null ? "null" : "\"" + escape(response.failureReason()) + "\"") + ",\"integrations\":[" + String.join(",", integrations) + "]}";
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String money(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
}
