# Implementation Plan: Migrate metadata-service Tests from WireMock to sf-localstack

**Branch**: `003-ms-test-sf-localstack` | **Date**: 2026-03-21 | **Spec**: `specs/003-ms-test-sf-localstack/spec.md`
**Input**: Feature specification from `/specs/003-ms-test-sf-localstack/spec.md`

## Summary

Replace WireMock-based test stubs in `metadata-service` with a real sf-localstack container started via Testcontainers. sf-localstack already implements all required SOAP operations (`describeMetadata`, `listMetadata`, `readMetadata`, `cancelDeploy`) and has a working reset endpoint. The work splits into three areas: (1) sf-localstack enhancements (version routing, response shape fixes, seed data for PDRI objects, Tooling API endpoint, reset body override), and (2) metadata-service test infrastructure changes (new Testcontainers base class, Mocks.java cleanup).

## Technical Context

**Language/Version**: Java 21 (both repos), Spring Boot 3.3.x
**Primary Dependencies**: Testcontainers 1.19+ (GenericContainer), WireMock (removed from metadata-service test scope), Spring Test
**Storage**: H2 in-memory (sf-localstack), PostgreSQL (metadata-service tests via existing Testcontainers setup)
**Testing**: JUnit 5, Spring Boot Test, Testcontainers
**Target Platform**: Docker (Testcontainers), Linux/macOS developer machines and CI
**Project Type**: Integration test infrastructure migration (no production code changes in metadata-service)
**Performance Goals**: Test suite wall-clock time must not regress by more than 30 seconds per class vs WireMock baseline
**Constraints**: Zero changes to test assertion logic; all existing assertions must pass unchanged
**Scale/Scope**: ~9 WireMock stub methods replaced; 1 new base class; sf-localstack gains Tooling API + seed additions + version-agnostic SOAP routing

## Constitution Check

- **API Fidelity**: Salesforce Metadata API v66 (SOAP), Salesforce REST API v60/v66. Every SOAP response shape is pinned to the existing `__files/*.xml` fixtures field-by-field. Every REST response shape is pinned to `__files/*.json` fixtures. Deviations documented in contracts/.
- **Test-First**: Failing contract tests in sf-localstack (Tooling API endpoint, reset override, SOAP version routing) written before implementation. The migration itself is validated by the existing metadata-service test suite passing green — no new assertions, same acceptance bar.
- **Runtime Reproducibility**: sf-localstack `default-seed.yml` is the stable baseline. `POST /reset` with URL override restores state between each test. Fixed IDs in seed match fixture IDs exactly. Testcontainers waits on `/actuator/health` before allowing tests to proceed.
- **Dependency Surface**: Testcontainers `GenericContainer` is already a dependency in metadata-service (`AbstractContainerTest` uses it). No new libraries added. sf-localstack Dockerfile already exists (feature 002). No new services.
- **Observability**: sf-localstack already logs all incoming requests. The new Testcontainers base class logs the container URL at startup. Reset calls are logged by the reset endpoint.
- **Scope Control**: Only changes listed stubs in `Mocks.java`; `mockAppOpsClientSettings` is retained. No new Salesforce operations beyond what the spec lists. `checkusingvcsrepo-response.json` is out of scope unless test analysis shows it blocks green suite.
- **Parity Verification**: Zero test failures in the existing metadata-service test suite after migration is the acceptance bar. No new real-org parity checks required (infra change only).

## Project Structure

### Documentation (this feature)

```text
specs/003-ms-test-sf-localstack/
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   ├── soap-metadata-api.md
│   └── rest-soql-api.md
└── spec.md
```

### Source Code

**sf-localstack** (this repo):
```text
service/src/main/java/co/prodly/sflocalstack/
├── controller/
│   ├── MetadataController.java          # path: /services/Soap/m/{version}
│   ├── ToolingController.java           # NEW: GET /services/data/{v}/tooling/query/
│   └── ResetController.java             # enhanced: accept seedOverrides body
├── service/
│   ├── MetadataSoapRenderer.java        # fix: add <suffix>, fix partialSaveAllowed, fix cancelDeploy shape
│   └── SeedService.java                 # enhanced: apply seedOverrides on reset
└── resources/seed/
    └── default-seed.yml                 # add PDRI objects, StaticResource, fix Organization
```

**metadata-service** (`~/code/metadata-service`):
```text
service/src/test/java/co/prodly/metadata/shared/
├── AbstractContainerTest.java           # unchanged
├── AbstractContainerWithWiremockTest.java  # DELETED or emptied
├── AbstractSfLocalstackTest.java        # NEW: Testcontainers base class
└── Mocks.java                           # remove Salesforce stubs; keep mockAppOpsClientSettings
```

**Structure Decision**: sf-localstack changes are in this repo; metadata-service changes are in `~/code/metadata-service`. Both must be coordinated — sf-localstack enhancements must be complete and tested before metadata-service migration.

## Feature Iteration Strategy

### Feature 0: sf-localstack SOAP Version Routing + Response Shape Fixes

- **Backend Scope**:
  - Change `MetadataController` `@RequestMapping` from `/services/Soap/m/60.0` to `/services/Soap/m/{version}` to accept v60 and v66 calls
  - Fix `MetadataSoapRenderer.renderDescribeMetadata()`: add `<suffix>` element; set `partialSaveAllowed` to `true`
  - Fix `MetadataSoapRenderer.renderCancelDeploy()`: match fixture shape `<done>false</done><id>...</id>` (no `<success>` tag) — verify test assertions first
  - Fix `MetadataSoapRenderer.renderReadMetadata()` for `StandardValueSet`: add `<sorted>false</sorted>` between `<fullName>` and `<standardValue>`
- **Tests First**: Add `MetadataControllerV66Test` verifying `POST /services/Soap/m/66.0` routes identically to `POST /services/Soap/m/60.0` for all operations
- **Integration Verification**: `mvn test -pl service` green in sf-localstack
- **Parity Check**: N/A — routing fix, no org comparison needed

### Feature 1: sf-localstack Seed Data (PDRI Objects + StaticResource)

- **Backend Scope**:
  - Update `default-seed.yml`: add `PDRI__Connection__c`, `PDRI__ComparisonView__c`, `PDRI__ComparisonViewRule__c`, `StaticResource` records; update `Organization` record (`IsSandbox: true`, add `OrganizationType`, `TrialExpirationDate`)
  - Enhance `POST /reset`: accept optional `seedOverrides` JSON body; apply field-level overrides after base seed restore (needed to inject dynamic `PDRI__Instance_URL__c`)
  - Ensure `SeedService` can handle custom sObject types (PDRI__ prefix) without schema validation errors
- **Tests First**: Add `SeedDataTest` verifying SOQL queries against seeded PDRI objects return fixture-matching JSON; add `ResetOverrideTest` verifying the override body is applied
- **Integration Verification**: `curl 'http://localhost:8080/services/data/v60.0/query?q=SELECT+Id+FROM+PDRI__Connection__c'` returns the seeded record
- **Parity Check**: Field names and values match `connections-response.json` and `comparisonview-response.json` exactly

### Feature 2: sf-localstack Tooling API Endpoint

- **Backend Scope**:
  - Add `ToolingController` mapping `GET /services/data/{version}/tooling/query/`
  - Route `SELECT BodyLength FROM StaticResource WHERE Name IN (...)` to the `StaticResource` sObject table
  - Return standard SOQL query response envelope with `totalSize`, `done`, `records[]`
- **Tests First**: Add `ToolingControllerTest` with small and large resource queries returning fixture-matching JSON
- **Integration Verification**: `curl 'http://localhost:8080/services/data/v66.0/tooling/query/?q=SELECT+BodyLength+FROM+StaticResource+WHERE+Name+IN+(%27SmallResource%27)'` returns `{"BodyLength":1}`
- **Parity Check**: Response shape matches `static-resources-small.json` and `static-resources-big.json`

### Feature 3: metadata-service Testcontainers Base Class

- **Backend Scope** (in `~/code/metadata-service`):
  - Create `AbstractSfLocalstackTest extends AbstractContainerTest`
  - Start sf-localstack via `new GenericContainer<>(new ImageFromDockerfile().withDockerfile(Paths.get("path/to/Dockerfile")))` or from Docker Hub image
  - Expose port 8080, wait on `GET /actuator/health` → `{"status":"UP"}`
  - `@DynamicPropertySource`: register `salesforce.instance.url` (or equivalent property) as `http://<host>:<mappedPort>`
  - `@BeforeEach`: `POST /reset` with `{"seedOverrides":{"PDRI__Connection__c":{"PDRI__Instance_URL__c":"http://<host>:<mappedPort>"}}}` to inject dynamic URL
- **Tests First**: A trivial test extending `AbstractSfLocalstackTest` that calls `GET /actuator/health` on the container and asserts HTTP 200 — confirms container starts without `Connection refused`
- **Integration Verification**: Run one existing test class that previously used `AbstractContainerWithWiremockTest` with the new base class and confirm it passes
- **Parity Check**: No WireMock process starts; port 8090 is never bound

### Feature 4: metadata-service Mocks.java Cleanup

- **Backend Scope** (in `~/code/metadata-service`):
  - Delete (or empty out) `AbstractContainerWithWiremockTest.java`
  - Remove Salesforce stub methods from `Mocks.java`: `mockDescribeMetadata`, `mockReadMetadataPicklist`, `mockReadMetadataStandardValueSet`, `mockListMetadata`, `mockComparisonViewRules`, `mockDefaultComparisonViewRules`, `mockFindConnectionByOrgId`, `mockFindControlConnection`, `mockFindVcConnection`, `mockSmallStaticResourceToolingQuery`, `mockBigStaticResourceToolingQuery`, `mockCancelJobResponse`, `mockCheckUsingVcsRepo`
  - Retain: `mockAppOpsClientSettings`
  - Update all test classes currently extending `AbstractContainerWithWiremockTest` to extend `AbstractSfLocalstackTest`
  - Remove all `Mocks.mock*()` call sites for the deleted methods
- **Tests First**: N/A (cleanup, validated by suite passing)
- **Integration Verification**: `mvn test -pl service` green in metadata-service with zero WireMock references remaining (grep confirms)
- **Parity Check**: SC-001 through SC-005 from spec — full test suite passes at same rate

## Salesforce Parity Verification

- **Reference Org**: N/A — acceptance bar is the existing test suite passing green
- **Parity Method**: `mvn test -pl service` in metadata-service
- **Compared Signals**: Zero test failures; no WireMock stubs for Salesforce endpoints; port 8090 never bound
- **Mutation Policy**: N/A — no real org involved
- **Accepted Deltas**:
  - `cancelDeploy` response: sf-localstack returns `<done>false</done>` (fixture value); actual Salesforce may return `<done>true</done>` — acceptable since tests only check operation completes successfully
  - `listMetadata` response: sf-localstack omits `createdById`/`id`/`lastModifiedById` fields not asserted by tests — acceptable if confirmed by test analysis
  - `organization-response.json` `totalSize: 3` vs. actual one Organization record — this is a WireMock fixture artifact; sf-localstack returns actual count from seed

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| `POST /reset` body override | Dynamic URL injection for `PDRI__Connection__c.PDRI__Instance_URL__c` | Static seed value `http://localhost:8090` would break metadata-service's downstream calls |
| Tooling API endpoint | metadata-service uses `/tooling/query/` path, not standard SOQL | Routing tooling queries through the same SOQL handler would conflate two distinct Salesforce API surfaces |
