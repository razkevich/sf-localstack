# sf-localstack — Detailed Build Specification

## What It Is

A Spring Boot and React app that emulates the Salesforce REST, Bulk, and Metadata APIs for development and integration testing. Engineers point Salesforce client code at `http://localhost:8080` instead of a real Salesforce org, inspect requests and responses in the dashboard, and verify supported behavior against a real org (`dev20`) as each feature slice is built.

## Repository

`/Users/razkevich/code/sf_localstack`, branch `001-sf-ci-emulator`, Java 21 + Spring Boot 3.3.5, Maven multi-module.

## Current State

- Spring Boot backend scaffold exists with version discovery, OAuth stubs, CRUD endpoints, a basic SOQL query endpoint, reset, and dashboard request log APIs.
- `OrgStateService` stores records in H2 with seeded baseline data loaded from YAML.
- `SoqlEngine` is still a stub and does not yet support realistic filtering or relationship fidelity.
- Frontend files exist and the dashboard shell is present, but the app needs to be built out feature-by-feature alongside the backend.

## Build Strategy

Build the app in feature slices, not packaging phases.

1. Scaffold the app shell
2. Build REST core + SOQL
3. Build REST upsert + error fidelity
4. Build Bulk API v2
5. Build Metadata SOAP
6. Polish the app

Every slice includes:

- backend tests
- backend implementation
- frontend tests
- frontend implementation
- local integration verification
- parity verification against `dev20`
- cleanup of temporary `dev20` records

## Current Priority Features

### 1. App Scaffold

- stabilize the Spring Boot + dashboard shell
- expose request log and reset flows cleanly
- create reusable frontend API/types and request inspection primitives

### 2. REST Core + SOQL

- replace the current regex SOQL stub with supported filtering and relationship projection
- improve REST query, CRUD, and describe fidelity
- build a query runner and org-state inspection UI in the dashboard

### 3. REST Upsert + Error Fidelity

- add external-ID upsert semantics
- tighten supported REST error envelopes and status behavior
- build dashboard mutation and error inspection flows

### 4. Bulk API v2

- add synchronous ingest job lifecycle support
- expose Bulk jobs and row results in the dashboard

### 5. Metadata SOAP

- add deploy, status, cancel, listMetadata, and describeMetadata
- expose Metadata workflows and SOAP inspection in the dashboard

## Explicit Non-Goals For Now

- Docker
- CI automation
- deployment packaging

Those can be revisited after the app itself is working.

## Parity Verification

Use `dev20` as the real-Salesforce reference org.

For each feature slice:

- run the local emulator flow
- run the equivalent flow against `dev20`
- compare status, top-level response shape, key fields, and supported error envelopes
- clean up temporary Salesforce records immediately after mutation checks

## Key Constraints

- Java 21, Spring Boot 3.3.5, no new Maven dependencies
- API compatibility focus is Salesforce `v60.0`
- resettable seeded state stays in scope because it is part of the app workflow
- SOAP uses standard XML parsers and string templates
- CSV parsing stays manual within the current dependency set
