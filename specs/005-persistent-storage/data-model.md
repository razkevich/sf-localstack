# Data Model: 005-persistent-storage

## Existing Entity (modify)

### SObjectRecord
- Already a JPA entity in `sobject_records` table
- No changes needed to entity structure
- Only change: datasource URL switches from mem to file

## New Entities

### BulkIngestJobEntity
**Table**: `bulk_ingest_jobs`

| Field | Type | JPA Annotation | Notes |
|-------|------|---------------|-------|
| id | String | @Id | "750" prefix + 15 UUID chars |
| operation | String | @Column(nullable=false) | insert/update/delete/upsert |
| object | String | @Column(nullable=false) | SObject type name |
| externalIdFieldName | String | @Column | nullable, for upsert |
| state | String | @Column(nullable=false) | Open/UploadComplete/JobComplete |
| lineEnding | String | @Column | default "LF" |
| columnDelimiter | String | @Column | default "COMMA" |
| createdDate | Instant | @Column(nullable=false) | |
| systemModstamp | Instant | @Column(nullable=false) | |

**Relationships**: One-to-many → BulkBatchEntity, One-to-many → BulkRowResultEntity

### BulkBatchEntity
**Table**: `bulk_batches`

| Field | Type | JPA Annotation | Notes |
|-------|------|---------------|-------|
| id | Long | @Id @GeneratedValue | Auto-generated |
| jobId | String | @Column(nullable=false) | FK to bulk_ingest_jobs |
| csvData | String | @Lob | Raw CSV text |
| sequenceNumber | int | @Column | Batch ordering |

**Relationship**: Many-to-one → BulkIngestJobEntity

### BulkRowResultEntity
**Table**: `bulk_row_results`

| Field | Type | JPA Annotation | Notes |
|-------|------|---------------|-------|
| id | Long | @Id @GeneratedValue | Auto-generated |
| jobId | String | @Column(nullable=false) | FK to bulk_ingest_jobs |
| resultType | String | @Column(nullable=false) | successfulResults/failedResults/unprocessedrecords |
| sfId | String | @Column | sf__Id value |
| sfCreated | Boolean | @Column | sf__Created value |
| sfError | String | @Column | sf__Error message |
| originalRow | String | @Column(columnDefinition="TEXT") | Original CSV row data |

**Relationship**: Many-to-one → BulkIngestJobEntity

### MetadataResourceEntity
**Table**: `metadata_resources`

| Field | Type | JPA Annotation | Notes |
|-------|------|---------------|-------|
| id | Long | @Id @GeneratedValue | Auto-generated |
| type | String | @Column(nullable=false) | Metadata type (ApexClass, etc.) |
| fullName | String | @Column(nullable=false) | Unique within type |
| fileName | String | @Column | |
| directoryName | String | @Column | |
| inFolder | boolean | @Column | |
| metaFile | boolean | @Column | |
| label | String | @Column | |
| suffix | String | @Column | |
| attributesJson | String | @Lob | JSON-serialized Map<String, Object> |
| lastModifiedDate | Instant | @Column | |

**Constraints**: Unique(type, fullName)

### MetadataDeployJobEntity
**Table**: `metadata_deploy_jobs`

| Field | Type | JPA Annotation | Notes |
|-------|------|---------------|-------|
| id | String | @Id | "0Af" prefix + 12 UUID chars |
| done | boolean | @Column | |
| success | boolean | @Column | |
| status | String | @Column | |
| numberComponentsTotal | int | @Column | |
| numberComponentsDeployed | int | @Column | |
| numberComponentErrors | int | @Column | |
| createdDate | Instant | @Column | |
| completedDate | Instant | @Column | nullable |

### MetadataRetrieveJobEntity
**Table**: `metadata_retrieve_jobs`

| Field | Type | JPA Annotation | Notes |
|-------|------|---------------|-------|
| id | String | @Id | "09S" prefix + 12 UUID chars |
| done | boolean | @Column | |
| success | boolean | @Column | |
| status | String | @Column | |
| zipFileBase64 | String | @Lob | nullable, base64 ZIP |
| numberComponentsTotal | int | @Column | |
| createdDate | Instant | @Column | |
| completedDate | Instant | @Column | nullable |

## State Transitions

### BulkIngestJob State Machine
```
Open → UploadComplete → JobComplete
Open → Aborted (via delete)
```

### MetadataDeployJob States
```
Created (done=false) → Succeeded (done=true, success=true)
                     → Failed (done=true, success=false)
```

### MetadataRetrieveJob States
```
Created (done=false) → Succeeded (done=true, success=true, zipFileBase64 populated)
```

## Reset Order (FK-safe)
1. BulkRowResultEntity (child)
2. BulkBatchEntity (child)
3. BulkIngestJobEntity (parent)
4. MetadataResourceEntity (independent)
5. MetadataDeployJobEntity (independent)
6. MetadataRetrieveJobEntity (independent)
7. SObjectRecord (independent)
