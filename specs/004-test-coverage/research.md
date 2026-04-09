# Research: 004-test-coverage

## Test Coverage Gap Analysis

### Current State
- **16 test classes**, **56 test methods**
- **Controller endpoint coverage**: 24/32 (75%)
- **Service method coverage**: 35/52 (67%)

### Untested Controller Endpoints

| Controller | Endpoint | Notes |
|-----------|----------|-------|
| VersionController | `GET /services/data` (no slash), `GET /data`, `GET /data/{v}` | Alias routes |
| BulkController | `GET /{jobId}/failedResults` | CSV result endpoint |
| BulkController | `GET /{jobId}/unprocessedrecords` | CSV result endpoint |
| DashboardController | `GET /events` (SSE) | Hard to test — SSE stream |
| MetadataRestController | `GET /sobjects/{obj}/describe` | Tooling describe |
| MetadataRestController | `GET /query/` (trailing slash) | Alias route |
| QueryController | `GET /query/` (trailing slash) | Alias route |
| SObjectController | `GET /{id}` | Get single record |
| SObjectController | `DELETE /{id}` | Delete record |

### Untested Service Methods

| Service | Method | Notes |
|---------|--------|-------|
| BulkJobService | `failedResults()`, `unprocessedResults()` | CSV generation |
| MetadataService | `listResources()`, `reset()` | Admin/lifecycle |
| OrgStateService | `findById()`, `findByTypeAndId()`, `findAll()` | Query methods |
| OrgStateService | `delete()`, `reset()`, `fromJson()` | Mutation/util |
| RequestLogService | `log()`, `reset()`, `newEmitter()` | Logging/SSE |
| MetadataToolingService | `executeStandardMetadataQuery()` | Standard query |

### Decisions

**Decision**: Test SSE endpoint via async MockMvc with timeout
**Rationale**: SSE endpoints need async handling; MockMvc supports this with `asyncDispatch()`
**Alternatives**: Skip SSE testing (rejected — it's a key user-facing endpoint)

**Decision**: Use `@DirtiesContext` sparingly, prefer `POST /reset` between tests
**Rationale**: `@DirtiesContext` is slow (restarts Spring context). `POST /reset` is fast and tests the reset path.
**Alternatives**: `@Transactional` rollback (doesn't work for in-memory maps like BulkJobService)

**Decision**: Create shared test helpers (TestDataFactory, SoapTestHelper, AssertionHelpers) first
**Rationale**: Reduces boilerplate across all new tests, makes them more readable
**Alternatives**: Inline setup in each test (rejected — too repetitive)

**Decision**: Group tests by controller/service, not by feature iteration
**Rationale**: Test files should mirror source structure for discoverability
**Alternatives**: Single large integration test file (rejected — hard to navigate)
