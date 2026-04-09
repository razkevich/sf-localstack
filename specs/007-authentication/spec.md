# Feature Specification: Authentication & User Management

**Feature Branch**: `007-authentication`
**Status**: Draft
**Created**: 2026-04-09

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: OAuth2 (token endpoint), all `/services/*` and `/api/*` surfaces (auth enforcement)
- **Compatibility Target**: sf CLI must work with `sf org login` using `grant_type=password` on `POST /services/oauth2/token`
- **In-Scope Operations**: User registration, login, JWT token issuance and validation, auth filter on all protected endpoints, RBAC enforcement (ADMIN/USER roles), login UI page, file-based user store, extensibility documentation
- **Out-of-Scope Operations**: OAuth2 authorization code flow, external identity providers (Auth0, Keycloak, Cognito), multi-tenant isolation, token introspection endpoint, remember-me / device sessions
- **API Shape Commitments**: `POST /services/oauth2/token` with `grant_type=password` MUST return a Salesforce-compatible token response; auth errors on `/services/*` MUST use Salesforce-compatible error format. All other `/api/auth/*` endpoints use standard JSON.
- **Frontend Scope**: Login page at `/login`, registration page, protected route wrapper, session management with auto-refresh, logout.
- **Test Isolation Plan**: Tests use test-specific users created in per-test setup; the `test` Spring profile bypasses or provides auth headers so existing tests continue to pass without modification.
- **Parity Verification Plan**: `sf org login` must successfully authenticate against sf_localstack using username/password grant. Verify token response shape matches Salesforce OAuth2 token endpoint for fields consumed by sf CLI.

## Feature Iterations *(mandatory)*

### Feature 0 - User Store & JWT Service (Priority: P1)

Define the `UserStore` interface and implement `FileBasedUserStore` backed by `data/users.json`. Implement `JwtService` for token generation and validation.

**Why this priority**: All other iterations depend on a working user store and JWT service. This is the foundation.

**Acceptance Scenarios**:

1. **Given** a user exists in `data/users.json`, **When** credentials are submitted, **Then** `validateCredentials` returns the matching `User` and `JwtService` issues a valid JWT that re-validates successfully.
2. **Given** `data/users.json` is missing at startup, **When** the application starts, **Then** an empty file is created and the first registration call succeeds and assigns the ADMIN role.
3. **Given** two concurrent requests attempt to register the first user simultaneously, **When** both arrive before either completes, **Then** file locking ensures exactly one succeeds as ADMIN and the other receives an appropriate conflict error.

**Key Entities**:

- `User`: `id` (UUID), `username`, `email`, `passwordHash` (bcrypt), `role` (ADMIN | USER), `createdAt`, `lastLoginAt`
- `UserStore` interface: `findByUsername`, `createUser`, `validateCredentials`, `findById`, `listUsers`, `deleteUser`
- `FileBasedUserStore`: reads/writes `data/users.json`; uses file locking for concurrent writes
- `JwtService`: generates access token (1 hr TTL), generates refresh token (7 day TTL), validates token, extracts claims; JWT payload contains `userId`, `username`, `role`, `issuedAt`, `expiration`; signing key configurable via `sf-localstack.jwt.secret` property with a random default in dev

---

### Feature 1 - Auth Endpoints (Priority: P1)

Expose REST endpoints for registration, login, token refresh, user info, user management, and update the existing Salesforce OAuth2 endpoints to use real credentials.

**Why this priority**: The auth API surface must exist before the filter can enforce it and before the frontend can drive flows.

**Acceptance Scenarios**:

1. **Given** no users exist, **When** `POST /api/auth/register` is called with valid credentials, **Then** the user is created as ADMIN and a token pair is returned.
2. **Given** at least one user already exists, **When** `POST /api/auth/register` is called without an admin JWT, **Then** 401 is returned.
3. **Given** valid credentials, **When** `POST /api/auth/login` is called, **Then** `{accessToken, refreshToken, user}` is returned and `lastLoginAt` is updated.
4. **Given** a valid refresh token, **When** `POST /api/auth/refresh` is called, **Then** a new access token is returned without requiring re-login.
5. **Given** a valid access token in the `Authorization` header, **When** `GET /api/auth/me` is called, **Then** the current user's info is returned.
6. **Given** an ADMIN JWT, **When** `GET /api/auth/users` is called, **Then** all users are listed.
7. **Given** an ADMIN JWT, **When** `DELETE /api/auth/users/{id}` is called for a different user, **Then** the user is deleted.
8. **Given** an ADMIN JWT, **When** `DELETE /api/auth/users/{id}` is called with the admin's own id, **Then** 400 is returned with a self-deletion error.
9. **Given** valid username and password, **When** `POST /services/oauth2/token` is called with `grant_type=password`, **Then** a real JWT is returned as `access_token` in a Salesforce-compatible response envelope.
10. **Given** a valid JWT in the `Authorization` header, **When** `GET /services/oauth2/userinfo` is called, **Then** real user info from the JWT is returned.

**Endpoints**:

| Method | Path | Auth Required | Role |
|--------|------|--------------|------|
| `POST` | `/api/auth/register` | No (first user) / Admin JWT (subsequent) | — |
| `POST` | `/api/auth/login` | No | — |
| `POST` | `/api/auth/refresh` | No (refresh token in body) | — |
| `GET` | `/api/auth/me` | Yes | Any |
| `GET` | `/api/auth/users` | Yes | ADMIN |
| `DELETE` | `/api/auth/users/{id}` | Yes | ADMIN |
| `POST` | `/services/oauth2/token` | No | — |
| `GET` | `/services/oauth2/userinfo` | Yes | Any |

---

### Feature 2 - Auth Filter (Priority: P1)

Implement `JwtAuthFilter` extending `OncePerRequestFilter` to enforce JWT authentication on all protected routes and return Salesforce-compatible errors on auth failure.

**Why this priority**: Without the filter, the auth endpoints exist in isolation. The filter is what makes auth real for every caller.

**Acceptance Scenarios**:

1. **Given** no `Authorization` header, **When** a request reaches `GET /services/data/v60.0/sobjects/Account`, **Then** 401 is returned with a Salesforce-compatible error format.
2. **Given** an expired or tampered JWT, **When** a request reaches any protected endpoint, **Then** 401 is returned with a clear error message.
3. **Given** a valid JWT, **When** a request reaches any protected endpoint, **Then** the request proceeds and `SecurityContextHolder` holds the user details.
4. **Given** any request to `POST /services/oauth2/token`, **When** it arrives at the filter, **Then** it passes through without requiring a JWT.
5. **Given** any request to `POST /api/auth/login` or `POST /api/auth/register` (first user), **When** it arrives at the filter, **Then** it passes through without requiring a JWT.

**Filter Rules**:

- All `/services/*` requests require a valid JWT **except** `POST /services/oauth2/token`
- All `/api/*` requests require a valid JWT **except** `POST /api/auth/login`, `POST /api/auth/register`, and `POST /api/auth/refresh`
- Dashboard static assets (`/`, `/login`, `/assets/*`) are public
- Token extraction: `Authorization: Bearer <token>` header

**Error Format** (Salesforce-compatible, used on `/services/*` 401 responses):

```json
[{"message": "Session expired or invalid", "errorCode": "INVALID_SESSION_ID"}]
```

---

### Feature 3 - RBAC Enforcement (Priority: P2)

Enforce role-based access control so that USER-role accounts cannot reach admin-only endpoints.

**Why this priority**: RBAC is necessary before opening auth to multiple users to prevent privilege escalation.

**Acceptance Scenarios**:

1. **Given** a USER-role JWT, **When** `POST /reset` is called, **Then** 403 is returned with a descriptive error.
2. **Given** a USER-role JWT, **When** `GET /api/auth/users` is called, **Then** 403 is returned.
3. **Given** a USER-role JWT, **When** `DELETE /api/auth/users/{id}` is called, **Then** 403 is returned.
4. **Given** an ADMIN-role JWT, **When** any endpoint is called, **Then** access is granted (subject to other constraints).
5. **Given** either role JWT, **When** any `/services/*` endpoint is called, **Then** access is granted.

**Role Matrix**:

| Endpoint | ADMIN | USER |
|----------|-------|------|
| `POST /reset` | Allowed | 403 |
| `GET /api/auth/users` | Allowed | 403 |
| `DELETE /api/auth/users/{id}` | Allowed | 403 |
| All `/services/*` | Allowed | Allowed |
| `GET /api/auth/me` | Allowed | Allowed |

---

### Feature 4 - Login UI (Priority: P2)

Provide a login page at `/login`, a registration page, protected route wrappers, session management with auto-refresh, and logout.

**Why this priority**: The UI makes auth accessible to humans using the dashboard directly rather than only via API clients.

**Acceptance Scenarios**:

1. **Given** the app loads with no token in localStorage, **When** the user navigates to any dashboard route, **Then** they are redirected to `/login`.
2. **Given** the user submits valid credentials on the login page, **When** the response returns, **Then** tokens are stored in localStorage and the user is redirected to the dashboard.
3. **Given** the user submits invalid credentials, **When** the response returns, **Then** a clear error message is shown without redirecting.
4. **Given** the access token is within 5 minutes of expiry, **When** the next API call is made, **Then** the token is auto-refreshed using the refresh token before the request proceeds.
5. **Given** the refresh token has expired, **When** a refresh attempt is made, **Then** the user is redirected to `/login`.
6. **Given** the user clicks logout, **When** the action completes, **Then** tokens are cleared from localStorage and the user is redirected to `/login`.
7. **Given** no users exist in the system, **When** the registration page is loaded, **Then** registration is available without requiring an admin token.
8. **Given** users already exist, **When** an ADMIN visits the registration page, **Then** they can register additional users using their admin token.

**Frontend Deliverables**:

- `/login` — login form with username, password, submit; SLDS-inspired styling
- `/register` — registration form, only accessible when no users exist or when visited by admin
- Protected route wrapper: redirects unauthenticated users to `/login`
- Token storage in `localStorage` (`sf_localstack_access_token`, `sf_localstack_refresh_token`)
- Auto-refresh logic: check TTL before each API call, refresh if within 5-minute window
- Logout: clear tokens, redirect

---

### Feature 5 - Extensibility Documentation (Priority: P3)

Document the `UserStore` interface and provide code skeletons for three production upgrade paths: `JpaUserStore`, Auth0, and Keycloak/Cognito.

**Why this priority**: Documentation enables future maintainers to graduate beyond the file-based MVP without touching the auth filter or JWT service.

**Acceptance Scenarios**:

1. **Given** a developer reads `docs/extensibility.md`, **When** they follow the `UserStore` implementation guide, **Then** they can implement a new store in under 1 hour with only the code skeleton and the interface javadoc as references.
2. **Given** the extensibility guide, **When** a developer reads the Auth0/Keycloak/Cognito sections, **Then** they understand which classes to replace and what configuration properties to add.

**Deliverables**:

- `docs/extensibility.md` documenting:
  - `UserStore` interface contract with all method signatures and expected behavior
  - Full `JpaUserStore` code skeleton (Spring Data JPA entity + repository + service implementation)
  - Auth0 integration skeleton: `Auth0UserStore` using Auth0 Management API
  - Keycloak / Cognito integration notes: token verification via JWKS URI, user lookup via admin API
  - Configuration properties table for each option

---

### Edge Cases

- What happens when JWT expires mid-request? 401 with clear error message; client must re-authenticate or use refresh token.
- What happens when `data/users.json` is missing at startup? An empty file is created; the first registration call creates the ADMIN user.
- What happens when an admin attempts to delete themselves? `DELETE /api/auth/users/{id}` returns 400 with a self-deletion error message.
- What happens when the refresh token expires? 401 is returned; the user must perform a full re-login.
- What happens when two requests race to register the first user? `FileBasedUserStore` uses file locking; exactly one succeeds as ADMIN, the other receives a conflict error.
- What happens when `sf-localstack.jwt.secret` is not set? A random 256-bit secret is generated at startup and logged once at WARN level; tokens issued with this secret become invalid on restart.
- What happens when a USER-role token is presented to an admin-only endpoint? 403 with a descriptive JSON error; no user or token data is leaked in the response.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All `/services/*` endpoints MUST require a valid JWT, except `POST /services/oauth2/token`
- **FR-002**: Passwords MUST be hashed with bcrypt; plaintext passwords must never be stored, logged, or transmitted in responses
- **FR-003**: `POST /services/oauth2/token` with `grant_type=password` MUST accept real credentials and return a real JWT as `access_token` in a Salesforce-compatible response envelope
- **FR-004**: The first `POST /api/auth/register` call MUST create an ADMIN user; all subsequent calls MUST require a valid ADMIN JWT
- **FR-005**: `UserStore` MUST be defined as an interface; `FileBasedUserStore` is the MVP implementation backed by `data/users.json`
- **FR-006**: The JWT signing secret MUST be configurable via the `sf-localstack.jwt.secret` application property
- **FR-007**: Auth errors on `/services/*` routes MUST use the Salesforce-compatible error format: `[{"message": "...", "errorCode": "INVALID_SESSION_ID"}]`
- **FR-008**: An admin user MUST NOT be able to delete their own account
- **FR-009**: The `test` Spring profile MUST allow existing tests to pass, either by bypassing auth or by supplying auth headers in test setup
- **FR-010**: `FileBasedUserStore` MUST use file locking to prevent data corruption under concurrent writes

### Key Entities

- **User**: `id` (UUID), `username`, `email`, `passwordHash` (bcrypt), `role` (ADMIN | USER), `createdAt`, `lastLoginAt`
- **TokenPair**: `accessToken` (JWT, 1 hr), `refreshToken` (JWT, 7 days)
- **UserStore**: interface — `findByUsername`, `createUser`, `validateCredentials`, `findById`, `listUsers`, `deleteUser`
- **FileBasedUserStore**: reads/writes `data/users.json`, file-locked writes, bcrypt password comparison
- **JwtService**: token generation, validation, claims extraction; signing key from property or random default

## Assumptions

- The MVP user store is file-based; production teams are expected to swap in a `JpaUserStore` or external provider using the documented extensibility guide.
- The sf CLI `sf org login` command uses `grant_type=password` (Resource Owner Password Credentials grant), which is supported by the existing `/services/oauth2/token` endpoint shape.
- Existing integration tests run under a `test` Spring profile that disables the auth filter or provides a pre-configured test user; no existing test should need to change its assertion logic.
- SLDS-inspired styling for the login UI is a starting point and will be refined in the planned F7 UI overhaul feature.
- JWTs are signed with HMAC-SHA256 using a symmetric secret; asymmetric signing is out of scope for MVP.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `sf org login` with username/password succeeds against sf_localstack without errors
- **SC-002**: Unauthenticated requests to any `/services/*` endpoint (except the token endpoint) return 401 with a Salesforce-compatible error body
- **SC-003**: A USER-role token cannot access `POST /reset`, `GET /api/auth/users`, or `DELETE /api/auth/users/{id}`; those calls return 403
- **SC-004**: Access tokens refresh automatically without requiring re-login, as long as the refresh token is valid
- **SC-005**: User data persists across application restarts via `data/users.json`
- **SC-006**: `docs/extensibility.md` documents three production auth upgrade paths (`JpaUserStore`, Auth0, Keycloak/Cognito) with working code skeletons
