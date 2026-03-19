# Tasks: Usable Salesforce Integration-Test Emulator

**Input**: Design documents from `/specs/001-sf-ci-emulator/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`

**Tests**: Tests are REQUIRED. Every feature slice starts with failing backend contract and integration coverage before implementation.

**Organization**: Tasks are grouped by feature so each slice can be implemented end-to-end across backend, frontend, local verification, and `dev20` parity checks.

## Format: `[ID] [P?] [Feature] Description`

- **[P]**: Can run in parallel when dependencies are already complete and files do not overlap
- **[Feature]**: Maps the task to a feature slice from `spec.md`
- Every task includes the exact file path to change or create

## Phase 1: Shared App Scaffold

**Purpose**: Establish the reusable application shell needed by all feature slices.

- [ ] T001 Align frontend build wiring and shared API base configuration in `frontend/package.json`, `frontend/vite.config.ts`, and `frontend/src/main.tsx`
- [ ] T002 [P] Create shared frontend API types, request models, and fetch helpers in `frontend/src/types/index.ts`, `frontend/src/services/api.ts`, and `frontend/src/hooks/`
- [ ] T003 [P] Add backend reset/request-log baseline coverage in `service/src/test/java/co/prodly/sflocalstack/controller/ResetControllerTest.java` and `service/src/test/java/co/prodly/sflocalstack/controller/DashboardControllerTest.java`
- [ ] T004 [P] Add shared request-log and Salesforce-style error primitives in `service/src/main/java/co/prodly/sflocalstack/service/RequestLogService.java`, `service/src/main/java/co/prodly/sflocalstack/interceptor/RequestLoggingInterceptor.java`, and `service/src/main/java/co/prodly/sflocalstack/model/SalesforceError.java`

**Checkpoint**: The app shell is ready for feature-by-feature delivery.

---

## Feature 0: App Scaffold (Priority: P1) 🎯 MVP

**Goal**: Stabilize the Spring Boot + dashboard shell so engineers can inspect requests, reset state, and build the rest of the emulator inside a coherent app.

**Independent Test**: Start the app locally, inspect request log data in the dashboard, trigger reset from the UI/API, and confirm baseline state returns.

### Backend Tests First ⚠️

- [ ] T005 [P] [F0] Add backend shell and reset contract coverage in `service/src/test/java/co/prodly/sflocalstack/controller/ResetControllerTest.java` and `service/src/test/java/co/prodly/sflocalstack/controller/DashboardControllerTest.java`
- [ ] T006 [P] [F0] Add local app-shell integration coverage in `service/src/test/java/co/prodly/sflocalstack/integration/ApplicationShellIntegrationTest.java`

### Backend Implementation

- [ ] T007 [F0] Tighten dashboard/reset backend responses in `service/src/main/java/co/prodly/sflocalstack/controller/ResetController.java` and `service/src/main/java/co/prodly/sflocalstack/controller/DashboardController.java`

### Frontend Tests

- [ ] T008 [P] [F0] Add dashboard shell rendering coverage in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, `frontend/src/components/RequestLog.tsx`, and `frontend/src/components/RequestDetail.tsx`

### Frontend Implementation

- [ ] T009 [F0] Build the shared dashboard shell, navigation, and reset UX in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, `frontend/src/components/RequestLog.tsx`, `frontend/src/components/RequestDetail.tsx`, and `frontend/src/components/ResetButton.tsx`

### Verification

- [ ] T010 [F0] Validate the local dashboard shell and reset workflow using the documented app flow
- [ ] T011 [F0] Run baseline REST parity verification against `dev20` for version discovery and simple list/query flows
- [ ] T012 [F0] Clean up any temporary `dev20` records created during baseline parity verification

**Checkpoint**: Feature 0 is complete across backend, frontend, local verification, and real-Salesforce parity.

---

## Feature 1: REST Core + SOQL (Priority: P2)

**Goal**: Deliver a useful REST + SOQL slice with filtered queries, relationship projection, CRUD, describe behavior, and matching dashboard inspection.

**Independent Test**: Reset the emulator, run CRUD/query/describe flows against seeded data, inspect the same results in the dashboard, and verify supported SOQL behavior.

### Backend Tests First ⚠️

- [ ] T013 [P] [F1] Add SOQL predicate and relationship coverage in `service/src/test/java/co/prodly/sflocalstack/service/SoqlEngineTest.java`
- [ ] T014 [P] [F1] Add REST query and describe contract coverage in `service/src/test/java/co/prodly/sflocalstack/controller/QueryControllerTest.java` and `service/src/test/java/co/prodly/sflocalstack/controller/SObjectControllerTest.java`
- [ ] T015 [P] [F1] Add local REST/SOQL integration coverage in `service/src/test/java/co/prodly/sflocalstack/integration/RestQueryIntegrationTest.java`

### Backend Implementation

- [ ] T016 [P] [F1] Introduce parsed SOQL query and condition models in `service/src/main/java/co/prodly/sflocalstack/service/SoqlQueryModel.java` and `service/src/main/java/co/prodly/sflocalstack/service/SoqlCondition.java`
- [ ] T017 [F1] Replace stub query execution with supported parser/executor logic in `service/src/main/java/co/prodly/sflocalstack/service/SoqlEngine.java`
- [ ] T018 [P] [F1] Add relationship resolution helpers and describe support in `service/src/main/java/co/prodly/sflocalstack/service/OrgStateService.java` and `service/src/main/java/co/prodly/sflocalstack/controller/SObjectController.java`
- [ ] T019 [F1] Expose supported REST query, CRUD, describe, and error responses in `service/src/main/java/co/prodly/sflocalstack/controller/QueryController.java` and `service/src/main/java/co/prodly/sflocalstack/controller/SObjectController.java`

### Frontend Tests

- [ ] T020 [P] [F1] Add query runner and org-state rendering coverage in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, and new REST-focused components under `frontend/src/components/`

### Frontend Implementation

- [ ] T021 [F1] Build dashboard query runner, object browser, and record detail workflows in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, and new REST-focused components under `frontend/src/components/`

### Verification

- [ ] T022 [F1] Validate local CRUD/query/describe workflows through API calls and dashboard interaction
- [ ] T023 [F1] Run REST Core + SOQL parity verification against `dev20`
- [ ] T024 [F1] Clean up temporary `dev20` records created during REST parity verification

**Checkpoint**: Feature 1 is complete across backend, frontend, local verification, and real-Salesforce parity.

---

## Feature 2: REST Upsert + Error Fidelity (Priority: P3)

**Goal**: Add external-ID upsert and supported REST error fidelity, with matching dashboard mutation and error inspection.

**Independent Test**: Reset the emulator, run create/update upsert flows plus supported REST failures, and inspect the resulting state and error envelopes in the dashboard.

### Backend Tests First ⚠️

- [ ] T025 [P] [F2] Add external-ID upsert controller coverage in `service/src/test/java/co/prodly/sflocalstack/controller/SObjectControllerTest.java`
- [ ] T026 [P] [F2] Add concurrent upsert determinism coverage in `service/src/test/java/co/prodly/sflocalstack/integration/ExternalIdUpsertIntegrationTest.java`
- [ ] T027 [P] [F2] Add supported REST error-envelope coverage in `service/src/test/java/co/prodly/sflocalstack/controller/QueryControllerTest.java` and `service/src/test/java/co/prodly/sflocalstack/controller/SObjectControllerTest.java`

### Backend Implementation

- [ ] T028 [F2] Implement synchronized external-ID upsert semantics in `service/src/main/java/co/prodly/sflocalstack/service/OrgStateService.java`
- [ ] T029 [F2] Tighten Salesforce-compatible success and error payload handling in `service/src/main/java/co/prodly/sflocalstack/controller/SObjectController.java`, `service/src/main/java/co/prodly/sflocalstack/controller/QueryController.java`, and `service/src/main/java/co/prodly/sflocalstack/controller/`

### Frontend Tests

- [ ] T030 [P] [F2] Add upsert and error-inspection rendering coverage in `frontend/src/App.tsx` and new REST-mutation components under `frontend/src/components/`

### Frontend Implementation

- [ ] T031 [F2] Build dashboard upsert playground and REST error inspection flows in `frontend/src/App.tsx` and new REST-mutation components under `frontend/src/components/`

### Verification

- [ ] T032 [F2] Validate local upsert and REST error workflows through API calls and dashboard interaction
- [ ] T033 [F2] Run upsert/error parity verification against `dev20`
- [ ] T034 [F2] Clean up temporary `dev20` records created during upsert/error parity verification

**Checkpoint**: Feature 2 is complete across backend, frontend, local verification, and real-Salesforce parity.

---

## Feature 3: Bulk API v2 (Priority: P4)

**Goal**: Support synchronous Bulk API v2 ingest flows and expose them through a dashboard job console.

**Independent Test**: Create a Bulk ingest job, upload CSV, close the job, inspect result endpoints, verify downstream record mutations, and inspect the same job in the dashboard.

### Backend Tests First ⚠️

- [ ] T035 [P] [F3] Add Bulk ingest controller contract coverage in `service/src/test/java/co/prodly/sflocalstack/controller/BulkControllerTest.java`
- [ ] T036 [P] [F3] Add Bulk CSV parsing and row-result coverage in `service/src/test/java/co/prodly/sflocalstack/service/BulkJobServiceTest.java`
- [ ] T037 [P] [F3] Add local Bulk ingest integration coverage in `service/src/test/java/co/prodly/sflocalstack/integration/BulkIngestIntegrationTest.java`

### Backend Implementation

- [ ] T038 [P] [F3] Add Bulk job and row-result models in `service/src/main/java/co/prodly/sflocalstack/model/BulkIngestJob.java` and `service/src/main/java/co/prodly/sflocalstack/model/BulkRowResult.java`
- [ ] T039 [P] [F3] Implement CSV parsing, synchronous processing, and result generation in `service/src/main/java/co/prodly/sflocalstack/service/BulkJobService.java`
- [ ] T040 [F3] Add Bulk API v2 endpoints and reset wiring in `service/src/main/java/co/prodly/sflocalstack/controller/BulkController.java`, `service/src/main/java/co/prodly/sflocalstack/service/OrgStateService.java`, and `service/src/main/java/co/prodly/sflocalstack/controller/ResetController.java`

### Frontend Tests

- [ ] T041 [P] [F3] Add Bulk job monitoring rendering coverage in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, and new Bulk-focused components under `frontend/src/components/`

### Frontend Implementation

- [ ] T042 [F3] Build dashboard Bulk job console, status summaries, and result panes in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, and new Bulk-focused components under `frontend/src/components/`

### Verification

- [ ] T043 [F3] Validate local Bulk ingest workflows through API calls and dashboard interaction
- [ ] T044 [F3] Run Bulk API v2 parity verification against `dev20`
- [ ] T045 [F3] Clean up temporary `dev20` records created during Bulk parity verification

**Checkpoint**: Feature 3 is complete across backend, frontend, local verification, and real-Salesforce parity.

---

## Feature 4: Metadata SOAP (Priority: P5)

**Goal**: Support basic Metadata SOAP workflows and expose them through a dashboard metadata explorer.

**Independent Test**: Submit local deploy/status/cancel/list/describe requests, inspect response envelopes, and verify the same workflows through the dashboard.

### Backend Tests First ⚠️

- [ ] T046 [P] [F4] Add SOAP controller contract coverage in `service/src/test/java/co/prodly/sflocalstack/controller/MetadataControllerTest.java`
- [ ] T047 [P] [F4] Add SOAP parsing and response rendering coverage in `service/src/test/java/co/prodly/sflocalstack/service/MetadataServiceTest.java`
- [ ] T048 [P] [F4] Add local Metadata integration coverage in `service/src/test/java/co/prodly/sflocalstack/integration/MetadataIntegrationTest.java`

### Backend Implementation

- [ ] T049 [P] [F4] Add metadata deploy and catalog models in `service/src/main/java/co/prodly/sflocalstack/model/MetadataDeployJob.java` and `service/src/main/java/co/prodly/sflocalstack/model/MetadataCatalogEntry.java`
- [ ] T050 [P] [F4] Implement namespace-tolerant SOAP parsing and rendering helpers in `service/src/main/java/co/prodly/sflocalstack/service/MetadataSoapParser.java` and `service/src/main/java/co/prodly/sflocalstack/service/MetadataSoapRenderer.java`
- [ ] T051 [F4] Implement deterministic Metadata behavior and SOAP routing in `service/src/main/java/co/prodly/sflocalstack/service/MetadataService.java` and `service/src/main/java/co/prodly/sflocalstack/controller/MetadataController.java`

### Frontend Tests

- [ ] T052 [P] [F4] Add Metadata workflow rendering coverage in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, and new Metadata-focused components under `frontend/src/components/`

### Frontend Implementation

- [ ] T053 [F4] Build dashboard Metadata workflow explorer, deploy/status panels, and type browser in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, and new Metadata-focused components under `frontend/src/components/`

### Verification

- [ ] T054 [F4] Validate local Metadata SOAP workflows through API calls and dashboard interaction
- [ ] T055 [F4] Run Metadata parity verification against `dev20`
- [ ] T056 [F4] Clean up temporary `dev20` records created during Metadata parity verification

**Checkpoint**: Feature 4 is complete across backend, frontend, local verification, and real-Salesforce parity.

---

## Feature 5: App Polish (Priority: P6)

**Goal**: Unify the UX and tighten cross-surface fidelity after the main API slices are complete.

**Independent Test**: Walk through the complete local REST, Bulk, and Metadata flows after reset and confirm the dashboard provides a coherent inspection experience.

### Tests First ⚠️

- [ ] T057 [P] [F5] Add cross-surface integration coverage in `service/src/test/java/co/prodly/sflocalstack/integration/`
- [ ] T058 [P] [F5] Add dashboard navigation and inspection coverage in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, and shared UI components

### Implementation

- [ ] T059 [F5] Tighten shared backend observability and cross-surface consistency in `service/src/main/java/co/prodly/sflocalstack/service/`
- [ ] T060 [F5] Polish shared dashboard navigation, empty states, and inspection UX in `frontend/src/App.tsx`, `frontend/src/components/Sidebar.tsx`, and shared UI components

### Verification

- [ ] T061 [F5] Validate the complete local app walkthrough across REST, Bulk, and Metadata slices
- [ ] T062 [F5] Re-run the approved parity suite against `dev20` and record accepted deltas
- [ ] T063 [F5] Confirm temporary `dev20` parity data has been cleaned up

**Checkpoint**: Feature 5 is complete across backend, frontend, local verification, and real-Salesforce parity.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Shared App Scaffold**: Starts immediately
- **Feature 0**: Depends on the shared scaffold
- **Feature 1**: Depends on Feature 0
- **Feature 2**: Depends on Feature 1
- **Feature 3**: Depends on Feature 2's REST mutation semantics
- **Feature 4**: Depends on the shared app shell and observability primitives
- **Feature 5**: Depends on all supported feature slices being complete

### Within Each Feature

- Backend tests fail first
- Backend implementation lands before the matching frontend feature is finalized
- Frontend tests and implementation move alongside the backend slice
- Local integration verification happens before parity verification
- `dev20` cleanup happens immediately after parity verification

### Parallel Opportunities

- `T002`, `T003`, and `T004` can run in parallel after `T001`
- Backend and frontend test tasks within the same feature can run in parallel once the slice scope is fixed
- Frontend implementation can overlap with late backend wiring once response contracts stabilize
- Verification remains sequential within each feature: local flow, parity check, cleanup

## Implementation Strategy

### MVP First

1. Complete the shared app scaffold
2. Deliver Feature 0 end-to-end
3. Deliver Feature 1 end-to-end
4. Validate locally and against `dev20`
5. Use that app as the base for later API surfaces

### Incremental Delivery

1. Build one feature slice at a time
2. Each slice includes backend, frontend, tests, local verification, and `dev20` parity verification
3. Record accepted deltas immediately so the next slice builds on known behavior

### Parallel Team Strategy

1. One developer stabilizes shared shell and backend contracts
2. Another developer builds the matching frontend once interfaces stabilize
3. A verification pass closes each slice before the next one begins

## Notes

- Feature completion requires FE + BE + local verification + parity verification
- Docker, CI automation, deployment, and packaging are intentionally deferred
- Reset behavior remains in scope because it is part of the working app, not just test infrastructure
