# Tasks: Persistent Storage

**Input**: Design documents from `/specs/005-persistent-storage/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md

**Tests**: Existing 101 tests serve as regression suite. Constitution requires test-first for new entities.

**Organization**: Tasks grouped by feature iteration (F0-F4) matching spec priority order.

## Format: `[ID] [P?] [Feature] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Feature]**: F0=Profiles, F1=Bulk, F2=Metadata, F3=Reset, F4=Docs

## Phase 1: Spring Profiles & H2 File Config (Feature 0 — P1)

**Purpose**: Split config into profiles so tests use H2 mem and dev uses H2 file.

- [ ] T001 [F0] Create `service/src/main/resources/application-dev.yml` with H2 file-based config: `spring.datasource.url: jdbc:h2:file:./data/sfdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, `spring.jpa.hibernate.ddl-auto: update`
- [ ] T002 [P] [F0] Create `service/src/main/resources/application-test.yml` with H2 mem config: `spring.datasource.url: jdbc:h2:mem:sfdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, `spring.jpa.hibernate.ddl-auto: create-drop`
- [ ] T003 [P] [F0] Create `service/src/main/resources/application-prod.yml` with PostgreSQL placeholder config (commented out datasource URL, driver class, credentials)
- [ ] T004 [F0] Modify `service/src/main/resources/application.yml` to set `spring.profiles.active: dev` as default, remove inline H2 datasource config (moved to profile files), keep non-profile-specific settings (server port, sf-localstack config, logging)
- [ ] T005 [F0] Add `data/` to `.gitignore` at project root
- [ ] T006 [F0] Run full test suite: `mvn -pl service test` — all 101 tests must pass (test profile activates via `@SpringBootTest`)
- [ ] T007 [F0] Commit: `feat: add Spring profiles for dev/test/prod storage config`

**Checkpoint**: Tests pass with H2 mem. Dev profile would use file-based H2.

---

## Phase 2: Bulk Job JPA Entities (Feature 1 — P1)

**Goal**: Replace ConcurrentHashMap in BulkJobService with JPA-persisted entities.

**Independent Test**: `mvn -pl service test -Dtest="BulkControllerTest,BulkJobServiceTest"` passes.

- [ ] T008 [P] [F1] Create `service/src/main/java/co/razkevich/sflocalstack/model/BulkBatchEntity.java` — JPA entity with fields: id (Long, @GeneratedValue), jobId (String), csvData (@Lob String), sequenceNumber (int). Table: `bulk_batches`
- [ ] T009 [P] [F1] Create `service/src/main/java/co/razkevich/sflocalstack/model/BulkRowResultEntity.java` — JPA entity with fields: id (Long, @GeneratedValue), jobId (String), resultType (String), sfId (String), sfCreated (Boolean), sfError (String), originalRow (@Column(columnDefinition="TEXT") String). Table: `bulk_row_results`
- [ ] T010 [F1] Modify `service/src/main/java/co/razkevich/sflocalstack/model/BulkIngestJob.java` — convert from plain POJO to JPA @Entity. Add @Id on id, @Column annotations, @OneToMany relationships to BulkBatchEntity and BulkRowResultEntity. Table: `bulk_ingest_jobs`. Keep existing field names and types. Remove `final` modifiers for JPA compatibility. Add no-arg constructor.
- [ ] T011 [P] [F1] Create `service/src/main/java/co/razkevich/sflocalstack/repository/BulkIngestJobRepository.java` — extends JpaRepository<BulkIngestJob, String>
- [ ] T012 [P] [F1] Create `service/src/main/java/co/razkevich/sflocalstack/repository/BulkBatchRepository.java` — extends JpaRepository<BulkBatchEntity, Long>, add `List<BulkBatchEntity> findByJobIdOrderBySequenceNumber(String jobId)`, `void deleteByJobId(String jobId)`
- [ ] T013 [P] [F1] Create `service/src/main/java/co/razkevich/sflocalstack/repository/BulkRowResultRepository.java` — extends JpaRepository<BulkRowResultEntity, Long>, add `List<BulkRowResultEntity> findByJobIdAndResultType(String jobId, String resultType)`, `void deleteByJobId(String jobId)`
- [ ] T014 [F1] Refactor `service/src/main/java/co/razkevich/sflocalstack/service/BulkJobService.java` — replace `ConcurrentHashMap<String, BulkIngestJob> jobs` with autowired BulkIngestJobRepository, BulkBatchRepository, BulkRowResultRepository. Update createJob() to save to repo, getJob() to findById, upload() to save BulkBatchEntity, close() to process batches from repo and save results to repo, delete() to delete from repos, successfulResults/failedResults/unprocessedResults to query from BulkRowResultRepository, reset() to deleteAll on all 3 repos. Mark methods @Transactional.
- [ ] T015 [F1] Run bulk tests: `mvn -pl service test -Dtest="BulkControllerTest,BulkJobServiceTest,CrossSurfaceIntegrationTest"` — all must pass
- [ ] T016 [F1] Commit: `feat: persist Bulk API jobs to H2 via JPA entities`

**Checkpoint**: Bulk jobs persisted. All bulk and cross-surface tests pass.

---

## Phase 3: Metadata JPA Entities (Feature 2 — P1)

**Goal**: Replace ConcurrentHashMaps in MetadataService with JPA-persisted entities.

**Independent Test**: `mvn -pl service test -Dtest="MetadataControllerTest,MetadataServiceTest,MetadataAdminControllerTest"` passes.

- [ ] T017 [P] [F2] Create `service/src/main/java/co/razkevich/sflocalstack/model/MetadataResourceEntity.java` — JPA entity with fields: id (Long, @GeneratedValue), type (String), fullName (String), fileName (String), directoryName (String), inFolder (boolean), metaFile (boolean), label (String), suffix (String), attributesJson (@Lob String), lastModifiedDate (Instant). Table: `metadata_resources`. Add @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"type", "fullName"}))
- [ ] T018 [P] [F2] Create `service/src/main/java/co/razkevich/sflocalstack/model/MetadataDeployJobEntity.java` — JPA entity with fields: id (String, @Id), done (boolean), success (boolean), status (String), numberComponentsTotal (int), numberComponentsDeployed (int), numberComponentErrors (int), createdDate (Instant), completedDate (Instant nullable). Table: `metadata_deploy_jobs`
- [ ] T019 [P] [F2] Create `service/src/main/java/co/razkevich/sflocalstack/model/MetadataRetrieveJobEntity.java` — JPA entity with fields: id (String, @Id), done (boolean), success (boolean), status (String), zipFileBase64 (@Lob String nullable), numberComponentsTotal (int), createdDate (Instant), completedDate (Instant nullable). Table: `metadata_retrieve_jobs`
- [ ] T020 [P] [F2] Create `service/src/main/java/co/razkevich/sflocalstack/repository/MetadataResourceRepository.java` — extends JpaRepository<MetadataResourceEntity, Long>, add `List<MetadataResourceEntity> findByType(String type)`, `Optional<MetadataResourceEntity> findByTypeAndFullName(String type, String fullName)`, `void deleteByTypeAndFullName(String type, String fullName)`
- [ ] T021 [P] [F2] Create `service/src/main/java/co/razkevich/sflocalstack/repository/MetadataDeployJobRepository.java` — extends JpaRepository<MetadataDeployJobEntity, String>
- [ ] T022 [P] [F2] Create `service/src/main/java/co/razkevich/sflocalstack/repository/MetadataRetrieveJobRepository.java` — extends JpaRepository<MetadataRetrieveJobEntity, String>
- [ ] T023 [F2] Refactor `service/src/main/java/co/razkevich/sflocalstack/service/MetadataService.java` — replace 3 ConcurrentHashMaps with autowired repositories. Update all CRUD methods to use repos: createResource saves MetadataResourceEntity, updateResource deletes+creates, deleteResource uses repo, listResources queries repo, deploy/retrieve/checkStatus use deploy/retrieve repos. Convert between entity and existing record types (MetadataResource, MetadataDeployJob, MetadataRetrieveJob). The existing record types remain as DTOs used by controllers. Mark methods @Transactional.
- [ ] T024 [F2] Run metadata tests: `mvn -pl service test -Dtest="MetadataControllerTest,MetadataServiceTest,MetadataAdminControllerTest,MetadataRestControllerTest,MetadataControllerVersionTest,MetadataSoapRendererShapeTest"` — all must pass
- [ ] T025 [F2] Commit: `feat: persist Metadata resources and jobs to H2 via JPA entities`

**Checkpoint**: Metadata state persisted. All metadata tests pass.

---

## Phase 4: Reset Behavior Update (Feature 3 — P2)

**Goal**: Reset clears all persistent tables in FK-safe order.

- [ ] T026 [F3] Modify `service/src/main/java/co/razkevich/sflocalstack/service/OrgStateService.java` — inject all new repositories (BulkIngestJobRepository, BulkBatchRepository, BulkRowResultRepository, MetadataResourceRepository, MetadataDeployJobRepository, MetadataRetrieveJobRepository). Update reset() to deleteAll in FK-safe order: BulkRowResultRepository, BulkBatchRepository, BulkIngestJobRepository, MetadataResourceRepository, MetadataDeployJobRepository, MetadataRetrieveJobRepository, then existing SObjectRecordRepository.deleteAll(). Also clear RequestLogService.
- [ ] T027 [F3] Run full test suite: `mvn -pl service test` — all tests must pass including CrossSurfaceIntegrationTest and ResetControllerTest
- [ ] T028 [F3] Commit: `feat: update reset to clear all persistent storage tables`

**Checkpoint**: Reset clears all state across all surfaces.

---

## Phase 5: Extensibility Documentation (Feature 4 — P2)

**Goal**: Document PostgreSQL migration path.

- [ ] T029 [F4] Create `docs/extensibility.md` covering: storage architecture overview (interface-driven via Spring Data repositories), MVP implementation (H2 file-based), PostgreSQL migration steps (add driver dep to pom.xml, create application-prod.yml with PG URL/credentials, activate prod profile), application-prod.yml template, future paths (Flyway migrations, multi-tenant schema isolation)
- [ ] T030 [F4] Commit: `docs: add extensibility guide for storage layer and PostgreSQL migration`

---

## Phase 6: Final Validation

- [ ] T031 Run full test suite: `mvn -pl service test` — verify all 101+ tests pass with 0 failures
- [ ] T032 Verify compilation: `mvn -pl service package -DskipTests` — JAR builds successfully
- [ ] T033 Final commit if any fixes needed

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Profiles)**: No dependencies — start immediately
- **Phase 2 (Bulk)**: Depends on Phase 1 (needs test profile)
- **Phase 3 (Metadata)**: Depends on Phase 1. Can run in parallel with Phase 2.
- **Phase 4 (Reset)**: Depends on Phase 2 and Phase 3 (needs all repos)
- **Phase 5 (Docs)**: Independent — can run any time
- **Phase 6 (Validation)**: Depends on all prior phases

### Parallel Opportunities

- T001, T002, T003 can run in parallel (different profile files)
- T008, T009 can run in parallel (different entity files)
- T011, T012, T013 can run in parallel (different repository files)
- T017, T018, T019 can run in parallel (different entity files)
- T020, T021, T022 can run in parallel (different repository files)
- Phase 2 and Phase 3 can overlap once Phase 1 complete
- Phase 5 is fully independent

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 (profiles) — enables persistence without breaking tests
2. Complete Phase 2 (Bulk entities) — highest-impact persistence
3. Run and validate

### Incremental Delivery

Each phase is independently committable and testable.
