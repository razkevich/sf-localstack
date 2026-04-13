# Implementation Plan: Salesforce-Native Data & Metadata UX

**Branch**: `014-sf-native-data-ux` | **Date**: 2026-04-12 | **Spec**: [spec.md](spec.md)

## Summary

Fix the Name field createability bug, expand the standard object catalog to 8 objects, add custom object creation from the UI, add a field list view, and improve form layout to match Salesforce Lightning patterns.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.3.5 (backend), React 18, TypeScript 5.x (frontend)
**Primary Dependencies**: All existing — no new deps
**Storage**: H2 file-based (existing)
**Testing**: JUnit 5 + MockMvc (backend), Chrome browser testing (frontend)
**Constraints**: Zero new dependencies. Backward compatible.

## Constitution Check

- **API Fidelity**: Fixing Name createable=true matches real Salesforce behavior. Adding standard objects is additive.
- **Test-First**: Integration tests for field createability, standard object catalog, custom object creation.
- **Dependency Surface**: Zero new dependencies.
- **Scope Control**: No custom field creation, no page layouts, no validation rules. Just the object catalog, forms, and field visibility.

## Project Structure

### Source Code Changes

```text
Backend:
  service/src/main/java/co/razkevich/sflocalstack/data/service/OrgStateService.java
    — Fix Name createable=true, add Case/User/Task/Event field catalogs
  service/src/main/java/co/razkevich/sflocalstack/observability/controller/DashboardController.java
    — Include all standard objects in overview (even with 0 records)
  service/src/main/java/co/razkevich/sflocalstack/metadata/controller/MetadataAdminController.java
    — Add POST /api/metadata/custom-objects endpoint for UI-driven creation

Frontend:
  frontend/src/views/ObjectManagerView.tsx
    — Add "New Custom Object" button, show all 8 standard objects
  frontend/src/views/ObjectListView.tsx
    — Add "Fields" tab, reorder form fields (Name first)
  frontend/src/views/RecordForm.tsx
    — Sort fields with Name/key fields first
  frontend/src/components/ui/CreateCustomObjectModal.tsx
    — NEW: Modal for custom object creation
  frontend/src/components/ui/FieldListView.tsx
    — NEW: Field list table component

Tests:
  service/src/test/java/co/razkevich/sflocalstack/integration/DataUxIntegrationTest.java
    — NEW: Tests for field catalog, custom object creation, standard object listing
```

## Feature Iteration Strategy

### Feature 0: Fix Field Catalog & Expand Standard Objects
- **Backend**: Fix Name createable in OrgStateService field catalog. Add Case, User, Task, Event catalogs. Update DashboardController to always include 8 standard objects.
- **Frontend**: ObjectManagerView already shows objects from overview — just needs the backend to return them.
- **Tests**: Verify Name appears in describe with createable=true. Verify 8 standard objects in overview.

### Feature 1: Create Custom Object from UI
- **Backend**: Add POST `/api/metadata/custom-objects` endpoint that creates a CustomObject metadata resource.
- **Frontend**: Add "New Custom Object" button + modal to ObjectManagerView.
- **Tests**: Create custom object via API, verify it appears in overview and is queryable.

### Feature 2: Field List View
- **Frontend**: New FieldListView component showing describe fields in a table with badges.
- **Tests**: Chrome test verifying fields displayed correctly.

### Feature 3: Improved Form Layout
- **Frontend**: Sort RecordForm fields — Name first, key fields next, then alphabetical.
- **Tests**: Chrome test verifying Name appears first in form.
