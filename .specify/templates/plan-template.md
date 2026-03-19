# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]
**Project Type**: [e.g., library/cli/web-service/mobile-app/compiler/desktop-app or NEEDS CLARIFICATION]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before implementation. Re-check after design updates.*

- **API Fidelity**: Identify the Salesforce surface (`REST`, `Bulk`, `Metadata`),
  compatibility target, and every request/response/error shape that must remain
  compatible.
- **Test-First**: List the failing contract and integration tests that will be written
  before implementation, plus how state reset/isolation is enforced between test runs.
- **Runtime Reproducibility**: Document fixture seeds, reset behavior, time/id controls,
  and environment constraints required for stable repeated local runs.
- **Dependency Surface**: Justify each new dependency, service, or abstraction and explain
  why a smaller option is insufficient.
- **Observability**: Define logs, metrics, traces, and correlation identifiers needed to
  debug the new behavior during feature development.
- **Scope Control**: Confirm the smallest viable compatibility slice being delivered now
  and identify what adjacent REST, Bulk, or Metadata behavior is explicitly out of scope.
- **Parity Verification**: Define how the emulator will be compared with the reference
  Salesforce org, including mutation cleanup and accepted-delta recording.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### Source Code (repository root)

```text
[Replace with the real project tree used by this feature]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Feature Iteration Strategy

### Feature 0: [Name]

- **Backend Scope**: [services/controllers/contracts delivered]
- **Frontend Scope**: [pages/components/flows delivered]
- **Tests First**: [backend + integration tests written first]
- **Integration Verification**: [local workflow used to validate the slice]
- **Parity Verification**: [how this slice is checked against the reference org]

### Feature 1: [Name]

- **Backend Scope**: [services/controllers/contracts delivered]
- **Frontend Scope**: [pages/components/flows delivered]
- **Tests First**: [backend + integration tests written first]
- **Integration Verification**: [local workflow used to validate the slice]
- **Parity Verification**: [how this slice is checked against the reference org]

[Add more features as needed]

## Salesforce Parity Verification

- **Reference Org**: [alias/org name, e.g. `dev20`]
- **Parity Method**: [CLI commands, direct REST/SOAP requests, or helper scripts]
- **Compared Signals**: [status codes, top-level response shape, key fields, error envelopes]
- **Mutation Policy**: [naming convention for temporary records and cleanup expectations]
- **Accepted Deltas**: [known differences explicitly tolerated for this slice]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
