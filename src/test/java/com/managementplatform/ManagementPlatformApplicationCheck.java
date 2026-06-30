package com.managementplatform;

import com.managementplatform.bootstrap.ManagementPlatformBootstrap;
import com.managementplatform.application.dto.CheckoutRequest;
import com.managementplatform.presentation.http.ManagementPlatformHttpAdapter;
import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public final class ManagementPlatformApplicationCheck {
    private ManagementPlatformApplicationCheck() {
    }

    public static void main(String[] args) throws Exception {
        usesDefaultPortWhenEnvironmentIsMissing();
        readsPortFromEnvironment();
        rejectsInvalidPort();
        parsesCheckoutJsonBody();
        servesOrderRoutesAndCheckout();
    }

    private static void usesDefaultPortWhenEnvironmentIsMissing() {
        require(ManagementPlatformBootstrap.portFromEnvironment(Map.of()) == ManagementPlatformBootstrap.DEFAULT_PORT,
            "missing PORT should use default port");
        require(ManagementPlatformBootstrap.portFromEnvironment(Map.of("PORT", " ")) == ManagementPlatformBootstrap.DEFAULT_PORT,
            "blank PORT should use default port");
    }

    private static void readsPortFromEnvironment() {
        require(ManagementPlatformBootstrap.portFromEnvironment(Map.of("PORT", "9090")) == 9090,
            "PORT environment value should be used");
        require(ManagementPlatformBootstrap.portFromEnvironment(Map.of("PORT", " 7070 ")) == 7070,
            "PORT environment value should be trimmed");
    }

    private static void rejectsInvalidPort() {
        expectInvalidPort("abc");
        expectInvalidPort("65536");
        expectInvalidPort("-1");
    }

    private static void parsesCheckoutJsonBody() {
        CheckoutRequest request = ManagementPlatformHttpAdapter.checkoutRequestFromJson("{\"idempotencyKey\":\"idem-1\",\"paymentMethodToken\":\"tok_success\"}");

        require(request.idempotencyKey().equals("idem-1"), "JSON parser should read idempotencyKey");
        require(request.paymentMethodToken().equals("tok_success"), "JSON parser should read paymentMethodToken");
    }

    private static void servesOrderRoutesAndCheckout() throws Exception {
        HttpServer server = ManagementPlatformBootstrap.createServer(0);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String baseUrl = "http://localhost:%d".formatted(server.getAddress().getPort());

            HttpResponse<String> root = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(root.statusCode() == 200, "GET / should return the landing page");
            require(root.headers().firstValue("Content-Type").orElse("").startsWith("text/html"),
                "landing page should use HTML content type");
            require(root.body().contains("Management Platform API"), "landing page should describe the service");
            require(root.body().contains("Search orders by name"), "landing page should include order search UI");
            require(root.body().contains("Checkout"), "landing page should include checkout UI");
            require(root.body().contains("Refresh dead letters"), "landing page should include dead-letter monitoring UI");
            require(root.body().contains("/admin"), "landing page should link to admin recovery UI");

            HttpResponse<String> admin = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/admin")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(admin.statusCode() == 200, "GET /admin should return HTTP 200");
            require(admin.body().contains("Retry payments"), "admin page should list retry payments");
            require(admin.body().contains("Dead letter messages"), "admin page should list dead letter messages");

            HttpResponse<String> orders = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/orders?page=1&pageSize=2&name=Acme")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(orders.statusCode() == 200, "GET /api/orders should return HTTP 200");
            require(orders.body().contains("\"page\":1"), "orders response should include parsed page");
            require(orders.body().contains("\"pageSize\":2"), "orders response should include parsed pageSize");
            require(orders.body().contains("\"totalCount\":"), "orders response should include documented totalCount");
            require(orders.body().contains("\"tenantName\":\"Acme Corp\""), "orders response should include summary tenant name");
            require(!orders.body().contains("\"tenant\":"), "orders response should not include nested tenant details");
            require(orders.body().contains("Acme"), "orders response should apply name query");

            HttpResponse<String> order = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/orders/1")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(order.statusCode() == 200, "GET /api/orders/{id} should return HTTP 200");
            require(order.body().equals("{\"id\":1,\"name\":\"Acme onboarding package\",\"tenantName\":\"Acme Corp\",\"tenantEmail\":\"ops@acme.example\",\"amount\":199.00,\"currency\":\"USD\",\"status\":\"Draft\",\"createdAt\":\"2026-04-28T08:02:00Z\",\"paidAt\":null}"),
                "order detail response should match the JSON snapshot");

            HttpResponse<String> checkout = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/orders/1/checkout"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"idempotencyKey\":\"http-checkout-1\",\"paymentMethodToken\":\"tok_success\"}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(checkout.statusCode() == 200, "POST /api/orders/{id}/checkout should return HTTP 200");
            require(checkout.body().equals("{\"checkoutId\":1,\"orderId\":1,\"status\":\"PaymentSucceeded\",\"paymentStatus\":\"Succeeded\",\"failureReason\":null,\"integrations\":[{\"type\":\"SendCheckoutEmail\",\"status\":\"Pending\",\"attemptCount\":0,\"lastError\":null}]}"),
                "checkout response should match the JSON snapshot");
            require(checkout.headers().firstValue("Content-Type").orElse("").startsWith("application/json"),
                "checkout response should use JSON content type");

            HttpResponse<String> checkoutStatus = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/checkouts/1")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(checkoutStatus.statusCode() == 200, "GET /api/checkouts/{id} should return HTTP 200");
            require(checkoutStatus.body().contains("\"checkoutId\":1"), "checkout status response should include checkout id");

            HttpResponse<String> missingCheckout = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/checkouts/9999")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(missingCheckout.statusCode() == 404, "missing checkout resources should map to HTTP 404");

            HttpResponse<String> failedCheckout = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/orders/2/checkout"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"idempotencyKey\":\"http-checkout-fail\",\"paymentMethodToken\":\"tok_fail\"}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(failedCheckout.statusCode() == 200, "failed payment checkout should still return HTTP 200");

            HttpResponse<String> retryCheckout = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/checkouts/2/retry"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"idempotencyKey\":\"http-checkout-retry\",\"paymentMethodToken\":\"tok_success\"}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(retryCheckout.statusCode() == 200, "POST /api/checkouts/{id}/retry should return HTTP 200");
            require(retryCheckout.body().contains("\"checkoutId\":3"), "retry payment should create a new checkout attempt");
            require(retryCheckout.body().contains("\"status\":\"PaymentSucceeded\""), "retry payment should succeed with a valid token");

            HttpResponse<String> deadLetters = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/dead-letters")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(deadLetters.statusCode() == 200, "GET /api/dead-letters should return HTTP 200");
            require(deadLetters.body().contains("\"type\":\"PaymentCharge\""), "dead letters response should include payment charge failure");
            require(!deadLetters.body().contains("\"payload\""),
                "dead letters response should match the documented API fields");
            require(deadLetters.body().contains("\"failureReason\":\"Payment failed in the mock gateway.\""),
                "dead letters response should include the failure reason");

            HttpResponse<String> badRequest = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/orders?page=abc")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(badRequest.statusCode() == 400, "validation errors should map to HTTP 400");
            require(badRequest.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"),
                "validation errors should use problem JSON content type");

            HttpResponse<String> missingOrder = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/orders/9999")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(missingOrder.statusCode() == 404, "missing resources should map to HTTP 404");

            HttpResponse<String> conflict = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/orders/1/checkout"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"idempotencyKey\":\"different-key\",\"paymentMethodToken\":\"tok_success\"}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            require(conflict.statusCode() == 409, "order conflicts should map to HTTP 409");
        } finally {
            server.stop(0);
        }
    }

    private static void expectInvalidPort(String value) {
        try {
            ManagementPlatformBootstrap.portFromEnvironment(Map.of("PORT", value));
            throw new AssertionError("invalid PORT should throw IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            require(exception.getMessage().startsWith("PORT must"), "invalid PORT message should mention PORT");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
