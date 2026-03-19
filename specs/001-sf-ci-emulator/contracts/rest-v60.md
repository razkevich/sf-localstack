# Contract: REST API v60.0

## Scope

- `GET /services/data/`
- `GET /services/data/v60.0/query?q=...`
- `GET /services/data/v60.0/sobjects/{Object}`
- `GET /services/data/v60.0/sobjects/{Object}/{id}`
- `POST /services/data/v60.0/sobjects/{Object}`
- `PATCH /services/data/v60.0/sobjects/{Object}/{id}`
- `PATCH /services/data/v60.0/sobjects/{Object}/{externalIdField}/{externalIdValue}`
- `DELETE /services/data/v60.0/sobjects/{Object}/{id}`
- `POST /reset`

## SOQL Query Expectations

- Supported query shape: `SELECT <fields>|COUNT() FROM <Object> [WHERE <cond> [AND <cond>...]] [LIMIT n]`
- Supported condition operators:
  - `=` string/number/null
  - `!=` string/number/null
  - `LIKE` with `%` and `_`
  - `IS NULL`
  - `IS NOT NULL`
- Supported projection:
  - direct fields like `Id`, `Name`
  - relationship fields like `Account.Name`
- Response shape:

```json
{
  "totalSize": 1,
  "done": true,
  "records": [
    {
      "Id": "003000000000001",
      "FirstName": "John",
      "Account": {
        "Name": "Acme Corp"
      }
    }
  ]
}
```

- Aggregate count response shape:

```json
{
  "totalSize": 1,
  "done": true,
  "records": [
    {
      "expr0": 2
    }
  ]
}
```

- Unsupported syntax must return a deterministic Salesforce-like error payload, not an empty success.

## External-ID Upsert Expectations

- Request:

```http
PATCH /services/data/v60.0/sobjects/Account/ExternalId__c/EXT-001
Content-Type: application/json

{"Name":"Acme Updated","ExternalId__c":"EXT-001"}
```

- Update case response: `204 No Content`
- Create case response:

```json
{
  "id": "001000000000003",
  "success": true,
  "errors": []
}
```

- Concurrent requests for the same `{Object, externalIdField, externalIdValue}` must converge on one logical record outcome.

## Error Expectations

- Record-not-found shape remains Salesforce-like array-based errors.
- Malformed or unsupported SOQL/upsert inputs return deterministic error codes and messages suitable for assertions.

## Reset Expectations

- `POST /reset` clears record mutations and all transient cross-surface state, then reloads the seed baseline.

## Accepted Deltas

- Feature 1 parity checks against `dev20` show that query envelopes now include Salesforce-style `attributes` objects, but `describe` remains intentionally narrow: the emulator currently returns only fields inferred from the seeded local org state instead of Salesforce's full Account field catalog. This delta is accepted for the current slice and should be revisited as describe fidelity expands.
