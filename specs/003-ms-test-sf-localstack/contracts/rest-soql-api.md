# Contract: REST / SOQL / Tooling API

All REST endpoints require a valid `Authorization: Bearer <token>` header (any non-empty value is accepted by sf-localstack).

---

## Organization Query

**Endpoint**: `GET /services/data/v60.0/query`
**Query**: `SELECT IsSandbox, OrganizationType, TrialExpirationDate FROM Organization`

### Response shape (must match `organization-response.json`)

```json
{
  "totalSize": 3,
  "done": true,
  "records": [
    {
      "attributes": {
        "type": "Organization",
        "url": "/services/data/v60.0/sobjects/Organization/00D000000000001AAA"
      },
      "IsSandbox": "true",
      "OrganizationType": "abcd",
      "TrialExpirationDate": "some-date"
    }
  ]
}
```

**Seed change required**: Update existing Organization seed record — set `IsSandbox: true`, add `OrganizationType: "abcd"`, set `TrialExpirationDate: "some-date"`.

---

## PDRI__Connection__c Queries

**Endpoint**: `GET /services/data/v60.0/query`

Three query patterns, all return the same fixture (`connections-response.json`):

1. `SELECT PDRI__Instance_URL__c, Id, PDRI__Org_Type__c FROM PDRI__Connection__c WHERE PDRI__OrganizationId__c = '<id>' AND PDRI__Active__c = true`
2. `SELECT PDRI__OrganizationId__c, PDRI__Instance_URL__c, Id, PDRI__Org_Type__c FROM PDRI__Connection__c WHERE PDRI__Local_Connection__c = true AND PDRI__Active__c = true`
3. `SELECT PDRI__OrganizationId__c, PDRI__Instance_URL__c, Id, PDRI__Org_Type__c FROM PDRI__Connection__c WHERE PDRI__Target__c = 'VCS' AND PDRI__Active__c = true AND OwnerId = '<id>' ORDER BY CreatedDate DESC LIMIT 1`

### Response shape (must match `connections-response.json`)

```json
{
  "totalSize": 4,
  "done": true,
  "records": [
    {
      "attributes": {
        "type": "PDRI__Connection__c",
        "url": "/services/data/v60.0/sobjects/PDRI__Connection__c/a0D5e00000Q9h43EAB"
      },
      "PDRI__Instance_URL__c": "<sf-localstack-container-url>",
      "PDRI__OrganizationId__c": "some-id",
      "Id": "a0D5e00000Q9h43EAB",
      "PDRI__Org_Type__c": "Production"
    }
  ]
}
```

**Dynamic field**: `PDRI__Instance_URL__c` must be sf-localstack's own base URL (Testcontainers-mapped host+port). This is injected via `POST /reset` body at test setup time.

---

## PDRI__ComparisonViewRule__c Queries

**Endpoint**: `GET /services/data/v60.0/query`

Two query patterns, both return `comparisonview-response.json`:

1. `SELECT Id, Name, PDRI__Action__c, PDRI__FilterType__c, PDRI__FilterValue__c, PDRI__ComparisonView__c, PDRI__ParentRule__c FROM PDRI__ComparisonViewRule__c WHERE PDRI__ComparisonView__c = '<id>'`
2. Same fields, `WHERE PDRI__ComparisonView__r.PDRI__Default__c = true`

### Response shape (must match `comparisonview-response.json`)

```json
{
  "totalSize": 3,
  "done": true,
  "records": [
    {
      "attributes": {
        "type": "PDRI__ComparisonViewRule__c",
        "url": "/services/data/v60.0/sobjects/PDRI__ComparisonViewRule__c/a1PDn000001K2qAMAS"
      },
      "Id": "a1PDn000001K2qAMAB",
      "Name": "CRN-0000000",
      "PDRI__Action__c": "Include",
      "PDRI__FilterType__c": "Component_Type",
      "PDRI__FilterValue__c": "Workflow",
      "PDRI__ComparisonView__c": "a1QDn000002BPrXMAW",
      "PDRI__ParentRule__c": null
    }
    // ... 4 more records
  ]
}
```

**Field name note**: The fixture uses `PDRI__FilterType__c` (not `PDRI__Filter_Type__c`) and `PDRI__FilterValue__c` (not `PDRI__Filter_Value__c`). The spec's key entity table has underscored versions — use the fixture spellings exactly.

---

## StaticResource Tooling Query

**Endpoint**: `GET /services/data/v66.0/tooling/query/`
**Query**: `SELECT BodyLength FROM StaticResource WHERE Name IN ('<name>')`

### Small resource response (must match `static-resources-small.json`)

```json
{
  "totalSize": 1,
  "done": true,
  "records": [
    {
      "attributes": {
        "type": "StaticResource",
        "url": "/services/data/v66.0/tooling/sobjects/StaticResource/SmallResource"
      },
      "BodyLength": 1
    }
  ]
}
```

### Large resource response (must match `static-resources-big.json`)

```json
{
  "totalSize": 1,
  "done": true,
  "records": [
    {
      "attributes": {
        "type": "StaticResource",
        "url": "/services/data/v66.0/tooling/sobjects/StaticResource/BigResource"
      },
      "BodyLength": 200000000
    }
  ]
}
```

**Routing note**: This endpoint uses the Tooling API path `/tooling/query/`, distinct from the standard SOQL path `/query`. sf-localstack needs a separate route (or path-matching logic in the existing SOQL controller) to handle this.

---

## checkusingvcsrepo (retain as-is)

The `checkusingvcsrepo-response.json` fixture is served by `mockCheckUsingVcsRepo()` which catches ALL GET requests to `/services/data/.*/query.*`. This is a catch-all stub — it should be retired along with the other WireMock stubs. The underlying SOQL query must be identified and the response seeded appropriately in sf-localstack.

**Action required**: Read the test(s) that call `mockCheckUsingVcsRepo()` to determine which SOQL query it intercepts, then seed the appropriate sObject in sf-localstack.
