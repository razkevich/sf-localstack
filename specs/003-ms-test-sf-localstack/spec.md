# Feature Specification: Migrate metadata-service Tests from WireMock to sf-localstack

**Feature Branch**: `003-ms-test-sf-localstack`
**Created**: 2026-03-21
**Status**: Draft

## Compatibility Context

- **Salesforce Surface**: `REST`, `Metadata (SOAP)`
- **Compatibility Target**: Salesforce Metadata API v66, Salesforce REST API v60+
- **In-Scope Operations**:
  - SOAP: `describeMetadata`, `listMetadata`, `readMetadata`, `cancelDeploy`
  - REST SOQL: `Organization`, `PDRI__Connection__c`, `PDRI__ComparisonView__c`, `PDRI__ComparisonViewRule__c`
  - REST Tooling: `StaticResource` BodyLength query
- **Out-of-Scope Operations**: `retrieve`, `deploy`, `checkDeployStatus`, `checkRetrieveStatus` (already supported in sf-localstack and not WireMock-stubbed in the current test suite)
- **API Shape Commitments**: Response shapes must match current `__files/` stub fixtures exactly — same field names, envelope structure, and value types — so existing test assertions pass without modification
- **Frontend Scope**: None
- **Test Isolation Plan**: sf-localstack starts once per test class via Testcontainers; state is reset before each test via sf-localstack's reset endpoint; seed data is re-applied on reset
- **Runtime Reproducibility Controls**: sf-localstack `default-seed.yml` is the stable baseline; tests must not mutate seed state; PDRI custom objects seeded with fixed IDs matching those in current `__files/*.json` stubs
- **Parity Verification Plan**: Zero test failures in the existing `metadata-service` test suite after migration is the acceptance bar; no new parity checks against a real Salesforce org required

## Feature Iterations

### Feature 0 — Testcontainers Base Class (Priority: P1)

Replace `AbstractContainerWithWiremockTest` (which starts WireMock on port 8090) with a new base class that starts sf-localstack via Testcontainers and injects its dynamic mapped URL as the Salesforce instance base URL.

**Why this priority**: All other changes depend on sf-localstack being reachable from tests.

**Independent Test**: A trivial test extending the new base class boots the Spring context without a `Connection refused` error and without starting WireMock.

**Acceptance Scenarios**:

1. **Given** a test class extends the new base class, **When** the test suite runs, **Then** sf-localstack starts as a Testcontainer and its base URL is wired as the Salesforce instance URL
2. **Given** sf-localstack is running, **When** `@BeforeEach` executes, **Then** a reset call clears state mutations from the prior test
3. **Given** the migration is complete, **When** tests run, **Then** no `WireMockServer` is started and port 8090 is never bound

**Frontend Deliverables**: None

**Parity Check**: N/A — infrastructure change only

---

### Feature 1 — SOAP Stub Replacement (Priority: P1)

Add `listMetadata`, `describeMetadata`, `readMetadata`, and `cancelDeploy` SOAP operations to sf-localstack with response bodies matching the existing `__files/` XML fixtures. Remove the corresponding `Mocks.mock*()` WireMock helpers.

**Why this priority**: Most comparison, snapshot, and Quartz job tests depend on these SOAP stubs.

**Stubs to retire**:

| `Mocks` method | XML fixture |
|---|---|
| `mockDescribeMetadata()` | `describemetadata-response.xml` |
| `mockListMetadata()` | `listmetadata-response.xml` |
| `mockReadMetadataPicklist()` | `readmetadata-picklist-response.xml` |
| `mockReadMetadataStandardValueSet()` | `readmetadata-standardvalueset-response.xml` |
| `mockCancelJobResponse()` | `canceldeployresult-response.xml` |

**Independent Test**: `POST /services/Soap/m/66.0` with a `describeMetadata` body against sf-localstack returns an envelope matching `describemetadata-response.xml`.

**Acceptance Scenarios**:

1. **Given** sf-localstack is running, **When** metadata-service issues a `listMetadata` SOAP request for `ApexClass`, **Then** sf-localstack returns `FileProperties` entries matching `listmetadata-response.xml`
2. **Given** sf-localstack is running, **When** metadata-service issues a `describeMetadata` SOAP request, **Then** sf-localstack returns all supported metadata type descriptors
3. **Given** sf-localstack is running, **When** metadata-service issues a `readMetadata` SOAP request for a `StandardValueSet`, **Then** sf-localstack returns the standard value set XML body
4. **Given** sf-localstack is running, **When** metadata-service issues a `cancelDeploy` SOAP request, **Then** sf-localstack returns a success cancellation response

**Frontend Deliverables**: None

**Parity Check**: Response XML field-by-field match between sf-localstack output and each retired `__files/` fixture

---

### Feature 2 — REST/SOQL Stub Replacement (Priority: P1)

Seed sf-localstack with `PDRI__Connection__c`, `PDRI__ComparisonView__c`, `PDRI__ComparisonViewRule__c`, and `StaticResource` records matching the shapes in current `__files/*.json` fixtures. Remove the corresponding `Mocks.mock*()` WireMock helpers.

**Why this priority**: REST SOQL stubs are called in nearly every comparison and deployment test.

**Stubs to retire**:

| `Mocks` method | JSON fixture |
|---|---|
| `mockOrganization()` | `organization-response.json` |
| `mockFindConnectionByOrgId()` | `connections-response.json` |
| `mockFindControlConnection()` | `connections-response.json` |
| `mockFindVcConnection()` | `connections-response.json` |
| `mockComparisonViewRules()` | `comparisonview-response.json` |
| `mockDefaultComparisonViewRules()` | `comparisonview-response.json` |
| `mockSmallStaticResourceToolingQuery()` | `static-resources-small.json` |
| `mockBigStaticResourceToolingQuery()` | `static-resources-big.json` |
| `mockCheckUsingVcsRepo()` | `checkusingvcsrepo-response.json` |

**Key constraint**: `connections-response.json` currently hardcodes `PDRI__Instance_URL__c: "http://localhost:8090"`. The seeded record must use sf-localstack's own base URL so metadata-service resolves subsequent calls correctly.

**Independent Test**: `GET /services/data/v60.0/query?q=SELECT+IsSandbox...+FROM+Organization` against sf-localstack returns JSON matching `organization-response.json`.

**Acceptance Scenarios**:

1. **Given** sf-localstack seed data is loaded, **When** metadata-service queries `PDRI__Connection__c` by org ID, **Then** sf-localstack returns a connection record with `PDRI__Instance_URL__c` pointing to sf-localstack's own URL
2. **Given** sf-localstack seed data is loaded, **When** metadata-service queries `PDRI__ComparisonViewRule__c`, **Then** sf-localstack returns rules matching `comparisonview-response.json`
3. **Given** sf-localstack seed data is loaded, **When** metadata-service queries `StaticResource` BodyLength via Tooling API, **Then** sf-localstack returns data satisfying both small and large static resource test scenarios
4. **Given** sf-localstack seed data is loaded, **When** metadata-service queries `Organization`, **Then** sf-localstack returns a sandbox org record

**Frontend Deliverables**: None

**Parity Check**: Each SOQL response from sf-localstack matches the field names and value types of the corresponding retired `__files/*.json` fixture

---

### Edge Cases

- What happens when a test needs "big" vs "small" static resource behavior? Seed must include both a small (<10 MB) and large (>10 MB) `StaticResource` so both code paths are exercised.
- What happens when `PDRI__Connection__c.PDRI__Instance_URL__c` is used for downstream calls? The seeded URL must resolve to sf-localstack's dynamic Testcontainers-mapped port, not a hardcoded value.
- What happens if sf-localstack is slow to start? Testcontainers must wait on sf-localstack's `/actuator/health` before allowing tests to run.
- What happens to `mockAppOpsClientSettings()` in `Mocks.java`? It stubs the AppOps client (not Salesforce) and must be retained unchanged.
- What happens if a test calls a Salesforce endpoint sf-localstack doesn't implement? The response must be a recognizable error (not a silent 400) so gaps are easy to diagnose.

## Requirements

### Functional Requirements

- **FR-001**: sf-localstack MUST implement `listMetadata` SOAP operation returning `FileProperties` for all seeded metadata types
- **FR-002**: sf-localstack MUST implement `describeMetadata` SOAP operation returning `DescribeMetadataResult` covering all seeded metadata types
- **FR-003**: sf-localstack MUST implement `readMetadata` SOAP operation for `StandardValueSet` and `GlobalValueSet` types
- **FR-004**: sf-localstack MUST implement `cancelDeploy` SOAP operation returning a success cancellation envelope
- **FR-005**: sf-localstack MUST seed `PDRI__Connection__c` with fields: `Id`, `PDRI__Instance_URL__c` (sf-localstack's own base URL), `PDRI__OrganizationId__c`, `PDRI__Org_Type__c`, `PDRI__Local_Connection__c`, `PDRI__Active__c`, `PDRI__Target__c`, `OwnerId`
- **FR-006**: sf-localstack MUST seed `PDRI__ComparisonView__c` and `PDRI__ComparisonViewRule__c` records with fields matching `comparisonview-response.json`
- **FR-007**: sf-localstack MUST serve `StaticResource` BodyLength via Tooling API with at least one small and one large resource in seed data
- **FR-008**: `AbstractContainerWithWiremockTest` MUST be replaced with a Testcontainers-based base class that starts sf-localstack and injects its URL as the Salesforce instance base URL
- **FR-009**: All `Mocks.java` methods stubbing Salesforce endpoints MUST be removed; non-Salesforce stubs (e.g., `mockAppOpsClientSettings`) are retained
- **FR-010**: sf-localstack's reset endpoint MUST restore all seed data between tests without restarting the container

### Key Entities

- **`PDRI__Connection__c`**: A Salesforce org connection. Fields: `Id`, `PDRI__Instance_URL__c`, `PDRI__OrganizationId__c`, `PDRI__Org_Type__c`, `PDRI__Local_Connection__c`, `PDRI__Active__c`, `PDRI__Target__c`
- **`PDRI__ComparisonView__c`**: A metadata filter configuration. Fields: `Id`, `Name`, `PDRI__Default__c`
- **`PDRI__ComparisonViewRule__c`**: An include/exclude rule within a comparison view. Fields: `Id`, `Name`, `PDRI__Action__c`, `PDRI__Filter_Type__c`, `PDRI__Filter_Value__c`, `PDRI__Comparison_View__c`, `PDRI__Parent_Rule__c`
- **`StaticResource`**: A Salesforce static resource. Fields: `Name`, `BodyLength`

## Success Criteria

### Measurable Outcomes

- **SC-001**: All tests previously extending `AbstractContainerWithWiremockTest` pass without changes to their assertion logic
- **SC-002**: Zero WireMock stubs remain for Salesforce SOAP or REST endpoints after migration
- **SC-003**: The full `metadata-service` test suite passes with the same pass rate as before migration
- **SC-004**: Test suite wall-clock time does not regress by more than 30 seconds per test class compared to the WireMock baseline
- **SC-005**: A developer with only Docker available can run the full test suite without a real Salesforce org, WireMock, or a manually started sf-localstack process

## Assumptions

- sf-localstack's reset endpoint can restore all seed data between tests; if it doesn't exist yet, adding it is in scope
- The Testcontainers base class builds sf-localstack from the local `Dockerfile` (created in feature 002); no published image required
- `PDRI__Connection__c.PDRI__Instance_URL__c` is used by metadata-service as the base URL for subsequent Salesforce calls; seeding it dynamically with the Testcontainers-mapped URL is sufficient
- Tests distinguishing large vs small static resources do so by `BodyLength` threshold; seeding both sizes covers both scenarios without per-test seed overrides
