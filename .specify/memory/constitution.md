<!--
Sync Impact Report
Version change: 1.0.0 -> 1.1.0
Modified principles:
- III. Deterministic CI Reproducibility -> III. Deterministic Runtime Reproducibility
Added sections:
- Real Salesforce Parity Verification
Removed sections:
- None
Templates requiring updates:
- ✅ updated `.specify/templates/plan-template.md`
- ✅ updated `.specify/templates/spec-template.md`
- ✅ updated `.specify/templates/tasks-template.md`
- ✅ no `.specify/templates/commands/*.md` files present
- ✅ updated runtime guidance in `intro.md`, `specs/001-sf-ci-emulator/quickstart.md`, and `specs/001-sf-ci-emulator/research.md`
Follow-up TODOs:
- None
-->

# sf-localstack Constitution

## Core Principles

### I. Salesforce API Fidelity
sf-localstack MUST emulate Salesforce behavior at the API boundary before it optimizes
for internal convenience. REST, Bulk, and Metadata surfaces MUST preserve request and
response shape compatibility, status code semantics, field naming, pagination patterns,
and documented error envelopes wherever the selected compatibility target defines them.
Any intentional deviation MUST be called out in the spec, represented in tests, and
gated behind explicit scope language so consumers can reason about fidelity.

Rationale: the product only creates value if client code, SDK integrations, and test
suites experience the emulator as a trustworthy stand-in for Salesforce.

### II. Test-First Emulator Behavior
Every change to emulator behavior MUST begin with failing tests that describe the
external contract first. Specs and plans MUST define contract, integration, and reset
expectations for each affected surface; implementation starts only after the intended
behavior is encoded as executable tests. Test fixtures MUST support full reset between
runs so each case can create, mutate, and destroy org state without leaking state into
subsequent tests.

Rationale: emulator correctness is proven through externally visible behavior, and
resettable isolation is necessary to keep the test suite trustworthy and debuggable.

### III. Deterministic Runtime Reproducibility
Local development and repeated test runs MUST produce the same outcomes from the same
inputs. Tests, snapshots, fixtures, clocks, seeded data, and reset flows MUST be
deterministic by default. New work MUST avoid hidden environmental dependencies such as
undeclared services, unstable clocks, or time-sensitive assertions that make emulator
behavior drift between runs.

Rationale: emulator regressions are expensive to diagnose when runtime behavior cannot
be reproduced exactly on a contributor's machine.

### IV. Minimal Dependency Surface
sf-localstack MUST prefer the smallest practical dependency set and the narrowest
integration boundary that can satisfy current scope. New libraries, services, and build
tooling MUST be justified in the plan with a concrete need that cannot be met by the
existing stack. Features MUST grow incrementally by surface area and behavior slice,
starting with the smallest REST, Bulk, or Metadata contract that unlocks user value
without committing the project to unsupported adjacent semantics.

Rationale: a thin dependency surface reduces drift, startup cost, supply-chain risk,
and the maintenance burden of keeping emulator behavior aligned with Salesforce.

### V. Operational Observability
Every user-visible emulator workflow MUST emit enough structured telemetry to explain
what happened, why it happened, and how to reproduce it. Logs, metrics, traces, and
debug artifacts MUST identify the API surface, operation, compatibility mode, request
correlation id, and reset lifecycle events. Observability MUST be designed as part of
the feature, not added after incidents.

Rationale: high-fidelity emulation requires operators and contributors to quickly locate
behavior mismatches and isolate failing scenarios.

## API Surface Scope & Compatibility

- Every spec MUST declare the targeted Salesforce surface (`REST`, `Bulk`, `Metadata`)
  and the exact operations, resources, and version assumptions in scope.
- Specs MUST define the expected request shape, response shape, error behavior, and
  compatibility constraints for each in-scope endpoint or operation.
- Plans MUST preserve API-shape compatibility by naming the source contract, planned
  parity tests, and any explicitly accepted deviations.
- Work MUST expand incrementally: ship the smallest coherent compatibility slice first,
  then add adjacent operations only when prior slices remain green under the existing
  regression suite.
- Cross-surface abstractions are allowed only when they do not blur user-visible
  semantics or make one surface's behavior diverge from Salesforce to satisfy another.

## Real Salesforce Parity Verification

- Every supported feature slice MUST define how it will be verified against a real
  Salesforce org before the slice is considered done.
- Parity verification MUST compare the emulator with the designated reference org for
  request paths, status codes, top-level response shape, key field names, and supported
  error envelopes.
- Parity checks MAY use temporary records in the reference org when mutation behavior is
  part of the supported slice, but those records MUST be clearly namespaced and cleaned
  up after verification.
- Any accepted delta between emulator behavior and the reference org MUST be documented
  in the spec, plan, or contract notes for that slice.

## Delivery Workflow & Quality Gates

- Specs MUST organize delivery by user-visible feature slices and note the backend,
  frontend, tests, and parity verification required for each slice.
- Plans MUST fail constitution review if they omit contract tests, reset/isolation
  behavior, parity verification, observability changes, or dependency justification for
  the proposed work.
- Tasks MUST sequence work test-first: write failing backend tests, implement backend,
  build the matching frontend, validate integration behavior, then run parity checks.
- Merge readiness requires passing contract and integration coverage for all changed
  surfaces plus proof that test state resets cleanly across repeated runs.
- Scope increases across REST, Bulk, and Metadata MUST happen by explicit plan update;
  hidden scope creep is non-compliant even if implementation appears reusable.

## Governance

This constitution overrides conflicting local process. Every spec, plan, task list,
review, and pull request MUST include an explicit constitution compliance check.

Amendments require: (1) a written rationale, (2) updates to dependent templates and
workflow guidance in the same change, and (3) semantic versioning of the constitution
itself. Versioning rules are: MAJOR for incompatible governance changes or removed
principles, MINOR for new principles or materially stronger requirements, and PATCH for
clarifications that do not change enforcement.

Compliance reviews MUST verify API-shape compatibility commitments, resettable test
isolation, runtime reproducibility controls, dependency justification, real-Salesforce
parity verification, and operational observability before implementation approval and
again before merge.

**Version**: 1.1.0 | **Ratified**: 2026-03-19 | **Last Amended**: 2026-03-19
