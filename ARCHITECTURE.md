# Architecture

## Recommended Structure

The best fit for `sf_localstack` is a hybrid, domain-first backend structure:

- `api/` or `controller/`
  - thin HTTP/SOAP adapters only
  - translate protocol details to application calls
  - no business rules beyond protocol validation and response shaping
- `service/data/`
  - record lifecycle, SOQL support, describe synthesis, upsert semantics
- `service/bulk/`
  - Bulk job lifecycle, CSV parsing, result generation
- `service/metadata/`
  - metadata catalog state, deploy state, SOAP operation logic, helper-query logic
- `service/observability/`
  - request logging, dashboard overview aggregation, streaming updates
- `model/`
  - protocol DTOs and persistent/runtime state objects
- `repository/`
  - persistence only

## Why This Structure

- It matches how the product is used: data, bulk, metadata, observability
- It keeps protocol adapters thin and prevents controller bloat
- It makes parity work easier because Salesforce-surface behavior stays close to the adapter layer
- It avoids over-engineering into full hexagonal ports/adapters before the app needs it

## Practical Rule

Use domain-first organization with thin protocol adapters.

- Good: `MetadataController -> MetadataService/MetadataSoapRenderer`
- Good: `BulkController -> BulkJobService`
- Avoid: controllers building domain state directly
- Avoid: one giant service holding unrelated data, bulk, and metadata behavior

## Current Direction

The codebase already follows this partially:

- data: `OrgStateService`, `SoqlEngine`, `SObjectController`, `QueryController`
- bulk: `BulkJobService`, `BulkController`
- metadata: `MetadataService`, `MetadataToolingService`, `MetadataController`, `MetadataRestController`, `MetadataAdminController`
- observability: `RequestLogService`, `DashboardController`, `RequestLoggingInterceptor`

Future refactors should continue pushing logic toward these domain groupings rather than adding more endpoint-specific branches in shared classes.
