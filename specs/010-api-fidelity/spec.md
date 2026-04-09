# Feature Specification: API Fidelity Fixes

**Feature Branch**: `010-api-fidelity`
**Status**: Draft
**Created**: 2026-04-09

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: REST, Bulk API v2, Metadata API (all surfaces where gaps are found by F4 parity tests)
- **Compatibility Target**: Real Salesforce org "dev 20" response shapes, HTTP status codes, error formats, and headers as of API version `v60.0`
- **In-Scope Operations**: Fix response shapes, HTTP status codes, error formats, response headers, and missing fields across all emulated surfaces; generate valid 18-char Salesforce IDs; align CSV output format for Bulk results
- **Out-of-Scope Operations**: New API surfaces not currently emulated; SOAP beyond Metadata API; any gap that requires architectural changes (those are flagged and deferred to a separate spec)
- **API Shape Commitments**: All error responses MUST use the Salesforce JSON error array format. All REST responses MUST include `Sforce-Limit-Info` and `Content-Type` headers. Record responses MUST include `attributes` wrappers. Query responses MUST include `totalSize`, `done`, and `records`. Salesforce IDs MUST be valid 18-char format.
- **Frontend Scope**: No new frontend features required for this slice. Existing dashboard benefits passively from corrected API response shapes.
- **Test Isolation Plan**: Each fix gets a dedicated regression test that asserts the exact response shape, status code, or header value. Regression tests run in isolation with a fresh emulator state seeded from the standard YAML baseline.
- **Parity Verification Plan**: F4 parity tests are re-run after each batch of fixes to measure pass rate improvement. The target is >90% pass rate for critical and major gap categories before this feature is considered complete.

## Feature Iterations *(mandatory)*

### Feature 0 - Error Response Format Alignment (Priority: P1)

Ensure all error responses across the emulator match the Salesforce JSON error array format and use correct HTTP status codes.

**Why this priority**: Error format is the single highest-impact compatibility gap. Clients parse error arrays to distinguish error types; a wrong format breaks every error-handling code path at once.

**Acceptance Scenarios**:

1. **Given** a SOQL query with an invalid field name, **When** the emulator processes the request, **Then** it returns HTTP 400 with body `[{"message": "...", "errorCode": "INVALID_FIELD", "fields": ["<fieldName>"]}]`.
2. **Given** a request referencing a non-existent object type, **When** the emulator processes the request, **Then** it returns HTTP 400 with body `[{"message": "...", "errorCode": "INVALID_TYPE", "fields": []}]`.
3. **Given** a malformed SOQL query string, **When** the emulator processes the request, **Then** it returns HTTP 400 with body `[{"message": "...", "errorCode": "MALFORMED_QUERY", "fields": []}]`.
4. **Given** a request for a record ID that does not exist, **When** the emulator processes the request, **Then** it returns HTTP 404 with body `[{"message": "...", "errorCode": "NOT_FOUND", "fields": []}]`.
5. **Given** an unauthenticated request to a protected endpoint, **When** the emulator processes the request, **Then** it returns HTTP 401 with body `[{"message": "Session expired or invalid", "errorCode": "INVALID_SESSION_ID"}]`.
6. **Given** a create request missing a required field, **When** the emulator processes the request, **Then** it returns HTTP 400 with body `[{"message": "...", "errorCode": "REQUIRED_FIELD_MISSING", "fields": ["<fieldName>"]}]`.
7. **Given** an internal server error, **When** the emulator encounters an unhandled exception, **Then** it returns HTTP 500 with body `[{"message": "An unexpected error occurred.", "errorCode": "UNKNOWN_EXCEPTION", "fields": []}]` rather than a Spring default error page.

**Key Deliverables**:

- `SalesforceErrorBuilder` utility class for constructing error response arrays with consistent field population
- Global exception handler that maps internal exceptions to Salesforce error codes and HTTP status codes
- Error code mapping table:

| Scenario | `errorCode` | HTTP Status |
|---|---|---|
| Bad request / malformed input | `INVALID_FIELD`, `INVALID_TYPE`, `MALFORMED_QUERY` | 400 |
| Authentication failure | `INVALID_SESSION_ID` | 401 |
| Forbidden | `INSUFFICIENT_ACCESS` | 403 |
| Record not found | `NOT_FOUND`, `ENTITY_IS_DELETED` | 404 |
| Duplicate / conflict | `DUPLICATE_VALUE` | 409 |
| Missing required field | `REQUIRED_FIELD_MISSING` | 400 |
| Internal error | `UNKNOWN_EXCEPTION` | 500 |

---

### Feature 1 - REST Response Shape Fixes (Priority: P1)

Correct the shape of REST API responses to match real Salesforce, covering describe, record, query, create, list-objects, and version-discovery endpoints.

**Why this priority**: Response shape mismatches silently break client deserialization. Clients written against real Salesforce expect specific top-level fields; missing fields cause null-pointer errors or silent data loss downstream.

**Acceptance Scenarios**:

1. **Given** a `GET /services/data/v60.0/sobjects/{object}/describe` request, **When** the emulator responds, **Then** the response includes at minimum: `name`, `label`, `keyPrefix`, `custom`, `fields[]`, `recordTypeInfos[]`, and `childRelationships[]` at the top level.
2. **Given** a `GET /services/data/v60.0/sobjects/{object}/{id}` request, **When** the emulator responds with a record, **Then** the record JSON includes an `attributes` object with `type` and `url` keys as the first field.
3. **Given** a `GET /services/data/v60.0/query?q=...` request, **When** the emulator responds, **Then** the response includes `totalSize` (integer), `done` (boolean), and `records` (array); each record in `records` includes an `attributes` object; if results are paginated, `nextRecordsUrl` is present.
4. **Given** a `POST /services/data/v60.0/sobjects/{object}` create request, **When** the emulator responds with success, **Then** the response body is `{"id": "<18-char-id>", "success": true, "errors": []}`.
5. **Given** a `GET /services/data/v60.0/sobjects` list-objects request, **When** the emulator responds, **Then** the response includes `encoding`, `maxBatchSize`, and `sobjects` array; each sobject entry includes `name`, `label`, and `keyPrefix`.
6. **Given** a `GET /services/data` version-discovery request, **When** the emulator responds, **Then** the response is an array of version objects each containing `version`, `label`, and `url`.

---

### Feature 2 - HTTP Headers Alignment (Priority: P1)

Add and correct HTTP response headers so they match the headers returned by real Salesforce on all REST and SOAP responses.

**Why this priority**: Some clients and middleware inspect headers before processing the body. Missing `Sforce-Limit-Info` causes API usage tracking tools to fail silently; wrong `Content-Type` can cause JSON parsers to reject responses.

**Acceptance Scenarios**:

1. **Given** any REST API response, **When** the client inspects response headers, **Then** `Sforce-Limit-Info: api-usage=X/15000` is present where X is the request count for the current session.
2. **Given** any REST API response, **When** the client inspects response headers, **Then** `Content-Type` is `application/json;charset=UTF-8`.
3. **Given** any SOAP/Metadata API response, **When** the client inspects response headers, **Then** `Content-Type` is `text/xml;charset=utf-8`.
4. **Given** a REST request that includes `Sforce-Query-Options: batchSize=200`, **When** the emulator processes a query, **Then** the batch size from the header is applied to paginate results at the specified size.
5. **Given** a successful `POST` create request that returns HTTP 201, **When** the client inspects response headers, **Then** a `Location` header is present containing the URL of the newly created resource.

**Key Deliverables**:

- Spring `HandlerInterceptor` (or response filter) that appends `Sforce-Limit-Info` to all REST responses
- Correction of `Content-Type` headers on REST and SOAP controllers
- `Sforce-Query-Options` header parsing in query controller
- `Location` header on 201 responses from create endpoints

---

### Feature 3 - Bulk API CSV Format Fixes (Priority: P2)

Correct the CSV output format for Bulk API v2 job results to match the exact format produced by real Salesforce.

**Why this priority**: Bulk job result CSV is parsed directly by data-loading tools. Column ordering and quoting inconsistencies cause silent row-mapping errors or parse failures in downstream consumers.

**Acceptance Scenarios**:

1. **Given** a completed Bulk ingest job with successful rows, **When** the client fetches the successful results CSV, **Then** the columns are ordered `sf__Id`, `sf__Created`, followed by the data columns in the same order they were submitted.
2. **Given** a completed Bulk ingest job with failed rows, **When** the client fetches the failed results CSV, **Then** the columns are ordered `sf__Id`, `sf__Error`, followed by the data columns.
3. **Given** a successful result row where a field value contains a comma, **When** the emulator writes the CSV, **Then** the field is enclosed in double-quotes.
4. **Given** a successful result row where a field value contains a newline, **When** the emulator writes the CSV, **Then** the field is enclosed in double-quotes and the newline is preserved inside the quoted value.
5. **Given** a successful result row where a field value contains a double-quote character, **When** the emulator writes the CSV, **Then** the double-quote is escaped as `""` within a double-quoted field.
6. **Given** a result row where a field has no value, **When** the emulator writes the CSV, **Then** the field appears as an empty string between delimiters with no quoting applied.

---

### Feature 4 - ID Format Handling (Priority: P2)

Generate valid 18-character Salesforce IDs with correct case-insensitive check digits, and accept both 15-char and 18-char IDs in API requests.

**Why this priority**: Tools that validate or compare Salesforce IDs by length or check digit silently misidentify records when given 15-char IDs or IDs with wrong check digits, causing lookup failures.

**Acceptance Scenarios**:

1. **Given** a `POST` request creates a new record, **When** the emulator responds, **Then** the `id` field in the response is exactly 18 characters and passes the standard Salesforce check-digit algorithm.
2. **Given** a request URL contains a 15-char Salesforce ID, **When** the emulator processes the request, **Then** it resolves the record correctly (as if the full 18-char ID had been supplied).
3. **Given** a request URL contains an 18-char Salesforce ID, **When** the emulator processes the request, **Then** it resolves the record correctly.
4. **Given** a request URL contains a string that is neither 15 nor 18 characters, or contains characters outside the valid Salesforce ID character set, **When** the emulator processes the request, **Then** it returns HTTP 400 with `errorCode: INVALID_ID_FIELD`.
5. **Given** all existing records in the seed data, **When** the emulator starts, **Then** all seed record IDs conform to valid 18-char format with correct check digits.

**Key Deliverables**:

- `SalesforceIdUtils` utility: `generate(keyPrefix)` produces a valid 18-char ID; `normalize(id)` accepts 15-char and returns 18-char; `isValid(id)` validates format and check digit
- Replacement of any `UUID`-based ID generation with `SalesforceIdUtils.generate()`
- ID normalization at request-handling boundaries before record lookup

---

### Feature 5 - Gap Report Driven Fixes (Priority: P2)

Address remaining gaps identified in the F4 parity report that are not covered by Features 0–4, prioritized by severity category.

**Why this priority**: This catch-all iteration ensures the overall parity pass rate target is met after the structural fixes in Features 0–4 are applied.

**Acceptance Scenarios**:

1. **Given** the F4 gap report, **When** all critical-severity gaps have been resolved, **Then** each resolved gap has a corresponding regression test that asserts the corrected behavior.
2. **Given** the F4 gap report, **When** all major-severity gaps have been resolved, **Then** each resolved gap has a corresponding regression test that asserts the corrected behavior.
3. **Given** all critical and major gaps are fixed, **When** the F4 parity test suite is re-run against the emulator, **Then** the pass rate for critical and major categories exceeds 90%.
4. **Given** a gap that requires architectural changes to fix, **When** it is identified during this iteration, **Then** it is flagged in the gap report with a `DEFERRED` label and a note linking to the planned follow-on spec; it is not implemented in this feature.

**Process**:

- Triage gap report entries by severity: Critical → Major → Minor → Cosmetic
- For each Critical and Major gap: implement fix, write regression test, mark gap resolved
- After each batch of fixes, re-run F4 parity suite and record updated pass rate
- Minor and Cosmetic gaps are addressed only if time permits and do not affect the >90% pass rate target

---

### Edge Cases

- What if a gap requires architectural changes? Flag it in the gap report with `DEFERRED` status and a note pointing to the planned follow-on spec; do not implement it in this feature.
- What about Salesforce API version differences? This feature targets v60.0 only. Behavior differences in other versions are out of scope.
- What about deprecated fields in describe responses? Include deprecated fields with `isDeprecatedAndHidden: true` so clients that reference them do not break.
- What about SOAP fault format? SOAP fault responses from the Metadata API MUST use the standard Salesforce SOAP fault envelope: `<soapenv:Fault><faultcode>...</faultcode><faultstring>...</faultstring></soapenv:Fault>`.
- What about the `Sforce-Limit-Info` counter across sessions? The emulator maintains a per-session counter; it is reset on org reset and starts at 0 for each new session.
- What about bulk jobs with zero rows? Result CSV files for zero-row jobs MUST still include the header row.
- What about ID normalization for IDs embedded in request bodies (not just URLs)? Normalize IDs in request bodies at the deserialization boundary as well.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Error responses MUST match the Salesforce JSON format exactly: `[{"message": "...", "errorCode": "...", "fields": [...]}]`
- **FR-002**: HTTP status codes MUST match Salesforce behavior for each error type as defined in the Feature 0 error code mapping table
- **FR-003**: All REST responses MUST include the `Sforce-Limit-Info` header with `api-usage=X/15000` and `Content-Type: application/json;charset=UTF-8`
- **FR-004**: All Metadata/SOAP responses MUST include `Content-Type: text/xml;charset=utf-8`
- **FR-005**: Salesforce record IDs MUST be valid 18-char format with correct check digits in all API responses
- **FR-006**: The emulator MUST accept both 15-char and 18-char IDs in request URLs and body fields
- **FR-007**: Bulk API v2 successful result CSV MUST use column order: `sf__Id`, `sf__Created`, then data columns
- **FR-008**: Bulk API v2 failed result CSV MUST use column order: `sf__Id`, `sf__Error`, then data columns
- **FR-009**: CSV output MUST quote fields containing commas, newlines, or double-quotes, following RFC 4180 escaping rules
- **FR-010**: Each fix from any Feature iteration MUST have a corresponding regression test verifying the exact corrected behavior
- **FR-011**: The F4 parity test pass rate for critical and major gap categories MUST exceed 90% after all fixes are applied
- **FR-012**: Gaps requiring architectural changes MUST be flagged as `DEFERRED` and not implemented in this feature
- **FR-013**: The `Location` header MUST be present on all HTTP 201 Created responses pointing to the newly created resource URL
- **FR-014**: Successful create responses MUST have the shape `{"id": "<18-char-id>", "success": true, "errors": []}`

### Key Entities

- **SalesforceErrorBuilder**: Utility for constructing `[{"message", "errorCode", "fields"}]` error arrays; used by all error-handling paths
- **SalesforceIdUtils**: Utility for generating, normalizing, and validating 18-char Salesforce IDs with check digits
- **GlobalExceptionHandler**: Spring `@ControllerAdvice` that maps internal exceptions to Salesforce-compatible error responses
- **ResponseHeaderFilter**: Servlet filter or Spring interceptor that appends `Sforce-Limit-Info` and enforces correct `Content-Type` on all REST responses
- **BulkCsvWriter**: Component responsible for producing RFC 4180-compliant CSV with correct column ordering for Bulk job results
- **GapReportEntry**: Record of a parity gap with fields: `id`, `surface`, `severity` (Critical/Major/Minor/Cosmetic), `description`, `status` (Open/Fixed/Deferred), `regressionTestRef`

## Assumptions

- The F4 parity test suite is available and runnable before this feature begins; gap report entries are already triaged by severity.
- Fixing error response format, response shapes, and headers is sufficient to resolve the majority of critical and major parity gaps without architectural changes.
- The emulator uses a single API version (v60.0); multi-version routing is not required.
- Seed data IDs that do not currently conform to 18-char format will be migrated as part of Feature 4; no existing tests will be broken by this migration because tests reference records by field values, not hardcoded IDs.
- The `Sforce-Limit-Info` counter does not need to persist across restarts; it resets when the service restarts or the org is reset.
- SOAP fault format fixes apply only to the Metadata API SOAP surface; other SOAP surfaces are out of scope.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `sf` CLI commands (query, create, describe, bulk ingest) work without errors against sf_localstack after this feature is complete
- **SC-002**: Every error scenario covered by FR-001 returns a response body that is byte-for-byte parseable as a Salesforce error array by the standard Salesforce Java/JavaScript clients
- **SC-003**: The F4 parity test suite pass rate for critical and major gap categories reaches >90% when run against the patched emulator
- **SC-004**: All REST responses include correct `Sforce-Limit-Info` and `Content-Type` headers, verified by header-assertion tests for each endpoint family
- **SC-005**: Both 15-char and 18-char IDs resolve correctly in record lookup requests; an invalid-format ID returns HTTP 400 with `INVALID_ID_FIELD`
- **SC-006**: Bulk API v2 result CSV files match real Salesforce column order and RFC 4180 quoting, verified by CSV diff tests against dev 20 sample outputs
