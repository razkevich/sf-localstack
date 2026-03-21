# Research: Migrate metadata-service Tests from WireMock to sf-localstack

## Finding 1: SOAP Metadata API Version Mismatch

**Decision**: Extend `MetadataController` to accept both `/services/Soap/m/60.0` and `/services/Soap/m/66.0` paths, or use a version-wildcard path pattern.

**Rationale**: metadata-service's `Constants.SALESFORCE_API_VERSION = 66` so every SOAP call goes to `/services/Soap/m/66.0`. sf-localstack's `MetadataController` is currently mapped only to `/services/Soap/m/60.0`. A wildcard `@RequestMapping("/services/Soap/m/{version}")` covers all versions without breaking existing tests.

**Alternatives considered**:
- Hard-code `/66.0` only: breaks sf CLI and any other consumer on v60.
- Keep `/60.0` only and patch metadata-service: out of scope per spec.
- Accept both paths with two `@PostMapping` annotations: works but verbose; wildcard is cleaner.

---

## Finding 2: SOAP Operations Already Implemented

**Decision**: No new SOAP operation implementations needed. `describeMetadata`, `listMetadata`, `readMetadata` (StandardValueSet + CustomField), and `cancelDeploy` are all handled in `MetadataController` + `MetadataSoapRenderer`.

**Rationale**: The controller routes all four operations. However, two response shape gaps must be fixed:

1. **`describeMetadata`**: sf-localstack omits `<suffix>` from `<metadataObjects>`. The fixture requires it. Fix: add `<suffix>%s</suffix>` to `MetadataSoapRenderer.renderDescribeMetadata()` and ensure `MetadataCatalogEntry` carries suffix.

2. **`cancelDeploy`**: sf-localstack renders `<done>true</done><success>true</success>` but the fixture has `<done>false</done><id>id</id>` with no success field. The test only checks that the cancel operation reaches the server and gets a valid response; the exact done value may not matter — but for fixture parity the renderer should match. Confirm by reading the actual test assertions.

3. **`listMetadata`**: sf-localstack omits `createdById`, `id`, `lastModifiedById`, `namespacePrefix` fields that appear in the fixture. Tests may only check `fileName`, `fullName`, `type`, `lastModifiedDate` — verify before adding fields.

---

## Finding 3: Seed Data for Custom PDRI Objects

**Decision**: Extend `default-seed.yml` with `PDRI__Connection__c`, `PDRI__ComparisonView__c`, `PDRI__ComparisonViewRule__c`, and `StaticResource` records using fixed IDs matching those in the `__files/*.json` fixtures.

**Rationale**: sf-localstack's seed system already supports arbitrary sObject types via YAML. All fixture IDs are stable and can be hardcoded in the seed. The seed is applied on container start and re-applied on `POST /reset`.

**Key constraint**: `PDRI__Connection__c.PDRI__Instance_URL__c` must be sf-localstack's own base URL (dynamic Testcontainers-mapped port), not `http://localhost:8090` from the WireMock fixture. Two approaches:

- **Option A (chosen)**: Inject the URL at reset time via a `POST /reset` body that accepts seed overrides. The Testcontainers base class calls `POST /reset` with `{"overrides":{"PDRI__Connection__c.PDRI__Instance_URL__c": "<container-url>"}}`.
- **Option B**: Use an environment variable `SF_LOCALSTACK_BASE_URL` set by the Testcontainers base class at container startup, referenced in the seed as `${SF_LOCALSTACK_BASE_URL}`. Requires YAML variable interpolation support.
- **Option C**: Accept `http://localhost:8090` and patch metadata-service tests. Out of scope.

**Option A** is preferred because it requires no new seed template language and the `/reset` endpoint already exists.

---

## Finding 4: Tooling API for StaticResource

**Decision**: Add `GET /services/data/v{version}/tooling/query/` to sf-localstack's `MetadataRestController` (or a new `ToolingController`) that handles `SELECT BodyLength FROM StaticResource WHERE Name IN (...)` SOQL queries.

**Rationale**: metadata-service calls the Tooling API at `/services/data/.*/tooling/query/` for StaticResource body length. sf-localstack's existing `SoqlQueryController` handles `/services/data/{version}/query` (standard REST). Tooling queries use a parallel path `/tooling/query/` and need to serve StaticResource records from the seed.

**Alternatives considered**:
- Route tooling queries through the same SOQL handler: possible but the tooling path is distinct in real Salesforce; using a separate controller is cleaner and matches Salesforce's actual URL structure.

---

## Finding 5: Testcontainers Base Class Strategy

**Decision**: Create `AbstractSfLocalstackTest` that extends `AbstractContainerTest` (keeping the existing Postgres/Redis/S3 setup). Add a static `GenericContainer` for sf-localstack built from the local Dockerfile, wait on `GET /actuator/health`, and register its base URL via `@DynamicPropertySource`.

**Rationale**: metadata-service already uses Testcontainers (`@Testcontainers`, `@Container`). The new sf-localstack container follows the same pattern. The base class posts a reset with the dynamic URL override in `@BeforeEach`.

**Key wiring**:
- `salesforce.instance.url` Spring property → sf-localstack container's mapped URL
- `@BeforeEach`: `POST /reset` with `{"urlOverride": "<container-url>"}` to seed dynamic URL
- `@BeforeAll`: wait for `/actuator/health` → `{"status":"UP"}` before allowing tests to proceed

---

## Finding 6: Static/Big Resource Scenario

**Decision**: Seed two `StaticResource` records with the same name prefix but differentiated by a query parameter. Since both small and big tests use `WHERE Name IN (...)` and a single sf-localstack endpoint, the test must control which resource is returned.

**Rationale**: The small fixture has `BodyLength: 1`; the big fixture has `BodyLength: 200000000`. Both tests call the same SOQL query pattern. The real distinction is which resource name is in the `IN (...)` clause. By seeding two records — `SmallStaticResource` (BodyLength=1) and `BigStaticResource` (BodyLength=200000000) — and ensuring each test's `WHERE Name IN (...)` targets the correct name, both code paths are covered without per-test seed overrides.
