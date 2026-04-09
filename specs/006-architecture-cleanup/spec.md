# Feature Specification: Architecture Cleanup

**Feature Branch**: `006-architecture-cleanup`
**Status**: Draft
**Created**: 2026-04-09

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: REST, Bulk, Metadata (all — internal restructure, external behavior unchanged)
- **Compatibility Target**: Zero behavior change; all existing tests must pass before and after
- **In-Scope Operations**: Package restructuring, interface extraction, cross-domain dependency cleanup
- **Out-of-Scope Operations**: New features, API changes, new endpoints
- **Test Isolation Plan**: Existing tests serve as regression suite; no new test behavior needed
- **Parity Verification Plan**: Not applicable (internal-only change)

## Feature Iterations *(mandatory)*

### Feature 0 - Domain Package Structure (Priority: P1)

Reorganize the flat package structure under `co.razkevich.sflocalstack` into a domain-first layout, where each domain owns its controllers, services, models, and repositories.

**Why this priority**: The flat structure makes it difficult to reason about domain boundaries and cross-domain coupling. Domain-first packaging is the prerequisite for safe interface extraction in Feature 1.

**Current flat layout**:
```
co.razkevich.sflocalstack/
  controller/
  service/
  model/
  repository/
  config/
  interceptor/
```

**Target domain layout**:
```
co.razkevich.sflocalstack/
  data/
    controller/   (SObjectController, QueryController)
    service/      (OrgStateService, SoqlEngine)
    model/        (SObjectRecord)
    repository/   (SObjectRecordRepository)
  bulk/
    controller/   (BulkController)
    service/      (BulkJobService)
    model/        (BulkIngestJob, BulkRowResult, + new JPA entities from F2)
  metadata/
    controller/   (MetadataController, MetadataRestController, MetadataAdminController)
    service/      (MetadataService, MetadataToolingService, MetadataSoapParser, MetadataSoapRenderer, MetadataZipService)
    model/        (MetadataResource, MetadataDeployJob, MetadataRetrieveJob)
  observability/
    controller/   (DashboardController, ResetController)
    service/      (RequestLogService)
    filter/       (RequestLoggingFilter, GzipRequestDecompressionFilter)
    model/        (RequestLogEntry, DashboardOverview)
  config/         (WebConfig, CorsConfig, ApplicationConfig)
  auth/           (placeholder — LoginController, JwtService, AuthFilter, UserStore, User, Role — populated by F3)
  common/
    model/        (shared model classes used by multiple domains)
```

**Class-to-domain mapping**:

| Class | Target Domain |
|---|---|
| `SObjectController` | `data/controller/` |
| `QueryController` | `data/controller/` |
| `OrgStateService` | `data/service/` |
| `SoqlEngine` | `data/service/` |
| `SObjectRecord` | `data/model/` |
| `SObjectRecordRepository` | `data/repository/` |
| `BulkController` | `bulk/controller/` |
| `BulkJobService` | `bulk/service/` |
| `BulkIngestJob` | `bulk/model/` |
| `BulkRowResult` | `bulk/model/` |
| `MetadataController` | `metadata/controller/` |
| `MetadataRestController` | `metadata/controller/` |
| `MetadataAdminController` | `metadata/controller/` |
| `MetadataService` | `metadata/service/` |
| `MetadataToolingService` | `metadata/service/` |
| `MetadataSoapParser` | `metadata/service/` |
| `MetadataSoapRenderer` | `metadata/service/` |
| `MetadataZipService` | `metadata/service/` |
| `MetadataResource` | `metadata/model/` |
| `MetadataDeployJob` | `metadata/model/` |
| `MetadataRetrieveJob` | `metadata/model/` |
| `DashboardController` | `observability/controller/` |
| `ResetController` | `observability/controller/` |
| `RequestLogService` | `observability/service/` |
| `RequestLoggingFilter` | `observability/filter/` |
| `GzipRequestDecompressionFilter` | `observability/filter/` |
| `RequestLogEntry` | `observability/model/` |
| `DashboardOverview` | `observability/model/` |
| `WebConfig` | `config/` |
| `CorsConfig` | `config/` |
| `ApplicationConfig` | `config/` |

**Acceptance Scenarios**:

1. **Given** all classes moved to their domain packages, **When** `mvn -pl service test` runs, **Then** all tests pass with 0 failures.
2. **Given** the `@SpringBootApplication` class is at the root package, **When** the application starts, **Then** Spring component scanning discovers all beans in all domain packages.
3. **Given** model classes used by multiple domains are identified, **When** moved, **Then** they go into `common/model/` and both domains import from there.

---

### Feature 1 - Interface Extraction at Domain Boundaries (Priority: P1)

Extract interfaces for every service that is called from outside its own domain. Domains must depend only on interfaces from other domains, never on concrete classes.

**Why this priority**: Without interface boundaries, domain packages are packaging only — renaming, not decoupling. Interfaces enforce the dependency inversion that makes domains independently evolvable.

**Interfaces to extract**:

| Concrete Class | Interface Name | Used By |
|---|---|---|
| `OrgStateService` | `OrgStateOperations` | `BulkJobService` (bulk), `SoqlEngine` (data) |
| `RequestLogService` | `RequestLogger` | `RequestLoggingFilter` (observability/filter) |
| `MetadataService` | `MetadataOperations` | `MetadataToolingService` (metadata/service) |

**Rules**:
- Interfaces are placed in the same domain package as the concrete class (e.g., `data/service/OrgStateOperations`).
- Cross-domain callers import the interface, not the concrete class.
- Spring wires the concrete implementation via `@Primary` or single-implementation auto-wiring.

**Acceptance Scenarios**:

1. **Given** interfaces extracted, **When** checking all imports in domain packages, **Then** no domain package imports a concrete service class from another domain.
2. **Given** `OrgStateOperations` interface exists, **When** `BulkJobService` is compiled, **Then** it depends only on `OrgStateOperations`, not `OrgStateService`.
3. **Given** `RequestLogger` interface exists, **When** `RequestLoggingFilter` is compiled, **Then** it depends only on `RequestLogger`, not `RequestLogService`.

---

### Feature 2 - Clean Up Code Smells (Priority: P2)

Remove dead code, unused imports, and inconsistent naming discovered during the restructure. Add `package-info.java` to document each domain's responsibility. Verify no circular dependencies.

**Why this priority**: Moving code without cleaning it transfers existing problems into the new structure. This step locks in the quality benefits of the restructure.

**Tasks**:
- Remove unused imports across all moved classes.
- Remove any commented-out code blocks.
- Ensure class and method naming follows consistent conventions across domains.
- Add `package-info.java` to each domain root package (`data`, `bulk`, `metadata`, `observability`, `config`, `auth`, `common`) documenting its responsibility in one sentence.
- Run Maven dependency analysis to verify no circular dependencies between domain packages.

**package-info.java responsibilities**:

| Package | Responsibility |
|---|---|
| `data` | Manages sObject CRUD operations and SOQL query execution |
| `bulk` | Implements Bulk API v2 job lifecycle and CSV record processing |
| `metadata` | Handles Metadata API SOAP, REST Tooling, and deployment operations |
| `observability` | Logs and exposes HTTP request history and dashboard state |
| `config` | Holds Spring Boot application-wide configuration beans |
| `auth` | Provides authentication and authorization (populated by F3) |
| `common` | Contains model classes shared across multiple domains |

**Acceptance Scenarios**:

1. **Given** cleanup complete, **When** running `mvn -pl service verify`, **Then** no circular dependency errors.
2. **Given** `package-info.java` added to each domain, **When** reading the file, **Then** each describes its responsibility in one sentence.
3. **Given** all imports reviewed, **When** compiling, **Then** zero unused import warnings.

---

### Edge Cases

- **What if a model class is used by multiple domains?** Shared models go in `common/model/`. Both domains import from there. No model duplication.
- **What if Spring component scanning breaks?** Ensure `@SpringBootApplication` is at the `co.razkevich.sflocalstack` root so it scans all sub-packages automatically. If explicit `@ComponentScan` is in use, update it to include all domain packages.
- **What if test imports break?** Update test import statements to reflect the new package paths, but do not modify any test logic, assertions, or test data.
- **What about the `interceptor/` package?** Filters move to `observability/filter/`. The `interceptor/` package is deleted after all classes are moved.
- **What if an interface would have only one implementation forever?** Still extract it — the purpose is compile-time boundary enforcement, not runtime polymorphism.
- **What if circular deps exist between `data` and `bulk`?** Break the cycle by introducing a shared interface in `common/` or by moving the shared logic to the domain that owns it.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Package restructure MUST NOT change any external API behavior (request/response shapes, URLs, HTTP methods, status codes)
- **FR-002**: All existing tests MUST pass after restructure with no modifications to test logic (only import path updates are permitted)
- **FR-003**: Cross-domain dependencies MUST go through interfaces — no domain may import a concrete class from another domain
- **FR-004**: No circular dependencies between domain packages
- **FR-005**: Each domain package MUST be self-contained with its own controllers, services, models, and repositories where applicable
- **FR-006**: The `@SpringBootApplication` root package MUST cover all domain sub-packages via component scanning

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `mvn -pl service test` passes with 0 failures after all class moves
- **SC-002**: No domain package imports concrete classes from another domain (only interfaces) — verified by import analysis
- **SC-003**: Package structure matches the target domain layout defined in Feature 0
- **SC-004**: No circular dependency detected via Maven or IDE analysis
- **SC-005**: `package-info.java` exists in each of the 7 domain/support packages with a single-sentence responsibility statement
- **SC-006**: The `interceptor/` package no longer exists after all filters are moved to `observability/filter/`
