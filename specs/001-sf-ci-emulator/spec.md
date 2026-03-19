# Feature Specification: Usable Salesforce Integration-Test Emulator

**Feature Branch**: `001-sf-ci-emulator`  
**Created**: 2026-03-19  
**Status**: Draft  
**Input**: User description: "Advance sf-localstack from scaffold to a usable Salesforce integration-test emulator. It must let backend engineers point Salesforce REST, Bulk API v2, and Metadata API clients at localhost instead of a real org so CI can run without provisioning Salesforce. The next feature slice should add realistic SOQL filtering and relationship field support, external-ID upsert behavior, synchronous Bulk ingest jobs, basic Metadata deploy and status workflows, a Dockerized quick start, and documentation for resettable test usage. Preserve deterministic seeded data, resettable org state, and compatibility with common Prodly-style Salesforce client flows."

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: `REST`, `Bulk API v2`, and `Metadata API`
- **Compatibility Target**: Common Salesforce client flows used against API version `v60.0`, with behavior close enough that backend engineers can swap a real org URL for `localhost` without client code changes.
- **In-Scope Operations**: App scaffold and dashboard shell; REST version discovery, query, describe, CRUD, and external-ID upsert flows; Bulk API v2 ingest create/upload/close/status/delete/result flows; Metadata API deploy, deploy-status, cancel-deploy, list-metadata, describe-metadata, and supported Metadata REST resource flows; resettable seeded org usage; request/response inspection in the dashboard.
- **Out-of-Scope Operations**: Docker, deployment packaging, CI automation, Bulk query jobs, advanced SOQL beyond the approved filter set, Metadata retrieve/retrieveResult flows, packaging semantics, auth model changes, and production-scale performance simulation.
- **API Shape Commitments**: In-scope operations must keep Salesforce-compatible paths, versioned routing, request field names, success envelopes, SOAP response shapes, status codes, and operation state values expected by common Prodly-style clients. Any unsupported input inside the in-scope surface must fail with a deterministic, Salesforce-like error response instead of silent fallback behavior.
- **Frontend Scope**: The dashboard must evolve with each backend slice and expose the active emulator features through navigable views for request logs, query exploration, org-state inspection, Bulk job monitoring, Metadata workflow inspection, and reset guidance.
- **Test Isolation Plan**: Every test run starts from a known seeded org, can explicitly reset the org after mutations, and verifies that REST, Bulk, and Metadata side effects do not leak into later tests. Reset must restore records, job state, and deploy state to the documented baseline.
- **Runtime Reproducibility Controls**: Seed data remains stable across runs, reset returns the same baseline every time, job completion and deploy status are deterministic, and local developer runs must produce the same supported outputs from the same inputs.
- **Parity Verification Plan**: Every feature slice is checked against the real Salesforce org aliased as `dev20`, comparing request paths, status codes, top-level response shape, key fields, and supported error envelopes. Temporary Salesforce records created during parity checks must use clear test prefixes and be cleaned up after verification.

## Feature Iterations *(mandatory)*

### Feature 0 - App Scaffold (Priority: P1)

An engineer starts the Spring Boot service and frontend dashboard locally, sees the existing emulator request log, can reset state, and has a shared inspection shell that later API slices can plug into.

**Why this priority**: The app needs a stable backend/frontend shell before adding more Salesforce surface area.

**Independent Test**: Start the backend and frontend, load the dashboard, trigger existing API requests, verify request details render, and confirm reset works from the UI and API.

**Acceptance Scenarios**:

1. **Given** the local app is running, **When** an engineer opens the dashboard, **Then** they can see current request log data and navigate the available shell views.
2. **Given** the seeded org has been mutated, **When** the engineer triggers reset, **Then** the app returns to the baseline state and the dashboard reflects the reset.

**Frontend Deliverables**:

- Shared app shell, navigation, request log/detail panels, reset affordance, and reusable API client/types for later features.

**Parity Check**:

- Verify that version discovery and baseline REST list/query flows exercised through the app match `dev20` at the API boundary for supported fields and response envelopes.

---

### Feature 1 - REST Core + SOQL (Priority: P2)

An engineer points an existing Salesforce REST client at the emulator and runs CRUD, describe, and filtered SOQL queries with relationship field projection while also inspecting the same behavior in the dashboard.

**Why this priority**: This is the first feature slice that turns the scaffold into a useful Salesforce API app for day-to-day development.

**Independent Test**: Reset the emulator, run REST CRUD and SOQL flows against seeded Account and Contact data, inspect the results in the dashboard, and verify filtered query results plus relationship field projection.

**Acceptance Scenarios**:

1. **Given** a reset seeded org with Accounts and Contacts, **When** a client queries Contacts with a filter and requests `Account.Name`, **Then** the emulator returns only matching Contacts and includes the expected relationship field value.
2. **Given** an engineer uses the dashboard query tooling, **When** they run supported SOQL queries, **Then** they can inspect the query input, response payload, and resulting org state in one place.
3. **Given** a client requests supported `describe` or CRUD flows, **When** the emulator handles the request, **Then** it responds with Salesforce-compatible success or error envelopes for the supported slice.

**Frontend Deliverables**:

- Query runner, object browser, record detail view, and response inspector for REST/SOQL flows.

**Parity Check**:

- Compare representative REST CRUD, `describe`, and supported SOQL query flows against `dev20`, including relationship-field response shape and supported error envelopes.

---

### Feature 2 - REST Upsert + Error Fidelity (Priority: P3)

An engineer exercises external-ID upsert and supported REST error paths locally, with the dashboard showing whether each request created, updated, or failed in a Salesforce-compatible way.

**Why this priority**: Upsert semantics and error fidelity are critical for real client compatibility once basic REST flows exist.

**Independent Test**: Reset the emulator, run create/update upsert flows and supported failure cases, confirm request outcomes in the dashboard, and verify repeated runs stay stable.

**Acceptance Scenarios**:

1. **Given** a client sends an external-ID upsert for an existing record, **When** the request is processed, **Then** the emulator updates that record and returns Salesforce-compatible update semantics.
2. **Given** a client sends an external-ID upsert for a missing record, **When** the request is processed, **Then** the emulator creates a new record and returns Salesforce-compatible create semantics.
3. **Given** a supported malformed or unsupported REST request, **When** the emulator rejects it, **Then** the response uses a deterministic, Salesforce-like error envelope that can be asserted in tests and inspected in the dashboard.

**Frontend Deliverables**:

- Upsert playground, mutation result viewer, and error inspection UX for supported REST failure cases.

**Parity Check**:

- Compare create/update/upsert and supported REST error flows against `dev20`, including status codes, success bodies, and error arrays.

---

### Feature 3 - Bulk API v2 (Priority: P4)

An engineer runs Bulk API v2 ingest flows against localhost and inspects job progress and row-level results through the dashboard without waiting on real asynchronous Salesforce infrastructure.

**Why this priority**: Bulk ingest is a major compatibility surface for data-loading services and builds directly on the REST foundation.

**Independent Test**: Create an ingest job, upload CSV rows, close the job, inspect job status and result endpoints, verify resulting record mutations, and inspect the same job details in the dashboard.

**Acceptance Scenarios**:

1. **Given** an open ingest job for a supported object type, **When** a client uploads valid CSV rows and marks the job upload complete, **Then** the emulator finishes the job before responding and exposes processed counts plus successful result rows.
2. **Given** an ingest job with invalid rows, **When** the job is closed, **Then** the emulator reports failed row details through the failure results workflow without corrupting previously valid org state.
3. **Given** the engineer opens the dashboard Bulk views, **When** they inspect a completed job, **Then** they can see job metadata, row result categories, and resulting org mutations.

**Frontend Deliverables**:

- Bulk job console with create/upload/close inspection, status summaries, and successful/failed/unprocessed result panes.

**Parity Check**:

- Compare representative Bulk ingest job lifecycle and result responses against `dev20`, including job states, counts, and CSV result shapes for the supported operation set.

---

### Feature 4 - Metadata APIs (Priority: P5)

An engineer submits a basic metadata deploy, checks deploy status, and inspects supported metadata listings and descriptions locally through SOAP clients, supported Metadata REST resources, and the dashboard.

**Why this priority**: Metadata compatibility broadens the app from data APIs to deployment-oriented workflows used by Salesforce integration platforms.

**Independent Test**: Send deploy, status, cancel, list, and describe SOAP requests plus supported Metadata REST requests to the emulator, inspect the returned payloads in the dashboard, and verify the same flows against supported client expectations.

**Acceptance Scenarios**:

1. **Given** a basic deploy request, **When** the client submits it and later checks status, **Then** the emulator returns a deploy identifier and a completed status payload that the client can interpret successfully.
2. **Given** a client requests metadata listings or metadata type descriptions, **When** the emulator receives those requests, **Then** it returns compatible SOAP responses for the supported metadata types.
3. **Given** a client requests supported Metadata REST resources, **When** the emulator receives those requests, **Then** it returns Salesforce-compatible REST payloads for the approved metadata slices.
4. **Given** the engineer opens the dashboard Metadata views, **When** they inspect supported SOAP or REST metadata workflows, **Then** they can see request details, response envelopes, and deploy lifecycle state.

**Frontend Deliverables**:

- Metadata workflow explorer with deploy/status panels, supported type browser, and SOAP plus Metadata REST inspection.

**Parity Check**:

- Compare representative Metadata deploy, status, cancel, listMetadata, and describeMetadata SOAP flows plus supported Metadata REST resource flows against `dev20`, focusing on body shape, key fields, status codes, and parseable fault behavior.

---

### Feature 5 - App Polish (Priority: P6)

An engineer uses the app as a coherent local Salesforce emulator, with consistent dashboard UX, clear request/response visibility, and documented behavior across the supported surfaces.

**Why this priority**: Polishing the app after the API surfaces exist makes the product easier to use and verify.

**Independent Test**: Walk through the supported dashboard flows for REST, Bulk, and Metadata after reset, verify consistent UI behavior, and confirm all supported local workflows are understandable without reading source code.

**Acceptance Scenarios**:

1. **Given** the supported API slices are implemented, **When** an engineer navigates the dashboard, **Then** the UI presents a clear, consistent flow across REST, Bulk, and Metadata features.
2. **Given** the engineer is troubleshooting a mismatch, **When** they inspect requests and responses in the app, **Then** they can quickly identify the relevant operation, outcome, and payload shape.

**Frontend Deliverables**:

- Final navigation polish, empty states, workflow copy, and consistent inspection UX across all supported surfaces.

**Parity Check**:

- Re-run the approved parity checks across all supported slices and record any accepted deltas still present after polish.

### Edge Cases

- Unsupported SOQL operators or clauses within the covered query surface return a deterministic, Salesforce-compatible error instead of a partial or silently broadened result set.
- Relationship field reads stay deterministic when seeded literal relationship values and record-linked relationship values both exist for the same field path.
- Concurrent upserts using the same external ID must not create ambiguous duplicates or inconsistent success reporting.
- Closing a Bulk ingest job with missing required data, malformed CSV, or an unsupported operation must produce stable job outcomes and row-level failures that tests can assert.
- Reset invoked after partial REST, Bulk, or Metadata mutations must clear transient job and deploy state as well as seeded record mutations.
- Metadata deploy status checks for unknown, canceled, or malformed deploy identifiers must return a compatible failure shape instead of an internal error.
- Parity verification mismatches must be documented immediately so the app does not silently drift from the real Salesforce reference org.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow supported Salesforce REST, Bulk API v2, and Metadata API clients to target `localhost` for the approved slices.
- **FR-002**: System MUST deliver backend behavior and matching frontend workflows for each in-scope feature slice.
- **FR-003**: System MUST preserve the documented Salesforce API shape for all in-scope operations unless a deviation is explicitly approved in this spec.
- **FR-004**: System MUST support repeatable, resettable tests for every in-scope behavior without relying on leaked state from prior runs.
- **FR-005**: System MUST expose structured observability for all changed emulator flows, including surface, operation, and correlation identifiers.
- **FR-006**: System MUST support realistic SOQL filtering and relationship field projection for the approved query slice.
- **FR-007**: System MUST support external-ID upsert behavior that distinguishes between update and create outcomes using Salesforce-compatible request paths, status codes, and success bodies.
- **FR-008**: System MUST support synchronous Bulk API v2 ingest job workflows for the supported operation set.
- **FR-009**: System MUST support basic Metadata API deploy, deploy-status, cancel-deploy, list-metadata, describe-metadata, and approved Metadata REST resource workflows with compatible SOAP or REST result shapes.
- **FR-010**: System MUST define and execute parity verification for each supported slice against `dev20` before the slice is considered complete.
- **FR-011**: System MUST clean up temporary `dev20` records created during parity verification unless an explicit exception is documented.
- **FR-012**: System MUST keep Docker, CI automation, and deployment packaging out of the current implementation scope.

### Key Entities *(include if feature involves data)*

- **Emulated Org State**: The resettable representation of seeded and test-created Salesforce records that local clients query and mutate.
- **SObject Record**: A single emulated Salesforce object row with object type, identity, business fields, external-ID values, and optional relationship references used by queries and upserts.
- **Bulk Ingest Job**: A deterministic ingest workflow containing job identity, requested operation, uploaded rows, completion state, processed counts, and per-row results visible to clients and the dashboard.
- **Metadata Deploy Job**: A deploy workflow containing deploy identity, status, completion outcome, and aggregate deployment counts used by SOAP status checks and dashboard inspection.
- **Reference Org Parity Session**: A bounded verification run against `dev20` that creates optional temporary records, compares emulator behavior to Salesforce responses, records accepted deltas, and cleans up artifacts.

## Assumptions

- The primary compatibility target for this slice is the client behavior already exercised by Prodly-style integrations against API version `v60.0`.
- The emulator may reject advanced Salesforce behaviors that are outside the listed in-scope flows as long as the rejection is deterministic and compatibility-friendly.
- Reset is an explicit part of documented app and test usage rather than an implicit action after every request.
- Parity checks against `dev20` can safely create short-lived, clearly prefixed temporary records when mutation behavior needs verification.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of approved parity checks for the in-scope REST, Bulk, and Metadata workflows pass against localhost or are recorded as explicit accepted deltas.
- **SC-002**: A developer can start the backend and frontend locally and reach the dashboard plus first successful supported API request within 10 minutes using the documented app flow.
- **SC-003**: Re-running the same seeded local integration flow after reset produces identical pass/fail results and identical baseline query outputs in 100% of validation runs.
- **SC-004**: Each completed feature slice includes a usable dashboard workflow alongside the backend behavior it exposes.
- **SC-005**: Temporary records created in `dev20` for parity verification are cleaned up in 100% of documented mutation-check flows.
- **SC-006**: Supported request and error envelopes for the approved slices remain parseable by common Prodly-style clients without client-side code changes beyond pointing the base URL at localhost.
