# SF LocalStack

[![CI](https://github.com/razkevich/sf-localstack/actions/workflows/ci.yml/badge.svg)](https://github.com/razkevich/sf-localstack/actions/workflows/ci.yml)

A local Salesforce org emulator for CI/CD pipelines and development workflows — no scratch orgs, no Developer Edition limits, no network dependency.

## What Is This?

SF LocalStack is a Spring Boot service that emulates the Salesforce REST, SOAP Metadata, Bulk, and Tooling APIs. It lets you run `sf` CLI commands, integration tests, and deployment tooling against a local HTTP server instead of a real Salesforce org.

It is designed for teams that need:
- **Fast, deterministic CI pipelines** without scratch org provisioning time
- **Offline development** of Salesforce tooling, automations, and integrations
- **Hermetic integration tests** for Salesforce-adjacent services (metadata processors, ETL pipelines, deploy orchestrators)

## Features

| Category | What's Emulated |
|---|---|
| **Auth** | OAuth2 token endpoint, session-based SOAP auth |
| **REST API** | sObject CRUD, SOQL queries, composite requests |
| **Metadata API (SOAP)** | `describeMetadata`, `listMetadata`, `readMetadata`, `retrieve` / `checkRetrieveStatus`, `deploy` / `checkDeployStatus` / `cancelDeploy` |
| **Tooling API** | SOQL queries over `CustomApplication`, `EntityDefinition`, `FieldDefinition`, `FlowDefinition`, `FlowDefinitionView`, `SourceMember`, `TabDefinition` |
| **Bulk API** | Job creation, batch upload, status polling |
| **Source tracking** | `SourceMember` stubs for `sf project retrieve preview` / `sf project retrieve start` |
| **Seed data** | YAML-based org baseline (Accounts, Contacts, Users, Organization, custom metadata) |
| **Dashboard** | React UI for inspecting sObjects, metadata resources, request logs, and Bulk jobs |
| **Reset endpoint** | `POST /reset` restores the org to seed state between test runs |

## Use Cases

- Run `sf project retrieve start --manifest manifest/package.xml --target-org sf-local` against a local server and get real source files written to disk
- Test deploy/retrieve pipelines in CI without provisioning scratch orgs
- Validate metadata-processing services (field transformers, package assemblers, deployment orchestrators) with repeatable fixture data
- Develop and debug Salesforce tooling offline

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.3, Spring Data JPA, H2 (in-memory)
- **Frontend**: React 18, TypeScript 5, Vite 5, Tailwind CSS 3
- **Build**: Maven (multi-module)
- **CLI compatibility**: Salesforce CLI (`sf`) v2, API version 60.0

## Quick Start

### Docker (recommended)

```bash
docker pull razkevich/sf-localstack:latest
docker run -p 8080:8080 razkevich/sf-localstack
```

The service starts on `http://localhost:8080`.

### Connect SF CLI

```bash
sf org login access-token \
  --instance-url http://localhost:8080 \
  --access-token "00D000000000001!FAKE_ACCESS_TOKEN" \
  --alias sf-local \
  --no-prompt
```

### Retrieve metadata

```bash
sf project retrieve start \
  --manifest manifest/package.xml \
  --target-org sf-local
```

### Reset org state

```bash
curl -X POST http://localhost:8080/reset
```

### Dashboard

The dashboard is bundled in the Docker image. Open `http://localhost:8080` after starting the container.

### Maven dependency (GitHub Packages)

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/razkevich/sf-localstack</url>
  </repository>
</repositories>

<dependency>
  <groupId>co.prodly</groupId>
  <artifactId>sf-localstack-service</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

<details>
<summary>Run from source</summary>

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+ (for the dashboard)
- [Salesforce CLI](https://developer.salesforce.com/tools/salesforcecli) (`sf`)

```bash
mvn -pl service spring-boot:run
```

```bash
cd frontend && npm install && npm run dev
# Dashboard: http://localhost:5173
```

</details>

## Roadmap

- [x] JUnit/Testcontainers extension for hermetic integration tests — use `razkevich/sf-localstack` as a Testcontainers `GenericContainer` (see [metadata-service AbstractSfLocalstackTest](https://github.com/prodly/metadata-service) for a reference implementation)
- [ ] Configurable seed files via environment variable
- [ ] Deploy validation with component-level error injection
- [ ] Expanded sObject schema (Opportunity, Lead, Case)
