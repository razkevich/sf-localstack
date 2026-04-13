# sf_localstack — Salesforce API Emulator

## Workflow Priority
- **Speckit workflow takes precedence over superpowers skills** when both apply. Follow speckit's specify → plan → tasks → implement cycle. Superpowers skills (brainstorming, writing-plans, etc.) are supplementary.
- Each feature gets its own git branch and PR, merged after implementation and testing.

## Project Purpose
Local Salesforce API emulator for development, CI/CD testing, and SaaS offering. Emulates REST, Bulk v2, Metadata SOAP, Tooling, and OAuth2 surfaces. Goal: drop-in replacement for real Salesforce orgs in dev/test workflows.

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
docker build -t sf-localstack .          # Docker image (multi-arch: --platform linux/amd64)
```

## Production Deployment (DigitalOcean)

### Live Instance
- **URL**: http://164.92.219.185
- **Droplet**: `sf-localstack` (s-1vcpu-1gb, $6/mo, AMS3, Ubuntu 22.04)
- **Droplet ID**: 564501038
- **SSH**: `ssh root@164.92.219.185` (key: `~/.ssh/id_ed25519`, DO key ID: 55545874)
- **Firewall**: `sf-localstack-fw` (ports 22, 80, 443)

### Architecture on Droplet
- **Java 21** (OpenJDK, apt-installed) runs the JAR directly (no Docker on server)
- **systemd service**: `/etc/systemd/system/sf-localstack.service`
- **nginx**: reverse proxy on port 80 → localhost:8080, SSE/WebSocket support
- **Data dir**: `/opt/sf-localstack-data/data/` (users.json, H2 sfdb files)
- **JWT secret**: configured via `SF_LOCALSTACK_JWT_SECRET` env var in systemd unit
- **Spring profile**: `dev` (H2 file-based storage)

### Deploy Commands
```bash
# Build locally and deploy to droplet
mvn -pl service package -DskipTests -q
scp service/target/sf-localstack-service-*.jar root@164.92.219.185:/opt/sf-localstack.jar
ssh root@164.92.219.185 'systemctl restart sf-localstack'

# Check status
ssh root@164.92.219.185 'systemctl status sf-localstack'
ssh root@164.92.219.185 'tail -50 /var/log/nginx/access.log'

# View/manage users
ssh root@164.92.219.185 'cat /opt/sf-localstack-data/data/users.json'
```

### Domain (TODO)
- No domain configured yet — using IP address directly
- Need to register a domain and configure Let's Encrypt SSL via certbot (already installed)
- certbot is installed: `ssh root@164.92.219.185 'certbot --nginx -d <domain>'`

## Architecture Overview
- **Domain-first organization** with thin protocol adapters (see ARCHITECTURE.md)
- **Controllers** (11): SObject, Query, Bulk, MetadataSOAP, MetadataREST, OAuth, Auth, Version, Dashboard, Reset, MetadataAdmin
- **Auth**: JWT-based (JJWT library), FileBasedUserStore (`data/users.json`), JwtAuthFilter on `/services/*` and `/api/*`
- **Services**: OrgStateService (sObject CRUD), SoqlEngine (regex SOQL), BulkJobService, MetadataService, MetadataToolingService, MetadataSoapParser/Renderer, MetadataZipService, RequestLogService, JwtService
- **Storage**: H2 file-based for sObjects (JPA entity `SObjectRecord`), `data/users.json` for auth users, ConcurrentHashMap for Bulk/Metadata jobs
- **Frontend**: Single-page React app with login/register UI, Sidebar navigation, 8 view panels, SSE for live request streaming

## Key Patterns
- All `/services/*` requests are captured by RequestLoggingFilter for dashboard inspection
- SOQL is parsed via regex (SoqlEngine) — supports single-table SELECT with simple WHERE, ORDER BY, LIMIT
- OAuth2 password grant at `/services/oauth2/token` returns Salesforce-formatted tokens (`<orgId>!<jwt>`)
- JwtAuthFilter strips org ID prefix from tokens before JWT validation
- User sObject lookups (`/services/data/.../sobjects/User/{id}`) fall back to auth UserStore (needed by SF CLI)
- `/id/{orgId}/{userId}` endpoint returns user identity (used by SF CLI during login)
- `instance_url` in OAuth responses resolves from `X-Forwarded-*` headers (nginx-aware)
- Metadata deploy/retrieve are state-machine simulations (no real package processing)
- API version hardcoded to v60.0 in config, version discovery serves v50–v60
- First registered user becomes ADMIN; subsequent registrations require admin JWT

## SF CLI Usage
```bash
# Get token via OAuth2 password grant
TOKEN=$(curl -s -X POST http://164.92.219.185/services/oauth2/token \
  -d "grant_type=password&username=USER&password=PASS" | jq -r '.access_token')

# Login with SF CLI
export SF_ACCESS_TOKEN="$TOKEN"
sf org login access-token --instance-url http://164.92.219.185 --alias sf-cloud --no-prompt

# Use SF CLI normally
sf data query --target-org sf-cloud --query "SELECT Id, Name FROM Account"
sf data create record --target-org sf-cloud --sobject Account --values "Name='Test'"
```

## Testing Conventions
- Integration tests: `@SpringBootTest` + `MockMvc`, test full request/response cycles
- Unit tests: per-controller and per-service, mock dependencies
- 23 test files total in `src/test/java/co/razkevich/sflocalstack/`
- Test naming: `*Test.java` for unit, `*IntegrationTest.java` for integration
- Test profile uses InMemoryUserStore, auth bypassed when no users exist

## Known Limitations (as of current state)
- **No HTTPS** — running on HTTP; need domain + Let's Encrypt for SSL
- **No OAuth2 authorization code flow** — `sf org login web` does NOT work (requires `/services/oauth2/authorize`); only `sf org login access-token` via password grant
- **SOQL gaps** — no relationship queries, sub-selects, aggregate functions, GROUP BY
- **Metadata** — synthetic ZIP generation, no schema validation
- **Field catalog** — hardcoded in OrgStateService, no custom field support
- **Error fidelity** — basic error responses, not all SF error codes
- **No multi-tenancy** — single org, all users share one data space
