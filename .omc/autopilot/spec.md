# sf-localstack Full Implementation Spec

## Goal
Complete the LocalStack-for-Salesforce implementation so Prodly engineers can run
integration tests against the emulator with no real Salesforce org.

## Components

### 1. Enhanced SOQL Engine
- WHERE field = 'value' (string, number equality)
- WHERE field LIKE '%value%'
- WHERE field != null / field = null
- LIMIT n
- Relationship fields: Account.Name in SELECT traverses related record
- COUNT() aggregate
- Multiple WHERE conditions with AND

### 2. Upsert Endpoint
- PATCH /services/data/v{n}/sobjects/{Object}/{externalIdField}/{externalIdValue}
- Synchronized to prevent race conditions
- Returns 201 (created) or 200 (updated)

### 3. Bulk API v2 Ingest
- POST   /services/data/v{n}/jobs/ingest               — create job
- PUT    /services/data/v{n}/jobs/ingest/{id}/batches  — upload CSV
- PATCH  /services/data/v{n}/jobs/ingest/{id}          — close job (UploadComplete)
- GET    /services/data/v{n}/jobs/ingest/{id}          — poll state
- GET    /services/data/v{n}/jobs/ingest/{id}/successfulResults
- GET    /services/data/v{n}/jobs/ingest/{id}/failedResults
- Operations: insert, update, delete, upsert
- CSV parsing (manual, no external lib)
- State machine: Open → UploadComplete → JobComplete

### 4. SOAP Metadata API
- POST /services/Soap/m/{version} (text/xml)
- Operations: listMetadata, describeMetadata, deploy, checkDeployStatus, cancelDeploy
- XML parsing via javax.xml.parsers.DocumentBuilder
- String-templated SOAP XML responses
- All deploys succeed immediately (done=true, success=true)
- In-memory DeployJob store

### 5. Tests (TODO-3)
- SoqlRelationshipQueryTest: Account.Name traversal
- BulkApiV2Test: full lifecycle
- MetadataApiTest: deploy + checkDeployStatus

### 6. Docker
- Dockerfile (eclipse-temurin:21-jre-alpine)
- docker-compose.yml

### 7. README.md
