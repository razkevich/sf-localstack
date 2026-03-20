# Implementation Plan: Usable Salesforce Integration-Test Emulator

**Branch**: `001-sf-ci-emulator` | **Date**: 2026-03-19 | **Spec**: `/Users/razkevich/code/sf_localstack/specs/001-sf-ci-emulator/spec.md`
**Input**: Feature specification from `/Users/razkevich/code/sf_localstack/specs/001-sf-ci-emulator/spec.md`

## Summary

Turn the current scaffold into a working Spring Boot plus React app that emulates the most important Salesforce v60.0 flows in feature slices: stabilize the dashboard and shared app shell, deliver REST CRUD/query/describe support with realistic SOQL, add external-ID upsert and REST error fidelity, then add synchronous Bulk API v2 ingest and basic Metadata SOAP workflows, validating each slice locally and against `dev20` before moving on. The next deferred slice after the shipped set is Metadata retrieve via `package.xml`.

## Technical Context

**Language/Version**: Java 21 for backend, TypeScript 5.x with React 18 for dashboard  
**Primary Dependencies**: Spring Boot 3.3.5 (`web`, `data-jpa`, `actuator`, `test`), H2, Jackson JSON/YAML, React 18, Vite 5, Tailwind 3  
**Storage**: H2 in-memory database for sObjects; in-memory maps for Bulk jobs and Metadata deploy jobs; YAML seed file for baseline org state  
**Testing**: JUnit 5, Spring Boot Test, MockMvc, AssertJ, frontend type-safe rendering/build checks, and targeted parity verification against `dev20`  
**Target Platform**: Local developer machines serving the Spring Boot app and frontend dashboard on `localhost:8080`  
**Project Type**: Maven multi-module backend service plus bundled frontend dashboard  
**Performance Goals**: Fast local startup, synchronous completion for supported Bulk and Metadata slices, and UI updates quick enough to inspect request/response behavior without friction  
**Constraints**: Salesforce API compatibility centered on `v60.0`, no new Maven dependencies, no Docker/CI/deployment work in this slice, standard XML parsers for SOAP, manual CSV parsing, resettable seeded org behavior, and cleanup of temporary `dev20` parity data  
**Scale/Scope**: Integration-test datasets in the low-thousands of records, tens of ingest/deploy jobs per run, and only the explicitly scoped REST/Bulk/Metadata operations from the feature spec

## Constitution Check

*GATE: Must pass before implementation. Re-check after design updates.*

- **API Fidelity**: In-scope compatibility covers REST query/CRUD/describe/upsert under `/services/data/v60.0`, Bulk API v2 ingest under `/services/data/v60.0/jobs/ingest`, and Metadata SOAP under `/services/Soap/m/60.0`. Request paths, JSON bodies, SOAP envelopes, state enums, status codes, and Salesforce-like errors remain compatible for the supported slice.
- **Test-First**: Write failing backend tests first for scaffold reset behavior, SOQL filtering plus relationship projection, REST external-ID upsert create/update semantics, supported REST error envelopes, Bulk ingest lifecycle/results, Metadata SOAP operations, and reset clearing transient state.
- **Runtime Reproducibility**: Keep resettable seeded data stable, avoid hidden services beyond `dev20` parity checks, make Bulk and Metadata completion synchronous for the supported slice, and ensure repeated local runs after reset produce the same supported outputs.
- **Dependency Surface**: No new Maven dependencies are required. SOQL parsing, CSV parsing, SOAP routing, and XML rendering use Java/Spring/Jackson facilities already present in the repo.
- **Observability**: Extend request logging and service logs so every new flow records surface (`REST`/`BULK`/`METADATA`), operation, correlation/request id, target object/type, and reset lifecycle events. Dashboard inspection reuses these observability primitives.
- **Scope Control**: This slice builds the app feature-by-feature and explicitly defers Docker, CI automation, deployment packaging, Bulk query jobs, advanced SOQL constructs, async simulation, and then treats Metadata retrieve/package semantics as the next planned slice rather than part of the current shipped baseline.
- **Parity Verification**: Every feature slice is compared with `dev20` using representative requests. Mutation checks must create clearly prefixed temporary records and clean them up immediately after verification.

## Project Structure

### Documentation (this feature)

```text
specs/001-sf-ci-emulator/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── bulk-v2.md
│   ├── metadata-soap.md
│   └── rest-v60.md
└── tasks.md
```

### Source Code (repository root)

```text
service/
├── src/main/java/co/prodly/sflocalstack/
│   ├── config/
│   ├── controller/
│   ├── interceptor/
│   ├── model/
│   ├── repository/
│   └── service/
├── src/main/resources/
│   ├── application*.yml
│   ├── seed/
│   └── static/
└── src/test/java/co/prodly/sflocalstack/
    ├── controller/
    ├── integration/
    └── service/

frontend/
├── src/
│   ├── components/
│   ├── hooks/
│   ├── services/
│   └── types/
├── index.html
├── package.json
└── vite.config.ts

client/
└── pom.xml

intro.md
README.md
AGENTS.md
```

**Structure Decision**: Keep the current multi-module layout. All emulator behavior lands in `service/`, the dashboard continues as the user-facing inspection app in `frontend/`, and the feature docs under `specs/001-sf-ci-emulator/` define the implementation and parity workflow.

## Feature Iteration Strategy

### Feature 0: App Scaffold

- **Backend Scope**: Stabilize existing REST discovery/reset/request-log behavior, expose clean API contracts for the dashboard, and ensure shared request inspection primitives are available.
- **Frontend Scope**: Build the reusable app shell, navigation, request log/detail panels, reset affordance, and shared API client/types.
- **Tests First**: Add failing reset/request-log/backend shell coverage plus frontend shell/rendering coverage.
- **Integration Verification**: Run the app locally, mutate/reset the seeded org, and verify the dashboard reflects request and reset behavior correctly.
- **Parity Verification**: Compare baseline version discovery and simple REST list/query flows against `dev20` for supported fields and envelopes.

### Feature 1: REST Core + SOQL

- **Backend Scope**: Replace the stub SOQL engine, improve query and describe fidelity, and complete the supported CRUD/query slice.
- **Frontend Scope**: Add query runner, object browser, record detail, and response inspection for REST/SOQL behavior.
- **Tests First**: Add failing backend tests for supported SOQL predicates, relationship fields, describe behavior, and REST query/controller flows; add frontend tests for query rendering and result inspection.
- **Integration Verification**: Run local CRUD/query/describe flows through both API calls and dashboard interaction, verifying resettable seeded behavior.
- **Parity Verification**: Compare representative CRUD, query, relationship-field, and describe requests against `dev20`, recording accepted deltas.

### Feature 2: REST Upsert + Error Fidelity

- **Backend Scope**: Add synchronized external-ID upsert, supported REST error helpers, and stable create/update/error semantics.
- **Frontend Scope**: Add upsert playground, mutation outcome display, and error-inspection flows.
- **Tests First**: Add failing backend tests for create/update upsert semantics, concurrency, and supported failure envelopes; add frontend tests for mutation and error rendering.
- **Integration Verification**: Run local create/update/upsert/error flows and inspect the resulting state through the dashboard.
- **Parity Verification**: Compare create/update/upsert and supported error responses against `dev20`, then clean up temporary Salesforce records.

### Feature 3: Bulk API v2

- **Backend Scope**: Add in-memory Bulk ingest jobs, CSV parsing, synchronous close semantics, and result endpoints.
- **Frontend Scope**: Add Bulk job creation, upload inspection, status monitoring, and result panes.
- **Tests First**: Add failing controller/service/integration tests for job lifecycle, row results, and resulting org-state mutations; add frontend tests for job views and result panes.
- **Integration Verification**: Run a complete local ingest flow and verify downstream query visibility plus dashboard inspection.
- **Parity Verification**: Compare representative job lifecycle and CSV result responses against `dev20`, then clean up temporary Salesforce records.

### Feature 4: Metadata SOAP

- **Backend Scope**: Add SOAP routing, namespace-tolerant parsing, deploy tracking, and supported metadata list/describe/status/cancel behavior.
- **Frontend Scope**: Add Metadata workflow explorer, deploy/status inspector, and supported-type browser.
- **Tests First**: Add failing controller/service/integration tests for deploy/status/cancel/list/describe flows; add frontend tests for SOAP workflow rendering.
- **Integration Verification**: Run local SOAP requests and inspect the deploy/status flows through the dashboard.
- **Parity Verification**: Compare representative SOAP envelopes and key response fields against `dev20`, recording accepted deltas.

### Feature 5: App Polish

- **Backend Scope**: Tighten cross-surface consistency, logs, and any remaining supported-envelope gaps.
- **Frontend Scope**: Unify navigation, empty states, request/response views, and workflow copy across all supported surfaces.
- **Tests First**: Add failing cross-surface integration checks and frontend coverage for polished navigation/inspection flows.
- **Integration Verification**: Walk through the complete local REST, Bulk, and Metadata app flows after reset.
- **Parity Verification**: Re-run the approved parity suite across all supported slices and confirm cleanup is complete.

### Feature 6: Metadata Retrieve via `package.xml` (Deferred Next)

- **Backend Scope**: Add Metadata SOAP `retrieve` and `checkRetrieveStatus`, manifest parsing, deterministic ZIP assembly, retrieve job tracking, and error handling for unsupported manifest members.
- **Frontend Scope**: Add a lightweight retrieve inspector to the Metadata manager so engineers can preview retrievable resources and inspect retrieve status/results.
- **Tests First**: Add failing controller/service/integration tests for manifest parsing, retrieve job lifecycle, ZIP content generation, and current-local-metadata round-tripping.
- **Integration Verification**: Run `sf project retrieve start --manifest package.xml` against localhost with supported metadata members and confirm the local project files materialize from the emulator's retrieve result.
- **Parity Verification**: Compare `retrieve` and `checkRetrieveStatus` SOAP flows against `dev20`, then record accepted deltas for unsupported wildcard or packaging behaviors.

## Salesforce Parity Verification

- **Reference Org**: `dev20`
- **Parity Method**: Use direct REST/SOAP calls and Salesforce CLI-assisted checks where helpful, comparing emulator behavior with `dev20` for representative supported flows.
- **Compared Signals**: Endpoint path, method, status code, top-level response shape, key payload fields, and supported error envelope shape.
- **Mutation Policy**: Temporary parity records must use clear prefixes such as `SFLOCALSTACK_TEST_` or dedicated external IDs, and must be deleted immediately after the verification step.
- **Accepted Deltas**: Any approved mismatch with real Salesforce must be recorded in the relevant spec, task, or contract notes before the feature slice is marked complete.

## Complexity Tracking

No constitution violations or extra complexity exemptions are required for this slice.
