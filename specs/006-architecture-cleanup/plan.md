# Implementation Plan: Architecture Cleanup

**Branch**: `006-architecture-cleanup` | **Date**: 2026-04-09 | **Spec**: [spec.md](spec.md)

## Summary

Restructure flat package layout into domain-first organization per ARCHITECTURE.md. Pure refactor — zero behavior changes. 101 tests serve as regression suite.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.3.5 (existing)
**Testing**: JUnit 5 + MockMvc (existing 101 tests as regression)
**Project Type**: Web service (Salesforce API emulator)

## Constitution Check

- **API Fidelity**: Zero API changes. Internal restructure only.
- **Test-First**: 101 existing tests serve as regression. No new tests needed.
- **Dependency Surface**: Zero new dependencies.
- **Scope Control**: Package moves + import updates only.

## Target Package Structure

```
co.razkevich.sflocalstack/
  data/
    controller/   → SObjectController, QueryController
    service/      → OrgStateService, SoqlEngine
    model/        → SObjectRecord, UpsertResult
    repository/   → SObjectRecordRepository
  bulk/
    controller/   → BulkController
    service/      → BulkJobService
    model/        → BulkIngestJob, BulkBatchEntity, BulkRowResult, BulkRowResultEntity
    repository/   → BulkIngestJobRepository, BulkBatchRepository, BulkRowResultRepository
  metadata/
    controller/   → MetadataController, MetadataRestController, MetadataAdminController
    service/      → MetadataService, MetadataToolingService, MetadataSoapParser, MetadataSoapRenderer, MetadataZipService, MetadataManifestParser
    model/        → MetadataResource, MetadataDeployJob, MetadataRetrieveJob, MetadataCatalogEntry, MetadataResourceEntity, MetadataDeployJobEntity, MetadataRetrieveJobEntity, ReadMetadataRecord
    repository/   → MetadataResourceRepository, MetadataDeployJobRepository, MetadataRetrieveJobRepository
  observability/
    controller/   → DashboardController, ResetController
    service/      → RequestLogService
    model/        → RequestLogEntry, DashboardOverview
    filter/       → RequestLoggingFilter, GzipRequestDecompressionFilter
  config/
    → WebConfig, CorsConfig (if exists)
  model/
    → SalesforceError (shared across domains)
  controller/
    → VersionController, OAuthController (cross-cutting, not domain-specific)
```

## Approach

Single-pass refactor: move files to new packages, update all imports in source and test files, verify all 101 tests pass.
