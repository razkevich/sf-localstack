# Data Model: Multi-Tenant Org Isolation

## New Entity: Org

Represents an isolated Salesforce org instance.

| Field | Type | Description |
|-------|------|-------------|
| orgId | String (18 chars) | Primary key, `00D`-prefixed Salesforce org ID |
| ownerUserId | String (UUID) | The user who owns this org |
| name | String | Display name (defaults to username's org) |
| createdAt | Instant | When the org was created |

## Modified Entities

### User (add orgId)

| Field | Change | Description |
|-------|--------|-------------|
| orgId | ADD | Foreign key to the user's org |

### SObjectRecord (add orgId)

| Field | Change | Description |
|-------|--------|-------------|
| orgId | ADD, indexed | Org that owns this record |

All repository queries change from `findByObjectType(type)` to `findByOrgIdAndObjectType(orgId, type)`.

### BulkIngestJob (add orgId)

| Field | Change | Description |
|-------|--------|-------------|
| orgId | ADD, indexed | Org that owns this job |

### MetadataResourceEntity (add orgId)

| Field | Change | Description |
|-------|--------|-------------|
| orgId | ADD | Org that owns this metadata component |

Unique constraint changes from `(type, fullName)` to `(orgId, type, fullName)`.

### MetadataDeployJobEntity (add orgId)

| Field | Change | Description |
|-------|--------|-------------|
| orgId | ADD | Org that owns this deploy job |

### MetadataRetrieveJobEntity (add orgId)

| Field | Change | Description |
|-------|--------|-------------|
| orgId | ADD | Org that owns this retrieve job |

## Data Flow

```
User registers → OrgService creates Org → orgId stored on User
                                        → OrgStateService seeds default data for orgId
User logs in   → JWT contains orgId claim
API request    → JwtAuthFilter extracts orgId → sets request attribute
               → Controller reads orgId from request → passes to service
               → Service/Repository filters by orgId
```
