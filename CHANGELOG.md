# Changelog

All notable changes to this project will be documented in this file.

## [0.1.0] - 2026-03-21

### Added

- Salesforce REST API emulation: sObject CRUD, SOQL queries, composite requests
- Salesforce SOAP Metadata API: `retrieve`, `checkRetrieveStatus`, `deploy`, `checkDeployStatus`, `cancelDeploy`, `listMetadata`, `describeMetadata`, `readMetadata`
- Salesforce Tooling API: SOQL queries over `SourceMember`, `EntityDefinition`, `FieldDefinition`, `FlowDefinitionView`, `TabDefinition`, `CustomApplication`, `StaticResource`
- Salesforce Bulk API v2: job creation, batch upload, status polling
- OAuth2 token endpoint and session-based SOAP authentication
- YAML seed data loader (`default-seed.yml`) with Account, Contact, Organization, User records
- `POST /reset` endpoint to restore org to seed state between test runs
- React 18 dashboard for inspecting sObjects, metadata, request logs, and Bulk jobs
- Full `sf project retrieve start` compatibility with real Salesforce CLI
- Docker image published to Docker Hub (`razkevich/sf-localstack`)
- GitHub Actions CI workflow: tests on every push/PR, Docker image on main merges
- GitHub Actions release workflow: multi-platform Docker image (amd64+arm64), JAR on GitHub Packages, GitHub Release on version tags
