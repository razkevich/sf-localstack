# Feature Specification: Persistent Storage for sf-localstack

**Feature Branch**: `005-persistent-storage`
**Created**: 2026-04-09
**Status**: Draft
**Input**: Migrate all in-memory state (H2 mem, ConcurrentHashMap for Bulk jobs and Metadata jobs) to persistent storage backed by H2 file-based database so that sObject records, Bulk job state, and Metadata resource state survive application restarts — enabling SaaS deployment without data loss.

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: REST, Bulk API v2, and Metadata API (all — storage underlies every surface)
- **Compatibility Target**: Existing API behavior must remain identical to the in-memory implementation. Clients that already target this emulator must not require any changes.
- **In-Scope Operations**: H2 file-based persistence for sObject records; JPA entities for Bulk ingest jobs, batches, and row results; JPA entities for Metadata resources and deploy/retrieve jobs; Spring profile configuration (`test`, `dev`, `prod`); `POST /reset` updated to truncate tables; extensibility documentation for PostgreSQL migration.
- **Out-of-Scope Operations**: PostgreSQL implementation (documented configuration path only); multi-tenant schema isolation; schema versioning tooling (Flyway/Liquibase); Bulk query job persistence; any change to existing API paths, request shapes, or response shapes.
- **API Shape Commitments**: All in-scope operations must keep Salesforce-compatible paths, status codes, request field names, and response envelopes unchanged. Storage is a pure internal implementation detail — no observable behavioral change is permitted except that state now survives restarts and reset truncates rather than clears in-memory maps.
- **Frontend Scope**: No frontend changes required. The dashboard already reads state through existing API endpoints, which remain unchanged.
- **Test Isolation Plan**: The `test` Spring profile uses `jdbc:h2:mem:sfdb` with `ddl-auto: create-drop` so each test run starts from a clean schema. Tests must not reference the `dev` or `prod` profile data directory. Existing tests must pass without modification after this feature.
- **Runtime Reproducibility Controls**: The `dev` profile uses `jdbc:h2:file:./data/sfdb` with `ddl-auto: update`. The `./data/` directory is gitignored. Developers can delete `./data/` to return to a clean-slate state. The `prod` profile is a placeholder with PostgreSQL config stubs.
- **Parity Verification Plan**: No new Salesforce parity checks are required. Storage is internal; the existing parity baseline remains valid. The reset endpoint behavior is verified locally by creating records, restarting the app, and asserting records are queryable.

## Feature Iterations *(mandatory)*

### Feature 0 - H2 File-Based Persistence for sObjects (Priority: P1)

Switch the H2 datasource from in-memory to file-backed and introduce Spring profiles so tests continue to use in-memory H2 while the dev runtime persists sObject records across restarts.

**Why this priority**: This is the smallest change that delivers observable persistence. All other iterations build on the profile infrastructure established here.

**Independent Test**: Start the app on the `dev` profile, create sObject records via REST, stop the app, restart the app, query the same records — they must be present.

**Acceptance Scenarios**:

1. **Given** sObject records created via REST on the `dev` profile, **When** the application is stopped and restarted, **Then** all records are still queryable through the REST API without any re-seeding.
2. **Given** the `test` profile is active, **When** tests run, **Then** H2 uses an in-memory database with `create-drop` so each test starts with a clean schema and tests are isolated from each other.
3. **Given** the `./data/` directory does not exist, **When** the application starts on the `dev` profile, **Then** the directory is created automatically and the app starts successfully.

**Parity Check**:

- No Salesforce parity check required. Verify locally that REST GET after restart returns records created before restart.

---

### Feature 1 - JPA Entities for Bulk Jobs (Priority: P1)

Replace the `ConcurrentHashMap` backing Bulk ingest job state with JPA entities persisted to H2, so that Bulk job status, batch data, and row results survive application restarts.

**Why this priority**: Bulk job state is the largest in-memory structure and is critical for clients that poll job status after a service interruption.

**Independent Test**: Create a Bulk ingest job, upload rows, close the job, stop the app, restart the app, retrieve job status and results — all state must be present and correct.

**Acceptance Scenarios**:

1. **Given** a Bulk ingest job that has been created and completed, **When** the application restarts, **Then** the job status, processed counts, and row-level results are retrievable via the Bulk API status and result endpoints.
2. **Given** a Bulk ingest job in the `Open` state (upload not yet complete), **When** the application restarts, **Then** the job remains in the `Open` state and the client can continue uploading or aborting it.
3. **Given** the `test` profile is active, **When** Bulk job tests run, **Then** each test starts with an empty Bulk jobs table and no state leaks between tests.

**Parity Check**:

- No Salesforce parity check required. Verify locally that job status endpoint returns correct state after restart.

---

### Feature 2 - JPA Entities for Metadata State (Priority: P1)

Replace the in-memory maps backing Metadata resources and deploy/retrieve jobs with JPA entities persisted to H2, so that Metadata state survives application restarts.

**Why this priority**: Metadata deploy and retrieve state must survive restarts to support long-running CI workflows where the emulator is not restarted between individual tool invocations.

**Independent Test**: Create metadata resources and trigger a deploy job, stop the app, restart the app, check deploy status and list metadata resources — all state must be present and correct.

**Acceptance Scenarios**:

1. **Given** metadata resources and deploy/retrieve jobs exist, **When** the application restarts, **Then** all metadata resources and job states are retrievable through the Metadata API.
2. **Given** a deploy job in a terminal state (`Succeeded` or `Failed`), **When** the application restarts, **Then** the deploy status endpoint returns the same terminal state as before restart.
3. **Given** the `test` profile is active, **When** Metadata tests run, **Then** each test starts with clean Metadata tables and no state leaks between tests.

**Parity Check**:

- No Salesforce parity check required. Verify locally that deploy status is retrievable after restart.

---

### Feature 3 - Reset Behavior Update (Priority: P2)

Update `POST /reset` to truncate all persistent tables instead of clearing in-memory maps, so reset is fast, atomic, and leaves the application in a clean functional state.

**Why this priority**: Reset is a core test primitive. It must work correctly after persistence is introduced or tests will accumulate stale state across runs.

**Independent Test**: Populate state across REST, Bulk, and Metadata surfaces, call `POST /reset`, verify all tables are empty, verify the app continues to respond correctly to new requests.

**Acceptance Scenarios**:

1. **Given** sObject records, Bulk jobs, and Metadata resources exist, **When** `POST /reset` is called, **Then** all tables are truncated and subsequent queries return empty results.
2. **Given** reset is called during active requests, **When** reset completes, **Then** the application remains in a consistent, functional state (reset is atomic).
3. **Given** reset completes, **When** the app receives new REST, Bulk, or Metadata requests, **Then** the app handles them correctly as if starting from a clean state.

**Parity Check**:

- No Salesforce parity check required. Verify that reset leaves all API surfaces functional.

---

### Feature 4 - Extensibility Documentation (Priority: P2)

Write `docs/extensibility.md` covering the storage layer architecture, the MVP H2 implementation, and the configuration-only path to swap in PostgreSQL for production deployments.

**Why this priority**: Documenting the PostgreSQL migration path now prevents future engineers from needing to understand the entire storage layer before making a database swap.

**Independent Test**: Follow the documented PostgreSQL migration steps in a local environment and confirm the application starts with PostgreSQL without code changes.

**Acceptance Scenarios**:

1. **Given** an engineer wants to run the emulator against PostgreSQL, **When** they follow `docs/extensibility.md`, **Then** they can do so by adding the PostgreSQL driver dependency and providing an `application-prod.yml` with no code changes required.
2. **Given** the documentation is complete, **When** an engineer reads it, **Then** they understand the interface names, the MVP implementation class, and how to extend or replace the storage backend.

**Parity Check**:

- No Salesforce parity check required.

---

### Edge Cases

- What happens when the `./data/` directory does not exist? The application must create it on startup before the datasource initializes.
- What happens when the H2 file is corrupted? Document the recovery procedure: delete `./data/sfdb.*` files and restart — the app recreates the schema with `update` ddl-auto. Data loss is expected and acceptable in `dev` mode; `prod` requires a PostgreSQL backup strategy.
- What happens to in-flight Bulk jobs (state `Open`) during restart? Jobs in `Open` state remain in `Open` state after restart. Clients may continue uploading or abort the job. No automatic state promotion occurs on restart.
- What happens when reset is called during active requests? Reset must acquire a lock or use a database transaction to truncate tables atomically. Requests that arrive during reset receive either a completed response (if processed before truncation) or an empty-result response (if processed after truncation). Partial mutations within a single reset transaction are not permitted.
- What happens when `ddl-auto: update` cannot apply a schema change (e.g., incompatible column type change during development)? Document the resolution: delete `./data/` and restart with a clean schema. This is acceptable in `dev` mode; production schema migrations require a proper migration tool.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All sObject records MUST persist across application restarts when running on the `dev` or `prod` profile.
- **FR-002**: All Bulk ingest job state (job metadata, batch data, row results) MUST persist across application restarts when running on the `dev` or `prod` profile.
- **FR-003**: All Metadata resource state (resources, deploy jobs, retrieve jobs) MUST persist across application restarts when running on the `dev` or `prod` profile.
- **FR-004**: The `test` Spring profile MUST use in-memory H2 with `ddl-auto: create-drop` so tests are fast and fully isolated.
- **FR-005**: `POST /reset` MUST clear all persistent state (truncate all tables) cleanly and leave the application in a functional state.
- **FR-006**: Migration from H2 to PostgreSQL MUST require only configuration changes (datasource URL, driver, credentials) with zero code changes.
- **FR-007**: The application MUST create the `./data/` directory automatically if it does not exist when starting on the `dev` profile.
- **FR-008**: All existing Salesforce API paths, request field names, response envelopes, status codes, and operation state values MUST remain unchanged.

### Key Entities *(include if feature involves data)*

- **BulkIngestJobEntity**: JPA entity representing a Bulk API v2 ingest job. Fields: `id` (String, PK), `operation` (String), `object` (String), `externalIdFieldName` (String, nullable), `state` (String), `lineEnding` (String), `columnDelimiter` (String), `createdDate` (OffsetDateTime), `systemModstamp` (OffsetDateTime).
- **BulkBatchEntity**: JPA entity representing raw CSV batch data uploaded to a Bulk job. Fields: `id` (Long, generated PK), `jobId` (String, FK to BulkIngestJobEntity), `csvData` (String/Clob). Relationship: many-to-one to BulkIngestJobEntity.
- **BulkRowResultEntity**: JPA entity representing a per-row result (success, failure, or unprocessed) for a Bulk job. Fields: `id` (Long, generated PK), `jobId` (String, FK), `resultType` (String — `successfulResults`, `failedResults`, `unprocessedrecords`), `sf__Id` (String), `sf__Created` (Boolean), `sf__Error` (String), `rowData` (String).
- **MetadataResourceEntity**: JPA entity replacing the in-memory MetadataResource map. Fields: `id` (Long, generated PK), `type` (String), `fullName` (String), `content` (String/Clob), `createdDate` (OffsetDateTime), `lastModifiedDate` (OffsetDateTime).
- **MetadataDeployJobEntity**: JPA entity replacing the in-memory deploy job map. Fields: `id` (String, PK — the async deploy ID), `state` (String), `numberComponentsDeployed` (Integer), `numberComponentErrors` (Integer), `numberComponentsTotal` (Integer), `createdDate` (OffsetDateTime), `completedDate` (OffsetDateTime, nullable).
- **MetadataRetrieveJobEntity**: JPA entity replacing the in-memory retrieve job map. Fields: `id` (String, PK — the async retrieve ID), `state` (String), `zipFileBase64` (String/Clob, nullable), `createdDate` (OffsetDateTime), `completedDate` (OffsetDateTime, nullable).

## Assumptions

- The existing `SObject` persistence via Spring Data JPA and H2 is already partially in place (H2 is on the classpath and entities are defined). This feature changes the datasource URL and ddl-auto; it does not redesign the sObject entity model.
- `ddl-auto: update` is sufficient for the `dev` profile. Schema versioning tools (Flyway, Liquibase) are out of scope.
- H2 file-based storage is the only persistence backend shipped in this feature. PostgreSQL support is documentation-only.
- The `./data/` directory is local and developer-owned. It is gitignored and not committed.
- Truncation order in `POST /reset` must respect foreign key constraints (child tables truncated before parent tables, or FK checks disabled for the duration of the reset transaction).
- Tests do not need to be modified. The `test` profile activation in `application-test.yml` is sufficient to ensure existing tests continue passing.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Given records created via REST on the `dev` profile, the application restarts, and records are still queryable — verified in an integration test.
- **SC-002**: Given a Bulk ingest job created and completed on the `dev` profile, the application restarts, and job status and row results are retrievable — verified in an integration test.
- **SC-003**: Given Metadata resources and deploy jobs created on the `dev` profile, the application restarts, and all Metadata state is retrievable — verified in an integration test.
- **SC-004**: All existing tests pass without modification when run against the `test` profile (in-memory H2 with `create-drop`).
- **SC-005**: `docs/extensibility.md` documents the PostgreSQL migration path including the `application-prod.yml` template with driver and datasource placeholders.
- **SC-006**: `POST /reset` empties all state across REST, Bulk, and Metadata surfaces in a single call, verified by creating state, calling reset, and asserting all query endpoints return empty results.
