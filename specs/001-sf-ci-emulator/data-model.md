# Data Model: Usable Salesforce Integration-Test Emulator

## Emulated Org State

- **Purpose**: Canonical resettable state for Salesforce records visible through REST query/CRUD/upsert and Bulk ingest mutations.
- **Backing store**: H2 via `SObjectRecord` JPA entity.
- **Fields**:
  - `id`: Salesforce-style record id, generated deterministically per object type.
  - `objectType`: sObject API name such as `Account` or `Contact`.
  - `fieldsJson`: full record field map, including `Id`, business fields, relationship literals, and audit timestamps.
  - `createdDate`, `lastModifiedDate`: deterministic instants mirrored into `fieldsJson`.
- **Rules**:
  - Reset replaces all records with the seed baseline.
  - External-ID upsert must either update one matching record or create one new record.
  - Relationship projection prefers literal dotted fields like `Account.Name`; otherwise it resolves via `AccountId` and a related record lookup.

## Seed Dataset

- **Purpose**: Stable YAML-defined baseline used on startup and reset.
- **Source**: `service/src/main/resources/seed/default-seed.yml`.
- **Rules**:
  - Seed load order is deterministic.
  - Seed-generated IDs and timestamps must also be deterministic.
  - Seed data can contain literal relationship fields used directly by SOQL projection.

## SOQL Query Model

- **Purpose**: Internal parsed representation for the supported SOQL subset.
- **Fields**:
  - `selectFields`: ordered list of requested fields or `COUNT()` marker.
  - `objectType`: source sObject.
  - `filters`: ordered `AND` conditions.
  - `limit`: optional row cap.
- **Condition fields**:
  - `fieldPath`: field or relationship field path.
  - `operator`: `EQ`, `NEQ`, `LIKE`, `IS_NULL`, `IS_NOT_NULL`.
  - `value`: string, number, or null.
- **Rules**:
  - Unsupported clauses or operators produce deterministic Salesforce-like errors.
  - Filtering runs before projection and limit.
  - `COUNT()` returns a Salesforce-like aggregate row with deterministic key naming.

## External-ID Upsert Result

- **Purpose**: Encapsulate whether an upsert updated or created a record.
- **Fields**:
  - `recordId`
  - `created` (`true` for insert, `false` for update)
  - `record` or persisted field map for downstream response building
- **Rules**:
  - Service method is synchronized.
  - Matching is constrained to `objectType`, `externalIdField`, and `externalIdValue`.

## Bulk Ingest Job

- **Purpose**: Deterministic in-memory representation of Bulk API v2 ingest state.
- **Fields**:
  - `id`: deterministic job id prefixed like Salesforce ingest jobs.
  - `operation`: `insert`, `update`, `delete`, or `upsert`.
  - `objectType`
  - `externalIdFieldName`: required for upsert jobs.
  - `state`: `Open`, `UploadComplete`, `JobComplete`, `Aborted`, or `Failed`.
  - `csvBatches`: uploaded raw CSV payloads in arrival order.
  - `successfulResults`: ordered per-row success results.
  - `failedResults`: ordered per-row failure results.
  - `unprocessedResults`: ordered rows skipped before processing, usually empty for this slice.
  - `numberRecordsProcessed`, `numberRecordsFailed`
  - `createdDate`
- **State transitions**:
  - `Open -> UploadComplete -> JobComplete`
  - `Open -> Aborted`
  - `Open|UploadComplete -> Failed` for malformed/unsupported inputs
- **Rules**:
  - Processing occurs synchronously when the client sets state to `UploadComplete`.
  - Successful row mutations are committed before the response reports `JobComplete`.
  - Reset clears all jobs and counters.

## Bulk Row Result

- **Purpose**: Materialized row outcome for CSV result endpoints.
- **Fields**:
  - `sf__Id`
  - `sf__Created` for successes
  - `sf__Error` for failures
  - original row values when needed for deterministic debugging
- **Rules**:
  - Result ordering matches uploaded row ordering.
  - Failures do not rewrite previously successful rows.

## Metadata Deploy Job

- **Purpose**: Deterministic in-memory record of a Metadata API deploy request.
- **Fields**:
  - `id`: deterministic async process id.
  - `done`
  - `success`
  - `status`: `Succeeded`, `Failed`, or `Canceled`.
  - `zipBytes` or retained payload metadata for status visibility.
  - `numberComponentsTotal`, `numberComponentsDeployed`, `numberComponentErrors`
  - `createdDate`
- **State transitions**:
  - `Pending -> Succeeded` immediately for successful supported deploys
  - `Pending -> Failed` for malformed requests
  - `Succeeded|Pending -> Canceled` when cancel is invoked on a known id
- **Rules**:
  - `checkDeployStatus` must be deterministic for known, unknown, and canceled ids.
  - Reset clears all deploy jobs and counters.

## Metadata Catalog Entry

- **Purpose**: Supported type metadata for `listMetadata` and `describeMetadata` SOAP responses.
- **Fields**:
  - `xmlName`
  - `directoryName`
  - `suffix`
  - `inFolder`
  - `metaFile`
  - optional seeded `FileProperties`
- **Rules**:
  - Catalog is static and versioned to the supported v60.0 slice.
  - Responses must be deterministic across runs.
