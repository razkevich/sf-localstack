# SF LocalStack MVP — Master Design Document

## Vision

SF LocalStack is a lightweight Salesforce API emulator that serves as a drop-in replacement for real Salesforce orgs in development and CI/CD workflows. The MVP targets deployment as a cloud SaaS with a free tier — simple but reliable, with clear extensibility paths to production-grade infrastructure.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   React Frontend                     │
│  (Tailwind + SLDS tokens, Lightning-pattern UI)      │
│  Login → App Shell → List Views / Record Pages       │
└────────────────────┬────────────────────────────────┘
                     │ REST API
┌────────────────────▼────────────────────────────────┐
│              Spring Boot Backend                     │
│                                                      │
│  ┌─────────┐  ┌──────────┐  ┌────────────────────┐  │
│  │ Auth    │  │ Protocol │  │ Dashboard/Admin    │  │
│  │ Filter  │  │ Adapters │  │ Controllers        │  │
│  └────┬────┘  └────┬─────┘  └────────┬───────────┘  │
│       │            │                  │              │
│  ┌────▼────────────▼──────────────────▼───────────┐  │
│  │              Service Layer                      │  │
│  │  OrgState │ SOQL │ Bulk │ Metadata │ RequestLog │  │
│  └────────────────────┬───────────────────────────┘  │
│                       │                              │
│  ┌────────────────────▼───────────────────────────┐  │
│  │          Repository / Store Interfaces          │  │
│  │  UserStore │ SObjectRepo │ BulkJobRepo │ ...    │  │
│  └────────────────────┬───────────────────────────┘  │
│                       │                              │
│  ┌────────────────────▼───────────────────────────┐  │
│  │        H2 File-Based (MVP) / PostgreSQL (later) │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

### Key Architectural Principles

1. **Interface-at-every-boundary**: Every service dependency is an interface. MVP implementations are simple; production swaps in a new class + Spring profile.
2. **Domain-first packages**: `auth/`, `data/`, `bulk/`, `metadata/`, `observability/`, each owning its controllers + services + models.
3. **Thin protocol adapters**: Controllers translate HTTP/SOAP to domain calls. No business logic in controllers.
4. **Profile-driven configuration**: `dev` (H2 file), `test` (H2 mem), `prod` (PostgreSQL placeholder).

## Features

### F1: Test Coverage Hardening

**Goal**: Comprehensive test suite as safety net before any refactoring.

**Scope**:
- Integration tests (`@SpringBootTest` + MockMvc) for every controller endpoint — happy path + error path
- Unit tests for SoqlEngine (parsing, filtering, projection), BulkJobService (CSV processing, state machine), MetadataService (catalog operations), OrgStateService (CRUD, upsert, ID generation)
- Cross-surface integration tests (REST → query → bulk → metadata in sequence)
- Target: every public API endpoint has at least 2 tests (success + failure)

**Existing coverage**: 23 test files already exist. Gap analysis needed to identify untested paths.

**Deliverables**: Test gap report, new test files, CI green.

---

### F2: Persistent Storage

**Goal**: Data survives application restarts.

**Changes**:
- H2 config: `jdbc:h2:file:./data/sfdb` (file-based) with `ddl-auto: update` (not `create-drop`)
- New JPA entities for BulkIngestJob, MetadataResource, MetadataDeployJob, MetadataRetrieveJob (currently in ConcurrentHashMap)
- Spring profiles:
  - `test`: `jdbc:h2:mem:testdb` + `create-drop` (fast isolated tests)
  - `dev`: `jdbc:h2:file:./data/sfdb` + `update`
  - `prod`: PostgreSQL datasource placeholder (documented, not implemented)
- Data directory: `./data/` with `.gitignore` entry
- Flyway or manual schema versioning for future migrations (document approach, use `update` for MVP)

**Migration path to PostgreSQL**: Document in `docs/extensibility.md` — add `spring.datasource.*` in `application-prod.yml`, add `postgresql` driver to `pom.xml`, activate `prod` profile. JPA + Hibernate handle the rest.

---

### F3: Authentication & User Management

**Goal**: Users can register, log in, and have isolated sessions. Extensible to production auth later.

**Design**:
```
UserStore (interface)
  ├─ findByUsername(String): Optional<User>
  ├─ createUser(String username, String password, Role role): User
  ├─ validateCredentials(String username, String password): boolean
  └─ findById(String): Optional<User>

FileBasedUserStore (MVP implementation)
  └─ reads/writes data/users.json
  └─ passwords hashed with bcrypt

JwtService
  ├─ generateToken(User): String (access token, 1hr expiry)
  ├─ generateRefreshToken(User): String (7-day expiry)
  └─ validateToken(String): Claims

JwtAuthFilter (OncePerRequestFilter)
  └─ intercepts /services/*, validates JWT, sets SecurityContext
  └─ skips /services/oauth2/token (login endpoint)
```

**Endpoints**:
- `POST /api/auth/register` → create user (admin-only after first user)
- `POST /api/auth/login` → validate credentials, return JWT pair
- `POST /api/auth/refresh` → refresh access token
- `GET /api/auth/me` → current user info

**OAuth2 stub update**: The existing `/services/oauth2/token` endpoint becomes a real token issuer for sf CLI compatibility — accepts `grant_type=password` with username/password, returns JWT as `access_token`.

**RBAC**: `admin` (manage users, reset org, all operations) and `user` (CRUD data/metadata only).

**First-run experience**: If no users exist, first registration creates an admin. Subsequent registrations require admin JWT.

**Extensibility**: `UserStore` interface → swap `FileBasedUserStore` for `JpaUserStore`, `OAuth2UserStore`, or `CognitoUserStore` by implementing the interface and activating a profile.

---

### F4: API Parity Verification

**Goal**: Systematically compare sf_localstack responses against real Salesforce to find and fix gaps.

**Approach**:
1. Shell scripts in `parity-tests/` that run `sf` CLI commands against dev 20 org and capture JSON responses
2. Same scripts run against local sf_localstack instance
3. Java/shell comparison framework that diffs response shapes (ignoring dynamic values like IDs, timestamps)
4. Parity report: matches, mismatches with field-level diffs

**Test categories**:
- REST CRUD: create, read, update, delete, upsert for Account, Contact, Lead
- SOQL: simple SELECT, WHERE with various operators, ORDER BY, LIMIT, relationship queries
- Describe: sObject describe, global describe
- Bulk API v2: full ingest lifecycle
- Metadata SOAP: describeMetadata, listMetadata, readMetadata
- Error responses: invalid object type, missing required field, bad SOQL syntax
- Version discovery: `/services/data` endpoint

**Output**: `parity-tests/reports/` with JSON diffs, summary document listing all gaps prioritized by severity.

---

### F5: SOQL Engine Enhancement

**Goal**: Replace regex parser with proper parser that handles real-world SOQL.

**Architecture**:
```
SOQL string → SoqlLexer → Token[] → SoqlParser → SoqlAst → SoqlExecutor → results
```

**Supported in MVP**:
- SELECT: field list, COUNT(), relationship fields (dot notation: `Account.Name`)
- FROM: single object, with alias support
- WHERE: =, !=, <, >, <=, >=, LIKE, IN, NOT IN, AND, OR, parenthesized groups, NULL checks
- ORDER BY: single/multiple fields, ASC/DESC, NULLS FIRST/LAST
- LIMIT, OFFSET
- Relationship queries: parent-to-child (dot notation in SELECT), child-to-parent (sub-select in SELECT)

**Not in MVP (documented)**:
- GROUP BY, HAVING, ROLLUP
- Aggregate functions beyond COUNT()
- TYPEOF, polymorphic queries
- Semi-joins, anti-joins
- Bind variables
- Date literals (TODAY, LAST_N_DAYS, etc.)

**Testing**: Unit tests for lexer, parser, and executor independently. Integration tests with real SOQL queries captured from F4.

---

### F6: API Fidelity Fixes

**Goal**: Fix gaps found by F4 parity tests so sf CLI works seamlessly against sf_localstack.

**Known areas** (to be expanded by F4):
- Error response format: must match `[{errorCode: "...", message: "...", fields: [...]}]`
- Describe response: missing fields, incorrect types, missing picklist values
- Bulk API: CSV format edge cases (quoted fields, newlines in values, empty fields)
- HTTP status codes: ensure exact match (e.g., 404 vs 400 for missing record)
- Response headers: `Sforce-Limit-Info`, `Content-Type` headers matching real SF
- ID format: 15-char vs 18-char handling

**Approach**: F4 produces a prioritized gap list. F6 works through it top-down, fixing the most impactful issues first. Each fix gets a test that verifies the exact response shape.

---

### F7: Salesforce-Familiar UI

**Goal**: UI that feels intuitively familiar to Salesforce users — tables, forms, record pages, not JSON editors.

**Design system**: Tailwind CSS config extended with SLDS design tokens:
- **Colors**: brand (#0176d3), success (#2e844a), warning (#fe9339), error (#ea001e), neutral grays from SLDS palette
- **Typography**: system font stack (SF Pro on Mac, Segoe UI on Windows), SLDS heading/body sizes
- **Spacing**: 4px base grid
- **Borders/shadows**: matching Lightning component specs
- **Icons**: Lucide (already in project) — maps well to SF icon patterns

**Layout**:
```
┌──────────────────────────────────────────────────────┐
│  [Logo] SF LocalStack    [Search]    [User ▼] [Setup]│  ← Global Nav Bar
├───────────┬──────────────────────────────────────────┤
│           │                                          │
│ Objects   │  List View / Record Detail / etc.        │
│ ─────────│                                          │
│ Accounts  │  ┌─────────────────────────────────────┐ │
│ Contacts  │  │ Name    │ Industry │ Created    │ ⋮ │ │
│ Leads     │  ├─────────┼──────────┼────────────┼───┤ │
│ Custom... │  │ Acme    │ Tech     │ 2026-04-09 │ ⋮ │ │
│           │  │ Globex  │ Mfg      │ 2026-04-08 │ ⋮ │ │
│ ─────────│  └─────────────────────────────────────┘ │
│ Metadata  │                                          │
│ Bulk Jobs │  [New] [Import] [Refresh]     Page 1 of 3│
│ API Log   │                                          │
│ Setup     │                                          │
│           │                                          │
├───────────┴──────────────────────────────────────────┤
│  SF LocalStack v0.1.0 │ API v60.0 │ Status: Running  │  ← Footer
└──────────────────────────────────────────────────────┘
```

**Views**:

| View | Description |
|------|-------------|
| **Object List View** | Data table with sortable columns, search/filter bar, pagination. Row actions (view, edit, delete). "New" button for creating records. |
| **Record Detail** | Two-column field layout in collapsible sections (Details, System Information). View mode shows field labels + values. Edit mode shows form controls (text, number, date, picklist). |
| **Metadata Manager** | Table of metadata resources by type. Expand type to see resources. Edit metadata attributes in a form, not raw JSON. |
| **Bulk Job Monitor** | Table of jobs with status badges (Open, UploadComplete, JobComplete, Failed). Click job for detail: upload CSV, view results. |
| **API Request Log** | Live-updating table with method/path/status/duration columns. Click row for request/response detail pane. Filter by method, status, path. |
| **Setup** | User management table (admin only). Org settings. API version info. Reset org button with confirmation. |
| **Login Page** | Clean login form with username/password. Register link (first-run) or admin-created accounts. |

**Component library** (built during F7, reusable):
- `DataTable` — sortable, paginated, with row actions
- `RecordForm` — dynamic form from field definitions, view/edit modes
- `PageHeader` — title, breadcrumbs, action buttons
- `Badge` — status badges with SF-style colors
- `Modal` — confirmation dialogs, record creation
- `Toast` — success/error notifications
- `Tabs` — section navigation within views

---

### F8: Architecture Cleanup

**Goal**: Restructure packages for long-term maintainability, after F1 tests provide safety.

**Target package structure**:
```
co.razkevich.sflocalstack/
  auth/
    controller/   → LoginController, AuthController
    service/      → JwtService, AuthService
    model/        → User, Role, JwtClaims
    store/        → UserStore (interface), FileBasedUserStore
    filter/       → JwtAuthFilter
  data/
    controller/   → SObjectController, QueryController
    service/      → OrgStateService, SoqlEngine, SoqlLexer, SoqlParser
    model/        → SObjectRecord, SoqlAst, QueryResult
    repository/   → SObjectRecordRepository
  bulk/
    controller/   → BulkController
    service/      → BulkJobService
    model/        → BulkIngestJob, BulkRowResult
    repository/   → BulkIngestJobRepository
  metadata/
    controller/   → MetadataController, MetadataRestController, MetadataAdminController
    service/      → MetadataService, MetadataToolingService, MetadataSoapParser, MetadataSoapRenderer, MetadataZipService
    model/        → MetadataResource, MetadataDeployJob, MetadataRetrieveJob
    repository/   → MetadataResourceRepository, MetadataDeployJobRepository
  observability/
    controller/   → DashboardController, ResetController
    service/      → RequestLogService
    model/        → RequestLogEntry, DashboardOverview
    filter/       → RequestLoggingFilter, GzipRequestDecompressionFilter
  config/
    → WebConfig, CorsConfig, SecurityConfig, ApplicationConfig
```

**Rules**:
- Each domain package owns its controllers, services, models, and repositories
- Cross-domain calls go through service interfaces only
- No circular dependencies between domain packages
- `config/` is the only package that wires everything together

---

## Extensibility Paths (Documented, Not Implemented)

| Component | MVP Implementation | Production Path |
|-----------|-------------------|-----------------|
| Database | H2 file-based | PostgreSQL via `application-prod.yml` |
| Auth store | `FileBasedUserStore` (JSON file) | `JpaUserStore` or OAuth2/OIDC provider (Auth0, Keycloak, Cognito) |
| Auth tokens | Self-signed JWT | External JWT issuer or session-based auth |
| Multi-tenancy | Single user, single org | Org-per-tenant with tenant ID in JWT, schema isolation |
| Rate limiting | None | Spring Cloud Gateway or Bucket4j |
| Deployment | Docker single container | K8s Helm chart, Terraform |
| Monitoring | Request log SSE | Prometheus metrics, structured logging |
| SOQL | Recursive-descent parser | Extended with date literals, GROUP BY, aggregates |

## Descoped from MVP

These are explicitly out of scope but documented for future:
- Multi-tenant isolation
- Rate limiting and quotas
- Cloud infra (Terraform, K8s manifests)
- SOAP APIs beyond Metadata (Partner, Enterprise WSDL)
- Streaming API / Platform Events
- Apex execution engine
- Custom field/object definition UI
- Automated CI parity regression

## Build Order

```
F1 (Tests) → F2 (Persistence) → F8 (Refactor) → F3 (Auth) → F4 (Parity) → F5 (SOQL) + F6 (Fidelity) → F7 (UI)
```

Each feature follows the speckit workflow: specify → plan → tasks → implement → verify.
