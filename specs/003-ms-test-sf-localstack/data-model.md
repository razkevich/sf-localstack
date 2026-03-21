# Data Model: Migrate metadata-service Tests to sf-localstack

## Seed Entities (additions to `default-seed.yml`)

### PDRI__Connection__c

Represents a Salesforce org connection used by metadata-service to resolve the target org's base URL and type.

| Field | Type | Seed Value | Notes |
|---|---|---|---|
| `Id` | String (18) | `a0D5e00000Q9h43EAB` | Fixed, matches `connections-response.json` |
| `PDRI__Instance_URL__c` | String | `${SF_LOCALSTACK_BASE_URL}` | Injected at reset time via `POST /reset` body |
| `PDRI__OrganizationId__c` | String | `some-id` | Matches `connections-response.json` |
| `PDRI__Org_Type__c` | String | `Production` | Matches fixture |
| `PDRI__Local_Connection__c` | Boolean | `true` | Required for `mockFindControlConnection` query |
| `PDRI__Active__c` | Boolean | `true` | Required for all connection queries |
| `PDRI__Target__c` | String | `VCS` | Required for `mockFindVcConnection` query |
| `OwnerId` | String | `005000000000001AAA` | References seeded User record |
| `CreatedDate` | String | `2024-01-01T00:00:00.000Z` | Needed for ORDER BY CreatedDate DESC |

**Query patterns served**:
- `SELECT PDRI__Instance_URL__c, Id, PDRI__Org_Type__c FROM PDRI__Connection__c WHERE PDRI__OrganizationId__c = '...' AND PDRI__Active__c = true`
- `SELECT PDRI__OrganizationId__c, PDRI__Instance_URL__c, Id, PDRI__Org_Type__c FROM PDRI__Connection__c WHERE PDRI__Local_Connection__c = true AND PDRI__Active__c = true`
- `SELECT PDRI__OrganizationId__c, PDRI__Instance_URL__c, Id, PDRI__Org_Type__c FROM PDRI__Connection__c WHERE PDRI__Target__c = 'VCS' AND PDRI__Active__c = true AND OwnerId = '...' ORDER BY CreatedDate DESC LIMIT 1`

---

### PDRI__ComparisonView__c

Represents a metadata filter configuration (used as a parent reference by rules).

| Field | Type | Seed Value | Notes |
|---|---|---|---|
| `Id` | String (18) | `a1QDn000002BPrXMAW` | Fixed, referenced by all ComparisonViewRule records |
| `Name` | String | `Default View` | Display name |
| `PDRI__Default__c` | Boolean | `true` | Required for `mockDefaultComparisonViewRules` query |

---

### PDRI__ComparisonViewRule__c

Represents an include/exclude rule within a comparison view. Five records matching `comparisonview-response.json`.

| Id | Name | PDRI__Action__c | PDRI__FilterType__c | PDRI__FilterValue__c | PDRI__ComparisonView__c | PDRI__ParentRule__c |
|---|---|---|---|---|---|---|
| `a1PDn000001K2qAMAB` | `CRN-0000000` | `Include` | `Component_Type` | `Workflow` | `a1QDn000002BPrXMAW` | null |
| `a1PDn000001K2qAMAS` | `CRN-0000000` | `Include` | `Component_Type` | `WorkflowRule` | `a1QDn000002BPrXMAW` | null |
| `a1PDn000001K2qBMAS` | `CRN-0000001` | `Include` | `Component_Type` | `CustomObject` | `a1QDn000002BPrXMAW` | null |
| `a1PDn000001K2qCMAS` | `CRN-0000002` | `Include` | `Component_Type` | `Flow` | `a1QDn000002BPrXMAW` | null |
| `a1PDn000001K2qCMAX` | `CRN-000000X` | `Include` | `Component_Type` | `ApexClass` | `a1QDn000002BPrXMAW` | null |

**Query patterns served**:
- `SELECT Id, Name, PDRI__Action__c, PDRI__FilterType__c, PDRI__FilterValue__c, PDRI__ComparisonView__c, PDRI__ParentRule__c FROM PDRI__ComparisonViewRule__c WHERE PDRI__ComparisonView__c = '...'`
- Same query with `WHERE PDRI__ComparisonView__r.PDRI__Default__c = true`

---

### StaticResource (Tooling API)

Two records to cover the small (<10 MB) and large (>10 MB) code paths.

| Field | Type | Small Record | Large Record |
|---|---|---|---|
| `Name` | String | `SmallResource` | `BigResource` |
| `BodyLength` | Long | `1` | `200000000` |

**Query pattern served** (Tooling API):
- `GET /services/data/v66.0/tooling/query/?q=SELECT+BodyLength+FROM+StaticResource+WHERE+Name+IN+(...)`

---

### Organization (existing, update)

The existing Organization seed record has `IsSandbox: false`. The fixture `organization-response.json` has `IsSandbox: "true"`. The seed must be updated to match.

| Field | Current Seed Value | Required Value |
|---|---|---|
| `Id` | `00D000000000001AAA` | unchanged |
| `IsSandbox` | `false` | `true` |
| `OrganizationType` | not present | `abcd` |
| `TrialExpirationDate` | `null` | `some-date` |

---

## Metadata Catalog Entries (sf-localstack seed)

The existing `MetadataCatalogEntry` model stores entries for `describeMetadata` and `listMetadata` responses. The seeded entries must include `ApexClass`, `EmailTemplate`, `CustomObject`, and `WorkFlow` to match the fixtures.

Current sf-localstack metadata catalog state needs verification — if types are stored in H2 they may already be seeded via the default YAML. The `suffix` field gap in `renderDescribeMetadata` must be filled.

---

## Reset Endpoint Enhancement

`POST /reset` must accept an optional JSON body to override specific seed field values at reset time:

```json
{
  "seedOverrides": {
    "PDRI__Connection__c": {
      "PDRI__Instance_URL__c": "http://host.docker.internal:XXXX"
    }
  }
}
```

This allows the Testcontainers base class to inject the container's dynamic URL without requiring environment variable support in the YAML seed parser.
