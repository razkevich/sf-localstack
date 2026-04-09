# Implementation Plan: Persistent Storage

**Branch**: `005-persistent-storage` | **Date**: 2026-04-09 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-persistent-storage/spec.md`

## Summary

Migrate all in-memory state (H2 mem, ConcurrentHashMap for Bulk/Metadata) to persistent H2 file-based storage using Spring profiles. sObject records, Bulk jobs, and Metadata resources survive restarts. All existing tests pass unchanged via test profile.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.3.5, Spring Data JPA, H2 (existing — no new dependencies)
**Storage**: H2 file-based (`jdbc:h2:file:./data/sfdb`) for dev; H2 mem for test
**Testing**: JUnit 5 + MockMvc + AssertJ (existing)
**Target Platform**: Spring Boot embedded Tomcat
**Project Type**: Web service (Salesforce API emulator)

## Constitution Check

- **API Fidelity**: Zero API changes. Storage is internal. All response shapes preserved.
- **Test-First**: Existing 101 tests serve as regression suite. New persistence tests added.
- **Runtime Reproducibility**: Test profile uses H2 mem with create-drop. Dev profile uses H2 file with update.
- **Dependency Surface**: Zero new dependencies. Uses existing H2 + JPA.
- **Observability**: Reset logging preserved. No new observability needed.
- **Persistent Storage (Principle VI)**: This feature implements the principle.
- **Scope Control**: Storage-only changes + new JPA entities. No API surface changes.
- **Parity Verification**: Not applicable (internal change).

## Project Structure

```text
service/src/main/resources/
├── application.yml                    (MODIFY — default to dev profile)
├── application-dev.yml                (CREATE — H2 file config)
├── application-test.yml               (CREATE — H2 mem config)
├── application-prod.yml               (CREATE — PostgreSQL placeholder)

service/src/main/java/co/razkevich/sflocalstack/
├── model/
│   ├── BulkIngestJob.java             (MODIFY → JPA entity)
│   ├── BulkBatchEntity.java           (CREATE)
│   ├── BulkRowResultEntity.java       (CREATE)
│   ├── MetadataResourceEntity.java    (CREATE)
│   ├── MetadataDeployJobEntity.java   (CREATE)
│   ├── MetadataRetrieveJobEntity.java (CREATE)
├── repository/
│   ├── BulkIngestJobRepository.java   (CREATE)
│   ├── BulkBatchRepository.java       (CREATE)
│   ├── BulkRowResultRepository.java   (CREATE)
│   ├── MetadataResourceRepository.java (CREATE)
│   ├── MetadataDeployJobRepository.java (CREATE)
│   ├── MetadataRetrieveJobRepository.java (CREATE)
├── service/
│   ├── BulkJobService.java            (MODIFY — use repositories)
│   ├── MetadataService.java           (MODIFY — use repositories)
│   ├── OrgStateService.java           (MODIFY — reset uses all repos)

docs/extensibility.md                  (CREATE)
.gitignore                             (MODIFY — add data/)
```

## Feature Iteration Strategy

### Feature 0: H2 File-Based + Spring Profiles

- **Backend Scope**: application.yml profile split, .gitignore update
- **Tests**: All 101 existing tests must pass unchanged (test profile = H2 mem)

### Feature 1: Bulk Job JPA Entities

- **Backend Scope**: Convert BulkIngestJob to JPA entity, create BulkBatchEntity + BulkRowResultEntity, repositories, refactor BulkJobService from ConcurrentHashMap to JPA
- **Tests**: Existing bulk tests pass + new persistence verification

### Feature 2: Metadata JPA Entities

- **Backend Scope**: Create MetadataResourceEntity, MetadataDeployJobEntity, MetadataRetrieveJobEntity, repositories, refactor MetadataService from ConcurrentHashMap to JPA
- **Tests**: Existing metadata tests pass + new persistence verification

### Feature 3: Reset Behavior Update

- **Backend Scope**: OrgStateService.reset() updated to clear ALL repositories (Bulk, Metadata, sObjects)
- **Tests**: Existing reset + cross-surface tests pass

### Feature 4: Extensibility Documentation

- **Backend Scope**: docs/extensibility.md with PostgreSQL migration guide

## Salesforce Parity Verification

- **Reference Org**: Not applicable (internal storage change)
- **Accepted Deltas**: None — zero API behavior changes

## Complexity Tracking

No constitution violations. Zero new dependencies.
