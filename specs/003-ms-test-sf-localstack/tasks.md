# Tasks: Migrate metadata-service Tests from WireMock to sf-localstack

**Input**: Design documents from `/specs/003-ms-test-sf-localstack/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/soap-metadata-api.md, contracts/rest-soql-api.md

**Tests**: Tests are REQUIRED before each implementation task. Every changed sf-localstack surface starts with failing contract/integration tests.

**Organization**: Tasks grouped by feature slice. sf-localstack enhancements (F0–F2) must be complete before metadata-service changes (F3–F4).

## Format: `[ID] [P?] [Feature] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Feature]**: Feature slice (F0–F4)
- Exact file paths included in all descriptions

---

## Phase 1: Setup

**Purpose**: Confirm test infrastructure baseline in sf-localstack before starting work.

- [ ] T001 Verify `mvn test -pl service` passes green in `sf-localstack` on branch `003-ms-test-sf-localstack`
- [ ] T002 Verify `mvn test -pl service` passes green in `metadata-service` on current branch (WireMock baseline)

**Checkpoint**: Both repos have a known-green test baseline before any changes.

---

## Feature 0: SOAP Version Routing + Response Shape Fixes (Priority: P1)

**Goal**: Make sf-localstack accept `/services/Soap/m/66.0` requests and return response shapes that exactly match the `__files/*.xml` fixtures used by metadata-service tests.

**Independent Test**: `POST /services/Soap/m/66.0` with a `describeMetadata` body returns HTTP 200 with a valid SOAP envelope containing `<suffix>cls</suffix>` and `<partialSaveAllowed>true</partialSaveAllowed>`.

### Tests First

- [ ] T003 [P] [F0] Add `MetadataControllerVersionTest` in `service/src/test/java/co/prodly/sflocalstack/controller/MetadataControllerVersionTest.java` — assert `POST /services/Soap/m/66.0` routes `describeMetadata`, `listMetadata`, `readMetadata`, and `cancelDeploy` identically to `/services/Soap/m/60.0` (4 test methods, all expected to fail before T005)
- [ ] T004 [P] [F0] Add `MetadataSoapRendererShapeTest` in `service/src/test/java/co/prodly/sflocalstack/service/MetadataSoapRendererShapeTest.java` — assert `renderDescribeMetadata` output contains `<suffix>`, `<partialSaveAllowed>true</partialSaveAllowed>`; assert `renderReadMetadata` for `StandardValueSet` contains `<sorted>false</sorted>`; assert `renderCancelDeploy` produces `<done>false</done><id>` envelope (expected to fail before T006–T008)

### Implementation

- [ ] T005 [F0] Change `@RequestMapping` in `service/src/main/java/co/prodly/sflocalstack/controller/MetadataController.java` from `/services/Soap/m/60.0` to `/services/Soap/m/{version}` to accept all API versions
- [ ] T006 [P] [F0] Fix `renderDescribeMetadata` in `service/src/main/java/co/prodly/sflocalstack/service/MetadataSoapRenderer.java` — add `<suffix>%s</suffix>` to the `metadataObjects` template; change `partialSaveAllowed` to `true`; verify `MetadataCatalogEntry` record has `suffix` field populated in seed
- [ ] T007 [P] [F0] Fix `renderReadMetadata` for `StandardValueSet` in `service/src/main/java/co/prodly/sflocalstack/service/MetadataSoapRenderer.java` — add `<sorted>false</sorted>` between `<fullName>` and `<standardValue>` in the `StandardValueSet` branch of `renderReadRecord`
- [ ] T008 [P] [F0] Fix `renderCancelDeploy` in `service/src/main/java/co/prodly/sflocalstack/service/MetadataSoapRenderer.java` — change body to `<result><done>false</done><id>%s</id></result>` to match `canceldeployresult-response.xml` (remove `<success>` tag)

### Verification

- [ ] T009 [F0] Run `mvn test -pl service` in `sf-localstack` — confirm `MetadataControllerVersionTest` and `MetadataSoapRendererShapeTest` pass green; confirm no existing tests regressed

**Checkpoint**: sf-localstack accepts v66 SOAP requests and all response shapes match the `__files/*.xml` fixtures field-by-field.

---

## Feature 1: Seed Data — PDRI Objects + StaticResource + Organization Fix (Priority: P1)

**Goal**: Seed sf-localstack with all PDRI custom objects and StaticResource records needed by metadata-service tests, and fix the Organization seed to match `organization-response.json`.

**Independent Test**: `GET /services/data/v60.0/query?q=SELECT+Id,PDRI__Instance_URL__c+FROM+PDRI__Connection__c` returns a record with `Id: "a0D5e00000Q9h43EAB"` after container start and reset.

### Tests First

- [ ] T010 [P] [F1] Add `SeedDataTest` in `service/src/test/java/co/prodly/sflocalstack/controller/SeedDataTest.java` — assert after `POST /reset`, SOQL queries for `PDRI__Connection__c`, `PDRI__ComparisonViewRule__c`, `StaticResource` (via Tooling path once T019 is done), and `Organization` each return records with IDs and field values matching the fixture files (expected to fail before T012–T015)
- [ ] T011 [P] [F1] Add `ResetOverrideTest` in `service/src/test/java/co/prodly/sflocalstack/controller/ResetOverrideTest.java` — assert `POST /reset` with body `{"seedOverrides":{"PDRI__Connection__c":{"PDRI__Instance_URL__c":"http://override-url"}}}` causes subsequent SOQL query for `PDRI__Connection__c` to return `PDRI__Instance_URL__c: "http://override-url"` (expected to fail before T016)

### Implementation

- [ ] T012 [F1] Update `service/src/main/resources/seed/default-seed.yml` — add `PDRI__Connection__c` record (Id: `a0D5e00000Q9h43EAB`, fields per `data-model.md`); add `PDRI__ComparisonView__c` record (Id: `a1QDn000002BPrXMAW`); add 5 `PDRI__ComparisonViewRule__c` records with IDs and fields from `data-model.md`; update `Organization` record to set `IsSandbox: true`, `OrganizationType: "abcd"`, `TrialExpirationDate: "some-date"`
- [ ] T013 [P] [F1] Update `service/src/main/resources/seed/default-seed.yml` — add 2 `StaticResource` records: `{Name: "SmallResource", BodyLength: 1}` and `{Name: "BigResource", BodyLength: 200000000}`
- [ ] T014 [P] [F1] Verify sf-localstack's `SeedService` / `SObjectRepository` handles arbitrary custom object types (PDRI__ prefix) without schema errors — add type registration in `service/src/main/java/co/prodly/sflocalstack/service/SeedService.java` (or equivalent) if needed
- [ ] T015 [P] [F1] Update `DashboardControllerTest` and `ResetControllerTest` in `service/src/test/java/co/prodly/sflocalstack/controller/` to reflect the new `totalRecords` count after adding PDRI and StaticResource seed records
- [ ] T016 [F1] Enhance `POST /reset` in `service/src/main/java/co/prodly/sflocalstack/controller/ResetController.java` to accept optional JSON body `{"seedOverrides": {"<ObjectType>": {"<field>": "<value>"}}}` and apply field-level overrides after the base seed restore; update `service/src/main/java/co/prodly/sflocalstack/service/SeedService.java` (or `ResetService`) to apply the overrides

### Verification

- [ ] T017 [F1] Run `mvn test -pl service` in `sf-localstack` — confirm `SeedDataTest` and `ResetOverrideTest` pass; confirm `DashboardControllerTest` and `ResetControllerTest` still pass with updated counts

**Checkpoint**: All PDRI objects and StaticResource records are seeded and survive reset; the reset override mechanism works for dynamic URL injection.

---

## Feature 2: Tooling API Endpoint (Priority: P1)

**Goal**: Add `GET /services/data/{version}/tooling/query/` to sf-localstack so that `SELECT BodyLength FROM StaticResource WHERE Name IN (...)` queries return the seeded StaticResource records.

**Independent Test**: `GET /services/data/v66.0/tooling/query/?q=SELECT+BodyLength+FROM+StaticResource+WHERE+Name+IN+('SmallResource')` returns `{"totalSize":1,"done":true,"records":[{"BodyLength":1}]}`.

### Tests First

- [ ] T018 [P] [F2] Add `ToolingControllerTest` in `service/src/test/java/co/prodly/sflocalstack/controller/ToolingControllerTest.java` — assert small resource query returns `BodyLength: 1`; assert big resource query returns `BodyLength: 200000000`; assert unknown resource returns `totalSize: 0, records: []`; assert the endpoint is at `/services/data/v66.0/tooling/query/` (expected to fail before T019)

### Implementation

- [ ] T019 [F2] Create `service/src/main/java/co/prodly/sflocalstack/controller/ToolingController.java` — map `GET /services/data/{version}/tooling/query/` with param `q`; parse `WHERE Name IN (...)` clause to extract resource names; query `StaticResource` from the sObject store; return standard SOQL response envelope `{"totalSize":N,"done":true,"records":[{"attributes":{"type":"StaticResource","url":"..."},"BodyLength":N}]}`

### Verification

- [ ] T020 [F2] Run `mvn test -pl service` in `sf-localstack` — confirm `ToolingControllerTest` passes; run `SeedDataTest` Tooling assertions that were skipped in F1

**Checkpoint**: Tooling API returns StaticResource BodyLength for both small and large records, matching the `__files/static-resources-*.json` fixture shapes.

---

## Feature 3: metadata-service Testcontainers Base Class (Priority: P1)

**Goal**: Create `AbstractSfLocalstackTest` that starts sf-localstack via Testcontainers, injects its URL as the Salesforce instance base URL, and resets state before each test.

**Independent Test**: A trivial test class extending `AbstractSfLocalstackTest` starts the Spring context, calls `GET /actuator/health` on the sf-localstack container, receives HTTP 200 with `{"status":"UP"}`, and no `Connection refused` error occurs. No WireMock port 8090 is bound.

### Tests First

- [ ] T021 [F3] Add `SfLocalstackContainerStartTest` in `metadata-service/service/src/test/java/co/prodly/metadata/shared/SfLocalstackContainerStartTest.java` — extend `AbstractSfLocalstackTest` (not yet created); assert container health endpoint returns 200; assert the `salesforce.instance.url` Spring property is set to the container's mapped URL (expected to fail until T022 is implemented)

### Implementation

- [ ] T022 [F3] Create `metadata-service/service/src/test/java/co/prodly/metadata/shared/AbstractSfLocalstackTest.java` — extend `AbstractContainerTest`; declare `static final GenericContainer<?> SF_LOCALSTACK` built from sf-localstack local `Dockerfile` (`ImageFromDockerfile` with `.withDockerfile(...)` or using `razkevich/sf-localstack:main-<sha>` Docker Hub image); expose port 8080; add `Wait.forHttp("/actuator/health").forStatusCode(200)`; `@DynamicPropertySource` registers `salesforce.instance.url` (or the exact property key used by metadata-service's Salesforce client) as `http://<host>:<mappedPort>`; `@BeforeEach` calls `POST /reset` with `{"seedOverrides":{"PDRI__Connection__c":{"PDRI__Instance_URL__c":"http://<host>:<mappedPort>"}}}` to inject dynamic URL

### Verification

- [ ] T023 [F3] Run `SfLocalstackContainerStartTest` in metadata-service — confirm container starts, health check passes, property is injected, no WireMock is started

**Checkpoint**: sf-localstack starts from Testcontainers, health-gated, URL-injected, reset before each test. Port 8090 is never bound.

---

## Feature 4: metadata-service Mocks.java Cleanup (Priority: P1)

**Goal**: Remove all Salesforce WireMock stubs from `Mocks.java`, update all test classes to extend `AbstractSfLocalstackTest`, and confirm the full test suite passes green with zero WireMock Salesforce stubs.

**Independent Test**: `grep -r "WireMockServer\|mockDescribeMetadata\|mockListMetadata\|mockReadMetadata\|mockComparisonView\|mockOrganization\|mockFindConnection\|mockFindControl\|mockFindVc\|mockSmallStatic\|mockBigStatic\|mockCancelJob\|mockCheckUsing\|port 8090" metadata-service/service/src/test` returns no matches. `mvn test -pl service` in metadata-service passes green.

### Implementation

- [ ] T024 [F4] Identify all test classes in `metadata-service/service/src/test/java/` that extend `AbstractContainerWithWiremockTest` — list their file paths and the `Mocks.*()` methods they call
- [ ] T025 [P] [F4] Update each test class identified in T024 to extend `AbstractSfLocalstackTest` instead of `AbstractContainerWithWiremockTest` — remove `@BeforeEach` WireMock setup calls and all `Mocks.mock*()` call sites for Salesforce stubs; retain `Mocks.mockAppOpsClientSettings()` calls
- [ ] T026 [P] [F4] Remove all Salesforce stub methods from `metadata-service/service/src/test/java/co/prodly/metadata/shared/Mocks.java`: `mockDescribeMetadata`, `mockReadMetadataPicklist`, `mockReadMetadataStandardValueSet`, `mockListMetadata`, `mockComparisonViewRules`, `mockDefaultComparisonViewRules`, `mockFindConnectionByOrgId`, `mockFindControlConnection`, `mockFindVcConnection`, `mockSmallStaticResourceToolingQuery`, `mockBigStaticResourceToolingQuery`, `mockCancelJobResponse`, `mockCheckUsingVcsRepo`; retain `mockAppOpsClientSettings`
- [ ] T027 [P] [F4] Delete or empty `metadata-service/service/src/test/java/co/prodly/metadata/shared/AbstractContainerWithWiremockTest.java` — if deleting, remove any import references; if emptying, convert to a deprecated stub that logs a warning
- [ ] T028 [F4] Remove WireMock dependencies from `metadata-service/service/pom.xml` test scope if they are no longer used anywhere (check for remaining WireMock references first with `grep -r "wiremock" service/src/test`)

### Verification

- [ ] T029 [F4] Run `grep -r "WireMockServer\|wiremock\|port 8090" metadata-service/service/src/test` — confirm zero Salesforce-related WireMock references remain (only `mockAppOpsClientSettings` imports if WireMock is still used there)
- [ ] T030 [F4] Run `mvn test -pl service` in `metadata-service` — confirm full test suite passes green with zero failures; confirm wall-clock time is within 30 seconds of WireMock baseline

**Checkpoint**: Zero WireMock stubs for Salesforce endpoints. Full metadata-service test suite passes at the same rate as the WireMock baseline.

---

## Final Polish

**Purpose**: Cross-cutting cleanup after all feature slices are complete.

- [ ] T031 [P] Update `metadata-service` README or test docs to document the new `AbstractSfLocalstackTest` base class and the requirement for Docker to be running
- [ ] T032 [P] Update `sf-localstack` README roadmap to mark "JUnit extension for hermetic integration tests" as complete (now delivered via Testcontainers pattern)
- [ ] T033 Run full test suite in both repos one final time to confirm joint green state: `mvn test -pl service` in sf-localstack and `mvn test -pl service` in metadata-service

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup baseline)
  └─> F0 (SOAP version routing + shape fixes) — sf-localstack only
  └─> F1 (Seed data) — sf-localstack only        [can run parallel with F0]
  └─> F2 (Tooling API) — sf-localstack only      [can run parallel with F0, depends on F1 seed]
        └─> F3 (Testcontainers base class) — metadata-service [requires F0+F1+F2 green]
              └─> F4 (Mocks cleanup) — metadata-service [requires F3 working]
                    └─> Final Polish
```

### Within Each Feature Slice

- Tests MUST be written and run (expect failure) BEFORE implementation
- Implementation tasks within a slice marked [P] can run in parallel
- Verification (mvn test) always runs after all implementation tasks in the slice

### Parallel Opportunities

- **F0 and F1** can start in parallel (different files: controller vs. seed YAML)
- **F2** can start after F1 seed data is written (ToolingController needs StaticResource in seed)
- **T006, T007, T008** (F0 renderer fixes) are independent files — fully parallel
- **T012 and T013** (seed YAML additions) are the same file — sequential
- **T025, T026, T027** (F4 cleanup tasks) touch different files — parallel
- **T031 and T032** (polish docs) are fully independent

---

## Implementation Strategy

### MVP Scope

Complete F0 + F1 + F2 (sf-localstack enhancements) first — these are independently testable within sf-localstack. Then F3 (base class) + F4 (cleanup) — these complete the migration in metadata-service.

### Incremental Delivery

1. **Day 1**: T001–T002 (baseline), T003–T009 (F0 SOAP fixes)
2. **Day 2**: T010–T017 (F1 seed data + reset override)
3. **Day 2–3**: T018–T020 (F2 Tooling API)
4. **Day 3**: T021–T023 (F3 Testcontainers base class)
5. **Day 4**: T024–T033 (F4 cleanup + polish)

### Parallel Team Strategy

- Developer A: F0 (SOAP shape fixes) + F2 (Tooling API)
- Developer B: F1 (seed data + reset override)
- Both join for F3 + F4 once sf-localstack enhancements are green

---

## Notes

- [P] tasks = different files, no incomplete task dependencies
- [Feature] label maps to plan.md feature iteration
- All sf-localstack test changes use `service/src/test/java/co/prodly/sflocalstack/`
- All metadata-service changes are in `~/code/metadata-service/service/src/test/java/co/prodly/metadata/`
- `mockAppOpsClientSettings` in `Mocks.java` is NOT a Salesforce stub — must be retained
- Tooling API path is `/services/data/{version}/tooling/query/` (note trailing slash — matches WireMock pattern)
- `PDRI__Connection__c.PDRI__Instance_URL__c` MUST be the Testcontainers-mapped URL, not `http://localhost:8090`
