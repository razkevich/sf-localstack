# Feature Specification: SOQL Engine Enhancement

**Feature Branch**: `009-soql-engine`
**Created**: 2026-04-09
**Status**: Draft
**Input**: Replace the regex-based `SoqlEngine.java` with a proper recursive-descent parser pipeline (lexer → parser → AST → executor) that supports relationship queries, IN/NOT IN, LIKE, complex WHERE with AND/OR/parentheses, multi-field ORDER BY, LIMIT/OFFSET, COUNT(), and Salesforce-compatible error responses.

## Compatibility Context *(mandatory)*

- **Salesforce Surface**: REST (`/services/data/{v}/query`)
- **Compatibility Target**: SOQL specification as documented by Salesforce, verified against dev 20 org
- **In-Scope Operations**: Lexer tokenization; recursive-descent parser producing a typed AST; executor evaluating the AST against the H2-backed sObject store; operators `=`, `!=`, `<`, `>`, `<=`, `>=`, `LIKE`, `IN`, `NOT IN`; `IS NULL` / `IS NOT NULL`; `AND`, `OR`, and parenthesized groups; multi-field `ORDER BY` with `ASC`/`DESC` and `NULLS FIRST`/`NULLS LAST`; `LIMIT` and `OFFSET`; `COUNT()` aggregate; relationship dot-notation field projection and filtering (parent-to-child); Salesforce-compatible response shape; Salesforce-compatible error codes with character-position messages.
- **Out-of-Scope Operations**: `GROUP BY`, `HAVING`, `ROLLUP`, aggregate functions beyond `COUNT()`, `TYPEOF`, polymorphic queries, semi-joins, anti-joins, bind variables (`:var`), date literals (`TODAY`, `LAST_N_DAYS`, etc.), subqueries in `SELECT`, nested relationship queries (child-to-parent beyond one level), batch SOQL, query more (`queryMore`).
- **API Shape Commitments**: Query response envelope must match Salesforce format `{totalSize, done, records: [{attributes: {type, url}, ...fields}]}`. Error responses must use `[{errorCode, message}]` array shape. `COUNT()` response must use `{totalSize: N, done: true, records: [{"expr0": N}]}`. All supported paths and status codes remain unchanged.
- **Test Isolation Plan**: Each test creates its own records via the REST sObject API before running SOQL assertions, then verifies results against those records. Tests do not depend on seed data. Records are not expected to be cleaned up between test methods but each test selects by values it inserted to avoid cross-contamination.
- **Runtime Reproducibility Controls**: Parser is deterministic (same input → same AST). Executor results depend only on current DB state. No randomness introduced.
- **Parity Verification Plan**: Run the same SOQL queries against dev 20 and sf_localstack; compare `totalSize`, `done`, field presence, field values, ordering, and error `errorCode` / `message` shape.

## Feature Iterations *(mandatory)*

### Feature 0 - Lexer & Parser Foundation (Priority: P1)

A SOQL string is tokenized and parsed into a typed AST that can represent any query within the in-scope surface. Invalid input produces a clear parse error with character position.

**Why this priority**: The lexer and parser are the structural foundation. No executor or error-handling work is possible without a working AST.

**Independent Test**: Unit tests only — no running service required. Feed raw SOQL strings to `SoqlLexer` and `SoqlParser`; assert the resulting AST node types, field names, operator tokens, and literal values.

**Acceptance Scenarios**:

1. **Given** a valid SOQL string `SELECT Id, Name FROM Account WHERE Name = 'Acme' AND AnnualRevenue > 1000 ORDER BY Name ASC LIMIT 10 OFFSET 5`, **When** parsed, **Then** the AST contains a `SelectStatement` with correct `FieldList`, `FromClause`, two `Condition` nodes joined by `AND`, an `OrderByClause` with one field, a `LimitClause` of 10, and an `OffsetClause` of 5.
2. **Given** a SOQL string with `WHERE Status IN ('Open','Closed') OR (Priority != 'Low' AND IsActive = true)`, **When** parsed, **Then** the AST correctly nests the `IN` condition and the parenthesized `AND` group under an `OR` node with proper precedence.
3. **Given** a SOQL string with dot-notation `SELECT Account.Name, FirstName FROM Contact`, **When** parsed, **Then** each `FieldRef` in the `FieldList` carries the correct path segments (`["Account","Name"]` and `["FirstName"]`).
4. **Given** a malformed SOQL string `SELECT FROM Account`, **When** parsed, **Then** a `SoqlParseException` is thrown with a message that includes the token position of the offending token.
5. **Given** SOQL keywords in any mix of case (`select Id from Account where Id != null`), **When** parsed, **Then** keywords are recognized case-insensitively and the AST is identical to the uppercase form.

---

### Feature 1 - Executor with Basic Operators (Priority: P1)

`SoqlExecutor` takes a `SelectStatement` AST and the sObject data source and returns a filtered, sorted, limited result set using all in-scope operators.

**Why this priority**: Without the executor, the parser produces no observable behavior at the REST surface. Both iterations together deliver the first end-to-end improvement.

**Independent Test**: Integration tests hit the running service via `GET /services/data/v60.0/query?q=...`. Each test inserts records via REST, runs a SOQL query, and asserts on the returned `records` array and `totalSize`.

**Acceptance Scenarios**:

1. **Given** Account records with various `AnnualRevenue` values inserted, **When** querying `SELECT Id, Name FROM Account WHERE AnnualRevenue >= 5000 AND AnnualRevenue <= 20000 ORDER BY AnnualRevenue DESC LIMIT 5`, **Then** only records in range are returned, sorted descending, capped at 5.
2. **Given** Contact records with `Status` field values `'Open'`, `'Closed'`, `'Pending'`, **When** querying `WHERE Status IN ('Open','Closed')`, **Then** only Open and Closed contacts are returned; Pending is excluded.
3. **Given** records where some have `NULL` in `Description`, **When** querying `WHERE Description IS NULL`, **Then** only records with null Description are returned; and `WHERE Description IS NOT NULL` returns the complement.
4. **Given** Account records with `Name` values `'Acme'`, `'Acme Corp'`, `'Beta'`, **When** querying `WHERE Name LIKE 'Acme%'`, **Then** only `'Acme'` and `'Acme Corp'` are returned; single-char wildcard `LIKE 'Acme_orp'` matches only `'Acme Corp'`.
5. **Given** records with mixed `Priority` values, **When** querying with `WHERE NOT Priority IN ('Low')`, **Then** only records with non-Low priority are returned.
6. **Given** a query `SELECT COUNT() FROM Account WHERE IsActive = true`, **When** executed, **Then** the response is `{totalSize: N, done: true, records: [{"expr0": N}]}` where `N` matches the number of active accounts.
7. **Given** a query with `ORDER BY Name ASC NULLS LAST` where some records have null `Name`, **When** executed, **Then** non-null names appear first in ascending order; null names appear last.
8. **Given** a query with `OFFSET 10 LIMIT 5` on a 20-record dataset, **When** executed, **Then** records 11–15 are returned in the response.

---

### Feature 2 - Relationship Queries (Priority: P2)

Dot-notation field references in `SELECT`, `WHERE`, and `ORDER BY` resolve parent records through the relationship map so callers can read parent fields without a second query.

**Why this priority**: Relationship field projection is a common Salesforce pattern; the parser already produces dot-notation AST nodes; this iteration makes the executor act on them.

**Independent Test**: Integration tests. Insert an Account and a Contact linked to it. Query `SELECT Account.Name, FirstName FROM Contact WHERE Account.Name = 'Acme'`. Assert the returned record contains `Account: {Name: 'Acme'}` nested under the Contact row.

**Acceptance Scenarios**:

1. **Given** a Contact linked to an Account with `Name = 'Acme'`, **When** querying `SELECT Account.Name, FirstName FROM Contact WHERE Account.Name = 'Acme'`, **Then** the response record contains both `FirstName` and a nested `Account` object with `Name = 'Acme'`.
2. **Given** Opportunity records linked to Accounts, **When** querying `SELECT Name, Account.Name FROM Opportunity ORDER BY Account.Name ASC`, **Then** results are sorted by the parent Account name in ascending order.
3. **Given** a Contact with no related Account (`AccountId = null`), **When** querying `SELECT Account.Name FROM Contact`, **Then** the `Account` field in the response is `null` (not an error).
4. **Given** a query referencing a relationship path that does not exist in the relationship map (e.g. `SELECT Foo.Name FROM Contact`), **When** executed, **Then** an `INVALID_FIELD` error is returned with the unresolvable path in the message.

**Relationship Map (hardcoded, extensible)**:

| Child Object | Relationship Field | Parent Object |
|---|---|---|
| Contact | AccountId | Account |
| Opportunity | AccountId | Account |
| Case | AccountId | Account |
| Case | ContactId | Contact |
| Task | WhatId | (polymorphic, out-of-scope) |

---

### Feature 3 - Field Projection & Response Shape (Priority: P2)

`SELECT`-specified fields are the only fields returned (plus `Id` and `attributes`). The response envelope matches the Salesforce format exactly.

**Why this priority**: Incorrect response shapes break Salesforce clients that deserialize into typed models even when the data is otherwise correct.

**Acceptance Scenarios**:

1. **Given** a query `SELECT Name, Phone FROM Account`, **When** executed, **Then** each record in `records` contains only `attributes`, `Id`, `Name`, and `Phone` — no other fields are present.
2. **Given** any successful query, **When** the response is serialized, **Then** each record's `attributes.type` equals the queried object name (e.g. `"Account"`) and `attributes.url` equals `/services/data/v60.0/sobjects/Account/{id}`.
3. **Given** a result set with 100 records and no `LIMIT`, **When** serialized, **Then** `done` is `true` and `totalSize` equals 100.
4. **Given** a query that matches no records, **When** executed, **Then** the response is `{totalSize: 0, done: true, records: []}` with HTTP 200.

---

### Feature 4 - Error Handling (Priority: P3)

Invalid field names, invalid object types, and malformed SOQL each produce a Salesforce-compatible error response with `errorCode` and a position-aware message.

**Why this priority**: Client code that parses Salesforce error responses must be able to switch to sf_localstack without changing its error-handling paths.

**Acceptance Scenarios**:

1. **Given** a query referencing a field that does not exist on the object (e.g. `SELECT BogusField FROM Account`), **When** executed, **Then** the response is HTTP 400 with body `[{errorCode: "INVALID_FIELD", message: "No such column 'BogusField' on entity 'Account'..."}]`.
2. **Given** a query targeting a non-existent object type (e.g. `SELECT Id FROM Bogus__c`), **When** executed, **Then** the response is HTTP 400 with `[{errorCode: "INVALID_TYPE", message: "sObject type 'Bogus__c' is not supported..."}]`.
3. **Given** a syntactically malformed SOQL string (e.g. `SELECT FROM Account`), **When** the query endpoint receives it, **Then** the response is HTTP 400 with `[{errorCode: "MALFORMED_QUERY", message: "...unexpected token 'FROM' at position 7..."}]`.
4. **Given** a query with valid syntax but an unresolvable relationship path, **When** executed, **Then** the response is HTTP 400 with `[{errorCode: "INVALID_FIELD", message: "...relationship 'Foo' is not defined on 'Contact'..."}]`.

---

### Edge Cases

- **Case sensitivity**: SOQL keywords (`SELECT`, `WHERE`, `AND`, etc.) are case-insensitive. Field names are case-insensitive in `WHERE` comparisons and `ORDER BY` but are echoed in the response with the casing from the `SELECT` clause (not normalized to schema casing). Object names in `FROM` are matched case-insensitively against the registered sObject registry.
- **Empty result sets**: Queries with no matching records return `{totalSize: 0, done: true, records: []}` with HTTP 200. This is not an error.
- **Single quotes in string literals**: Escaped single quotes (`\'`) inside SOQL string literals must be handled by the lexer and round-tripped correctly to the value comparator. A literal `'O\'Brien'` matches a record with `Name = "O'Brien"`.
- **Long IN lists**: The executor must support at least 200 values in a single `IN (...)` list. Salesforce supports 4000; this is not a target but the implementation must not impose an artificially low limit.
- **Query on non-existent object**: Return `INVALID_TYPE` immediately without attempting to query the H2 database.
- **COUNT() with WHERE**: `SELECT COUNT() FROM Account WHERE IsActive = true` must count only the filtered rows, not all rows.
- **OFFSET without LIMIT**: SOQL allows `OFFSET` without `LIMIT`; the executor must not reject this combination.
- **Backward compatibility**: Simple single-table queries that the current regex-based engine handles correctly must continue to work identically after the replacement.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: SOQL engine MUST support `SELECT`, `FROM`, `WHERE`, `ORDER BY`, `LIMIT`, and `OFFSET` clauses.
- **FR-002**: `WHERE` clause MUST support operators `=`, `!=`, `<`, `>`, `<=`, `>=`, `LIKE`, `IN`, `NOT IN`, `IS NULL`, `IS NOT NULL`, boolean connectives `AND` and `OR`, and parenthesized grouping with correct precedence (`AND` binds tighter than `OR`).
- **FR-003**: `ORDER BY` MUST support multiple fields, `ASC`/`DESC` direction, and `NULLS FIRST`/`NULLS LAST` modifiers.
- **FR-004**: `COUNT()` aggregate MUST be supported and return the Salesforce `expr0` response shape.
- **FR-005**: Relationship dot-notation MUST resolve parent fields for the hardcoded relationship map (Contact→Account, Opportunity→Account, Case→Account, Case→Contact) in `SELECT`, `WHERE`, and `ORDER BY`.
- **FR-006**: Query response envelope MUST match the Salesforce format: `{totalSize: int, done: boolean, records: [{attributes: {type: string, url: string}, ...fields}]}`.
- **FR-007**: Error responses MUST use the Salesforce error array shape `[{errorCode: string, message: string}]` and MUST include character position in `MALFORMED_QUERY` messages.
- **FR-008**: The parser MUST be implemented as a clean lexer → parser → AST pipeline; regex-based field extraction is not permitted in the new implementation.
- **FR-009**: Field names in `WHERE` and `ORDER BY` comparisons MUST be resolved case-insensitively against the stored record fields.
- **FR-010**: `LIKE` operator MUST support `%` (zero-or-more chars) and `_` (exactly one char) wildcards with Salesforce semantics.
- **FR-011**: `IN` lists MUST support at least 200 literal values without error.
- **FR-012**: The replacement engine MUST be backward compatible — all SOQL patterns accepted by the current regex engine MUST produce equivalent results.

### Key Entities *(include if feature involves data)*

- **SoqlToken**: A typed token produced by `SoqlLexer` carrying token kind, raw text, and character offset.
- **SoqlAst**: The sealed class hierarchy of AST node types: `SelectStatement`, `FieldList`, `FieldRef`, `FromClause`, `WhereClause`, `Condition` (subtypes: `ComparisonCondition`, `InCondition`, `LikeCondition`, `NullCondition`, `LogicalCondition`), `OrderByClause`, `OrderByField`, `LimitClause`, `OffsetClause`, `CountFunction`.
- **SoqlParser**: The recursive-descent parser that consumes a `List<SoqlToken>` and produces a `SelectStatement`.
- **SoqlExecutor**: The executor that accepts a `SelectStatement` and a data-source handle, evaluates filters, resolves relationships, sorts, paginates, and returns a `QueryResult`.
- **RelationshipResolver**: The component that maps a dot-notation `FieldRef` to a parent object lookup using the hardcoded relationship map.
- **QueryResult**: Internal DTO carrying `totalSize`, `done`, and the list of projected record maps before HTTP serialization.

## Assumptions

- The H2 sObject store already persists records by object type and exposes a method to retrieve all records of a given type as `Map<String, Object>` instances.
- The existing `SoqlEngine.java` can be deleted once all its callers are migrated to `SoqlExecutor` and backward-compatibility tests pass.
- Relationship metadata (which field on Child refers to which Parent object) is hardcoded in a static map for this iteration and does not need to be derived from the describe API response.
- `done` is always `true` in this iteration because `queryMore` (cursor-based pagination) is out of scope.
- String comparisons in `WHERE` are case-sensitive by default (matching Salesforce `=` operator behavior); `LIKE` is case-insensitive (matching Salesforce behavior).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All SOQL queries from the feature 008 API parity test suite produce matching results between dev 20 and sf_localstack (zero unaccepted deltas).
- **SC-002**: Parser unit tests cover at least 20 distinct SOQL patterns (combinations of clauses, operators, and nesting) and all pass.
- **SC-003**: Relationship queries resolve parent fields for Contact→Account and Opportunity→Account in integration tests with live-inserted records.
- **SC-004**: Error messages for `MALFORMED_QUERY` include the offending token text and its character position; `INVALID_FIELD` includes the field name; `INVALID_TYPE` includes the object name.
- **SC-005**: All pre-existing SOQL integration tests that passed with the old regex engine continue to pass with the new pipeline (zero regressions).
- **SC-006**: `COUNT()` queries return the correct count against filtered and unfiltered datasets verified in integration tests.
