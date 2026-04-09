<!--
Sync Impact Report
Version change: 1.1.0 -> 2.0.0
Modified principles:
- IV. Minimal Dependency Surface -> IV. Minimal Dependency Surface (unchanged)
- V. Operational Observability -> V. Operational Observability (unchanged)
Added sections:
- VI. Persistent & Extensible Storage
- VII. Authentication & Authorization
- VIII. Salesforce-Familiar User Interface
- Extensibility Strategy (new section)
- MVP Scope Boundaries (new section)
Removed sections:
- None
Templates requiring updates:
- ✅ reviewed `.specify/templates/plan-template.md` (no changes needed — constitution check covers new principles via generic gate)
- ✅ reviewed `.specify/templates/spec-template.md` (no changes needed — spec template is generic enough)
- ✅ reviewed `.specify/templates/tasks-template.md` (no changes needed — task template is generic enough)
- ✅ no `.specify/templates/commands/*.md` files present
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

### VI. Persistent & Extensible Storage
All user-visible state (sObject records, bulk jobs, metadata resources, user accounts)
MUST survive application restarts. Storage implementations MUST be accessed through
interfaces so that the backing store can be swapped without changing service logic.
MVP implementations MAY use simple backing stores (H2 file-based, JSON files) but MUST
NOT leak storage implementation details into service or controller layers.

Rationale: persistence is a prerequisite for SaaS deployment and multi-session
workflows. Interface-driven storage enables migration from MVP shortcuts to
production-grade databases without architectural rewrites.

### VII. Authentication & Authorization
All API endpoints serving Salesforce protocol surfaces (`/services/*`) MUST require
valid authentication. The authentication mechanism MUST be accessed through a `UserStore`
interface so implementations can be swapped (file-based, DB-backed, external provider).
Authorization MUST enforce role-based access where defined. The OAuth2 token endpoint
MUST issue real tokens that the auth filter validates, maintaining sf CLI compatibility.

Rationale: authentication is required for SaaS deployment and multi-user access.
Interface-driven auth enables migration from file-based MVP to production identity
providers without changing the rest of the application.

### VIII. Salesforce-Familiar User Interface
The web UI MUST be intuitively familiar to Salesforce users. Data MUST be presented in
tables with sortable columns, not raw JSON. Record editing MUST use form controls, not
text areas. Navigation MUST follow Lightning-style patterns (global nav bar, object
sidebar, list views, record detail pages). The UI MUST use SLDS-inspired design tokens
(colors, typography, spacing) to create visual familiarity without requiring the full
SLDS dependency.

Rationale: the product targets Salesforce developers and admins who expect to interact
with data and metadata using patterns they already know from Salesforce orgs.

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

## Extensibility Strategy

- Every infrastructure boundary (storage, authentication, configuration) MUST be defined
  by a Java interface with the MVP implementation as one concrete class.
- Swapping to a production-grade implementation MUST require only: (1) a new class
  implementing the interface, (2) a Spring profile or configuration change. No service
  or controller code should need modification.
- Each extensible boundary MUST be documented in `docs/extensibility.md` with: the
  interface name, the MVP implementation, and instructions for creating a production
  replacement.
- Extensibility is a design constraint, not a feature. MVP implementations SHOULD be
  simple and direct. Over-engineering for hypothetical future needs violates Principle IV.

## MVP Scope Boundaries

- The MVP targets single-user, single-org deployment with a clear path to multi-tenant.
- Features MUST be production-quality in what they implement but MAY use simple backing
  implementations (file-based storage, self-signed JWT, H2 database).
- The following are explicitly out of MVP scope: multi-tenant isolation, rate limiting,
  cloud infrastructure provisioning, SOAP APIs beyond Metadata, Streaming API, Platform
  Events, Apex execution, and custom field/object definition.
- Descoped items MUST be documented in the master design doc with their extensibility
  path so future work has a clear starting point.

## Delivery Workflow & Quality Gates

- Specs MUST organize delivery by user-visible feature slices and note the backend,
  frontend, tests, and parity verification required for each slice.
- Plans MUST fail constitution review if they omit contract tests, reset/isolation
  behavior, parity verification, observability changes, or dependency justification for
  the proposed work.
- Tasks MUST sequence work test-first: write failing backend tests, implement backend,
  build the matching frontend, validate integration behavior, then run parity checks.
- Each feature MUST be implemented on its own branch with a pull request created and
  merged after implementation and testing is complete.
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
parity verification, operational observability, persistent storage boundaries,
authentication enforcement, and UI design alignment before implementation approval
and again before merge.

**Version**: 2.0.0 | **Ratified**: 2026-03-19 | **Last Amended**: 2026-04-09
