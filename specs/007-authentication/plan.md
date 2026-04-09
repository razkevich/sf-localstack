# Implementation Plan: Authentication & User Management

**Branch**: `007-authentication` | **Date**: 2026-04-09 | **Spec**: [spec.md](spec.md)

## Summary

Add JWT-based authentication with file-based user store, RBAC (ADMIN/USER roles), auth filter on all protected endpoints, login UI, and sf CLI compatibility via the OAuth2 token endpoint.

## Technical Context

**Language/Version**: Java 21
**New Dependencies**: `io.jsonwebtoken:jjwt-api/impl/jackson` 0.12.x (JWT), `spring-security-crypto` (BCrypt only)
**Storage**: `data/users.json` (file-based, MVP)
**Testing**: JUnit 5 + MockMvc (existing 101 tests + new auth tests)
**Frontend**: React 18 + TypeScript + Tailwind (login/register pages)

## Constitution Check

- **API Fidelity**: OAuth2 token endpoint upgraded from stub to real JWT. SF-compatible error format for 401s.
- **Test-First**: New auth tests + existing 101 tests pass via test profile auth bypass.
- **Dependency Surface**: 2 new deps justified — JJWT (JWT standard), spring-security-crypto (BCrypt standard). Both minimal.
- **Authentication (Principle VII)**: This feature implements the principle. UserStore interface enables swapping.
- **Scope Control**: Auth + user management only. No other API changes.
- **Parity Verification**: sf CLI `sf org login` must authenticate against emulator.

## Project Structure

```text
service/src/main/java/co/razkevich/sflocalstack/
  auth/
    controller/  → AuthController.java
    service/     → JwtService.java, AuthService.java
    model/       → User.java, Role.java, TokenPair.java
    store/       → UserStore.java (interface), FileBasedUserStore.java
    filter/      → JwtAuthFilter.java
  controller/
    → OAuthController.java (MODIFY — real JWT tokens)

service/src/main/resources/
  → application.yml (MODIFY — add jwt.secret property)

service/pom.xml (MODIFY — add JJWT + spring-security-crypto deps)

frontend/src/
  → services/api.ts (MODIFY — add auth headers to all requests)
  → components/LoginPage.tsx (CREATE)
  → components/RegisterPage.tsx (CREATE)
  → hooks/useAuth.ts (CREATE — token management)
  → App.tsx (MODIFY — protected routes)
```

## Feature Iterations

### Feature 0: User Store + JWT Service (backend foundation)
### Feature 1: Auth Endpoints (REST API)
### Feature 2: Auth Filter (enforcement)
### Feature 3: RBAC (role checks)
### Feature 4: Login UI (frontend)
### Feature 5: Extensibility Docs
