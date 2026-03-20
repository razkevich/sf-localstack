# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`  
**Created**: [DATE]  
**Status**: Draft  
**Input**: User description: "$ARGUMENTS"

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: [`REST`, `Bulk`, `Metadata` - specify all that apply]
- **Compatibility Target**: [Salesforce API/version/behavior reference being emulated]
- **In-Scope Operations**: [Exact endpoints, jobs, or metadata operations covered]
- **Out-of-Scope Operations**: [Adjacent behavior explicitly deferred to control scope]
- **API Shape Commitments**: [Request fields, response envelopes, status codes, error
  semantics, pagination/batch behavior that MUST remain compatible]
- **Frontend Scope**: [Dashboard pages, explorers, viewers, or workflows added alongside
  the backend slice]
- **Test Isolation Plan**: [How org state is seeded, reset, and verified between
  independent runs]
- **Runtime Reproducibility Controls**: [Seeded data, time/id controls, reset rules, and
  environment constraints needed for stable repeated local runs]
- **Parity Verification Plan**: [How this slice will be compared against the real
  Salesforce reference org, including temporary-record cleanup expectations]

## Feature Iterations *(mandatory)*

<!--
  Organize the spec as feature slices, not infrastructure phases.
  Each slice should be a user-visible increment that includes backend, frontend,
  automated verification, and real-Salesforce parity verification.
-->

### Feature 0 - [Brief Title] (Priority: P1)

[Describe the first shippable slice in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this slice can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]
2. **Given** [initial state], **When** [action], **Then** [expected outcome]

**Frontend Deliverables**:

- [UI behavior, navigation, or inspection flow added in this slice]

**Parity Check**:

- [Representative request/response comparison that must pass against the reference org]

---

### Feature 1 - [Brief Title] (Priority: P2)

[Describe this slice in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this slice can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

**Frontend Deliverables**:

- [UI behavior, navigation, or inspection flow added in this slice]

**Parity Check**:

- [Representative request/response comparison that must pass against the reference org]

---

### Feature 2 - [Brief Title] (Priority: P3)

[Describe this slice in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this slice can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

**Frontend Deliverables**:

- [UI behavior, navigation, or inspection flow added in this slice]

**Parity Check**:

- [Representative request/response comparison that must pass against the reference org]

---

[Add more feature slices as needed, each with an assigned priority]

### Edge Cases

- What happens when [boundary condition]?
- How does the system handle [error scenario]?
- How does the emulator reset state after partial REST, Bulk, or Metadata mutations?
- What happens when Salesforce-compatible error shapes conflict with internal shortcuts?
- What happens when parity verification reveals a real-Salesforce mismatch?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST preserve the documented Salesforce API shape for all in-scope
  operations unless a deviation is explicitly approved in this spec.
- **FR-002**: System MUST deliver backend behavior and matching frontend workflows for
  every in-scope feature slice.
- **FR-003**: System MUST support repeatable, resettable tests for every in-scope
  behavior without relying on leaked state from prior runs.
- **FR-004**: System MUST expose structured observability for all changed emulator flows,
  including surface, operation, and correlation identifiers.
- **FR-005**: System MUST define and execute parity verification for each supported slice
  against the designated Salesforce reference org.
- **FR-006**: System MUST clean up temporary reference-org records created during parity
  verification unless a spec-approved exception is documented.

*Example of marking unclear requirements:*

- **FR-007**: System MUST authenticate users via [NEEDS CLARIFICATION: auth method not specified - email/password, SSO, OAuth?]
- **FR-008**: System MUST retain user data for [NEEDS CLARIFICATION: retention period not specified]

### Key Entities *(include if feature involves data)*

- **[Entity 1]**: [What it represents, key attributes without implementation]
- **[Entity 2]**: [What it represents, relationships to other entities]

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: [Measurable metric, e.g., "Users can complete account creation in under 2 minutes"]
- **SC-002**: [Measurable metric, e.g., "System handles 1000 concurrent users without degradation"]
- **SC-003**: [User satisfaction metric, e.g., "90% of users successfully complete primary task on first attempt"]
- **SC-004**: [Business metric, e.g., "Reduce support tickets related to [X] by 50%"]
- **SC-005**: [Compatibility metric, e.g., "100% of approved Salesforce parity checks pass
  for the in-scope REST, Bulk, or Metadata operations"]
- **SC-006**: [Reproducibility metric, e.g., "Repeated local runs after reset complete with
  identical results and no state leakage across test cases"]
