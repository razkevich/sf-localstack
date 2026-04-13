# Implementation Plan: OAuth2 Authorization Code Flow (Web Login)

**Branch**: `012-oauth2-web-login` | **Date**: 2026-04-12 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/012-oauth2-web-login/spec.md`

## Summary

Implement the OAuth2 authorization code grant flow so that `sf org login web --instance-url <url>` works against sf_localstack. This requires: (1) a `GET /services/oauth2/authorize` endpoint that renders a server-side HTML login page, (2) a `POST /services/oauth2/authorize` endpoint that validates credentials and redirects with an auth code, (3) extending the existing token endpoint to handle `grant_type=authorization_code`. Authorization codes are stored in a ConcurrentHashMap with 5-minute TTL and single-use enforcement.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.3.5
**Primary Dependencies**: Spring Web (existing), JJWT 0.12.6 (existing), Thymeleaf or inline HTML for login page (no new dependency — use inline HTML string)
**Storage**: ConcurrentHashMap for authorization codes (in-memory, expires after 5 min)
**Testing**: JUnit 5 + MockMvc (`@SpringBootTest`), test profile with InMemoryUserStore
**Target Platform**: Linux server (DigitalOcean droplet), Docker
**Project Type**: Web service (Salesforce API emulator)
**Performance Goals**: Authorization code exchange completes in <100ms
**Constraints**: No new dependencies — use inline HTML for login page, ConcurrentHashMap for code store
**Scale/Scope**: Single-org MVP, ~5 concurrent OAuth flows

## Constitution Check

*GATE: Must pass before implementation. Re-check after design updates.*

- **API Fidelity**: Surface is OAuth2. The authorize endpoint accepts standard OAuth2 parameters (`response_type=code`, `client_id`, `redirect_uri`, `state`). The redirect response uses `?code=<code>&state=<state>` format. The token exchange returns the same Salesforce-compatible envelope as the existing password grant. All shapes match Salesforce's documented OAuth2 Web Server Flow.
- **Test-First**: Tests will: (1) verify GET /authorize returns HTML with correct form fields, (2) verify POST /authorize with valid creds redirects with a code, (3) verify POST /token with authorization_code grant returns valid JWT, (4) verify code single-use enforcement, (5) verify code expiry, (6) verify redirect_uri mismatch rejection. State reset: each test creates its own user via InMemoryUserStore.
- **Runtime Reproducibility**: Auth codes use UUID generation (deterministic per test seed). Code TTL checked against Instant.now(). Tests don't depend on wall-clock timing — expiry tests use a short TTL or directly manipulate the code store.
- **Dependency Surface**: Zero new dependencies. HTML rendered as inline String in the controller. Auth codes stored in existing ConcurrentHashMap pattern (same as BulkJobService). No new libraries.
- **Observability**: Authorization requests logged by existing RequestLoggingFilter. Auth code generation and exchange logged at DEBUG level. Failed login attempts logged at WARN.
- **Scope Control**: Smallest slice: authorize endpoint + code exchange + inline HTML form. Out of scope: PKCE, connected app registry, client_secret validation, refresh token on code exchange, device flow.
- **Parity Verification**: Run `sf org login web --instance-url http://localhost:8080`, complete login in browser, verify CLI reports success. Then run `sf data query` to verify the token works.

## Project Structure

### Documentation (this feature)

```text
specs/012-oauth2-web-login/
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   └── oauth2-authorize.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
service/src/main/java/co/razkevich/sflocalstack/
├── controller/
│   └── OAuthController.java          # Extend with authorize + code exchange
├── auth/
│   ├── filter/JwtAuthFilter.java      # Whitelist /services/oauth2/authorize
│   └── service/
│       └── AuthCodeService.java       # NEW: auth code generation, storage, exchange
└── ...
service/src/test/java/co/razkevich/sflocalstack/
├── controller/
│   └── OAuthControllerTest.java       # Extend with authorize + code exchange tests
└── auth/
    └── AuthCodeServiceTest.java       # NEW: unit tests for code lifecycle
```

**Structure Decision**: Extend the existing OAuthController with new authorize endpoints and add a small AuthCodeService for code lifecycle management. This follows the existing pattern where the controller delegates to services.

## Feature Iteration Strategy

### Feature 0: Authorization Endpoint & Login Page

- **Backend Scope**: Add `GET /services/oauth2/authorize` and `POST /services/oauth2/authorize` to OAuthController. Create AuthCodeService for code generation/storage/validation. Whitelist authorize path in JwtAuthFilter.
- **Frontend Scope**: Server-rendered HTML login page returned as a String from the controller. SLDS-inspired inline styling. Form with username, password, hidden OAuth params, submit button, error display.
- **Tests First**: (1) GET /authorize returns 200 with HTML containing form fields. (2) POST /authorize with valid creds returns 302 redirect to redirect_uri with code and state. (3) POST /authorize with invalid creds returns 200 with error message HTML. (4) GET /authorize without response_type=code returns 400.
- **Integration Verification**: Manually open authorize URL in browser, submit login, verify redirect.
- **Parity Verification**: Compare authorize URL parameters with what SF CLI sends to login.salesforce.com.

### Feature 1: Authorization Code Token Exchange

- **Backend Scope**: Extend OAuthController.token() to handle `grant_type=authorization_code`. Consume code via AuthCodeService, verify redirect_uri match, issue JWT.
- **Frontend Scope**: None.
- **Tests First**: (1) Token exchange with valid code returns 200 with Salesforce-compatible response. (2) Replayed code returns 400 invalid_grant. (3) Expired code returns 400 invalid_grant. (4) Mismatched redirect_uri returns 400. (5) Missing code returns 400.
- **Integration Verification**: Full flow: GET authorize → POST login → receive code → POST token → verify JWT works.
- **Parity Verification**: `sf org login web --instance-url http://localhost:8080` completes successfully.

### Feature 2: End-to-End SF CLI Integration

- **Backend Scope**: None (verification only).
- **Frontend Scope**: None.
- **Tests First**: Integration test that simulates the full OAuth flow via MockMvc.
- **Integration Verification**: `sf org login web` → browser login → `sf org display` → `sf data query`.
- **Parity Verification**: Side-by-side with real Salesforce org login flow.

## Salesforce Parity Verification

- **Reference Org**: `dev20`
- **Parity Method**: Compare the authorize URL parameters and token exchange response shape between sf_localstack and real Salesforce. Use `sf org login web` against both.
- **Compared Signals**: (1) Authorize URL query parameters (response_type, client_id, redirect_uri, state). (2) Redirect URL format (code, state params). (3) Token exchange response fields (access_token, instance_url, id, token_type, issued_at, signature).
- **Mutation Policy**: No mutations needed for OAuth parity verification.
- **Accepted Deltas**: (1) `id` URL points to sf_localstack instance, not login.salesforce.com. (2) No PKCE support. (3) No client_secret validation. (4) No connected app registration.

## Complexity Tracking

No constitution violations. Zero new dependencies. All patterns follow existing codebase conventions.
