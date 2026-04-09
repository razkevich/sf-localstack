# Research: 005-persistent-storage

## H2 File-Based vs In-Memory

**Decision**: Switch from `jdbc:h2:mem:sfdb` to `jdbc:h2:file:./data/sfdb` for dev profile
**Rationale**: Minimal change, zero new dependencies. H2 file mode uses the same dialect and SQL. Just a URL change.
**Alternatives**: SQLite (rejected — no JPA dialect), PostgreSQL (future, not MVP)

## DDL Strategy

**Decision**: `ddl-auto: update` for dev, `create-drop` for test
**Rationale**: `update` preserves data across restarts while auto-creating new tables/columns. `create-drop` keeps tests isolated.
**Alternatives**: Flyway migrations (out of scope for MVP — documented for future)

## Spring Profiles

**Decision**: Three profiles: `test` (H2 mem), `dev` (H2 file, default), `prod` (PostgreSQL placeholder)
**Rationale**: Follows Spring Boot conventions. Test profile ensures existing tests pass without changes.
**Alternatives**: Single profile with env vars (rejected — less explicit, harder to document)

## Bulk Job Entity Design

**Decision**: Three entities: BulkIngestJobEntity (parent), BulkBatchEntity (CSV data), BulkRowResultEntity (per-row results)
**Rationale**: Normalizes the current in-memory model. Batches are large text (CSV), stored as @Lob. Results need individual rows for queryability.
**Alternatives**: Single entity with JSON columns for batches/results (rejected — harder to query, H2 JSON support limited)

## Metadata Entity Design

**Decision**: Three entities: MetadataResourceEntity, MetadataDeployJobEntity, MetadataRetrieveJobEntity
**Rationale**: Maps 1:1 with the current in-memory records. MetadataResource uses composite unique key (type + fullName).
**Alternatives**: Store attributes as JSON column (chosen — attributes is a Map<String, Object>, JSON is simplest)

## Reset Strategy

**Decision**: Use `@Transactional` with `deleteAll()` on each repository, respecting FK order (children before parents)
**Rationale**: Simple, uses existing JPA. Truncate via native query would be faster but adds complexity.
**Alternatives**: Native SQL TRUNCATE with FK disable (rejected for MVP — deleteAll() is fast enough for dev/test scale)

## Data Directory

**Decision**: `./data/` directory, gitignored, auto-created by H2 on first connection
**Rationale**: H2 auto-creates the database file. Spring Boot doesn't need explicit directory creation.
**Alternatives**: Configurable path via property (future enhancement, not needed for MVP)

## Record Model Approach

**Decision**: Convert BulkIngestJob (mutable POJO) to JPA entity. Keep existing record types (BulkRowResult) as embedded or entity.
**Rationale**: BulkIngestJob has mutable state (state changes, results populated). JPA entity with setters is natural.
**Alternatives**: Keep in-memory with periodic flush (rejected — defeats purpose of persistence)
