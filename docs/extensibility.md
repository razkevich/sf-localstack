# Extensibility Guide

## Storage Layer

### Architecture

sf_localstack uses Spring Data JPA repositories for all persistent storage. Each domain (sObjects, Bulk jobs, Metadata) has its own repository interfaces. The backing database is swappable via Spring profiles without code changes.

### MVP Implementation (H2 File-Based)

The `dev` profile uses H2 in file mode:

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:file:./data/sfdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
```

Data is stored in `./data/sfdb.mv.db`. Delete this file to reset to a clean state.

### Migrating to PostgreSQL

**Step 1**: Add the PostgreSQL driver to `service/pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Step 2**: Configure `application-prod.yml` (already provided as a template):

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/sfdb}
    driver-class-name: org.postgresql.Driver
    username: ${DATABASE_USERNAME:sfadmin}
    password: ${DATABASE_PASSWORD:changeme}
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
```

**Step 3**: Start with the prod profile:

```bash
java -jar sf-localstack.jar --spring.profiles.active=prod
```

Or via environment variable:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar sf-localstack.jar
```

That's it — no code changes required. JPA and Hibernate handle the dialect differences.

### Schema Management

| Profile | DDL Strategy | Use Case |
|---------|-------------|----------|
| `test` | `create-drop` | Automated tests — clean schema per run |
| `dev` | `update` | Local development — schema auto-migrates |
| `prod` | `update` | Production — auto-creates tables on first run |

For production deployments with strict schema control, replace `ddl-auto: update` with `ddl-auto: validate` and use Flyway or Liquibase for migrations:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Then add migration scripts in `src/main/resources/db/migration/`.

### Repository Interfaces

| Repository | Entity | Domain |
|-----------|--------|--------|
| `SObjectRecordRepository` | `SObjectRecord` | sObject CRUD |
| `BulkIngestJobRepository` | `BulkIngestJob` | Bulk API jobs |
| `BulkBatchRepository` | `BulkBatchEntity` | Bulk CSV batches |
| `BulkRowResultRepository` | `BulkRowResultEntity` | Bulk row results |
| `MetadataResourceRepository` | `MetadataResourceEntity` | Metadata catalog |
| `MetadataDeployJobRepository` | `MetadataDeployJobEntity` | Deploy jobs |
| `MetadataRetrieveJobRepository` | `MetadataRetrieveJobEntity` | Retrieve jobs |

All repositories extend `JpaRepository` and work with any JPA-compatible database.

## Authentication (Future)

The authentication system will use a `UserStore` interface:

```java
public interface UserStore {
    Optional<User> findByUsername(String username);
    User createUser(String username, String password, Role role);
    boolean validateCredentials(String username, String password);
}
```

**MVP**: `FileBasedUserStore` — reads/writes `data/users.json`, passwords hashed with bcrypt.

**Production options**:
- `JpaUserStore` — persists users to the database via JPA
- `OAuth2UserStore` — delegates to Auth0, Keycloak, or Cognito
- Custom implementation of the `UserStore` interface

Swap by implementing the interface and activating it via a Spring `@Profile` or `@ConditionalOnProperty`.

## Multi-Tenancy (Future)

Not implemented in MVP. Documented path:

1. Add `tenantId` column to all tables
2. JWT token includes tenant ID
3. Repository queries filter by tenant
4. Option: separate schema per tenant (PostgreSQL schema support)

## Monitoring (Future)

- Spring Boot Actuator endpoints (already included): `/actuator/health`, `/actuator/info`
- Add Prometheus metrics: `spring-boot-starter-actuator` + `micrometer-registry-prometheus`
- Add structured logging: Logback JSON encoder for ELK/Datadog
