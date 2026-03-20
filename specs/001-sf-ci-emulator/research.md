# Research: Usable Salesforce Integration-Test Emulator

## Decision: Keep SOQL execution as a small in-memory parser/executor

- **Rationale**: The supported slice is intentionally narrow (`SELECT`, `FROM`, `WHERE`, `AND`, `LIKE`, null checks, `COUNT()`, `LIMIT`), so a dedicated parser/executor in `service/` is simpler and more controllable than translating to H2 SQL.
- **Alternatives considered**:
  - Translating SOQL to H2 SQL: rejected because relationship projection and Salesforce-like error behavior become harder to control.
  - Adding a parser dependency: rejected by the no-new-dependencies constraint.

## Decision: Resolve relationship fields with literal-first, related-record-second precedence

- **Rationale**: Existing seed data already stores `Account.Name` as a literal field, and the spec explicitly requires deterministic behavior when literal relationship values and linked records both exist. Literal-first preserves current seeded behavior while still allowing `AccountId`-backed lookups for created or upserted records.
- **Alternatives considered**:
  - Related-record-first lookup: rejected because it could silently change seeded query outputs.
  - Only literal support: rejected because it would block realistic post-seed relationship access.

## Decision: Keep runtime state resettable and reproducible during local app development

- **Rationale**: The app must behave the same way on repeated local runs after reset. Seeded records, transient job state, and supported response semantics all need to return to a known baseline so feature slices can be verified predictably.
- **Alternatives considered**:
  - Keep random IDs and only assert loosely in tests: rejected because repeated local verification would drift.
  - Auto-reset after each request: rejected because the app needs explicit reset control for debugging and UI inspection.

## Decision: Use synchronized external-ID upsert in `OrgStateService`

- **Rationale**: The spec requires deterministic concurrent upsert behavior and the user explicitly asked for synchronized upsert. A synchronized service method is enough for the current single-node app and avoids adding locking infrastructure.
- **Alternatives considered**:
  - Database uniqueness constraints on dynamic external ID fields: rejected because field definitions are flexible JSON, not modeled columns.
  - Per-key lock map: rejected as unnecessary complexity for this scope.

## Decision: Implement Bulk API v2 ingest jobs as synchronous in-memory workflows

- **Rationale**: Developers need create/upload/close/status/result compatibility without Salesforce async delays. A map-backed job store plus synchronous processing on `UploadComplete` matches the required behavior and stays easy to reset.
- **Alternatives considered**:
  - Async executors with polling delays: rejected because they complicate local feature verification.
  - JPA-backed jobs: rejected because transient ingest state does not need relational persistence.

## Decision: Parse Metadata SOAP with JDK DOM parsers and render responses from string templates

- **Rationale**: DOM parsing via standard XML parsers is sufficient for the small operation set, works with current dependencies, and gives exact control over Salesforce-like SOAP envelopes.
- **Alternatives considered**:
  - JAXB/CXF/SAAJ stack: rejected because it adds dependency and configuration weight.
  - Regex XML parsing: rejected because SOAP namespace handling and body extraction would be brittle.

## Decision: Use `dev20` as the real-Salesforce parity source during feature delivery

- **Rationale**: Each feature slice needs a concrete reference point beyond local tests. `dev20` provides the real request/response shapes needed to confirm that the emulator is staying compatible with supported client flows.
- **Alternatives considered**:
  - Manual memory of Salesforce behavior: rejected because it is too easy to drift.
  - CI-only parity verification: rejected because the project is intentionally app-first right now.

## Decision: Use explicit temporary-record naming and cleanup for parity checks

- **Rationale**: Some parity checks need real Salesforce mutations for create/update/upsert/Bulk validation. Using explicit prefixes such as `SFLOCALSTACK_TEST_` and cleaning up immediately keeps the reference org usable while still allowing strong verification.
- **Alternatives considered**:
  - Read-only parity only: rejected because mutation semantics are part of the supported slices.
  - Leaving temp records behind for inspection: rejected because it pollutes the reference org and makes later checks harder to trust.
