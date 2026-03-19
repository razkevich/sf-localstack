# sf-localstack — Detailed Build Specification

## What It Is

A Docker container that emulates the Salesforce REST, Bulk, and Metadata APIs for integration testing. Engineers point their Salesforce client code at `http://localhost:8080` instead of a real Salesforce org. The emulator responds with correct-shaped API responses, stores records in an H2 in-memory database, and resets on demand. No Salesforce org required in CI.

## Repository

`/Users/razkevich/code/sf_localstack`, branch `feat/scaffold`, Java 21 + Spring Boot 3.3.5, Maven multi-module.

## Current State (already built and committed, 10 tests passing)

### Module: `service/`

**Controllers (all working):**
- `POST /services/oauth2/token` → returns fake JWT `{ access_token, instance_url, token_type }`
- `GET /services/oauth2/userinfo` → returns fake org/user metadata
- `GET /services/data/` → version discovery (`[{ version: "60.0", url: "/services/data/v60.0" }]`)
- `GET /services/data/v{n}/sobjects/{Object}/describe` → stub describe (empty fields list)
- `GET /services/data/v{n}/sobjects/{Object}` → list all records of that type
- `GET /services/data/v{n}/sobjects/{Object}/{id}` → get one record
- `POST /services/data/v{n}/sobjects/{Object}` → create record (returns `{ id, success: true }`)
- `PATCH /services/data/v{n}/sobjects/{Object}/{id}` → update record
- `DELETE /services/data/v{n}/sobjects/{Object}/{id}` → delete record
- `GET /services/data/v{n}/query?q=SOQL` → executes SOQL (basic regex engine)
- `POST /reset` → wipes all H2 records, re-applies YAML seed data
- `GET /api/dashboard/events` → SSE stream of live requests
- `GET /api/dashboard/requests?limit=100` → recent request log as JSON

**Services:**
- `OrgStateService` — H2-backed CRUD via JPA (`SObjectRecord` entity: id, objectType, fieldsJson TEXT, timestamps)
- `SoqlEngine` — STUB: regex-based, handles basic `SELECT fields FROM Object [LIMIT n]`, no WHERE, no relationships
- `SeedDataLoader` — reads `seed/default-seed.yml` on startup and on reset
- `RequestLogService` — ring buffer of last 1000 requests + SSE broadcast
- `RequestLoggingInterceptor` — intercepts every HTTP request, logs to RequestLogService

**Seed data** (`service/src/main/resources/seed/default-seed.yml`):
```yaml
objects:
  - type: Account
    records:
      - Name: "Acme Corp", Industry: "Technology", Phone: "555-0100"
      - Name: "Globex Corp", Industry: "Manufacturing", Phone: "555-0200"
  - type: Contact
    records:
      - FirstName: "John", LastName: "Doe", Email: "john.doe@acme.com", Account.Name: "Acme Corp"
      - FirstName: "Jane", LastName: "Smith", Email: "jane.smith@globex.com", Account.Name: "Globex Corp"
```
Note: `Account.Name` is stored as a literal string field on the Contact record (not a real FK join). This is intentional for v1 simplicity.

### Module: `client/` — empty stub, just a pom.xml

### Module: `frontend/` — React 18 + Vite + Tailwind, not yet built/wired
Files exist but `npm install` has never been run. The Vite config proxies `/api` → `localhost:8080` and builds into `service/src/main/resources/static/`.

---

## What Needs To Be Built

### 1. Enhanced SOQL Engine (`SoqlEngine.java` — full replacement)

The current regex stub ignores WHERE clauses entirely. Replace it with:

**Supported syntax:**
```sql
SELECT Id, Name FROM Account
SELECT Id, Name FROM Account WHERE Name = 'Acme Corp'
SELECT Id, Name FROM Account WHERE Name LIKE '%Corp%' LIMIT 5
SELECT Id, Name FROM Account WHERE Name != null
SELECT Id, FirstName, Account.Name FROM Contact WHERE LastName = 'Doe'
SELECT COUNT() FROM Account
SELECT Id FROM Account WHERE Industry = 'Technology' AND Name LIKE '%Corp%'
```

**Implementation approach (in-memory, no SQL):**
1. Parse with regex: extract `SELECT fields`, `FROM objectType`, `WHERE clause`, `LIMIT n`
2. Load all records of that objectType from `OrgStateService.findByType(objectType)`
3. For each record, deserialize `fieldsJson` → `Map<String, Object>`
4. Apply WHERE filter against the map
5. Project the requested fields
6. For relationship fields like `Account.Name`:
   - First try: look up literal key `"Account.Name"` in the record's fields map (covers seed data)
   - Then try: look up `AccountId` FK → fetch that Account record → return its `Name` field
7. Apply LIMIT
8. For `COUNT()`: return `[{ "expr0": N }]`

**WHERE condition support:**
- `field = 'string'` — case-insensitive string equality
- `field = 42` — numeric equality
- `field != 'string'`
- `field LIKE '%pattern%'` — convert `%` → `.*`, `_` → `.` regex
- `field = null` / `field != null`
- `field IS NULL` / `field IS NOT NULL`
- Multiple conditions joined by `AND`

### 2. Upsert Endpoint

**Add to `SObjectController`:**
```
PATCH /services/data/v{n}/sobjects/{Object}/{externalIdField}/{externalIdValue}
```

**Behavior:**
- Find existing record where `fields[externalIdField] == externalIdValue`
- If found: update fields, return `204 No Content`
- If not found: create new record with those fields, return `201 Created` with `{ id, success: true }`
- Must be `synchronized` to prevent race conditions on concurrent upserts with the same external ID

**Add `upsert()` method to `OrgStateService`** (synchronized):
```java
public synchronized UpsertResult upsert(String objectType, String externalIdField,
                                         String externalIdValue, Map<String, Object> fields)
```

### 3. Bulk API v2 (new `BulkController.java` + `BulkJobService.java`)

**All endpoints under `/services/data/v{n}/jobs/ingest/`:**

```
POST   /services/data/v{n}/jobs/ingest
Body:  { "operation": "insert", "object": "Account", "contentType": "CSV" }
→ 200: { "id": "750...", "operation": "insert", "object": "Account", "state": "Open", ... }

PUT    /services/data/v{n}/jobs/ingest/{jobId}/batches
Content-Type: text/csv
Body:  Name,Industry\nTest Corp,Technology\nAcme Inc,Finance
→ 201: (empty)

PATCH  /services/data/v{n}/jobs/ingest/{jobId}
Body:  { "state": "UploadComplete" }
→ 200: { "id": "...", "state": "JobComplete", "numberRecordsProcessed": 2, ... }
(emulator processes the CSV synchronously before returning)

GET    /services/data/v{n}/jobs/ingest/{jobId}
→ 200: { "id": "...", "state": "JobComplete", "numberRecordsProcessed": 2, "numberRecordsFailed": 0 }

DELETE /services/data/v{n}/jobs/ingest/{jobId}
→ 204: (empty)

GET    /services/data/v{n}/jobs/ingest/{jobId}/successfulResults
Content-Type: text/csv
→ 200: sf__Id,sf__Created\n{id},{true|false}\n...

GET    /services/data/v{n}/jobs/ingest/{jobId}/failedResults
Content-Type: text/csv
→ 200: sf__Id,sf__Error\n{id},{errorMessage}\n...

GET    /services/data/v{n}/jobs/ingest/{jobId}/unprocessedrecords
→ 200: (empty CSV)
```

**BulkJob model (in-memory, not JPA):**
```java
class BulkJob {
    String id;          // "750" + UUID
    String operation;   // insert | update | delete | upsert
    String objectType;  // Account, Contact, etc.
    String externalIdFieldName;  // for upsert operations
    String state;       // Open → UploadComplete → JobComplete | Aborted | Failed
    List<String> csvBatches;  // raw CSV strings accumulated from PUT /batches
    List<Map<String,Object>> successfulResults;
    List<Map<String,Object>> failedResults;
    int numberRecordsProcessed;
    int numberRecordsFailed;
    Instant createdDate;
}
```

**BulkJobService:**
- `ConcurrentHashMap<String, BulkJob>` for job storage
- `createJob(operation, objectType, externalIdFieldName)` → new job, state=Open
- `addBatch(jobId, csvContent)` → appends CSV string to job's batch list
- `closeJob(jobId)` → processes all CSV batches, sets state=JobComplete
- `processCSV(job)` → parse CSV (first row = headers, remaining rows = data), call OrgStateService per row based on operation

**CSV processing logic:**
- `insert`: call `OrgStateService.create(objectType, rowFields)` for each row
- `update`: call `OrgStateService.update(id, rowFields)` — Id column required
- `delete`: call `OrgStateService.delete(id)` — Id column required
- `upsert`: call `OrgStateService.upsert(objectType, externalIdField, externalIdValue, rowFields)`

**CSV parsing:** Manual (no external library). Split by `\n`, first line = headers, remaining = data rows. Handle quoted fields with commas. Return `List<Map<String,String>>`.

**CSV output format for results:**
- `successfulResults`: `sf__Id,sf__Created\n{id},{true|false}\n...`
- `failedResults`: `sf__Id,sf__Error\n{id},{errorMessage}\n...`

### 4. SOAP Metadata API (new `MetadataController.java` + `MetadataService.java`)

**Single endpoint:**
```
POST /services/Soap/m/{version}
Content-Type: text/xml
→ Content-Type: text/xml
```

**Operations (detected by parsing the SOAP XML body):**

**a) listMetadata**
Input XML contains `<listMetadata>` element with `<queries>` containing `<type>` elements.
Output: list of `FileProperties` elements (one per queried type).

**b) describeMetadata**
Input: `<describeMetadata>` with `<asOfVersion>`.
Output: list of `DescribeMetadataObject` elements covering standard SF metadata types.

**c) deploy**
Input: `<deploy>` with `<ZipFile>` (base64 zip) and `<DeployOptions>`.
Output: `{ id: "0Af..." }` — async process ID.
Implementation: decode base64 zip, store as `DeployJob` in memory, mark `done=true, success=true` immediately.

**d) checkDeployStatus**
Input: `<checkDeployStatus>` with `<asyncProcessId>` and `<includeDetails>`.
Output: `DeployResult` with `id, done, success, status, numberComponentsDeployed, numberComponentsTotal, numberComponentErrors, componentFailures (empty list)`.

**e) cancelDeploy**
Input: `<cancelDeploy>` with `<asyncProcessId>`.
Output: `<result><done>true</done><id>{asyncProcessId}</id></result>`

**DeployJob model (in-memory):**
```java
class DeployJob {
    String id;
    boolean done;
    boolean success;
    String status;  // "Succeeded" | "Failed" | "Canceled"
    int numberComponentsTotal;
    int numberComponentsDeployed;
    int numberComponentErrors;
    Instant createdDate;
}
```

**Implementation:** Parse XML with `javax.xml.parsers.DocumentBuilder`, extract operation name from SOAP body element, route to handler, return string-templated SOAP XML wrapped in standard envelope:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:m="http://soap.sforce.com/2006/04/metadata">
  <soapenv:Body>
    <!-- operation response here -->
  </soapenv:Body>
</soapenv:Envelope>
```

**Spring config needed:** Add `text/xml` to accepted content types. Add `StringHttpMessageConverter` for `text/xml` → `String` mapping.

### 5. Tests to Write

**`SoqlRelationshipQueryTest.java`** (`@SpringBootTest @AutoConfigureMockMvc`):
- After reset (which loads seed data), `SELECT Id, FirstName, Account.Name FROM Contact WHERE LastName = 'Doe'` → returns record where `Account.Name = "Acme Corp"` (not null)

**`ConcurrentInsertTest.java`** (`@SpringBootTest @AutoConfigureMockMvc`):
- After reset, fire 10 concurrent `POST /services/data/v60.0/sobjects/Account` requests with same `ExternalId__c = "EXT-001"` using `CompletableFuture`
- Check total Account records: either 1 (if upsert semantics applied) or 10 (if insert semantics) — test documents the actual behavior
- The real assertion: no NPE, no 500 errors, server stays healthy

**`BulkApiV2Test.java`**:
- `POST /jobs/ingest` → 200, get jobId
- `PUT /jobs/ingest/{jobId}/batches` with CSV `"Name,Industry\nBulk Corp,Tech"` → 201
- `PATCH /jobs/ingest/{jobId}` with `{"state":"UploadComplete"}` → 200, state=JobComplete
- `GET /jobs/ingest/{jobId}` → numberRecordsProcessed = 1
- `GET /query?q=SELECT Name FROM Account WHERE Name = 'Bulk Corp'` → totalSize = 1

**`MetadataApiTest.java`**:
- `POST /services/Soap/m/60.0` with deploy body → 200, response contains `<id>`
- `POST /services/Soap/m/60.0` with checkDeployStatus body → `<done>true</done>`, `<success>true</success>`
- `POST /services/Soap/m/60.0` with listMetadata body → response contains `<result>` elements

### 6. Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY service/target/sf-localstack-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

`docker-compose.yml`:
```yaml
version: '3.8'
services:
  sf-localstack:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
```

### 7. README.md (root level)

Sections:
- What it is (1 paragraph)
- Quick start (docker run command)
- Running locally (`sdk env`, `mvn spring-boot:run -pl service`)
- API endpoints table (method + path + description)
- Seed data format (YAML example)
- Test isolation (`POST /reset` in `@BeforeEach`)
- SF CLI usage (`export SF_INSTANCE_URL=http://localhost:8080`)
- Building the frontend (`cd frontend && npm install && npm run build`)

---

## Package Structure

All Java under `co.prodly.sflocalstack`. New files go in:
- `controller/BulkController.java`
- `controller/MetadataController.java`
- `service/BulkJobService.java`
- `service/MetadataService.java`
- `model/BulkJob.java`
- `model/DeployJob.java`

## Key Constraints

- Java 21, Spring Boot 3.3.5, no new Maven dependencies (use only what's already in pom.xml)
- All 10 existing tests must keep passing
- `@Transactional` on all OrgStateService write methods
- `synchronized` on upsert to prevent race conditions
- SOAP XML: no JAXB, no CXF — just `javax.xml.parsers` + string templates
- CSV parsing: no opencsv — just `String.split("\n")` + `String.split(",")` with quote handling

## Additional Context

### Why This Exists
Salesforce ISVs (companies building products on Salesforce) cannot write reliable integration
tests against Salesforce APIs in CI. The current workaround — hardcoded XML response mocks
via Mockito/WireMock — is unmaintainable and has been abandoned by most teams. The result:
dead test suites, no CI coverage for Salesforce integrations.

### Primary User
Backend engineers at Salesforce ISVs (specifically Prodly, an AppExchange partner) who own
integration testing infrastructure. They need CI to pass without provisioning a real Salesforce
scratch org (which takes 5-10 min/run and is fragile).

### Prodly's API Usage (discovered from codebase analysis)
- API version: v60.0 (confirmed from describe fixtures)
- SF client libraries: `co.prodly:force-wsc` (SOAP), `co.prodly:salesforce-java-api` (REST)
- metadata-service uses: REST SOQL, REST CRUD, SOAP Metadata API (listMetadata, describeMetadata, checkDeployStatus, cancelDeploy), SF CLI (sf project retrieve/deploy start)
- athena-salesforce-load-service uses: Bulk API v2 (insert/update/delete/upsert operations)
- athena-salesforce-extract-service uses: SOQL queries + describe endpoints

### SF CLI Shim
SF CLI reads `SF_INSTANCE_URL` / `SFDX_INSTANCE_URL` env var. Set it to `http://localhost:8080`
and `sf data query`, `sf project deploy start` etc. will hit the emulator automatically.
No code changes needed in the client — just the env var.

