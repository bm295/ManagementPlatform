package com.managementplatform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class ArchitectureBoundaryCheck {
    private static final Path APPLICATION_SOURCE_ROOT = Path.of("src/main/java/com/managementplatform/application");
    private static final List<String> FORBIDDEN_HTTP_SERVER_TYPES = List.of(
        "com.sun.net.httpserver.HttpServer",
        "com.sun.net.httpserver.HttpExchange",
        "HttpServer",
        "HttpExchange"
    );

    private ArchitectureBoundaryCheck() {
    }

    public static void main(String[] args) throws IOException {
        try (Stream<Path> paths = Files.walk(APPLICATION_SOURCE_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                .forEach(ArchitectureBoundaryCheck::requireNoHttpServerDependency);
        }
    }

    private static void requireNoHttpServerDependency(Path path) {
        try {
            String source = Files.readString(path);
            for (String forbiddenType : FORBIDDEN_HTTP_SERVER_TYPES) {
                if (source.contains(forbiddenType)) {
                    throw new AssertionError("Application code must not depend on " + forbiddenType + ": " + path);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read source file: " + path, exception);
        }
    }
}
