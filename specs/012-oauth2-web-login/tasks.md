# Tasks: OAuth2 Authorization Code Flow (Web Login)

**Input**: Design documents from `/specs/012-oauth2-web-login/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Tests are REQUIRED. Every feature slice must start with failing backend tests before implementation.

**Organization**: Tasks grouped by feature slice. Features 0 and 1 are tightly coupled (code exchange needs authorize), so they share a dependency chain but are testable independently.

## Format: `[ID] [P?] [Feature] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Feature]**: Which feature slice (F0, F1, F2)
- Exact file paths included

## Phase 1: Shared Scaffold

**Purpose**: Create the AuthCodeService and whitelist the authorize endpoint before feature work begins.

- [x] T001 [P] Create AuthCodeService with code generation, storage, validation, and expiry in `service/src/main/java/co/razkevich/sflocalstack/auth/service/AuthCodeService.java`
- [x] T002 [P] Whitelist `/services/oauth2/authorize` in JwtAuthFilter skip rules in `service/src/main/java/co/razkevich/sflocalstack/auth/filter/JwtAuthFilter.java`

**Checkpoint**: AuthCodeService exists and authorize path is accessible without JWT.

---

## Feature 0: Authorization Endpoint & Login Page (Priority: P1) 🎯 MVP

**Goal**: `GET /services/oauth2/authorize` returns an HTML login page; `POST /services/oauth2/authorize` validates credentials and redirects with an authorization code.

**Independent Test**: GET the authorize URL, verify HTML form is returned. POST with valid creds, verify 302 redirect with code param. POST with invalid creds, verify error HTML re-rendered.

### Backend Tests First ⚠️

- [x] T003 [P] [F0] Add test: GET /services/oauth2/authorize with valid params returns 200 HTML with form fields in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthAuthorizeTest.java`
- [x] T004 [P] [F0] Add test: GET /services/oauth2/authorize without response_type=code returns 400 in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthAuthorizeTest.java`
- [x] T005 [P] [F0] Add test: POST /services/oauth2/authorize with valid credentials redirects 302 to redirect_uri with code and state params in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthAuthorizeTest.java`
- [x] T006 [P] [F0] Add test: POST /services/oauth2/authorize with invalid credentials returns 200 HTML with error message in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthAuthorizeTest.java`

### Backend Implementation

- [x] T007 [F0] Implement GET /services/oauth2/authorize in OAuthController — accept OAuth params, render inline HTML login form with hidden fields in `service/src/main/java/co/razkevich/sflocalstack/controller/OAuthController.java`
- [x] T008 [F0] Implement POST /services/oauth2/authorize in OAuthController — validate credentials via UserStore, generate auth code via AuthCodeService, redirect to redirect_uri with code and state in `service/src/main/java/co/razkevich/sflocalstack/controller/OAuthController.java`

### Verification

- [x] T009 [F0] Manually open authorize URL in browser, verify login page renders with SLDS-inspired styling, submit credentials, verify redirect occurs

**Checkpoint**: Authorization endpoint works — login page renders, valid credentials redirect with code, invalid credentials show error.

---

## Feature 1: Authorization Code Token Exchange (Priority: P1)

**Goal**: `POST /services/oauth2/token` with `grant_type=authorization_code` exchanges a code for a Salesforce-compatible JWT token response.

**Independent Test**: Generate auth code via Feature 0 flow, POST to token endpoint with code, verify valid JWT returned. Replay same code, verify rejection. Use expired code, verify rejection.

### Backend Tests First ⚠️

- [x] T010 [P] [F1] Add test: AuthCodeService unit tests — code generation, single-use, expiry, redirect_uri match in `service/src/test/java/co/razkevich/sflocalstack/auth/AuthCodeServiceTest.java`
- [x] T011 [P] [F1] Add test: POST /services/oauth2/token with grant_type=authorization_code and valid code returns 200 with Salesforce-compatible token response in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthAuthorizeTest.java`
- [x] T012 [P] [F1] Add test: POST /services/oauth2/token with replayed code returns 400 invalid_grant in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthAuthorizeTest.java`
- [x] T013 [P] [F1] Add test: POST /services/oauth2/token with mismatched redirect_uri returns 400 invalid_grant in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthAuthorizeTest.java`
- [x] T014 [P] [F1] Add test: POST /services/oauth2/token with missing code returns 400 invalid_request in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthAuthorizeTest.java`

### Backend Implementation

- [x] T015 [F1] Extend OAuthController.token() to handle grant_type=authorization_code — consume code via AuthCodeService, verify redirect_uri, look up user, generate JWT, return Salesforce-compatible response in `service/src/main/java/co/razkevich/sflocalstack/controller/OAuthController.java`

### Verification

- [x] T016 [F1] Full integration test via MockMvc: GET authorize → POST login → extract code → POST token → verify JWT is valid and contains correct user claims in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthAuthorizeTest.java`
- [x] T017 [F1] Verify existing password grant tests still pass — no regressions in `service/src/test/java/co/razkevich/sflocalstack/controller/OAuthControllerTest.java`

**Checkpoint**: Code exchange works — valid codes produce JWTs, replayed/expired/mismatched codes are rejected, password grant still works.

---

## Feature 2: End-to-End SF CLI Integration (Priority: P1)

**Goal**: `sf org login web --instance-url <url>` completes the full OAuth flow and the resulting token works for all CLI commands.

**Independent Test**: Run `sf org login web`, complete browser login, verify CLI reports success. Then run `sf data query` and `sf org display` to verify the token works.

### Verification

- [x] T018 [F2] Build and deploy updated JAR to production droplet at 164.92.219.185
- [x] T019 [F2] Run `sf org login web --instance-url http://164.92.219.185 --alias sf-web`, complete browser login, verify CLI reports `Successfully authorized <username>`
- [x] T020 [F2] Run `sf org display --target-org sf-web` and verify Connected status, correct username, and instance URL
- [x] T021 [F2] Run `sf data query --target-org sf-web --query "SELECT Id, Name FROM Account"` and verify results returned
- [x] T022 [F2] Run `sf data create record --target-org sf-web --sobject Account --values "Name='WebLogin Test'"` and verify record created
- [x] T023 [F2] Verify existing `sf org login access-token` flow still works (no regression)

**Checkpoint**: Full SF CLI web login flow works end-to-end against production deployment.

---

## Final Polish

**Purpose**: Cross-cutting improvements after all features pass.

- [x] T024 [P] Run full test suite (`mvn -pl service test`) and fix any failures
- [x] T025 [P] Update SF_CLI_USAGE.md with web login instructions in `/Users/razkevich/code/sf_localstack/SF_CLI_USAGE.md`
- [x] T026 Update CLAUDE.md with OAuth2 authorization code flow documentation in `/Users/razkevich/code/sf_localstack/CLAUDE.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Scaffold)**: No dependencies — T001, T002 can start immediately and run in parallel
- **Feature 0**: Depends on Phase 1 (T001, T002)
- **Feature 1**: Depends on Feature 0 (needs authorize endpoint to generate codes for testing)
- **Feature 2**: Depends on Feature 1 (needs full flow working)
- **Final Polish**: Depends on all features complete

### Within Each Feature Slice

- Backend tests written and FAIL before backend implementation
- Backend implementation makes tests pass
- Integration verification after implementation
- Parity verification (Feature 2) after local verification

### Parallel Opportunities

- T001 and T002 run in parallel (different files)
- T003–T006 (F0 tests) run in parallel with each other
- T010–T014 (F1 tests) run in parallel with each other
- T024–T025 (polish) run in parallel

---

## Implementation Strategy

### MVP First

1. T001 + T002 (scaffold) — 10 min
2. T003–T009 (Feature 0: authorize endpoint) — 30 min
3. T010–T017 (Feature 1: code exchange) — 30 min
4. T018–T023 (Feature 2: SF CLI verification) — 15 min
5. T024–T026 (polish) — 10 min

### Incremental Delivery

All three features are P1 and form a single delivery unit. The flow doesn't work without all three, but they're structured for incremental testing: authorize works standalone, code exchange needs authorize, SF CLI needs both.
