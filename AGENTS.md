# sf_localstack Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-04-12

## Active Technologies
- H2 in-memory database for sObjects; in-memory maps for Bulk jobs and Metadata deploy jobs; YAML seed file for baseline org state (001-sf-ci-emulator)
- Java 21, YAML (GitHub Actions), Dockerfile + Spring Boot Maven plugin (`spring-boot:repackage`), `docker/build-push-action`, `docker/setup-buildx-action`, QEMU for arm64 emulation (002-cicd-packaging)
- N/A — build artifacts only; GitHub Actions cache for Maven `.m2` (002-cicd-packaging)
- Java 21 (both repos), Spring Boot 3.3.x + Testcontainers 1.19+ (GenericContainer), WireMock (removed from metadata-service test scope), Spring Tes (003-ms-test-sf-localstack)
- H2 in-memory (sf-localstack), PostgreSQL (metadata-service tests via existing Testcontainers setup) (003-ms-test-sf-localstack)
- Java 21 + Spring Boot 3.3.5 (spring-boot-starter-test), JUnit 5, MockMvc, AssertJ (004-test-coverage)
- H2 in-memory (test profile) (004-test-coverage)
- Java 21 + Spring Boot 3.3.5, Spring Data JPA, H2 (existing — no new dependencies) (005-persistent-storage)
- H2 file-based (`jdbc:h2:file:./data/sfdb`) for dev; H2 mem for tes (005-persistent-storage)
- `data/users.json` (file-based, MVP) (007-authentication)
- Java 21, Spring Boot 3.3.5 + Spring Web (existing), JJWT 0.12.6 (existing), Thymeleaf or inline HTML for login page (no new dependency — use inline HTML string) (012-oauth2-web-login)
- ConcurrentHashMap for authorization codes (in-memory, expires after 5 min) (012-oauth2-web-login)

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
- 012-oauth2-web-login: Added Java 21, Spring Boot 3.3.5 + Spring Web (existing), JJWT 0.12.6 (existing), Thymeleaf or inline HTML for login page (no new dependency — use inline HTML string)
- 007-authentication: Added Java 21
- 005-persistent-storage: Added Java 21 + Spring Boot 3.3.5, Spring Data JPA, H2 (existing — no new dependencies)


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
