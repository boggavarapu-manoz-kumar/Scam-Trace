# Digital Risk Protection Platform — Backend

Spring Boot 3.5.x backend service for the Digital Risk Protection Platform (Kampus.VC).

## Requirements

- **Java Development Kit (JDK):** Java 21 or higher (release target: Java 21)
- **Maven:** Bundled Maven Wrapper (`./mvnw.cmd` on Windows, `./mvnw` on Unix)
- **Database:** PostgreSQL 15+ (production / local dev) or in-memory H2 (unit test profile)

## Architecture & Database Migration

- **Security & Multi-Tenancy:** Stateless JWT token authentication with HMAC-SHA256, BCrypt password hashing (work factor 12), and `@tenantSecurity` organization-scoped RBAC (`OWNER`, `ADMIN`, `ANALYST`, `VIEWER`).
- **ORM & Persistence:** Spring Data JPA with Hibernate.
- **Schema Ownership:** Schema migrations are strictly managed via **Flyway** (`db/migration/V...sql`).
  - `V1__baseline_schema.sql`: Initial baseline tables.
  - `V2__create_identity_and_tenant_tables.sql`: Users, organizations, memberships, roles, and security audit logs.
- **Hibernate DDL Policy:** `spring.jpa.hibernate.ddl-auto=validate`. Hibernate never mutates, alters, or drops schema automatically.

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/kampusvc/digitalrisk/
│   │   │   ├── DigitalRiskPlatformApplication.java
│   │   │   ├── auth/                 # Authentication endpoints, DTOs & AuthService
│   │   │   ├── common/api/           # ApiResponse, ApiError envelopes
│   │   │   ├── common/exception/     # GlobalExceptionHandler (@RestControllerAdvice)
│   │   │   ├── config/               # WebConfig (CORS)
│   │   │   ├── health/api/           # /api/v1/health application status endpoint
│   │   │   ├── identity/             # Users, Organizations, Memberships, Audit Logs
│   │   │   └── security/             # SecurityConfig, JwtTokenProvider, TenantSecurityService
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │           ├── V1__baseline_schema.sql
│   │           └── V2__create_identity_and_tenant_tables.sql
│   └── test/
│       ├── java/com/kampusvc/digitalrisk/
│       │   ├── auth/AuthIntegrationTest.java
│       │   ├── common/exception/GlobalExceptionHandlerTest.java
│       │   ├── config/DatabaseMigrationTest.java
│       │   ├── config/WebConfigCorsTest.java
│       │   ├── health/api/AppHealthControllerTest.java
│       │   ├── identity/IdentityPersistenceTest.java
│       │   └── security/MultiTenantAuthorizationTest.java
│       └── resources/
│           └── application-test.properties  # Isolated test configuration
├── .env.example                      # Template for environment variables
├── .gitignore                        # Java/Maven ignore rules
├── mvnw / mvnw.cmd                   # Cross-platform Maven Wrapper executables
└── pom.xml                           # Project dependencies
```

## Running & Testing

```powershell
# Compile and package
.\mvnw.cmd package -DskipTests

# Run full test regression suite (36 tests)
.\mvnw.cmd test

# Run application
.\mvnw.cmd spring-boot:run
```
