# Implementation Plan: Multi-Tenant Org Isolation

**Branch**: `013-multi-tenant-isolation` | **Date**: 2026-04-12 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/013-multi-tenant-isolation/spec.md`

## Summary

Add `orgId` field to all persistent entities (SObjectRecord, BulkIngestJob, MetadataResourceEntity, deploy/retrieve jobs). Auto-create an org on user registration. Embed `orgId` in JWT claims. Filter all data access by org. Build comprehensive integration test suite verifying tenant isolation across REST, Bulk, and Metadata surfaces.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.3.5
**Primary Dependencies**: Spring Web, Spring Data JPA, H2, JJWT (all existing)
**Storage**: H2 file-based (existing), add `org_id` column to all tables
**Testing**: JUnit 5, MockMvc, @SpringBootTest with test profile
**Target Platform**: Linux server (DigitalOcean), Docker
**Project Type**: Web service (Salesforce API emulator)
**Constraints**: Zero new dependencies. Backward compatible with existing data.

## Constitution Check

- **API Fidelity**: All API response shapes remain identical. Only change: `organization_id` reflects the user's actual org instead of the hardcoded value. All Salesforce error formats preserved.
- **Test-First**: Integration test suite (Feature 3) written before/alongside implementation. Tests verify: cross-org data invisibility, per-org seeding, bulk job isolation, metadata isolation, OAuth flow with org claims. All tests run via `mvn test` in CI.
- **Runtime Reproducibility**: Each test creates its own users+orgs. InMemoryUserStore resets per test. H2 `create-drop` in test profile ensures clean state.
- **Dependency Surface**: Zero new dependencies. `orgId` field added to existing JPA entities. Org ID generation uses UUID-based logic already present in codebase.
- **Observability**: Request logs tagged with orgId for per-org filtering. Existing RequestLoggingFilter captures org context.
- **Scope Control**: MVP: 1 user = 1 org. No sharing, no org deletion, no cross-org queries. Multi-user orgs deferred.
- **Parity Verification**: Two SF CLI aliases with different users, verify cross-org invisibility.

## Project Structure

### Source Code Changes

```text
service/src/main/java/co/razkevich/sflocalstack/
├── auth/
│   ├── model/User.java                    # ADD orgId field
│   ├── model/Org.java                     # NEW: org entity
│   ├── service/JwtService.java            # ADD orgId to JWT claims
│   ├── service/OrgService.java            # NEW: org provisioning + seeding
│   ├── store/UserStore.java               # ADD orgId to createUser
│   ├── store/FileBasedUserStore.java       # ADD orgId support
│   ├── store/InMemoryUserStore.java        # ADD orgId support
│   └── filter/JwtAuthFilter.java           # EXTRACT orgId from token, set as request attr
├── data/
│   ├── model/SObjectRecord.java            # ADD orgId field
│   ├── repository/SObjectRecordRepository  # ADD findByOrgIdAndObjectType, etc.
│   └── service/OrgStateService.java        # ADD orgId filtering to all methods
├── bulk/
│   ├── model/BulkIngestJob.java            # ADD orgId field
│   └── service/BulkJobService.java         # ADD orgId filtering
├── metadata/
│   ├── model/MetadataResourceEntity.java   # ADD orgId field
│   ├── model/MetadataDeployJobEntity.java  # ADD orgId field
│   ├── model/MetadataRetrieveJobEntity.java # ADD orgId field
│   └── service/MetadataService.java        # ADD orgId filtering
├── controller/
│   ├── OAuthController.java               # USE real orgId in responses
│   ├── AuthController.java                # AUTO-CREATE org on register
│   └── ResetController.java              # RESET only user's org
└── observability/
    └── service/RequestLogService.java      # TAG entries with orgId

service/src/test/java/co/razkevich/sflocalstack/
├── integration/
│   ├── TenantIsolationIntegrationTest.java  # NEW: cross-org isolation
│   ├── SfCliWorkflowIntegrationTest.java    # NEW: full SF CLI flow
│   ├── OAuthFlowIntegrationTest.java        # NEW: auth code + password grant
│   └── BulkAndMetadataIntegrationTest.java  # NEW: bulk/metadata isolation
```

## Feature Iteration Strategy

### Feature 0: Org Entity & Auto-Provisioning

- **Backend Scope**: Add `orgId` to User model. Create OrgService for org provisioning. Update registration to auto-create org. Update JWT to include orgId claim. Update auth filter to extract orgId. Update OAuth responses.
- **Frontend Scope**: None (org ID already displayed in Setup view via API).
- **Tests First**: Register two users, verify different org IDs in tokens and OAuth responses.
- **Integration Verification**: `sf org display` shows unique org IDs per user.

### Feature 1: sObject Data Isolation

- **Backend Scope**: Add `orgId` column to `sobject_records`. Update SObjectRecordRepository with org-filtered queries. Update OrgStateService to accept and filter by orgId. Update all controllers to pass orgId from request attributes.
- **Tests First**: Create record as User A, query as User B → 0 results. GET by ID from wrong org → 404.
- **Integration Verification**: Two SF CLI aliases, create/query demonstrates isolation.

### Feature 2: Bulk Job & Metadata Isolation

- **Backend Scope**: Add `orgId` to BulkIngestJob, MetadataResourceEntity, MetadataDeployJobEntity, MetadataRetrieveJobEntity. Update services and repositories.
- **Tests First**: Create bulk job as A, list as B → not visible. Deploy metadata as A, list as B → not visible.

### Feature 3: Integration Test Suite

- **Backend Scope**: Comprehensive MockMvc integration tests covering all surfaces.
- **Tests**: TenantIsolationIntegrationTest (cross-org), SfCliWorkflowIntegrationTest (full flow), OAuthFlowIntegrationTest (auth), BulkAndMetadataIntegrationTest (bulk/metadata isolation).

## Salesforce Parity Verification

- **Reference Org**: `dev20`
- **Parity Method**: Two SF CLI aliases against sf_localstack, verify data isolation matches two separate SF orgs.
- **Accepted Deltas**: Org IDs are UUID-based instead of Salesforce's sequential format.

## Complexity Tracking

No constitution violations. Zero new dependencies.
