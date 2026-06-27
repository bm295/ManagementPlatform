package com.managementplatform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ArchitectureBoundaryCheck {
    private static final Path APPLICATION_SOURCE_ROOT = Path.of("src/main/java/com/managementplatform/application");
    private static final Path MAIN_SOURCE_ROOT = Path.of("src/main/java/com/managementplatform");
    private static final List<String> FORBIDDEN_HTTP_SERVER_TYPES = List.of(
        "com.sun.net.httpserver.HttpServer",
        "com.sun.net.httpserver.HttpExchange",
        "HttpServer",
        "HttpExchange"
    );
    private static final Map<String, String> STABLE_LAYER_PACKAGES = Map.of(
        "application", "com.managementplatform.application",
        "domain", "com.managementplatform.domain",
        "infrastructure", "com.managementplatform.infrastructure"
    );

    private ArchitectureBoundaryCheck() {
    }

    public static void main(String[] args) throws IOException {
        requireApplicationLayerHasNoHttpServerDependency();
        requireStableLayerPackageNames();
    }

    private static void requireApplicationLayerHasNoHttpServerDependency() throws IOException {
        try (Stream<Path> paths = Files.walk(APPLICATION_SOURCE_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                .forEach(ArchitectureBoundaryCheck::requireNoHttpServerDependency);
        }
    }

    private static void requireStableLayerPackageNames() throws IOException {
        for (Map.Entry<String, String> layer : STABLE_LAYER_PACKAGES.entrySet()) {
            Path layerRoot = MAIN_SOURCE_ROOT.resolve(layer.getKey());
            try (Stream<Path> paths = Files.walk(layerRoot)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> requirePackageName(path, layer.getValue()));
            }
        }
    }

    private static void requirePackageName(Path path, String expectedPackagePrefix) {
        try {
            String source = Files.readString(path);
            String packageDeclaration = source.lines()
                .filter(line -> line.startsWith("package "))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Source file is missing package declaration: " + path));
            if (!packageDeclaration.startsWith("package " + expectedPackagePrefix)) {
                throw new AssertionError(
                    "Layer package moved unexpectedly. Expected " + path + " to stay under " + expectedPackagePrefix
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read source file: " + path, exception);
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
