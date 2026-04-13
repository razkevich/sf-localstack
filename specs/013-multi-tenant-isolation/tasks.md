# Tasks: Multi-Tenant Org Isolation

**Input**: Design documents from `/specs/013-multi-tenant-isolation/`

## Phase 1: Scaffold — Org Entity & JWT Changes

- [ ] T001 Add `orgId` field to User model in `service/src/main/java/co/razkevich/sflocalstack/auth/model/User.java`
- [ ] T002 [P] Update UserStore interface, FileBasedUserStore, and InMemoryUserStore to support orgId in `service/src/main/java/co/razkevich/sflocalstack/auth/store/`
- [ ] T003 [P] Add orgId claim to JWT generation and extraction in JwtService in `service/src/main/java/co/razkevich/sflocalstack/auth/service/JwtService.java`
- [ ] T004 Extract orgId from JWT in JwtAuthFilter and set as request attribute in `service/src/main/java/co/razkevich/sflocalstack/auth/filter/JwtAuthFilter.java`
- [ ] T005 Generate unique org ID on user registration in AuthController — format `00D` + 15 alphanumeric chars in `service/src/main/java/co/razkevich/sflocalstack/auth/controller/AuthController.java`
- [ ] T006 Update OAuthController to use real orgId from user/JWT instead of hardcoded `00D000000000001AAA` in `service/src/main/java/co/razkevich/sflocalstack/controller/OAuthController.java`

**Checkpoint**: Users get unique org IDs. Tokens carry orgId. OAuth responses reflect real org.

---

## Feature 1: sObject Data Isolation

- [ ] T007 Add `orgId` column to SObjectRecord entity with default value for backward compat in `service/src/main/java/co/razkevich/sflocalstack/data/model/SObjectRecord.java`
- [ ] T008 Add org-filtered query methods to SObjectRecordRepository in `service/src/main/java/co/razkevich/sflocalstack/data/repository/SObjectRecordRepository.java`
- [ ] T009 Update OrgStateService — all find/create/update/delete methods accept and filter by orgId in `service/src/main/java/co/razkevich/sflocalstack/data/service/OrgStateService.java`
- [ ] T010 Update SObjectController to pass orgId from request attributes to OrgStateService in `service/src/main/java/co/razkevich/sflocalstack/data/controller/SObjectController.java`
- [ ] T011 Update QueryController to pass orgId for SOQL queries in `service/src/main/java/co/razkevich/sflocalstack/data/controller/QueryController.java`
- [ ] T012 Update ResetController to reset only the user's org in `service/src/main/java/co/razkevich/sflocalstack/controller/ResetController.java`
- [ ] T013 Seed default data per org on registration using OrgStateService in AuthController

**Checkpoint**: sObject CRUD is org-isolated. Reset is per-org.

---

## Feature 2: Bulk & Metadata Isolation

- [ ] T014 [P] Add `orgId` to BulkIngestJob entity and update BulkIngestJobRepository in `service/src/main/java/co/razkevich/sflocalstack/bulk/`
- [ ] T015 [P] Add `orgId` to MetadataResourceEntity, MetadataDeployJobEntity, MetadataRetrieveJobEntity and update repositories in `service/src/main/java/co/razkevich/sflocalstack/metadata/`
- [ ] T016 Update BulkJobService to filter all operations by orgId in `service/src/main/java/co/razkevich/sflocalstack/bulk/service/BulkJobService.java`
- [ ] T017 Update MetadataService to filter all operations by orgId in `service/src/main/java/co/razkevich/sflocalstack/metadata/service/MetadataService.java`
- [ ] T018 Update all Bulk and Metadata controllers to pass orgId from request attributes

**Checkpoint**: Bulk jobs and metadata are org-isolated.

---

## Feature 3: Integration Test Suite

- [ ] T019 [P] Create TenantIsolationIntegrationTest — register two users, verify cross-org data invisibility for sObjects, bulk jobs, and metadata in `service/src/test/java/co/razkevich/sflocalstack/integration/TenantIsolationIntegrationTest.java`
- [ ] T020 [P] Create SfCliWorkflowIntegrationTest — full REST workflow: register, login, create records, query, update, delete, describe in `service/src/test/java/co/razkevich/sflocalstack/integration/SfCliWorkflowIntegrationTest.java`
- [ ] T021 [P] Create OAuthFlowIntegrationTest — test both password grant and authorization code flow end-to-end in `service/src/test/java/co/razkevich/sflocalstack/integration/OAuthFlowIntegrationTest.java`

**Checkpoint**: All integration tests pass in CI.

---

## Final Polish

- [ ] T022 Fix any existing test failures caused by orgId changes
- [ ] T023 Run full test suite and verify 0 failures
- [ ] T024 Build and deploy to production droplet at 164.92.219.185
- [ ] T025 E2E test: two SF CLI aliases, verify tenant isolation on production

---

## Dependencies

- Phase 1 → Feature 1 → Feature 2 (sequential: entities need orgId before services can filter)
- Feature 3 can start after Feature 1 (sObject tests) and expand as Feature 2 lands
- T014/T015 can run in parallel (different entity groups)
- T019/T020/T021 can run in parallel (different test files)
