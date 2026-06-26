package com.managementplatform.bootstrap;

import com.managementplatform.application.port.CheckoutRepository;
import com.managementplatform.application.port.DeadLetterRepository;
import com.managementplatform.application.port.OrderRepository;
import com.managementplatform.application.port.TimeProvider;
import com.managementplatform.application.usecase.CheckoutUseCase;
import com.managementplatform.infrastructure.gateway.MockPaymentGateway;
import com.managementplatform.infrastructure.repository.InMemoryCheckoutRepository;
import com.managementplatform.infrastructure.repository.InMemoryDeadLetterRepository;
import com.managementplatform.infrastructure.repository.InMemoryOrderRepository;
import com.managementplatform.presentation.http.ManagementPlatformHttpAdapter;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public final class ManagementPlatformBootstrap {
    public static final int DEFAULT_PORT = 8080;
    private static final String PORT_ENVIRONMENT_VARIABLE = "PORT";

    private ManagementPlatformBootstrap() {
    }

    public static void start(Map<String, String> environment) throws IOException {
        HttpServer server = createServer(portFromEnvironment(environment));
        server.start();
        System.out.printf("Management Platform HTTP server listening on port %d%n", server.getAddress().getPort());
    }

    public static HttpServer createServer(int port) throws IOException {
        OrderRepository orderRepository = new InMemoryOrderRepository();
        CheckoutRepository checkoutRepository = new InMemoryCheckoutRepository();
        DeadLetterRepository deadLetterRepository = new InMemoryDeadLetterRepository();
        TimeProvider timeProvider = Instant::now;
        CheckoutUseCase checkoutUseCase = new CheckoutUseCase(
            orderRepository,
            checkoutRepository,
            deadLetterRepository,
            new MockPaymentGateway(),
            timeProvider
        );
        ManagementPlatformHttpAdapter adapter = new ManagementPlatformHttpAdapter(
            orderRepository,
            checkoutRepository,
            deadLetterRepository,
            checkoutUseCase
        );
        return adapter.createServer(port);
    }

    public static int portFromEnvironment(Map<String, String> environment) {
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
}
