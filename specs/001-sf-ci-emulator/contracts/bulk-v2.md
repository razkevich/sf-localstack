# Contract: Bulk API v2 Ingest

## Scope

- `POST /services/data/v60.0/jobs/ingest`
- `PUT /services/data/v60.0/jobs/ingest/{jobId}/batches`
- `PATCH /services/data/v60.0/jobs/ingest/{jobId}`
- `GET /services/data/v60.0/jobs/ingest/{jobId}`
- `DELETE /services/data/v60.0/jobs/ingest/{jobId}`
- `GET /services/data/v60.0/jobs/ingest/{jobId}/successfulResults`
- `GET /services/data/v60.0/jobs/ingest/{jobId}/failedResults`
- `GET /services/data/v60.0/jobs/ingest/{jobId}/unprocessedrecords`

## Create Job

```json
{
  "operation": "insert",
  "object": "Account",
  "contentType": "CSV"
}
```

- Response includes deterministic job id, `state: "Open"`, object, operation, and created date fields expected by common clients.

## Upload Batch

- Request content type: `text/csv`
- Body example:

```csv
Name,Industry
Bulk Corp,Technology
```

- Response: `201 Created` with empty body.

## Close Job

```json
{
  "state": "UploadComplete"
}
```

- Emulator processes all accumulated CSV synchronously before replying.
- Success response includes `state: "JobComplete"`, `numberRecordsProcessed`, and `numberRecordsFailed`.

## Result Endpoints

- Successful results content type: `text/csv`

```csv
sf__Id,sf__Created
001000000000003,true
```

- Failed results content type: `text/csv`

```csv
sf__Id,sf__Error
,Missing required Id
```

- Unprocessed results return an empty CSV for this slice unless the parser rejects rows before processing.

## Behavioral Guarantees

- Supported operations: `insert`, `update`, `delete`, `upsert`.
- `update` and `delete` require `Id` in CSV rows.
- `upsert` requires `externalIdFieldName` on the job and must delegate to synchronized org-state upsert.
- Successful row mutations are visible to REST query immediately after close returns.
- Reset clears all jobs and result files.
