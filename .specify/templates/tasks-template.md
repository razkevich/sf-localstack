---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required for feature slices), research.md, data-model.md, contracts/

**Tests**: Tests are REQUIRED. Every feature slice and every changed Salesforce surface
must start with failing backend contract and integration coverage before implementation.

**Organization**: Tasks are grouped by feature slice so each slice can be implemented,
validated locally, and checked against the reference Salesforce org.

## Format: `[ID] [P?] [Feature] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Feature]**: Which feature slice this task belongs to (e.g. F0, F1, F2)
- Include exact file paths in descriptions

## Phase 1: Shared App Scaffold

**Purpose**: Establish the reusable application shell needed by all feature slices.

- [ ] T001 Create or align the backend and frontend project structure described in `plan.md`
- [ ] T002 [P] Establish shared frontend API client/types and request inspection primitives
- [ ] T003 [P] Establish resettable test fixtures and seed management
- [ ] T004 [P] Establish shared observability and Salesforce-style error helpers

**Checkpoint**: The app shell is ready for feature-by-feature delivery.

---

## Feature 0: [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this slice delivers]

**Independent Test**: [How to verify this slice works on its own]

### Backend Tests First ⚠️

- [ ] T005 [P] [F0] Add backend contract coverage in [backend test path]
- [ ] T006 [P] [F0] Add local integration coverage in [integration test path]

### Backend Implementation

- [ ] T007 [P] [F0] Implement backend models/services in [backend path]
- [ ] T008 [F0] Implement backend controllers/endpoints in [backend path]

### Frontend Tests

- [ ] T009 [P] [F0] Add frontend rendering/state coverage in [frontend test path]

### Frontend Implementation

- [ ] T010 [F0] Build matching frontend workflow in [frontend path]

### Verification

- [ ] T011 [F0] Validate the local end-to-end workflow for Feature 0 using the documented app flow
- [ ] T012 [F0] Run parity verification against the reference Salesforce org and record accepted deltas
- [ ] T013 [F0] Clean up temporary reference-org records created during parity checks

**Checkpoint**: Feature 0 is complete across backend, frontend, local verification, and real-Salesforce parity.

---

## Feature 1: [Title] (Priority: P2)

**Goal**: [Brief description of what this slice delivers]

**Independent Test**: [How to verify this slice works on its own]

### Backend Tests First ⚠️

- [ ] T014 [P] [F1] Add backend contract coverage in [backend test path]
- [ ] T015 [P] [F1] Add local integration coverage in [integration test path]

### Backend Implementation

- [ ] T016 [P] [F1] Implement backend models/services in [backend path]
- [ ] T017 [F1] Implement backend controllers/endpoints in [backend path]

### Frontend Tests

- [ ] T018 [P] [F1] Add frontend rendering/state coverage in [frontend test path]

### Frontend Implementation

- [ ] T019 [F1] Build matching frontend workflow in [frontend path]

### Verification

- [ ] T020 [F1] Validate the local end-to-end workflow for Feature 1 using the documented app flow
- [ ] T021 [F1] Run parity verification against the reference Salesforce org and record accepted deltas
- [ ] T022 [F1] Clean up temporary reference-org records created during parity checks

**Checkpoint**: Feature 1 is complete across backend, frontend, local verification, and real-Salesforce parity.

---

## Feature 2: [Title] (Priority: P3)

**Goal**: [Brief description of what this slice delivers]

**Independent Test**: [How to verify this slice works on its own]

### Backend Tests First ⚠️

- [ ] T023 [P] [F2] Add backend contract coverage in [backend test path]
- [ ] T024 [P] [F2] Add local integration coverage in [integration test path]

### Backend Implementation

- [ ] T025 [P] [F2] Implement backend models/services in [backend path]
- [ ] T026 [F2] Implement backend controllers/endpoints in [backend path]

### Frontend Tests

- [ ] T027 [P] [F2] Add frontend rendering/state coverage in [frontend test path]

### Frontend Implementation

- [ ] T028 [F2] Build matching frontend workflow in [frontend path]

### Verification

- [ ] T029 [F2] Validate the local end-to-end workflow for Feature 2 using the documented app flow
- [ ] T030 [F2] Run parity verification against the reference Salesforce org and record accepted deltas
- [ ] T031 [F2] Clean up temporary reference-org records created during parity checks

**Checkpoint**: Feature 2 is complete across backend, frontend, local verification, and real-Salesforce parity.

---

[Add more feature slices as needed, following the same pattern]

---

## Final Polish

**Purpose**: Improvements that affect multiple feature slices.

- [ ] TXXX [P] Tighten shared UI consistency and request/response inspection flows
- [ ] TXXX Code cleanup and refactoring across completed slices
- [ ] TXXX [P] Additional unit tests for cross-slice behavior
- [ ] TXXX Re-run local end-to-end walkthroughs across completed slices
- [ ] TXXX Re-run parity checks for all supported slices and confirm cleanup is complete

---

## Dependencies & Execution Order

### Phase Dependencies

- **Shared App Scaffold**: No dependencies - can start immediately
- **Feature Slices**: Depend on the shared scaffold, then proceed in priority order unless
  the plan explicitly allows overlap
- **Final Polish**: Depends on all desired feature slices being complete

### Within Each Feature Slice

- Backend tests MUST be written and FAIL before backend implementation
- Backend implementation lands before frontend implementation depends on it
- Frontend tests land before or alongside frontend implementation
- Local integration verification must pass before parity verification starts
- Parity verification must include cleanup of temporary reference-org records

### Parallel Opportunities

- Shared scaffold tasks marked [P] can run in parallel
- Backend tests within a feature marked [P] can run in parallel
- Backend models/services and frontend tests can overlap once interfaces stabilize
- Verification tasks remain sequential: local verification before parity, parity before cleanup

---

## Implementation Strategy

### MVP First

1. Complete the shared app scaffold
2. Complete the highest-priority feature slice end-to-end
3. Validate locally
4. Run parity checks against the reference org
5. Clean up temporary reference-org records

### Incremental Delivery

1. Ship one feature slice at a time
2. Each slice includes backend, frontend, tests, local verification, and parity verification
3. Record accepted deltas immediately so later slices build on known behavior

### Parallel Team Strategy

With multiple developers:

1. One developer stabilizes the shared scaffold
2. One developer focuses on backend contract-first delivery for the active slice
3. Another developer builds the matching frontend once interfaces stabilize
4. Verification happens after both sides are integrated

---

## Notes

- [P] tasks = different files, no dependencies
- [Feature] label maps task to a specific feature slice for traceability
- Each slice should be independently completable and testable
- Verify tests fail before implementing
- Preserve explicit REST, Bulk, and Metadata scope boundaries in every task list
- Treat real-Salesforce parity as a completion criterion, not a nice-to-have
