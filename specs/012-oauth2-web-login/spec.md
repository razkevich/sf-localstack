# Feature Specification: OAuth2 Authorization Code Flow (Web Login)

**Feature Branch**: `012-oauth2-web-login`
**Created**: 2026-04-12
**Status**: Draft
**Input**: User description: "Implement the OAuth2 authorization code grant flow so that `sf org login web` opens a browser login page, the user enters credentials, and gets redirected back to SF CLI with an auth code that gets exchanged for a real JWT token."

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: OAuth2 (authorization endpoint, token endpoint)
- **Compatibility Target**: Salesforce OAuth2 Web Server Flow as consumed by `sf org login web --instance-url <url>`. Must match the redirect behavior, query parameters, and token response shape that SF CLI expects.
- **In-Scope Operations**:
  - `GET /services/oauth2/authorize` — accept authorization request, show login form
  - `POST /services/oauth2/authorize` — process credentials, redirect with authorization code
  - `POST /services/oauth2/token` with `grant_type=authorization_code` — exchange code for access token
  - Standalone server-rendered login page for the OAuth flow
- **Out-of-Scope Operations**: PKCE (Proof Key for Code Exchange), refresh token rotation on code exchange, connected app registration UI, client_secret validation (all clients treated as trusted for MVP), device flow, JWT bearer flow
- **API Shape Commitments**:
  - `GET /services/oauth2/authorize` MUST accept `response_type=code`, `client_id`, `redirect_uri`, and optional `state` parameters per Salesforce OAuth2 spec
  - Redirect on success MUST use `redirect_uri?code=<auth_code>&state=<state>` format
  - `POST /services/oauth2/token` with `grant_type=authorization_code` MUST return the same Salesforce-compatible token response envelope as the existing password grant (`access_token`, `instance_url`, `id`, `token_type`, `issued_at`, `signature`)
- **Frontend Scope**: Standalone HTML login page served during the authorize flow. Styled to match the existing SLDS-inspired login page. Shows username/password form, submits credentials, and redirects on success.
- **Test Isolation Plan**: Tests create a user, initiate an authorization request, submit credentials, receive a code, exchange it for a token, and verify the token works. Tests use the `test` Spring profile with InMemoryUserStore.
- **Parity Verification Plan**: Run `sf org login web --instance-url <url>` against sf_localstack, complete the browser login, and verify the CLI reports successful authorization with the correct username and org ID. Then run `sf data query` to verify the token works.

## Feature Iterations *(mandatory)*

### Feature 0 - Authorization Endpoint & Login Page (Priority: P1)

Implement the `/services/oauth2/authorize` endpoint that SF CLI calls to start the web login flow. When the CLI runs `sf org login web`, it opens a browser to this URL. The endpoint shows a standalone login page where the user enters their username and password. On successful login, it redirects back to the CLI's callback URL with an authorization code.

**Why this priority**: This is the entry point for the entire web login flow. Without the authorize endpoint and login page, SF CLI cannot initiate the flow at all.

**Independent Test**: Send a GET request to `/services/oauth2/authorize?response_type=code&client_id=SfdcInternalClient&redirect_uri=http://localhost:1717/OauthRedirect&state=abc123`, verify it returns an HTML login page. Submit valid credentials, verify the response is a redirect to `redirect_uri?code=<code>&state=abc123`.

**Acceptance Scenarios**:

1. **Given** the authorize endpoint exists, **When** SF CLI sends `GET /services/oauth2/authorize?response_type=code&client_id=SfdcInternalClient&redirect_uri=http://localhost:1717/OauthRedirect&state=abc123`, **Then** the server returns an HTML page with a username/password form that includes the original query parameters as hidden fields.
2. **Given** a user submits valid credentials on the login page, **When** the form POSTs to `/services/oauth2/authorize`, **Then** the server generates a temporary authorization code, stores it with the associated user and redirect_uri, and redirects to `redirect_uri?code=<auth_code>&state=<state>`.
3. **Given** a user submits invalid credentials, **When** the form POSTs to `/services/oauth2/authorize`, **Then** the login page re-renders with an error message and the original query parameters preserved.
4. **Given** the `response_type` parameter is missing or not `code`, **When** the authorize request arrives, **Then** the server returns a 400 error with `error=unsupported_response_type`.
5. **Given** no users are registered in the system, **When** the authorize endpoint is accessed, **Then** the login page shows a registration form so the first user can create an account during the OAuth flow.

**Frontend Deliverables**:

- Server-rendered HTML login page at the authorize endpoint
- SLDS-inspired styling matching the existing login page appearance
- Username and password fields, submit button, error message area
- Hidden fields preserving `client_id`, `redirect_uri`, `state`, `response_type`

**Parity Check**:

- `sf org login web --instance-url http://localhost:8080` opens a browser to the authorize URL with the expected query parameters

---

### Feature 1 - Authorization Code Token Exchange (Priority: P1)

Extend the existing `/services/oauth2/token` endpoint to accept `grant_type=authorization_code` with a `code` parameter. Exchange the authorization code for a real JWT access token, returning the same Salesforce-compatible response shape as the password grant.

**Why this priority**: Without the code exchange, the browser redirect completes but SF CLI cannot obtain a token, making the flow useless.

**Independent Test**: Generate an authorization code via Feature 0, then POST to `/services/oauth2/token` with `grant_type=authorization_code&code=<code>&redirect_uri=<uri>`, verify the response contains a valid JWT access token in the Salesforce-compatible envelope.

**Acceptance Scenarios**:

1. **Given** a valid authorization code exists, **When** `POST /services/oauth2/token` is called with `grant_type=authorization_code&code=<valid_code>&redirect_uri=<matching_uri>`, **Then** the server returns a Salesforce-compatible token response with a real JWT as `access_token`.
2. **Given** an authorization code has already been used, **When** the same code is submitted again, **Then** the server returns `400` with `error=invalid_grant`.
3. **Given** an authorization code has expired (codes expire after 5 minutes), **When** it is submitted for exchange, **Then** the server returns `400` with `error=invalid_grant`.
4. **Given** a valid code but a mismatched `redirect_uri`, **When** the token request arrives, **Then** the server returns `400` with `error=invalid_grant`.
5. **Given** the `grant_type` is `authorization_code` but no `code` is provided, **When** the token request arrives, **Then** the server returns `400` with `error=invalid_request`.

---

### Feature 2 - End-to-End SF CLI Integration (Priority: P1)

Verify that `sf org login web --instance-url <url>` completes the full flow: opens browser, user logs in, CLI receives the token, and subsequent CLI commands work with the authorized org.

**Why this priority**: This is the acceptance test for the whole feature. Without end-to-end verification, individual pieces may work in isolation but fail when composed.

**Independent Test**: Run `sf org login web --instance-url http://localhost:8080 --alias test-web` in a terminal, complete the browser login, then run `sf org display --target-org test-web` and `sf data query --target-org test-web --query "SELECT Id FROM Account"` to verify the token works.

**Acceptance Scenarios**:

1. **Given** sf_localstack is running with at least one registered user, **When** `sf org login web --instance-url <url> --alias test-web` is executed, **Then** a browser opens to the authorize URL, the user logs in, and the CLI reports `Successfully authorized <username> with org ID 00D000000000001AAA`.
2. **Given** a successful web login, **When** `sf org display --target-org test-web` is called, **Then** it shows the correct username, instance URL, and Connected status.
3. **Given** a successful web login, **When** `sf data query --target-org test-web --query "SELECT Id, Name FROM Account"` is called, **Then** it returns results using the token obtained via the web flow.

**Parity Check**:

- The full `sf org login web` → `sf data query` flow must work identically to how it works against a real Salesforce org, with the only difference being the instance URL.

---

### Edge Cases

- What happens when the authorization code expires before exchange? The server returns `error=invalid_grant` and SF CLI shows an authentication error.
- What happens when the user closes the browser without completing login? SF CLI times out waiting for the callback and shows a timeout error. No server-side cleanup is needed as unused codes expire naturally.
- What happens when `redirect_uri` contains special characters or is URL-encoded? The server must URL-decode and compare redirect URIs consistently.
- What happens when multiple authorization codes are generated concurrently for the same user? Each code is independent with its own expiry. All valid codes can be exchanged.
- What happens when no users exist and someone initiates the web login? The login page offers a registration form so the first user can be created during the OAuth flow.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `GET /services/oauth2/authorize` MUST accept `response_type=code`, `client_id`, `redirect_uri`, and optional `state` parameters and return a server-rendered HTML login form
- **FR-002**: Successful credential submission on the login page MUST redirect to `redirect_uri?code=<auth_code>&state=<state>` with a single-use, time-limited authorization code
- **FR-003**: `POST /services/oauth2/token` with `grant_type=authorization_code` MUST exchange a valid code for a Salesforce-compatible JWT token response identical in shape to the existing password grant response
- **FR-004**: Authorization codes MUST be single-use; a second exchange attempt for the same code MUST return `error=invalid_grant`
- **FR-005**: Authorization codes MUST expire after 5 minutes if not exchanged
- **FR-006**: The `redirect_uri` provided during code exchange MUST match the one provided during authorization; mismatches MUST return `error=invalid_grant`
- **FR-007**: The login page MUST be a standalone HTML page rendered by the server, not the React SPA, to work correctly in the OAuth redirect context
- **FR-008**: Invalid credentials on the login page MUST re-render the page with an error message and preserve all OAuth parameters
- **FR-009**: The authorization endpoint MUST be whitelisted in the auth filter (no JWT required to access it)
- **FR-010**: The existing password grant flow MUST continue to work unchanged

### Key Entities

- **Authorization Code**: A temporary, single-use, time-limited string that maps to a user identity and the original redirect_uri. Generated on successful login, consumed on token exchange. Expires after 5 minutes.

## Assumptions

- SF CLI uses `http://localhost:1717/OauthRedirect` as the default callback URL. The server must accept any `redirect_uri` provided by the client without validation against a registered list (no connected app registry in MVP).
- `client_id` is accepted but not validated against a registry. Any client_id is treated as trusted.
- PKCE is not required for MVP. SF CLI versions that require PKCE will need a future enhancement.
- The HTML login page uses inline styles or a minimal embedded stylesheet that matches the existing SLDS appearance without requiring the React build pipeline.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `sf org login web --instance-url <url>` completes successfully and the CLI reports the correct username and org ID
- **SC-002**: Tokens obtained via the web login flow work identically to tokens from the password grant for all SF CLI commands (query, create, describe, metadata)
- **SC-003**: Authorization codes are enforced as single-use — a replayed code is rejected
- **SC-004**: The login page renders and functions correctly in all major browsers (Chrome, Firefox, Safari)
- **SC-005**: The existing password grant flow (`sf org login access-token`) continues to work without any regressions
- **SC-006**: The full web login flow completes in under 30 seconds (from browser open to CLI confirmation), excluding user credential entry time
