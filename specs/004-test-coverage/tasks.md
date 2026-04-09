# Tasks: Test Coverage Hardening

**Input**: Design documents from `/specs/004-test-coverage/`
**Prerequisites**: plan.md (required), spec.md (required), research.md

**Tests**: Tests ARE the deliverable for this feature. Every task produces test code.

**Organization**: Tasks grouped by feature slice — test infrastructure first, then controller tests, service tests, cross-surface tests, and final validation.

## Format: `[ID] [P?] [Feature] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Feature]**: Which feature slice this task belongs to (F0, F1, F2, F3)

## Phase 1: Test Infrastructure (Feature 3 — done first, used by all)

**Purpose**: Shared test helpers to reduce boilerplate across all new tests.

- [ ] T001 [P] [F3] Create TestDataFactory helper in `service/src/test/java/co/razkevich/sflocalstack/helpers/TestDataFactory.java` — static methods: `createAccountJson(name)`, `createContactJson(first, last)`, `createLeadJson(company, last)`, `createAccountViaApi(MockMvc)`, `createAccountViaApi(MockMvc, name)`, `resetOrg(MockMvc)`
- [ ] T002 [P] [F3] Create SoapTestHelper in `service/src/test/java/co/razkevich/sflocalstack/helpers/SoapTestHelper.java` — static methods: `envelope(op, body)`, `describeMetadata()`, `listMetadata(type)`, `readMetadata(type, fullNames...)`, `deploy(zipBase64)`, `checkDeployStatus(asyncId)`, `retrieve(type, members...)`, `checkRetrieveStatus(asyncId)`, `cancelDeploy(asyncId)`
- [ ] T003 [P] [F3] Create AssertionHelpers in `service/src/test/java/co/razkevich/sflocalstack/helpers/AssertionHelpers.java` — static methods: `assertCreatedResponse(MvcResult)`, `assertSalesforceError(MvcResult, status, errorCode)`, `assertQueryResult(MvcResult, expectedSize)`, `assertRecordShape(json, objectType)`, `extractId(MvcResult)`
- [ ] T004 Verify test infrastructure compiles: run `mvn -pl service test-compile` and fix any issues
- [ ] T005 Commit test infrastructure: `git commit -m "test: add TestDataFactory, SoapTestHelper, AssertionHelpers"`

**Checkpoint**: Test helpers compile and are importable by all test classes.

---

## Phase 2: Controller Integration Tests (Feature 0) — P1

**Goal**: Fill coverage gaps for all 10 controllers. Each endpoint gets happy-path + error-path tests.

**Independent Test**: `mvn -pl service test -Dtest="*ControllerTest"` passes.

- [ ] T006 [P] [F0] Add `getRecordByIdReturnsFullRecord()` and `getRecordByIdReturns404ForMissingRecord()` to `service/src/test/java/co/razkevich/sflocalstack/controller/SObjectControllerTest.java` — create Account via POST, GET by ID, assert 200 with attributes; GET missing ID, assert 404 with SF error shape
- [ ] T007 [P] [F0] Add `deleteRecordReturns204()` and `deleteNonexistentRecordReturns404()` to `service/src/test/java/co/razkevich/sflocalstack/controller/SObjectControllerTest.java` — create Account, DELETE by ID, assert 204; DELETE missing, assert 404
- [ ] T008 [P] [F0] Add `failedResultsReturnsCsvFormat()` and `unprocessedRecordsReturnsCsvFormat()` and `resultsOnOpenJobReturns400()` to `service/src/test/java/co/razkevich/sflocalstack/controller/BulkControllerTest.java` — test GET failedResults/unprocessedrecords CSV endpoints and error on open job
- [ ] T009 [P] [F0] Add `sseEventStreamReturnsTextEventStream()` to `service/src/test/java/co/razkevich/sflocalstack/controller/DashboardControllerTest.java` — GET /api/dashboard/events with Accept: text/event-stream, assert content type
- [ ] T010 [P] [F0] Add `toolingDescribeReturnsFieldMetadata()` and `toolingDescribeUnknownObjectReturns404()` to `service/src/test/java/co/razkevich/sflocalstack/controller/MetadataRestControllerTest.java` — GET tooling/sobjects/{obj}/describe
- [ ] T011 [P] [F0] Add `dataAliasReturnsVersionList()`, `dataAliasVersionReturnsResourceMap()`, `servicesDataNoSlashReturnsVersionList()` to `service/src/test/java/co/razkevich/sflocalstack/controller/VersionControllerTest.java` — test /data alias routes
- [ ] T012 [P] [F0] Add `queryWithTrailingSlashWorks()` to `service/src/test/java/co/razkevich/sflocalstack/controller/QueryControllerTest.java` — GET /query/ with trailing slash
- [ ] T013 [F0] Run all controller tests: `mvn -pl service test -Dtest="*ControllerTest"` — verify 0 failures
- [ ] T014 [F0] Commit controller tests: `git commit -m "test: add integration tests for all untested controller endpoints"`

**Checkpoint**: All 32 controller endpoints have at least one integration test.

---

## Phase 3: Service Unit Tests (Feature 1) — P1

**Goal**: Fill coverage gaps for all 9 services. Every public method has at least one unit test.

**Independent Test**: `mvn -pl service test -Dtest="*ServiceTest"` passes.

- [ ] T015 [P] [F1] Create `service/src/test/java/co/razkevich/sflocalstack/service/OrgStateServiceTest.java` with tests: `findByIdReturnsCreatedRecord()`, `findByIdReturnsEmptyForMissingId()`, `findByTypeAndIdReturnsMatchingRecord()`, `findByTypeAndIdReturnsEmptyForWrongType()`, `findAllReturnsAllRecords()`, `deleteRemovesRecord()`, `deleteReturnsFalseForMissingId()`, `resetClearsAllRecords()`, `fromJsonParsesValidJson()` — use `@SpringBootTest` with real OrgStateService
- [ ] T016 [P] [F1] Add `failedResultsReturnsCsvWithErrors()` and `unprocessedResultsReturnsEmptyOnSuccess()` to `service/src/test/java/co/razkevich/sflocalstack/service/BulkJobServiceTest.java` — test failedResults/unprocessedResults CSV generation
- [ ] T017 [P] [F1] Add `listResourcesReturnsAllCreatedResources()` and `resetClearsAllResources()` to `service/src/test/java/co/razkevich/sflocalstack/service/MetadataServiceTest.java` — test listResources() and reset()
- [ ] T018 [P] [F1] Create `service/src/test/java/co/razkevich/sflocalstack/service/RequestLogServiceTest.java` with tests: `logAddsEntryToRecentList()`, `resetClearsAllEntries()`, `newEmitterReturnsNonNull()` — unit test with plain instantiation
- [ ] T019 [P] [F1] Create `service/src/test/java/co/razkevich/sflocalstack/service/MetadataToolingServiceTest.java` with test: `executeStandardMetadataQueryReturnsResults()`, `executeStandardMetadataQueryOnEmptyReturnsEmpty()` — test standard metadata query path
- [ ] T020 [F1] Run all service tests: `mvn -pl service test -Dtest="*ServiceTest"` — verify 0 failures
- [ ] T021 [F1] Commit service tests: `git commit -m "test: add unit tests for all untested service methods"`

**Checkpoint**: All 52 service public methods have at least one test.

---

## Phase 4: Cross-Surface Integration Tests (Feature 2) — P2

**Goal**: Add 2 multi-surface workflow tests verifying end-to-end behavior across API surfaces.

**Independent Test**: `mvn -pl service test -Dtest="CrossSurfaceIntegrationTest"` passes.

- [ ] T022 [P] [F2] Add `restCreateThenBulkUpsertThenSoqlQuery()` to `service/src/test/java/co/razkevich/sflocalstack/integration/CrossSurfaceIntegrationTest.java` — create Account via REST, Bulk upsert to update, SOQL query to verify updated data
- [ ] T023 [P] [F2] Add `fullLifecycleWithResetVerifiesCleanState()` to `service/src/test/java/co/razkevich/sflocalstack/integration/CrossSurfaceIntegrationTest.java` — REST create → SOQL query → Describe → Reset → verify empty → verify describe still works → verify dashboard overview
- [ ] T024 [F2] Run cross-surface tests: `mvn -pl service test -Dtest="CrossSurfaceIntegrationTest"` — verify 0 failures
- [ ] T025 [F2] Commit cross-surface tests: `git commit -m "test: add cross-surface integration tests for REST/Bulk/SOQL/Reset workflows"`

**Checkpoint**: 4+ multi-API workflows tested (2 existing + 2 new).

---

## Phase 5: Final Validation

**Purpose**: Verify full suite passes and coverage targets met.

- [ ] T026 Run full test suite: `mvn -pl service test` — verify 0 failures, execution time < 60 seconds
- [ ] T027 Count total test methods — target: 75+ (up from 56)
- [ ] T028 Verify all endpoints from gap analysis have coverage — spot-check GET by ID, DELETE, failedResults, SSE, tooling describe
- [ ] T029 Final commit if any fixes needed: `git commit -m "test: final test suite fixes"`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Test Infrastructure)**: No dependencies — start immediately
- **Phase 2 (Controller Tests)**: Depends on Phase 1 completion (uses helpers)
- **Phase 3 (Service Tests)**: Depends on Phase 1 completion (uses helpers). Can run in parallel with Phase 2.
- **Phase 4 (Cross-Surface)**: Depends on Phase 1. Can run in parallel with Phase 2 and 3.
- **Phase 5 (Validation)**: Depends on all prior phases

### Within Each Phase

- Tasks marked [P] can run in parallel (different files, no dependencies)
- Run/verify tasks (T004, T013, T020, T024, T026) are sequential within their phase
- Commit tasks are sequential after their verify task

### Parallel Opportunities

- T001, T002, T003 can all run in parallel (different helper files)
- T006–T012 can all run in parallel (different controller test files)
- T015–T019 can all run in parallel (different service test files)
- T022, T023 can run in parallel (same file but independent test methods)
- Phases 2, 3, 4 can overlap once Phase 1 is complete

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 (test helpers)
2. Complete Phase 2 (controller tests) — highest coverage impact
3. Run and validate

### Incremental Delivery

1. Each phase is independently committable
2. Each commit message follows: `test: <what was added>`
3. Full suite validation at end

---

## Notes

- [P] tasks = different files, no dependencies
- [Feature] label maps task to spec feature slice
- All tests use H2 in-memory (default test profile)
- Reset org via POST /reset between tests that mutate state
- No source code modifications — test-only changes
