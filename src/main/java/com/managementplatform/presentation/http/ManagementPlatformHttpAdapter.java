package com.managementplatform.presentation.http;

import com.managementplatform.application.dto.CheckoutRequest;
import com.managementplatform.application.port.CheckoutRepository;
import com.managementplatform.application.port.DeadLetterRepository;
import com.managementplatform.application.port.OrderRepository;
import com.managementplatform.application.usecase.CheckoutUseCase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class ManagementPlatformHttpAdapter {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=utf-8";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String PROBLEM_CONTENT_TYPE = "application/problem+json; charset=utf-8";
    private static final byte[] LANDING_PAGE_BODY = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Management Platform</title></head><body><main><h1>Management Platform API</h1><p>The service is running.</p></main></body></html>".getBytes(StandardCharsets.UTF_8);

    private final ManagementPlatformRouteHandlers routeHandlers;

    public ManagementPlatformHttpAdapter(OrderRepository orderRepository,
                                         CheckoutRepository checkoutRepository,
                                         DeadLetterRepository deadLetterRepository,
                                         CheckoutUseCase checkoutUseCase) {
        this(new ManagementPlatformRouteHandlers(
            orderRepository,
            checkoutRepository,
            deadLetterRepository,
            checkoutUseCase
        ));
    }

    public ManagementPlatformHttpAdapter(ManagementPlatformRouteHandlers routeHandlers) {
        this.routeHandlers = routeHandlers;
    }

    public HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleRoot);
        server.createContext("/health", this::handleHealth);
        server.createContext("/api/orders", routeHandlers::handleOrders);
        server.createContext("/api/checkouts", routeHandlers::handleCheckouts);
        server.createContext("/api/dead-letters", routeHandlers::handleDeadLetters);
        server.setExecutor(null);
        return server;
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, "GET");
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, "GET");
            return;
        }
        if (!"/".equals(exchange.getRequestURI().getPath())) {
            sendProblem(exchange, 404, "Not Found", "Route was not found.");
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, LANDING_PAGE_BODY.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(LANDING_PAGE_BODY);
        }
    }

    public static CheckoutRequest checkoutRequestFromJson(String json) {
        return ManagementPlatformRouteHandlers.checkoutRequestFromJson(json);
    }

    private static void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        sendProblem(exchange, 405, "Method Not Allowed", "HTTP method is not allowed for this route.");
    }

    private static void sendProblem(HttpExchange exchange, int statusCode, String title, String detail) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", PROBLEM_CONTENT_TYPE);
        byte[] body = "{\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"}".formatted(escapeJson(title), statusCode, escapeJson(detail)).getBytes(StandardCharsets.UTF_8);
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

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
