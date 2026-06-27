# Clean Architecture Refactor Tasks

This checklist tracks the refactor needed to move `ManagementPlatformApplication.java` toward stricter clean architecture boundaries.

## Goals

- Keep the domain and use case layers independent of HTTP/runtime wiring.
- Move composition-root responsibilities out of the main application class.
- Keep HTTP adapters isolated from application orchestration.

## Tasks

- [x] Create a dedicated bootstrap or composition-root package for app startup.
- [x] Move `main(String[] args)` into a small launcher class.
- [x] Move server creation and route registration out of `ManagementPlatformApplication.java`.
- [x] Keep only wiring and startup code in the launcher layer.
- [x] Introduce a separate HTTP adapter class for route handlers.
- [x] Split root landing page handling from API route handling.
- [x] Keep `/health` and `/` responses in the HTTP adapter layer.
- [x] Keep `/api/orders`, `/api/checkouts`, and `/api/dead-letters` handlers isolated from startup code.
- [x] Ensure the use case layer is constructed through ports only.
- [x] Avoid direct dependency from application/use case code to `HttpServer` or `HttpExchange`.
- [x] Update package names or module boundaries only if needed for the new structure.
- [x] Keep domain, application, and infrastructure packages unchanged unless the refactor requires a move.
- [x] Update the manifest `mainClass` in `pom.xml` if the launcher class changes.
- [x] Update README run instructions if the startup entrypoint changes.
- [x] Add or update tests so the server still serves the same API routes after refactor.
- [ ] Run `mvn test` after each structural change set.

## Suggested End State

- `ManagementPlatformApplication.java` becomes a thin launcher or is replaced by a launcher class.
- HTTP routing lives in a dedicated adapter class.
- Dependency wiring lives in a composition-root class or configuration package.
- The architecture diagram and system design docs remain accurate after the code move.
