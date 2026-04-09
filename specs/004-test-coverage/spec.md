# Feature Specification: Test Coverage Hardening

**Feature Branch**: `004-test-coverage`
**Status**: Draft
**Created**: 2026-04-09

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: REST, Bulk, Metadata (all existing surfaces)
- **Compatibility Target**: All currently supported endpoints
- **In-Scope Operations**: Adding test coverage for all existing controller endpoints and service methods
- **Out-of-Scope Operations**: New functionality, API changes, refactoring
- **Test Isolation Plan**: Each test uses `@Transactional` rollback or calls `POST /reset` between tests. H2 in-memory for test profile.
- **Parity Verification Plan**: Not applicable for this feature (test-only)

## Feature Iterations *(mandatory)*

### Feature 0 - Controller Integration Test Coverage (Priority: P1)

Write `@SpringBootTest` + MockMvc integration tests for every endpoint across all controllers.

**Why this priority**: Controllers are the outermost integration surface. Without endpoint coverage, any refactoring or behavioral regression goes undetected.

**Acceptance Scenarios**:

1. **Given** a running app, **When** all integration tests run, **Then** 100% of endpoints have coverage.

**Controllers covered**:
- `SObjectController`: GET/POST/PATCH/PUT/DELETE + upsert
- `QueryController`: SOQL execution, error cases
- `BulkController`: full job lifecycle — create → upload → close → status → results → delete
- `MetadataController`: all 8 SOAP operations
- `MetadataRestController`: tooling queries, describes
- `OAuthController`: token, userinfo
- `VersionController`: version discovery, resource listing
- `DashboardController`: overview, request log, SSE
- `ResetController`
- `MetadataAdminController`

**Coverage standard**: Each endpoint gets at least 1 happy-path test and 1 error-path test (bad input, missing resource, etc.).

---

### Feature 1 - Service Unit Test Coverage (Priority: P1)

Unit tests for every service class, exercising all public methods with mocked dependencies.

**Why this priority**: Services contain the core business logic. Unit tests isolate and document that logic independently of HTTP wiring.

**Acceptance Scenarios**:

1. **Given** mocked dependencies, **When** all unit tests run, **Then** every public method has coverage.

**Services covered**:
- `SoqlEngine`: parse various SOQL patterns, execute with mock data, error on invalid SOQL
- `BulkJobService`: state machine transitions, CSV parsing, result generation
- `MetadataService`: describe, list, read, deploy, retrieve, status checks
- `OrgStateService`: create with ID generation, update, upsert, delete, findByType, reset
- `MetadataSoapParser`: parse various SOAP XML envelopes
- `MetadataSoapRenderer`: render each operation response
- `MetadataZipService`: ZIP generation/extraction
- `MetadataToolingService`: tooling query execution
- `RequestLogService`: logging, retrieval, SSE emitter management

---

### Feature 2 - Cross-Surface Integration Tests (Priority: P2)

Tests that exercise multiple API surfaces in sequence to verify end-to-end workflows.

**Why this priority**: Individual surface tests do not catch interactions between REST, Bulk, Metadata, and SOQL. Cross-surface tests provide that confidence.

**Acceptance Scenarios**:

1. **Given** a running app, **When** cross-surface tests run, **Then** all multi-surface workflows pass.

**Workflows covered**:
- Create records via REST → Query via SOQL → Verify results
- Create via REST → Bulk upsert → Query to verify
- Metadata deploy → list → read → retrieve cycle
- Full lifecycle: REST create → Bulk update → SOQL query → Metadata describe → Reset → Verify clean

---

### Feature 3 - Test Infrastructure (Priority: P2)

Create shared test helper utilities to reduce boilerplate across the test suite.

**Why this priority**: Without shared helpers, test code becomes repetitive and harder to maintain. Infrastructure built here accelerates all future test writing.

**Acceptance Scenarios**:

1. **Given** test helpers exist, **When** writing new tests, **Then** boilerplate is minimal.

**Helpers to create**:
- `TestDataFactory`: create test records with sensible defaults
- `SoapTestHelper`: build SOAP envelopes in tests
- `AssertionHelpers`: common Salesforce response shape assertions (e.g., `assertSalesforceError`, `assertCreatedResponse`)

---

### Edge Cases

- What happens when tests run in parallel? Tests must not share state.
- What happens when SOQL parsing fails? Tests verify specific error messages.
- What happens when Bulk job state transitions are invalid? Tests verify rejection.
- What happens when SOAP XML is malformed? Tests verify fault response.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST have integration test coverage for 100% of public API endpoints
- **FR-002**: System MUST have unit test coverage for all service public methods
- **FR-003**: Tests MUST be independent — no test depends on state from another test
- **FR-004**: Tests MUST run in under 60 seconds total on CI
- **FR-005**: Test helpers MUST reduce boilerplate for common assertion patterns

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every REST, Bulk, Metadata, OAuth, and Dashboard endpoint has at least 2 integration tests
- **SC-002**: Every service public method has at least 1 unit test
- **SC-003**: `mvn -pl service test` passes with 0 failures
- **SC-004**: Cross-surface tests validate 4+ multi-API workflows
- **SC-005**: All tests complete in under 60 seconds
