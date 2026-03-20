# Contract: Metadata SOAP API

## Scope

- `POST /services/Soap/m/60.0`
- Supported operations: `listMetadata`, `describeMetadata`, `readMetadata`, `deploy`, `checkDeployStatus`, `cancelDeploy`
- Deferred next operation set: `retrieve`, `checkRetrieveStatus`
- Supporting query surfaces used by `metadata-service`:
  - `GET /services/data/v60.0/tooling/query` for `TabDefinition`, `CustomApplication`, `EntityDefinition`, `FlowDefinition`, and `Flow`
  - `GET /services/data/v60.0/query` for `FlowDefinitionView`

## Envelope Expectations

- Request and response content type: `text/xml`
- Response wrapper:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:m="http://soap.sforce.com/2006/04/metadata">
  <soapenv:Body>
    <!-- operation-specific response -->
  </soapenv:Body>
</soapenv:Envelope>
```
## Deploy

- Input contains `<deploy>` with a base64 zip payload and options.
- Response contains a deterministic async process id.
- Supported deploys complete immediately for this slice with `done=true` and `success=true` on status checks unless the request is malformed.

## Check Deploy Status

- Input contains `<checkDeployStatus>` and `<asyncProcessId>`.
- Response includes `id`, `done`, `success`, `status`, `numberComponentsTotal`, `numberComponentsDeployed`, `numberComponentErrors`, and empty failure/detail collections when appropriate.

## Cancel Deploy

- Known deploy ids return a completed cancel result with the same id.
- Unknown ids return a deterministic Salesforce-like fault or failure result that clients can parse.

## List Metadata / Describe Metadata

- `listMetadata` returns deterministic `FileProperties` results for the supported metadata catalog.
- `describeMetadata` returns deterministic `DescribeMetadataObject` entries for the same supported catalog.
- `readMetadata` returns typed records for the supported metadata catalog (`CustomField`, `StandardValueSet`, `GlobalValueSet`, and generic typed reads).

## Deferred Next: Retrieve via `package.xml`

- The next planned metadata feature adds `retrieve` and `checkRetrieveStatus` with manifest-driven ZIP results.
- The intended target flow is `sf project retrieve start --manifest package.xml` against localhost.
- Retrieve scope should be limited to metadata types already present in the local metadata catalog unless explicitly expanded by a later spec update.
- Unsupported manifest members, unsupported wildcards, and unsupported packaging semantics must return deterministic, parseable failures.

## Behavioral Guarantees

- SOAP operation routing is based on the first body element local name.
- Namespace-tolerant parsing is required.
- Reset clears all deploy jobs and restores the metadata catalog to baseline behavior.

## Accepted Deltas

- The emulator exposes only the metadata catalog entries and tooling/helper rows needed by current `metadata-service` flows; `dev20` returns a much larger real-org dataset.
- The emulator accepts `FlowDefinitionView` through the standard query endpoint because that matches current service usage; querying `FlowDefinitionView` through Tooling returns `INVALID_TYPE` on `dev20`.
