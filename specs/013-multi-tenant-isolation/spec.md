# Feature Specification: Multi-Tenant Org Isolation

**Feature Branch**: `013-multi-tenant-isolation`
**Created**: 2026-04-12
**Status**: Draft
**Input**: User description: "Each registered user gets their own isolated Salesforce org with unique org ID. Data, metadata, and bulk jobs in one org must be invisible to other orgs. OAuth token carries org identity. Each org gets seeded data on creation. Backward compatible with existing single-org behavior. Critical for SaaS MVP."

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: REST, Bulk, Metadata, OAuth2 — all surfaces affected by tenant isolation
- **Compatibility Target**: Real Salesforce multi-org behavior — each org is fully isolated. Users in one org cannot see, query, or modify data in another org. The org ID in the token determines which data partition serves the request.
- **In-Scope Operations**:
  - Add `orgId` to all persistent entities (sObjects, bulk jobs, metadata resources, deploy/retrieve jobs)
  - Auto-create an org when a user registers (one org per user for MVP)
  - Embed `orgId` in JWT tokens and OAuth responses
  - Filter all queries and mutations by the authenticated user's org
  - Seed each new org with default data on creation
  - Generate unique Salesforce-format org IDs (18-character, `00D` prefix)
- **Out-of-Scope Operations**: Sharing data between orgs, org-to-org migration, multi-user per org (MVP: 1 user = 1 org), org deletion, org admin transfer, cross-org queries
- **API Shape Commitments**: All existing API response shapes remain unchanged. The only visible difference is that each user sees only their org's data, and the `organization_id` / org ID values reflect their specific org instead of the hardcoded `00D000000000001AAA`.
- **Frontend Scope**: The web dashboard shows only the logged-in user's org data. The org ID is displayed in the Setup view.
- **Test Isolation Plan**: Integration tests create two users, each with their own org. Tests verify that CRUD operations in one org are invisible from the other. Each test class resets state via the test profile.
- **Parity Verification Plan**: Create two users, log in with SF CLI as each, create records in each org, verify cross-org invisibility. Compare with the behavior of two separate Salesforce orgs.

## Feature Iterations *(mandatory)*

### Feature 0 - Org Entity & Auto-Provisioning (Priority: P1)

When a user registers, the system automatically creates a new isolated org for them with a unique Salesforce-format org ID. The org ID is stored in the user record and embedded in all JWT tokens and OAuth responses. The hardcoded `00D000000000001AAA` is replaced with the user's actual org ID.

**Why this priority**: The org entity is the foundation. Without org IDs on users and tokens, no isolation is possible.

**Independent Test**: Register two users, verify each gets a different org ID in their JWT claims and OAuth responses. Verify `sf org display` shows different org IDs for each alias.

**Acceptance Scenarios**:

1. **Given** a new user registers, **When** registration completes, **Then** a new org with a unique `00D`-prefixed 18-character ID is created and associated with the user.
2. **Given** a user logs in via password grant or authorization code, **When** the token is issued, **Then** the JWT contains the user's org ID and the OAuth response `id` field reflects it.
3. **Given** the first user who ever registered (existing ADMIN), **When** the system starts with existing data, **Then** they are assigned the legacy org ID `00D000000000001AAA` for backward compatibility.
4. **Given** two registered users, **When** each calls `sf org display`, **Then** they see different org IDs.

---

### Feature 1 - Data Isolation (sObjects) (Priority: P1)

All sObject records are tagged with the org ID of the user who created them. All queries and mutations filter by the authenticated user's org ID. Users can only see, create, update, and delete records belonging to their own org. Each new org is seeded with default data.

**Why this priority**: sObject data isolation is the core tenant isolation requirement. Without it, the SaaS product cannot serve multiple users.

**Independent Test**: User A creates an Account. User B queries Accounts. User B's results must not include User A's Account. User B creates their own Account. User A's query still returns only their records.

**Acceptance Scenarios**:

1. **Given** User A creates an Account "Alpha Corp", **When** User B queries `SELECT Id, Name FROM Account`, **Then** "Alpha Corp" does not appear in User B's results.
2. **Given** User B creates an Account "Beta Corp", **When** User A queries `SELECT Id, Name FROM Account`, **Then** "Beta Corp" does not appear in User A's results.
3. **Given** User A has 3 Accounts, **When** User A calls describe on Account, **Then** the response is scoped to their org.
4. **Given** User A creates a record and gets an ID back, **When** User B tries to GET that record by ID, **Then** they receive a 404 Not Found.
5. **Given** a new user registers, **When** they query Accounts for the first time, **Then** they see the default seed data (not data from other orgs).

---

### Feature 2 - Bulk Job & Metadata Isolation (Priority: P1)

Bulk ingest jobs and metadata resources (components, deploy jobs, retrieve jobs) are tagged with org ID and filtered in all operations. Users can only see their own org's bulk jobs and metadata.

**Why this priority**: Bulk and metadata are the other two major stateful surfaces. Without isolation here, data leaks across orgs via these channels.

**Independent Test**: User A creates a bulk ingest job. User B lists bulk jobs — User A's job must not appear. User A deploys metadata. User B lists metadata — User A's metadata must not appear.

**Acceptance Scenarios**:

1. **Given** User A creates a bulk ingest job for Account, **When** User B lists bulk jobs, **Then** User A's job does not appear.
2. **Given** User A deploys a CustomObject metadata component, **When** User B calls describeMetadata or listMetadata, **Then** User A's component does not appear.
3. **Given** User A creates a metadata deploy job, **When** User B lists deploy jobs, **Then** User A's job does not appear.

---

### Feature 3 - Integration Test Suite (Priority: P1)

Implement a comprehensive integration test suite that exercises the full SF CLI workflow programmatically via MockMvc. Tests must cover: user registration, OAuth login (both password grant and authorization code), sObject CRUD, SOQL queries, bulk jobs, metadata operations, and tenant isolation verification. These tests run in CI without external dependencies.

**Why this priority**: The user requires that all manual SF CLI testing performed during development is codified as automated integration tests that run in CI. This ensures regressions are caught automatically.

**Independent Test**: Run `mvn -pl service test` — all integration tests pass, including multi-tenant isolation assertions.

**Acceptance Scenarios**:

1. **Given** the test suite runs in CI, **When** `mvn -pl service test` executes, **Then** all tenant isolation tests pass without external dependencies (no real SF org, no running server, no browser).
2. **Given** two test users in different orgs, **When** User A creates records via REST API, **Then** integration tests verify User B cannot see those records.
3. **Given** the full OAuth flow (authorize → code → token), **When** tested via MockMvc, **Then** the resulting token works for authenticated API calls.
4. **Given** bulk job creation and metadata deploy, **When** tested via MockMvc with two users, **Then** cross-org isolation is verified.

---

### Edge Cases

- What happens when an existing single-user deployment upgrades? The first/existing user gets the legacy org ID `00D000000000001AAA`, and all existing data is associated with that org. No data loss.
- What happens when a user's token doesn't contain an org ID (legacy token)? The system falls back to the legacy org ID for backward compatibility.
- What happens when a user tries to access a record ID that exists in another org? 404 Not Found — the same behavior as if the record didn't exist.
- What happens when the org reset endpoint is called? It resets only the authenticated user's org data, not all orgs.
- What happens when request logs are queried? Request logs are filtered by org ID so each user sees only their own API traffic.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each user registration MUST create a new org with a unique Salesforce-format org ID (`00D` prefix, 18 characters)
- **FR-002**: All JWT tokens MUST include the user's `orgId` claim
- **FR-003**: All OAuth token responses MUST reflect the user's actual org ID in the `id` field and identity responses
- **FR-004**: All sObject queries and mutations MUST filter by the authenticated user's org ID
- **FR-005**: All bulk job operations (create, list, get, close) MUST filter by org ID
- **FR-006**: All metadata operations (describe, list, deploy, retrieve) MUST filter by org ID
- **FR-007**: The org reset endpoint MUST reset only the authenticated user's org, not all orgs
- **FR-008**: New orgs MUST be seeded with the default seed data on creation
- **FR-009**: Existing data from before this feature MUST be migrated to the first user's org (backward compatible)
- **FR-010**: An automated integration test suite MUST verify tenant isolation across all surfaces (REST, Bulk, Metadata) and run in CI without external dependencies

### Key Entities

- **Org**: Represents an isolated Salesforce org. Has a unique org ID (`00D`-prefixed, 18 chars), an owner user ID, and a creation timestamp. Each user owns exactly one org (MVP).
- **SObjectRecord** (modified): Gains an `orgId` field. All queries filter by `orgId`.
- **BulkIngestJob** (modified): Gains an `orgId` field.
- **MetadataResourceEntity** (modified): Gains an `orgId` field.
- **MetadataDeployJobEntity** (modified): Gains an `orgId` field.
- **MetadataRetrieveJobEntity** (modified): Gains an `orgId` field.

## Assumptions

- MVP: one user = one org. Multi-user orgs (teams/sharing) is a future feature.
- The org ID is generated at registration time and never changes.
- The first registered user (existing ADMIN) is assigned the legacy org ID `00D000000000001AAA` to preserve backward compatibility with existing data.
- Request logs in the in-memory buffer are tagged with org ID for filtering but are not persisted across restarts.
- The `User` model gains an `orgId` field. The `FileBasedUserStore` and `InMemoryUserStore` both support it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Two users logged in via SF CLI see completely different data sets — zero cross-org record visibility
- **SC-002**: Creating, updating, or deleting a record in Org A produces zero side effects visible in Org B
- **SC-003**: Bulk jobs created in Org A are invisible when listing jobs from Org B
- **SC-004**: Metadata deployed in Org A is invisible when listing metadata from Org B
- **SC-005**: The integration test suite passes in CI with 100% of tenant isolation assertions green
- **SC-006**: Existing single-user deployments continue to work without data loss after upgrade
- **SC-007**: New user registration and org provisioning completes in under 2 seconds including seed data
