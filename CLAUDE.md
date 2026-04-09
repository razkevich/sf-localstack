# sf_localstack — Salesforce API Emulator

## Workflow Priority
- **Speckit workflow takes precedence over superpowers skills** when both apply. Follow speckit's specify → plan → tasks → implement cycle. Superpowers skills (brainstorming, writing-plans, etc.) are supplementary.
- Each feature gets its own git branch and PR, merged after implementation and testing.

## Project Purpose
Local Salesforce API emulator for development, CI/CD testing, and eventually SaaS offering. Emulates REST, Bulk v2, Metadata SOAP, Tooling, and OAuth2 surfaces. Goal: drop-in replacement for real Salesforce orgs in dev/test workflows.

## Tech Stack
- **Backend**: Java 21, Spring Boot 3.3.5 (`web`, `data-jpa`, `actuator`, `test`), H2, Jackson JSON/YAML, Lombok
- **Frontend**: React 18, TypeScript 5.x, Vite 5, Tailwind 3, Lucide icons
- **Build**: Maven multi-module (root pom → `service`, `client`), frontend builds into `service/src/main/resources/static/`
- **Docker**: Multi-stage Dockerfile (maven build → JRE runtime), published to Docker Hub `razkevich/sf-localstack`
- **CI**: GitHub Actions (`ci.yml` runs tests on push/PR, `release.yml` for Docker + GitHub Release)

## Build & Run Commands
```bash
mvn -pl service spring-boot:run          # Dev mode (backend on :8080)
cd frontend && npm run dev               # Frontend dev (Vite on :5173)
cd frontend && npm run build             # Build frontend into service static/
mvn clean package -pl service            # Build JAR
mvn -pl service test                     # Run tests
docker build -t sf-localstack .          # Docker image
```

## Architecture Overview
- **Domain-first organization** with thin protocol adapters (see ARCHITECTURE.md)
- **Controllers** (10): SObject, Query, Bulk, MetadataSOAP, MetadataREST, OAuth, Version, Dashboard, Reset, MetadataAdmin
- **Services**: OrgStateService (sObject CRUD), SoqlEngine (regex SOQL), BulkJobService, MetadataService, MetadataToolingService, MetadataSoapParser/Renderer, MetadataZipService, RequestLogService
- **Storage**: H2 in-memory for sObjects (JPA entity `SObjectRecord`), ConcurrentHashMap for Bulk/Metadata jobs
- **Frontend**: Single-page React app with Sidebar navigation, 8 view panels, SSE for live request streaming

## Key Patterns
- All `/services/*` requests are captured by RequestLoggingFilter for dashboard inspection
- SOQL is parsed via regex (SoqlEngine) — supports single-table SELECT with simple WHERE, ORDER BY, LIMIT
- OAuth2 is stub-only: hardcoded token, no validation
- Metadata deploy/retrieve are state-machine simulations (no real package processing)
- API version hardcoded to v60.0 in config, version discovery serves v50–v60

## Testing Conventions
- Integration tests: `@SpringBootTest` + `MockMvc`, test full request/response cycles
- Unit tests: per-controller and per-service, mock dependencies
- 23 test files total in `src/test/java/co/razkevich/sflocalstack/`
- Test naming: `*Test.java` for unit, `*IntegrationTest.java` for integration

## Known Limitations (as of current state)
- **No persistence** — H2 `create-drop`, ConcurrentHashMap; all state lost on restart
- **No real auth** — stub OAuth, no token validation, no RBAC
- **SOQL gaps** — no relationship queries, sub-selects, aggregate functions, GROUP BY
- **Metadata** — synthetic ZIP generation, no schema validation
- **Field catalog** — hardcoded in OrgStateService, no custom field support
- **Error fidelity** — basic error responses, not all SF error codes
