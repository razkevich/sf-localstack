# sf_localstack Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-04-09

## Active Technologies
- H2 in-memory database for sObjects; in-memory maps for Bulk jobs and Metadata deploy jobs; YAML seed file for baseline org state (001-sf-ci-emulator)
- Java 21, YAML (GitHub Actions), Dockerfile + Spring Boot Maven plugin (`spring-boot:repackage`), `docker/build-push-action`, `docker/setup-buildx-action`, QEMU for arm64 emulation (002-cicd-packaging)
- N/A — build artifacts only; GitHub Actions cache for Maven `.m2` (002-cicd-packaging)
- Java 21 (both repos), Spring Boot 3.3.x + Testcontainers 1.19+ (GenericContainer), WireMock (removed from metadata-service test scope), Spring Tes (003-ms-test-sf-localstack)
- H2 in-memory (sf-localstack), PostgreSQL (metadata-service tests via existing Testcontainers setup) (003-ms-test-sf-localstack)
- Java 21 + Spring Boot 3.3.5 (spring-boot-starter-test), JUnit 5, MockMvc, AssertJ (004-test-coverage)
- H2 in-memory (test profile) (004-test-coverage)

- Java 21 for backend, TypeScript 5.x with React 18 for dashboard + Spring Boot 3.3.5 (`web`, `data-jpa`, `actuator`, `test`), H2, Jackson JSON/YAML, React 18, Vite 5, Tailwind 3 (001-sf-ci-emulator)

## Project Structure

```text
src/
tests/
```

## Commands

npm test && npm run lint

## Code Style

Java 21 for backend, TypeScript 5.x with React 18 for dashboard: Follow standard conventions

## Recent Changes
- 004-test-coverage: Added Java 21 + Spring Boot 3.3.5 (spring-boot-starter-test), JUnit 5, MockMvc, AssertJ
- 003-ms-test-sf-localstack: Added Java 21 (both repos), Spring Boot 3.3.x + Testcontainers 1.19+ (GenericContainer), WireMock (removed from metadata-service test scope), Spring Tes
- 002-cicd-packaging: Added Java 21, YAML (GitHub Actions), Dockerfile + Spring Boot Maven plugin (`spring-boot:repackage`), `docker/build-push-action`, `docker/setup-buildx-action`, QEMU for arm64 emulation


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
