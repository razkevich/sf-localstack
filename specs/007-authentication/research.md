# Research: 007-authentication

## JWT Library

**Decision**: Use `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson` (JJWT 0.12.x)
**Rationale**: Most popular Java JWT library, lightweight, well-maintained, supports HMAC-SHA256. Spring Security's JWT support pulls in the full security framework which is overkill.
**Alternatives**: `com.auth0:java-jwt` (viable but less popular in Spring ecosystem), `spring-boot-starter-oauth2-resource-server` (too heavy for MVP)

## Password Hashing

**Decision**: Use `org.springframework.security:spring-security-crypto` for BCryptPasswordEncoder only
**Rationale**: Single class import, no full Spring Security framework. BCrypt is the standard for password hashing.
**Alternatives**: Manual BCrypt via `org.mindrot:jbcrypt` (works but Spring's encoder is better tested), Argon2 (overkill for MVP)

## Auth Filter Approach

**Decision**: Custom `OncePerRequestFilter` — not Spring Security's filter chain
**Rationale**: We need precise control over which paths require auth, and Salesforce-compatible error formats. Spring Security's auto-configuration would fight us on error shapes.
**Alternatives**: Full Spring Security (rejected — too much configuration, opinionated error formats)

## Test Profile Auth Strategy

**Decision**: Auth filter checks for `test` profile and auto-creates a test admin user + injects auth header via test helper
**Rationale**: Keeps existing tests working with minimal changes. Test helper `TestDataFactory` gets `withAuth(MockMvc)` method.
**Alternatives**: Disable filter entirely in test (rejected — misses auth integration bugs), `@MockBean` the filter (rejected — fragile)

## File-Based User Store

**Decision**: JSON file at `data/users.json`, ObjectMapper for serialization, `FileLock` for concurrent writes
**Rationale**: Simple, portable, no database dependency. File locking prevents corruption from concurrent requests.
**Alternatives**: YAML file (rejected — JSON is simpler for programmatic access), SQLite (rejected — adds dependency)

## JWT Configuration

**Decision**: Secret configurable via `sf-localstack.jwt.secret` property. Random 256-bit default generated at startup.
**Rationale**: Allows production deployments to set a stable secret. Dev mode gets a random one (tokens invalidated on restart, which is fine).
**Alternatives**: Key file (too complex for MVP), environment variable only (less flexible than property)
