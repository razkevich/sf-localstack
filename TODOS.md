# TODOS

## Pre-implementation (blockers)

### ~~TODO-1: Spike apex-parser for standalone SOQL parsing in Java~~ ✅ RESOLVED
**Result:** apex-parser works for standalone SOQL. **Java + Spring Boot confirmed.**
Use `io.github.apex-dev-tools:apex-parser` for SOQL parsing.

---

### ~~TODO-2: Capture real HTTP traffic from one failing Prodly integration test~~ ✅ RESOLVED
**Result:** Codebase analysis of metadata-service and athena-salesforce-{extract,load}-service
reveals the full API surface. Key findings:
- **API version:** v60.0 (confirmed from describe fixtures in athena-salesforce-load-service)
- **SF client libraries:** `co.prodly:force-wsc` (SOAP), `co.prodly:salesforce-java-api` (REST)
- **metadata-service uses:**
  - REST API: SOQL queries, CRUD (createObjects, delete, update) via `SalesforceClient.servicesData()`
  - SOAP Metadata API: `MetadataConnection.listMetadata()`, `describeMetadata()`, `checkDeployStatus()`, `cancelDeploy()`
  - SF CLI: `sf project retrieve start` / `sf project deploy start` via SalesforceSfdxGateway
- **athena-salesforce-load-service uses:** Bulk API v2 (ingest jobs, insert/update/delete/upsert operations)
- **athena-salesforce-extract-service uses:** SOQL queries + describe endpoints + CSV extraction
- **Mock fixtures found:** `AccountDescribe.json`, `CaseDescribe.json` — full SF describe shape with all fields

---

### TODO-3: Write first 3 tests before any feature code (critical gaps)

These are the first tests that MUST exist before any feature implementation. They cover
the failure modes most likely to be silent and dangerous.

**Test 1 — SOQL relationship query produces correct results**
```
Given: Account "Acme" with Contact "John"
When:  SELECT Id, Name, Account.Name FROM Contact WHERE Account.Name = 'Acme'
Then:  Returns John with Account.Name = "Acme" (JOIN resolved correctly)
Failure mode: apex-parser→SQL transpiler generates invalid JOIN → wrong results returned silently
```

**Test 2 — Metadata deploy partial failure is surfaced in DeployResult**
```
Given: A deployment zip with one valid component and one invalid component type
When:  POST /services/Soap/m/60.0 (deploy)
Then:  DeployResult.success = false, componentFailures contains the invalid component
Failure mode: emulator returns success=true for all deploys → tests never catch bad deployments
```

**Test 3 — Parallel inserts with the same external ID do not corrupt state**
```
Given: Two concurrent POST /services/data/v60.0/sobjects/Account requests with same ExternalId__c
When:  Both requests execute simultaneously
Then:  One returns success, one returns DUPLICATE_VALUE error (SF error shape)
       Total Account records = 1, not 2
Failure mode: race condition → two records created, or NPE, or silent data corruption
```

**Why:** These are the three scenarios most likely to fail silently in production with no
test coverage. "Silent" means the emulator returns 200 OK with plausible-looking data while
actually doing the wrong thing. Write these tests first (red), then make them pass (green).
**Depends on:** Project scaffolded with Spring Boot + JUnit 5 + H2.

---

## Post-MVP

### TODO-4: Define SF API version compatibility matrix
**What:** Document which SF API versions the emulator supports and what
"version-aware" means in practice. Are v55.0 and v62.0 handled identically
or does behavior differ?
**Why:** `/services/data/v{n}/query` includes a version number that clients set.
Without a policy, adding version-specific behavior later becomes a breaking change.
**Context:** API version v60.0 confirmed from Prodly codebase. Initial recommendation:
accept any v{n} ≥ v55.0 and route to same handlers; add version-specific behavior
only when a test requires it.

---

### TODO-5: Bulk v1 support
**What:** Add Bulk API v1 endpoints alongside v2.
**Why:** Some older SF integrations use Bulk v1 (SOAP-based). Not needed for v1
but required for broader market coverage.
**Depends on:** Bulk v2 working correctly first.

### TODO-6: Open-source core emulator
**What:** Evaluate open-sourcing the emulator core once API fidelity is validated.
**Why:** Community contributions will cover the long tail of SF API endpoints
that would take years to implement alone. LocalStack's model.
**Context:** Don't open-source until you have paying users and know which API
surfaces matter most. Build in private first.
**Depends on:** First paying customer.
