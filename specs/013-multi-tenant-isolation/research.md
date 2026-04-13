# Research: Multi-Tenant Org Isolation

## Isolation Strategy

**Decision**: Column-level isolation — add `orgId` to all JPA entities and filter all queries.

**Rationale**: Simplest approach for H2/single-DB MVP. No need for separate databases or schemas per tenant. All existing repository queries get a `findByOrgId*` variant.

**Alternatives considered**:
- Separate H2 database file per org: Complex connection management, harder to query across orgs for admin views
- Schema-per-tenant: H2 supports schemas but Spring Data JPA doesn't switch schemas per request easily
- Hibernate multi-tenancy with discriminator: Adds framework complexity, same result as manual column filtering

## Org ID Format

**Decision**: Generate `00D` + 15-character alphanumeric ID (18-char Salesforce format). Use UUID and encode to base62 for compactness.

**Rationale**: Matches Salesforce org ID format. SF CLI expects org IDs starting with `00D`.

## Backward Compatibility

**Decision**: On first startup after migration, assign all existing data to legacy org `00D000000000001AAA`. The first registered user (ADMIN) gets this org. H2 `ALTER TABLE ADD COLUMN` with default value handles the migration automatically via Hibernate `ddl-auto: update`.

**Rationale**: Zero data loss on upgrade. Existing SF CLI aliases continue to work.

## Org-per-User vs Multi-User-per-Org

**Decision**: MVP is 1 user = 1 org. Each registration creates a new org.

**Rationale**: Simplest model for SaaS MVP. Multi-user orgs (teams) can be added later by making org association configurable.
