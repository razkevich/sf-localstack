# SF LocalStack

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

> **Note**: Docker packaging and JUnit test support are in progress. The steps below run the service directly from source.

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+ (for the dashboard)
- [Salesforce CLI](https://developer.salesforce.com/tools/salesforcecli) (`sf`)

### Run the backend

```bash
JAVA_HOME=/path/to/java21 mvn -pl service spring-boot:run
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

```bash
cd frontend && npm install && npm run dev
```

Open `http://localhost:5173`.

## Roadmap

- [ ] Docker image with single-command startup
- [ ] JUnit extension for hermetic integration tests
- [ ] Configurable seed files via environment variable
- [ ] Deploy validation with component-level error injection
- [ ] Expanded sObject schema (Opportunity, Lead, Case)
