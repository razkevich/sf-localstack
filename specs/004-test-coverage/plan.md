# Implementation Plan: Test Coverage Hardening

**Branch**: `004-test-coverage` | **Date**: 2026-04-09 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-test-coverage/spec.md`

## Summary

Add comprehensive test coverage for all existing controller endpoints and service methods before any refactoring work begins. Currently at 75% endpoint coverage and 67% service method coverage — target is 100% of public API endpoints with happy+error paths, and 100% of service public methods with at least one test each.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.3.5 (spring-boot-starter-test), JUnit 5, MockMvc, AssertJ
**Storage**: H2 in-memory (test profile)
**Testing**: JUnit 5 + MockMvc + AssertJ (existing stack — no new dependencies)
**Target Platform**: Spring Boot embedded Tomcat
**Project Type**: Web service (Salesforce API emulator)

## Constitution Check

- **API Fidelity**: Tests verify existing response shapes. No API changes introduced.
- **Test-First**: This feature IS the test suite. Tests written before any refactoring begins.
- **Runtime Reproducibility**: All tests use H2 in-memory with reset between tests. No environmental dependencies.
- **Dependency Surface**: Zero new dependencies. Uses existing spring-boot-starter-test.
- **Observability**: Test request logging verified via DashboardController tests.
- **Scope Control**: Test-only changes. No source code modifications.
- **Parity Verification**: Not applicable (test infrastructure only).

## Project Structure

### Source Code (repository root)

```text
service/src/test/java/co/razkevich/sflocalstack/
├── helpers/
│   ├── TestDataFactory.java          (NEW)
│   ├── SoapTestHelper.java           (NEW)
│   └── AssertionHelpers.java         (NEW)
├── controller/
│   ├── SObjectControllerTest.java    (MODIFY — add GET by ID, DELETE tests)
│   ├── BulkControllerTest.java       (MODIFY — add failed/unprocessed results)
│   ├── DashboardControllerTest.java  (MODIFY — add SSE test)
│   ├── MetadataRestControllerTest.java (MODIFY — add describe test)
│   ├── VersionControllerTest.java    (MODIFY — add alias route tests)
│   └── QueryControllerTest.java      (MODIFY — add trailing slash test)
├── service/
│   ├── OrgStateServiceTest.java      (NEW — findById, findAll, delete, reset, fromJson)
│   ├── BulkJobServiceTest.java       (MODIFY — add failedResults, unprocessedResults)
│   ├── MetadataServiceTest.java      (MODIFY — add listResources, reset)
│   ├── RequestLogServiceTest.java    (NEW — log, reset, newEmitter)
│   └── MetadataToolingServiceTest.java (NEW — executeStandardMetadataQuery)
├── integration/
│   └── CrossSurfaceIntegrationTest.java (MODIFY — add 2 more workflows)
```

## Feature Iteration Strategy

### Feature 3: Test Infrastructure (done first — used by all other features)

- **Backend Scope**: TestDataFactory, SoapTestHelper, AssertionHelpers
- **Tests First**: These ARE test utilities — verified by usage in subsequent features

### Feature 0: Controller Integration Tests

- **Backend Scope**: Fill gaps in all 10 controllers
- **Tests First**: Each test written and run individually

### Feature 1: Service Unit Tests

- **Backend Scope**: Fill gaps in all 9 services
- **Tests First**: Each test written and run individually

### Feature 2: Cross-Surface Integration Tests

- **Backend Scope**: 2 additional multi-surface workflows
- **Tests First**: Each workflow test written and run individually

---

## Phase 1: Test Infrastructure (Feature 3)

### T001 — Create TestDataFactory

**Files**:
- Create: `service/src/test/java/co/razkevich/sflocalstack/helpers/TestDataFactory.java`

**Steps**:
- [ ] Create `TestDataFactory` with static methods:
  - `createAccountJson(String name)` → JSON string for Account with defaults
  - `createContactJson(String firstName, String lastName)` → JSON string for Contact
  - `createLeadJson(String company, String lastName)` → JSON string for Lead
  - `createAccountViaApi(MockMvc mvc)` → creates Account, returns ID string
  - `createAccountViaApi(MockMvc mvc, String name)` → creates named Account, returns ID
  - `resetOrg(MockMvc mvc)` → calls POST /reset
- [ ] Verify compilation: `mvn -pl service test-compile`
- [ ] Commit: `test: add TestDataFactory helper`

### T002 — Create SoapTestHelper

**Files**:
- Create: `service/src/test/java/co/razkevich/sflocalstack/helpers/SoapTestHelper.java`

**Steps**:
- [ ] Create `SoapTestHelper` with static methods:
  - `envelope(String operation, String body)` → wraps in SOAP envelope with proper namespaces
  - `describeMetadata()` → complete describeMetadata SOAP request
  - `listMetadata(String type)` → listMetadata SOAP request
  - `readMetadata(String type, String... fullNames)` → readMetadata SOAP request
  - `deploy(String zipBase64)` → deploy SOAP request
  - `checkDeployStatus(String asyncId)` → checkDeployStatus SOAP request
  - `retrieve(String type, String... members)` → retrieve SOAP request
  - `checkRetrieveStatus(String asyncId)` → checkRetrieveStatus SOAP request
  - `cancelDeploy(String asyncId)` → cancelDeploy SOAP request
- [ ] Verify compilation: `mvn -pl service test-compile`
- [ ] Commit: `test: add SoapTestHelper for SOAP envelope construction`

### T003 — Create AssertionHelpers

**Files**:
- Create: `service/src/test/java/co/razkevich/sflocalstack/helpers/AssertionHelpers.java`

**Steps**:
- [ ] Create `AssertionHelpers` with static methods:
  - `assertCreatedResponse(MvcResult result)` → asserts 201, has `id`, `success: true`, `errors: []`
  - `assertSalesforceError(MvcResult result, int status, String errorCode)` → asserts status and error shape
  - `assertQueryResult(MvcResult result, int expectedSize)` → asserts `totalSize`, `done`, `records` array
  - `assertRecordShape(String json, String objectType)` → asserts `attributes.type` and `attributes.url` present
  - `extractId(MvcResult result)` → extracts `id` from JSON response body
- [ ] Verify compilation: `mvn -pl service test-compile`
- [ ] Commit: `test: add AssertionHelpers for Salesforce response validation`

---

## Phase 2: Controller Integration Tests (Feature 0)

### T004 — SObjectController: GET by ID and DELETE

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/controller/SObjectControllerTest.java`

**Steps**:
- [ ] Add test `getRecordByIdReturnsFullRecord()`:
  - Create Account via POST, extract ID
  - GET `/services/data/v60.0/sobjects/Account/{id}`
  - Assert 200, response has `Id`, `Name`, `attributes.type = "Account"`
- [ ] Add test `getRecordByIdReturns404ForMissingRecord()`:
  - GET `/services/data/v60.0/sobjects/Account/001000000000000AAA`
  - Assert 404, Salesforce-style error response
- [ ] Add test `deleteRecordReturns204()`:
  - Create Account via POST, extract ID
  - DELETE `/services/data/v60.0/sobjects/Account/{id}`
  - Assert 204 No Content
  - GET same ID → assert 404
- [ ] Add test `deleteNonexistentRecordReturns404()`:
  - DELETE `/services/data/v60.0/sobjects/Account/001000000000000AAA`
  - Assert 404
- [ ] Run: `mvn -pl service test -Dtest=SObjectControllerTest`
- [ ] Commit: `test: add SObjectController GET by ID and DELETE tests`

### T005 — BulkController: Failed and Unprocessed Results

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/controller/BulkControllerTest.java`

**Steps**:
- [ ] Add test `failedResultsReturnsCsvFormat()`:
  - Create job, upload CSV with invalid data (update with missing Id), close job
  - GET `/{jobId}/failedResults`
  - Assert 200, Content-Type `text/csv`, body contains `sf__Error` column
- [ ] Add test `unprocessedRecordsReturnsCsvFormat()`:
  - Create job, upload CSV, close job
  - GET `/{jobId}/unprocessedrecords`
  - Assert 200, Content-Type `text/csv`
- [ ] Add test `resultsOnOpenJobReturns400()`:
  - Create job (state=Open), don't close
  - GET `/{jobId}/successfulResults`
  - Assert 400 or appropriate error
- [ ] Run: `mvn -pl service test -Dtest=BulkControllerTest`
- [ ] Commit: `test: add BulkController failed/unprocessed result tests`

### T006 — DashboardController: SSE Event Stream

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/controller/DashboardControllerTest.java`

**Steps**:
- [ ] Add test `sseEventStreamReturnsTextEventStream()`:
  - GET `/api/dashboard/events` with Accept: text/event-stream
  - Assert response content type is `text/event-stream`
- [ ] Run: `mvn -pl service test -Dtest=DashboardControllerTest`
- [ ] Commit: `test: add DashboardController SSE endpoint test`

### T007 — MetadataRestController: Tooling Describe

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/controller/MetadataRestControllerTest.java`

**Steps**:
- [ ] Add test `toolingDescribeReturnsFieldMetadata()`:
  - GET `/services/data/v60.0/tooling/sobjects/FlowDefinition/describe`
  - Assert 200, response has `name`, `fields` array
- [ ] Add test `toolingDescribeUnknownObjectReturns404()`:
  - GET `/services/data/v60.0/tooling/sobjects/NonExistent/describe`
  - Assert 404 or appropriate error
- [ ] Run: `mvn -pl service test -Dtest=MetadataRestControllerTest`
- [ ] Commit: `test: add MetadataRestController tooling describe tests`

### T008 — VersionController: Alias Routes

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/controller/VersionControllerTest.java`

**Steps**:
- [ ] Add test `dataAliasReturnsVersionList()`:
  - GET `/data`
  - Assert 200, response is array of versions
- [ ] Add test `dataAliasVersionReturnsResourceMap()`:
  - GET `/data/v60.0`
  - Assert 200, response has `sobjects`, `query` keys
- [ ] Add test `servicesDataNoSlashReturnsVersionList()`:
  - GET `/services/data` (no trailing slash)
  - Assert 200 (same as `/services/data/`)
- [ ] Run: `mvn -pl service test -Dtest=VersionControllerTest`
- [ ] Commit: `test: add VersionController alias route tests`

### T009 — QueryController: Trailing Slash Alias

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/controller/QueryControllerTest.java`

**Steps**:
- [ ] Add test `queryWithTrailingSlashWorks()`:
  - Create Account via POST
  - GET `/services/data/v60.0/query/?q=SELECT Id, Name FROM Account`
  - Assert 200, records returned
- [ ] Run: `mvn -pl service test -Dtest=QueryControllerTest`
- [ ] Commit: `test: add QueryController trailing slash test`

---

## Phase 3: Service Unit Tests (Feature 1)

### T010 — OrgStateService Unit Tests

**Files**:
- Create: `service/src/test/java/co/razkevich/sflocalstack/service/OrgStateServiceTest.java`

**Steps**:
- [ ] Add test `findByIdReturnsCreatedRecord()`:
  - Create Account, get ID
  - `findById(id)` returns present Optional with correct data
- [ ] Add test `findByIdReturnsEmptyForMissingId()`:
  - `findById("nonexistent")` returns empty Optional
- [ ] Add test `findByTypeAndIdReturnsMatchingRecord()`:
  - Create Account, get ID
  - `findByTypeAndId("Account", id)` returns present Optional
- [ ] Add test `findByTypeAndIdReturnsEmptyForWrongType()`:
  - Create Account, get ID
  - `findByTypeAndId("Contact", id)` returns empty Optional
- [ ] Add test `findAllReturnsAllRecords()`:
  - Create 3 records (2 Account, 1 Contact)
  - `findAll()` returns 3 records
- [ ] Add test `deleteRemovesRecord()`:
  - Create Account, get ID
  - `delete(id)` returns true
  - `findById(id)` returns empty
- [ ] Add test `deleteReturnsFalseForMissingId()`:
  - `delete("nonexistent")` returns false
- [ ] Add test `resetClearsAllRecords()`:
  - Create 3 records
  - `reset()`
  - `findAll()` returns empty list
- [ ] Add test `fromJsonParsesValidJson()`:
  - `fromJson("{\"Name\":\"Test\"}")` returns map with Name=Test
- [ ] Run: `mvn -pl service test -Dtest=OrgStateServiceTest`
- [ ] Commit: `test: add OrgStateService unit tests for findById, delete, reset, fromJson`

### T011 — BulkJobService: Failed and Unprocessed Results

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/service/BulkJobServiceTest.java`

**Steps**:
- [ ] Add test `failedResultsReturnsCsvWithErrors()`:
  - Create update job, upload CSV with rows missing Id field
  - Close job
  - `failedResults(jobId)` returns CSV with `sf__Error` column
- [ ] Add test `unprocessedResultsReturnsEmptyOnSuccess()`:
  - Create insert job, upload valid CSV, close job
  - `unprocessedResults(jobId)` returns empty or header-only CSV
- [ ] Run: `mvn -pl service test -Dtest=BulkJobServiceTest`
- [ ] Commit: `test: add BulkJobService failed/unprocessed result tests`

### T012 — MetadataService: listResources and Reset

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/service/MetadataServiceTest.java`

**Steps**:
- [ ] Add test `listResourcesReturnsAllCreatedResources()`:
  - Create 3 resources
  - `listResources()` returns list of size 3+
- [ ] Add test `resetClearsAllResources()`:
  - Create resources
  - `reset()`
  - `listResources()` returns empty or default-only list
- [ ] Run: `mvn -pl service test -Dtest=MetadataServiceTest`
- [ ] Commit: `test: add MetadataService listResources and reset tests`

### T013 — RequestLogService Unit Tests

**Files**:
- Create: `service/src/test/java/co/razkevich/sflocalstack/service/RequestLogServiceTest.java`

**Steps**:
- [ ] Add test `logAddsEntryToRecentList()`:
  - Create RequestLogEntry, call `log(entry)`
  - `getRecent(10)` contains the entry
- [ ] Add test `resetClearsAllEntries()`:
  - Log 3 entries
  - `reset()`
  - `getRecent(10)` returns empty list, `size()` returns 0
- [ ] Add test `newEmitterReturnsNonNull()`:
  - `newEmitter()` returns non-null SseEmitter
- [ ] Run: `mvn -pl service test -Dtest=RequestLogServiceTest`
- [ ] Commit: `test: add RequestLogService unit tests`

### T014 — MetadataToolingService: Standard Metadata Query

**Files**:
- Create: `service/src/test/java/co/razkevich/sflocalstack/service/MetadataToolingServiceTest.java`

**Steps**:
- [ ] Add test `executeStandardMetadataQueryReturnsResults()`:
  - Create metadata resources first
  - Call `executeStandardMetadataQuery("SELECT ... FROM EntityDefinition")`
  - Assert returns non-empty list with expected shape
- [ ] Add test `executeStandardMetadataQueryOnEmptyReturnsEmpty()`:
  - Query with no matching data
  - Assert returns empty list
- [ ] Run: `mvn -pl service test -Dtest=MetadataToolingServiceTest`
- [ ] Commit: `test: add MetadataToolingService standard query tests`

---

## Phase 4: Cross-Surface Integration Tests (Feature 2)

### T015 — REST → Bulk → SOQL Workflow

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/integration/CrossSurfaceIntegrationTest.java`

**Steps**:
- [ ] Add test `restCreateThenBulkUpsertThenSoqlQuery()`:
  - POST Account "TestCorp" via REST, get ID
  - Create Bulk upsert job for Account
  - Upload CSV with updated Name for same external ID
  - Close job, wait for completion
  - SOQL query `SELECT Id, Name FROM Account WHERE Name = 'Updated'`
  - Assert query returns updated record
- [ ] Commit: `test: add REST→Bulk→SOQL cross-surface integration test`

### T016 — Full Lifecycle with Reset

**Files**:
- Modify: `service/src/test/java/co/razkevich/sflocalstack/integration/CrossSurfaceIntegrationTest.java`

**Steps**:
- [ ] Add test `fullLifecycleWithResetVerifiesCleanState()`:
  - Create Account via REST
  - Query via SOQL — assert found
  - Describe Account — assert returns field metadata
  - POST /reset
  - Query via SOQL — assert empty
  - Describe Account — assert still returns metadata (describe works without data)
  - GET /api/dashboard/overview — assert totalRecords = 0
- [ ] Run: `mvn -pl service test -Dtest=CrossSurfaceIntegrationTest`
- [ ] Commit: `test: add full lifecycle + reset cross-surface test`

---

## Phase 5: Final Validation

### T017 — Run Full Test Suite and Verify

**Steps**:
- [ ] Run full test suite: `mvn -pl service test`
- [ ] Verify 0 failures
- [ ] Verify test execution time < 60 seconds
- [ ] Count test methods — target: at least 75 total (currently 56)
- [ ] Verify all endpoints listed in gap analysis now have coverage
- [ ] Commit any final fixes: `test: fix any test issues found during full suite run`

---

## Salesforce Parity Verification

- **Reference Org**: Not applicable (test-only feature, no API changes)
- **Parity Method**: N/A
- **Compared Signals**: N/A
- **Mutation Policy**: N/A
- **Accepted Deltas**: N/A

## Complexity Tracking

No constitution violations. Zero new dependencies. Test-only changes.
