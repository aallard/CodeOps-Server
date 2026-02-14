# CodeOps-Server — Comprehensive Codebase Audit

| Field | Value |
|-------|-------|
| **Project** | CodeOps-Server |
| **Audit Date** | 2026-02-14 |
| **Branch** | main |
| **Commit** | `2363205e3a19e27ee68b160ba959b27cb273865a` |
| **Quality Grade** | **A** (85/92 = 92.4%) |
| **Auditor** | Claude Code |

---

## Section 1: Project Identity

| Field | Value |
|-------|-------|
| **Project Name** | CodeOps Server |
| **Repository URL** | https://github.com/aallard/CodeOps-Server.git |
| **Primary Language / Framework** | Java / Spring Boot 3.3.0 |
| **Java Version** | 21 (source/target), running on OpenJDK 25.0.2 (Homebrew) |
| **Build Tool + Version** | Apache Maven 3.9.12 (system-installed, no Maven wrapper) |
| **Current Branch** | main |
| **Latest Commit Hash** | `2363205e3a19e27ee68b160ba959b27cb273865a` |
| **Latest Commit Message** | Add comprehensive test suite — 777 tests, 97% line coverage |
| **Audit Timestamp** | 2026-02-14T15:05:01Z |

**Notes:**
- The pom.xml declares `<java.version>21</java.version>` for source/target compilation, but the runtime JDK is OpenJDK 25.0.2 installed via Homebrew at `/opt/homebrew/Cellar/openjdk/25.0.2/`.
- `JAVA_HOME` is not set; Maven auto-resolves via system PATH.
- There is no Maven wrapper (`mvnw` / `.mvn/`) in this repository. The project relies on a system-installed Maven.
- Compatibility overrides for Java 25 are present: Lombok 1.18.42, Mockito 5.21.0, ByteBuddy 1.18.4.

---

## Section 2: Directory Structure

### Full File Tree

```
.
├── .gitignore
├── claude.md
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md
├── docs/
│   ├── CodeOps-Server-Audit.md
│   ├── Old-CodeOps-Server-Audit.md
│   ├── openapi.json
│   └── openapi.yaml
└── src/
    ├── main/
    │   ├── java/com/codeops/
    │   │   ├── CodeOpsApplication.java
    │   │   ├── config/
    │   │   │   ├── AppConstants.java
    │   │   │   ├── AsyncConfig.java
    │   │   │   ├── CorsConfig.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── HealthController.java
    │   │   │   ├── JwtProperties.java
    │   │   │   ├── RestTemplateConfig.java
    │   │   │   ├── S3Config.java
    │   │   │   └── SesConfig.java
    │   │   ├── controller/
    │   │   │   ├── AdminController.java
    │   │   │   ├── AuthController.java
    │   │   │   ├── ComplianceController.java
    │   │   │   ├── DependencyController.java
    │   │   │   ├── DirectiveController.java
    │   │   │   ├── FindingController.java
    │   │   │   ├── HealthMonitorController.java
    │   │   │   ├── IntegrationController.java
    │   │   │   ├── JobController.java
    │   │   │   ├── MetricsController.java
    │   │   │   ├── PersonaController.java
    │   │   │   ├── ProjectController.java
    │   │   │   ├── ReportController.java
    │   │   │   ├── TaskController.java
    │   │   │   ├── TeamController.java
    │   │   │   ├── TechDebtController.java
    │   │   │   └── UserController.java
    │   │   ├── dto/
    │   │   │   ├── request/  (37 Java record files)
    │   │   │   │   ├── AdminUpdateUserRequest.java
    │   │   │   │   ├── AssignDirectiveRequest.java
    │   │   │   │   ├── BulkUpdateFindingsRequest.java
    │   │   │   │   ├── ChangePasswordRequest.java
    │   │   │   │   ├── CreateAgentRunRequest.java
    │   │   │   │   ├── CreateBugInvestigationRequest.java
    │   │   │   │   ├── CreateComplianceItemRequest.java
    │   │   │   │   ├── CreateDependencyScanRequest.java
    │   │   │   │   ├── CreateDirectiveRequest.java
    │   │   │   │   ├── CreateFindingRequest.java
    │   │   │   │   ├── CreateGitHubConnectionRequest.java
    │   │   │   │   ├── CreateHealthScheduleRequest.java
    │   │   │   │   ├── CreateHealthSnapshotRequest.java
    │   │   │   │   ├── CreateJiraConnectionRequest.java
    │   │   │   │   ├── CreateJobRequest.java
    │   │   │   │   ├── CreatePersonaRequest.java
    │   │   │   │   ├── CreateProjectRequest.java
    │   │   │   │   ├── CreateSpecificationRequest.java
    │   │   │   │   ├── CreateTaskRequest.java
    │   │   │   │   ├── CreateTeamRequest.java
    │   │   │   │   ├── CreateTechDebtItemRequest.java
    │   │   │   │   ├── CreateVulnerabilityRequest.java
    │   │   │   │   ├── InviteMemberRequest.java
    │   │   │   │   ├── LoginRequest.java
    │   │   │   │   ├── PasswordResetRequest.java
    │   │   │   │   ├── RefreshTokenRequest.java
    │   │   │   │   ├── RegisterRequest.java
    │   │   │   │   ├── UpdateAgentRunRequest.java
    │   │   │   │   ├── UpdateBugInvestigationRequest.java
    │   │   │   │   ├── UpdateDirectiveRequest.java
    │   │   │   │   ├── UpdateFindingStatusRequest.java
    │   │   │   │   ├── UpdateJobRequest.java
    │   │   │   │   ├── UpdateMemberRoleRequest.java
    │   │   │   │   ├── UpdateNotificationPreferenceRequest.java
    │   │   │   │   ├── UpdatePersonaRequest.java
    │   │   │   │   ├── UpdateProjectRequest.java
    │   │   │   │   ├── UpdateSystemSettingRequest.java
    │   │   │   │   ├── UpdateTaskRequest.java
    │   │   │   │   ├── UpdateTeamRequest.java
    │   │   │   │   ├── UpdateTechDebtStatusRequest.java
    │   │   │   │   └── UpdateUserRequest.java
    │   │   │   └── response/  (31 Java record files)
    │   │   │       ├── AgentRunResponse.java
    │   │   │       ├── AuditLogResponse.java
    │   │   │       ├── AuthResponse.java
    │   │   │       ├── BugInvestigationResponse.java
    │   │   │       ├── ComplianceItemResponse.java
    │   │   │       ├── DependencyScanResponse.java
    │   │   │       ├── DirectiveResponse.java
    │   │   │       ├── ErrorResponse.java
    │   │   │       ├── FindingResponse.java
    │   │   │       ├── GitHubConnectionResponse.java
    │   │   │       ├── HealthScheduleResponse.java
    │   │   │       ├── HealthSnapshotResponse.java
    │   │   │       ├── InvitationResponse.java
    │   │   │       ├── JiraConnectionResponse.java
    │   │   │       ├── JobResponse.java
    │   │   │       ├── JobSummaryResponse.java
    │   │   │       ├── NotificationPreferenceResponse.java
    │   │   │       ├── PageResponse.java
    │   │   │       ├── PersonaResponse.java
    │   │   │       ├── ProjectDirectiveResponse.java
    │   │   │       ├── ProjectMetricsResponse.java
    │   │   │       ├── ProjectResponse.java
    │   │   │       ├── SpecificationResponse.java
    │   │   │       ├── SystemSettingResponse.java
    │   │   │       ├── TaskResponse.java
    │   │   │       ├── TeamMemberResponse.java
    │   │   │       ├── TeamMetricsResponse.java
    │   │   │       ├── TeamResponse.java
    │   │   │       ├── TechDebtItemResponse.java
    │   │   │       ├── UserResponse.java
    │   │   │       └── VulnerabilityResponse.java
    │   │   ├── entity/
    │   │   │   ├── AgentRun.java
    │   │   │   ├── AuditLog.java
    │   │   │   ├── BaseEntity.java
    │   │   │   ├── BugInvestigation.java
    │   │   │   ├── ComplianceItem.java
    │   │   │   ├── DependencyScan.java
    │   │   │   ├── DependencyVulnerability.java
    │   │   │   ├── Directive.java
    │   │   │   ├── enums/
    │   │   │   │   ├── AgentResult.java
    │   │   │   │   ├── AgentStatus.java
    │   │   │   │   ├── AgentType.java
    │   │   │   │   ├── BusinessImpact.java
    │   │   │   │   ├── ComplianceStatus.java
    │   │   │   │   ├── DebtCategory.java
    │   │   │   │   ├── DebtStatus.java
    │   │   │   │   ├── DirectiveCategory.java
    │   │   │   │   ├── DirectiveScope.java
    │   │   │   │   ├── Effort.java
    │   │   │   │   ├── FindingStatus.java
    │   │   │   │   ├── GitHubAuthType.java
    │   │   │   │   ├── InvitationStatus.java
    │   │   │   │   ├── JobMode.java
    │   │   │   │   ├── JobResult.java
    │   │   │   │   ├── JobStatus.java
    │   │   │   │   ├── Priority.java
    │   │   │   │   ├── ScheduleType.java
    │   │   │   │   ├── Scope.java
    │   │   │   │   ├── Severity.java
    │   │   │   │   ├── SpecType.java
    │   │   │   │   ├── TaskStatus.java
    │   │   │   │   ├── TeamRole.java
    │   │   │   │   └── VulnerabilityStatus.java
    │   │   │   ├── Finding.java
    │   │   │   ├── GitHubConnection.java
    │   │   │   ├── HealthSchedule.java
    │   │   │   ├── HealthSnapshot.java
    │   │   │   ├── Invitation.java
    │   │   │   ├── JiraConnection.java
    │   │   │   ├── NotificationPreference.java
    │   │   │   ├── Persona.java
    │   │   │   ├── Project.java
    │   │   │   ├── ProjectDirective.java
    │   │   │   ├── ProjectDirectiveId.java
    │   │   │   ├── QaJob.java
    │   │   │   ├── RemediationTask.java
    │   │   │   ├── Specification.java
    │   │   │   ├── SystemSetting.java
    │   │   │   ├── Team.java
    │   │   │   ├── TeamMember.java
    │   │   │   ├── TechDebtItem.java
    │   │   │   └── User.java
    │   │   ├── exception/
    │   │   │   ├── AuthorizationException.java
    │   │   │   ├── CodeOpsException.java
    │   │   │   ├── NotFoundException.java
    │   │   │   └── ValidationException.java
    │   │   ├── notification/
    │   │   │   ├── EmailService.java
    │   │   │   ├── NotificationDispatcher.java
    │   │   │   └── TeamsWebhookService.java
    │   │   ├── repository/  (25 repository interfaces)
    │   │   │   ├── AgentRunRepository.java
    │   │   │   ├── AuditLogRepository.java
    │   │   │   ├── BugInvestigationRepository.java
    │   │   │   ├── ComplianceItemRepository.java
    │   │   │   ├── DependencyScanRepository.java
    │   │   │   ├── DependencyVulnerabilityRepository.java
    │   │   │   ├── DirectiveRepository.java
    │   │   │   ├── FindingRepository.java
    │   │   │   ├── GitHubConnectionRepository.java
    │   │   │   ├── HealthScheduleRepository.java
    │   │   │   ├── HealthSnapshotRepository.java
    │   │   │   ├── InvitationRepository.java
    │   │   │   ├── JiraConnectionRepository.java
    │   │   │   ├── NotificationPreferenceRepository.java
    │   │   │   ├── PersonaRepository.java
    │   │   │   ├── ProjectDirectiveRepository.java
    │   │   │   ├── ProjectRepository.java
    │   │   │   ├── QaJobRepository.java
    │   │   │   ├── RemediationTaskRepository.java
    │   │   │   ├── SpecificationRepository.java
    │   │   │   ├── SystemSettingRepository.java
    │   │   │   ├── TeamMemberRepository.java
    │   │   │   ├── TeamRepository.java
    │   │   │   ├── TechDebtItemRepository.java
    │   │   │   └── UserRepository.java
    │   │   ├── security/
    │   │   │   ├── JwtAuthFilter.java
    │   │   │   ├── JwtTokenProvider.java
    │   │   │   ├── RateLimitFilter.java
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── SecurityUtils.java
    │   │   └── service/  (24 service classes)
    │   │       ├── AdminService.java
    │   │       ├── AgentRunService.java
    │   │       ├── AuditLogService.java
    │   │       ├── AuthService.java
    │   │       ├── BugInvestigationService.java
    │   │       ├── ComplianceService.java
    │   │       ├── DependencyService.java
    │   │       ├── DirectiveService.java
    │   │       ├── EncryptionService.java
    │   │       ├── FindingService.java
    │   │       ├── GitHubConnectionService.java
    │   │       ├── HealthMonitorService.java
    │   │       ├── JiraConnectionService.java
    │   │       ├── MetricsService.java
    │   │       ├── NotificationService.java
    │   │       ├── PersonaService.java
    │   │       ├── ProjectService.java
    │   │       ├── QaJobService.java
    │   │       ├── RemediationTaskService.java
    │   │       ├── ReportStorageService.java
    │   │       ├── S3StorageService.java
    │   │       ├── TeamService.java
    │   │       ├── TechDebtService.java
    │   │       ├── TokenBlacklistService.java
    │   │       └── UserService.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       └── application-prod.yml
    └── test/
        ├── java/com/codeops/
        │   ├── config/  (8 test classes)
        │   ├── controller/  (17 test classes)
        │   ├── notification/  (3 test classes)
        │   ├── security/  (4 test classes)
        │   └── service/  (24 test classes)
        └── resources/
            └── application-test.yml
```

### Narrative Summary

CodeOps-Server is a **single-module Spring Boot application** organized in a classic **layered architecture** (not domain-driven). The base package is `com.codeops` and contains six primary sub-packages:

1. **`config/`** (9 files) -- Spring configuration classes covering CORS, async thread pools, JWT property binding, AWS S3/SES client beans, RestTemplate, global exception handling, application constants, and a health check controller.

2. **`controller/`** (17 files) -- REST controllers exposing approximately 140 endpoints under the `/api/v1/` prefix. Each controller corresponds to a domain area: Auth, Admin, Team, Project, Job, Task, Finding, Directive, Persona, Compliance, Dependency, HealthMonitor, Integration, Metrics, Report, TechDebt, and User.

3. **`dto/`** -- Split into `request/` (37 Java records with Jakarta Validation annotations) and `response/` (31 Java records). DTOs are pure data carriers with no business logic.

4. **`entity/`** (25 JPA entities + 23 enums) -- Domain model classes. Most extend `BaseEntity` (UUID primary key, `createdAt`, `updatedAt`). Three exceptions: `SystemSetting` (String key PK), `AuditLog` (Long auto-increment PK), and `ProjectDirective` (composite PK). Enums stored as `@Enumerated(EnumType.STRING)`.

5. **`service/`** (24 classes) -- Business logic layer. Services use `@RequiredArgsConstructor` for constructor injection. The `AuditLogService` uses `@Async` for fire-and-forget logging. `TokenBlacklistService` uses an in-memory `ConcurrentHashMap` (not persisted).

6. **`security/`** (5 files) -- Stateless JWT authentication with `JwtAuthFilter` (OncePerRequestFilter), `JwtTokenProvider` (HS256 signing/validation), `RateLimitFilter` (in-memory per-IP rate limiting for `/api/v1/auth/` endpoints), `SecurityConfig` (filter chain, BCrypt encoder), and `SecurityUtils` (static helper for current user).

7. **`notification/`** (3 files) -- Email via AWS SES, Microsoft Teams via webhook, and a `NotificationDispatcher` that routes notifications.

8. **`exception/`** (4 files) -- Custom exception hierarchy: `CodeOpsException` (base), `NotFoundException`, `ValidationException`, `AuthorizationException`. All mapped to HTTP status codes by `GlobalExceptionHandler`.

The **test tree** mirrors the main source structure with 56 test classes (777 tests total per latest commit).

Supporting files include a `docs/` directory with OpenAPI specs (JSON + YAML), a Dockerfile, docker-compose.yml (PostgreSQL only), and a `.gitignore`. There is no Maven wrapper, no CI/CD pipeline configuration, and no database migration scripts (Flyway/Liquibase).

---

## Section 3: Build & Dependency Manifest

### Parent POM

| Field | Value |
|-------|-------|
| Parent | `org.springframework.boot:spring-boot-starter-parent:3.3.0` |
| GroupId | `com.codeops` |
| ArtifactId | `codeops-server` |
| Version | `0.1.0-SNAPSHOT` |

### Properties

| Property | Value | Purpose |
|----------|-------|---------|
| `java.version` | `21` | Compilation source/target level |
| `jjwt.version` | `0.12.6` | JJWT library version |
| `mapstruct.version` | `1.5.5.Final` | MapStruct version |
| `lombok.version` | `1.18.42` | Lombok override (Java 25 compat -- 1.18.34 default crashes with `TypeTag :: UNKNOWN`) |
| `mockito.version` | `5.21.0` | Mockito override (Java 25 compat -- default 5.11.0 cannot mock) |
| `byte-buddy.version` | `1.18.4` | ByteBuddy override (Java 25 compat -- default 1.14.x insufficient) |

### Dependencies

| # | GroupId:ArtifactId | Version | Scope | Purpose |
|---|-------------------|---------|-------|---------|
| 1 | `org.springframework.boot:spring-boot-starter-web` | (managed: 3.3.0) | compile | Embedded Tomcat, Spring MVC, REST support |
| 2 | `org.springframework.boot:spring-boot-starter-data-jpa` | (managed: 3.3.0) | compile | Hibernate ORM, Spring Data JPA repositories |
| 3 | `org.springframework.boot:spring-boot-starter-security` | (managed: 3.3.0) | compile | Spring Security framework, filter chain |
| 4 | `org.springframework.boot:spring-boot-starter-validation` | (managed: 3.3.0) | compile | Jakarta Bean Validation (Hibernate Validator) |
| 5 | `org.postgresql:postgresql` | (managed) | runtime | PostgreSQL JDBC driver |
| 6 | `io.jsonwebtoken:jjwt-api` | 0.12.6 | compile | JWT API interfaces |
| 7 | `io.jsonwebtoken:jjwt-impl` | 0.12.6 | runtime | JWT implementation (signing, parsing) |
| 8 | `io.jsonwebtoken:jjwt-jackson` | 0.12.6 | runtime | JWT Jackson serialization support |
| 9 | `software.amazon.awssdk:s3` | 2.25.0 | compile | AWS S3 client for report/file storage |
| 10 | `software.amazon.awssdk:ses` | 2.25.0 | compile | AWS SES client for transactional email |
| 11 | `org.projectlombok:lombok` | 1.18.42 | provided | Compile-time annotation processor (getters, setters, constructors) |
| 12 | `org.mapstruct:mapstruct` | 1.5.5.Final | compile | Object mapping framework (declared but no @Mapper interfaces found in source) |
| 13 | `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | (managed) | compile | Java 8+ date/time serialization for Jackson |
| 14 | `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 2.5.0 | compile | Swagger UI + OpenAPI 3 documentation |
| 15 | `org.springframework.boot:spring-boot-starter-test` | (managed: 3.3.0) | test | JUnit 5, Mockito, AssertJ, MockMvc |
| 16 | `org.springframework.security:spring-security-test` | (managed) | test | Security test utilities (@WithMockUser, etc.) |
| 17 | `org.testcontainers:postgresql` | 1.19.8 | test | Testcontainers PostgreSQL module (declared, not currently used -- tests use H2) |
| 18 | `org.testcontainers:junit-jupiter` | 1.19.8 | test | Testcontainers JUnit 5 integration |
| 19 | `com.h2database:h2` | (managed) | test | H2 in-memory database for unit tests |

### Build Plugins

| # | Plugin | Version | Configuration |
|---|--------|---------|---------------|
| 1 | `spring-boot-maven-plugin` | (managed: 3.3.0) | Excludes Lombok from final JAR. Builds executable fat JAR. |
| 2 | `maven-compiler-plugin` | (managed) | Source/target = 21. Explicit `<annotationProcessorPaths>` for Lombok 1.18.42 and MapStruct 1.5.5.Final (required for Java 22+ which disables auto-discovery). |
| 3 | `maven-surefire-plugin` | (managed) | `--add-opens` for `java.base/java.lang`, `java.base/java.lang.reflect`, `java.base/java.util` (all to `ALL-UNNAMED`). Required for Mockito reflective access on Java 25. Uses `@{argLine}` placeholder for JaCoCo agent integration. |
| 4 | `jacoco-maven-plugin` | 0.8.14 | `prepare-agent` goal (instruments classes), `report` goal (generates coverage report during test phase). |

### Build Commands

```bash
# Compile only (no tests)
mvn clean compile -DskipTests

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=AuthServiceTest

# Package (build JAR, skip tests)
mvn clean package -DskipTests

# Run application
mvn spring-boot:run

# Generate coverage report (runs tests first)
mvn test    # JaCoCo report at target/site/jacoco/index.html
```

**Note:** There is no Maven wrapper (`mvnw`). The project requires Maven 3.9.x installed on the system.

---

## Section 4: Configuration Files

### 4.1 application.yml (Main)

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: codeops-server
  profiles:
    active: dev

server:
  port: 8090
```

**Annotations:**
- **`server.port: 8090`** -- Hardcoded. The server binds to port 8090 (not the Spring Boot default 8080).
- **`spring.profiles.active: dev`** -- Hardcoded default profile. The `application-dev.yml` profile is activated by default.
- This file contains only the minimal bootstrap configuration. All substantive config is in the profile-specific files.

---

### 4.2 application-dev.yml (Development Profile)

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/codeops
    username: ${DB_USERNAME:codeops}
    password: ${DB_PASSWORD:codeops}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    open-in-view: false

  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null

# JWT
codeops:
  jwt:
    secret: ${JWT_SECRET:dev-secret-key-minimum-32-characters-long-for-hs256}
    expiration-hours: 24
    refresh-expiration-days: 30

  encryption:
    key: dev-only-encryption-key-minimum-32ch

  cors:
    allowed-origins: http://localhost:3000,http://localhost:5173

  # AWS (local dev — use localstack or disable)
  aws:
    s3:
      enabled: false
      bucket: codeops-dev
      region: us-east-1
    ses:
      enabled: false
      from-email: noreply@codeops.dev
      region: us-east-1

  local-storage:
    path: ${user.home}/.codeops/storage

logging:
  level:
    com.codeops: DEBUG
    org.hibernate.SQL: DEBUG
```

**Annotations:**

| Property | Type | Notes |
|----------|------|-------|
| `datasource.url` | Hardcoded | Points to `localhost:5432/codeops` -- requires local/Docker PostgreSQL |
| `datasource.username` | Env var with default | `${DB_USERNAME:codeops}` -- defaults to `codeops` |
| `datasource.password` | Env var with default | `${DB_PASSWORD:codeops}` -- defaults to `codeops` |
| `jpa.hibernate.ddl-auto` | Hardcoded | `update` -- Hibernate auto-creates/alters tables. No migration tool. |
| `jpa.show-sql` | Hardcoded | `true` -- SQL logged to console in dev |
| `jpa.open-in-view` | Hardcoded | `false` -- no lazy-loading in view layer (good practice) |
| `codeops.jwt.secret` | Env var with default | `${JWT_SECRET:dev-secret-key-...}` -- WARNING: hardcoded dev secret in file |
| `codeops.encryption.key` | **Hardcoded** | `dev-only-encryption-key-minimum-32ch` -- no env var fallback, plaintext in source |
| `codeops.cors.allowed-origins` | Hardcoded | `localhost:3000` (frontend) and `localhost:5173` (Vite dev server) |
| `codeops.aws.s3.enabled` | Hardcoded | `false` -- S3 disabled, local filesystem used |
| `codeops.aws.ses.enabled` | Hardcoded | `false` -- SES disabled, emails logged to console |
| `codeops.local-storage.path` | Env var | `${user.home}/.codeops/storage` |
| `logging.level` | Hardcoded | `com.codeops: DEBUG`, `org.hibernate.SQL: DEBUG` |

---

### 4.3 application-prod.yml (Production Profile)

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false

codeops:
  jwt:
    secret: ${JWT_SECRET}
    expiration-hours: 24
    refresh-expiration-days: 30
  encryption:
    key: ${ENCRYPTION_KEY}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}
  aws:
    s3:
      enabled: true
      bucket: ${S3_BUCKET}
      region: ${AWS_REGION}
    ses:
      enabled: true
      from-email: ${SES_FROM_EMAIL}
      region: ${AWS_REGION}

logging:
  level:
    com.codeops: INFO
```

**Annotations:**

| Property | Type | Notes |
|----------|------|-------|
| `datasource.url` | Env var (required) | `${DATABASE_URL}` -- no default, must be provided |
| `datasource.username` | Env var (required) | `${DATABASE_USERNAME}` |
| `datasource.password` | Env var (required) | `${DATABASE_PASSWORD}` |
| `jpa.hibernate.ddl-auto` | Hardcoded | `validate` -- schema must pre-exist (good for production) |
| `jpa.show-sql` | Hardcoded | `false` |
| `codeops.jwt.secret` | Env var (required) | `${JWT_SECRET}` -- no default |
| `codeops.encryption.key` | Env var (required) | `${ENCRYPTION_KEY}` -- no default |
| `codeops.cors.allowed-origins` | Env var (required) | `${CORS_ALLOWED_ORIGINS}` |
| `codeops.aws.s3.enabled` | Hardcoded | `true` -- S3 active in production |
| `codeops.aws.s3.bucket` | Env var (required) | `${S3_BUCKET}` |
| `codeops.aws.s3.region` | Env var (required) | `${AWS_REGION}` |
| `codeops.aws.ses.enabled` | Hardcoded | `true` -- SES active in production |
| `codeops.aws.ses.from-email` | Env var (required) | `${SES_FROM_EMAIL}` |
| `logging.level` | Hardcoded | `com.codeops: INFO` |

**Required production environment variables:** `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `ENCRYPTION_KEY`, `CORS_ALLOWED_ORIGINS`, `S3_BUCKET`, `AWS_REGION`, `SES_FROM_EMAIL`.

---

### 4.4 application-test.yml (Test Profile)

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: false
    open-in-view: false

  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null

  flyway:
    enabled: false

codeops:
  jwt:
    secret: test-secret-key-minimum-32-characters-long-for-hs256-testing
    expiration-hours: 24
    refresh-expiration-days: 30

  encryption:
    key: test-encryption-key-minimum-32chars-ok

  cors:
    allowed-origins: http://localhost:3000

  aws:
    s3:
      enabled: false
      bucket: test-bucket
      region: us-east-1
    ses:
      enabled: false
      from-email: test@codeops.dev
      region: us-east-1

  local-storage:
    path: ${java.io.tmpdir}/codeops-test-storage

logging:
  level:
    com.codeops: WARN
    org.hibernate.SQL: WARN
```

**Annotations:**

| Property | Type | Notes |
|----------|------|-------|
| `datasource` | Hardcoded | H2 in-memory database (`jdbc:h2:mem:testdb`), no external DB needed |
| `jpa.hibernate.ddl-auto` | Hardcoded | `create-drop` -- schema created on startup, dropped on shutdown |
| `jpa.hibernate.dialect` | Hardcoded | `H2Dialect` (differs from prod `PostgreSQLDialect`) |
| `spring.flyway.enabled` | Hardcoded | `false` -- explicitly disables Flyway (not a dependency, but prevents auto-config) |
| `codeops.jwt.secret` | Hardcoded | Test-only secret |
| `codeops.encryption.key` | Hardcoded | Test-only encryption key |
| `codeops.aws.s3.enabled` | Hardcoded | `false` |
| `codeops.aws.ses.enabled` | Hardcoded | `false` |
| `codeops.local-storage.path` | System property | `${java.io.tmpdir}/codeops-test-storage` |
| `logging.level` | Hardcoded | `WARN` for both app and Hibernate |

---

### 4.5 docker-compose.yml

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: codeops-db
    environment:
      POSTGRES_DB: codeops
      POSTGRES_USER: codeops
      POSTGRES_PASSWORD: codeops
    ports:
      - "127.0.0.1:5432:5432"
    volumes:
      - codeops_data:/var/lib/postgresql/data

volumes:
  codeops_data:
```

**Annotations:**
- **Single service:** PostgreSQL 16 Alpine only. No Redis, Kafka, or application container.
- **Port binding:** `127.0.0.1:5432:5432` -- bound to localhost only (not exposed to network).
- **Credentials:** Hardcoded `codeops`/`codeops`/`codeops` (matches `application-dev.yml` defaults).
- **Volume:** Named volume `codeops_data` for persistent data across container restarts.
- **No healthcheck** defined for the PostgreSQL container.
- **No init scripts** referenced (unlike the Zevaro project which has `init-scripts/`).

---

### 4.6 Dockerfile

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/Dockerfile`

```dockerfile
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY target/codeops-server-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Annotations:**
- **Base image:** Eclipse Temurin 21 JRE Alpine (minimal JRE, no JDK in production image).
- **Security:** Non-root user `appuser` in `appgroup` -- follows container security best practices.
- **Build artifact:** Copies `target/codeops-server-*.jar` using a glob pattern.
- **Port:** Exposes 8090 (matches `server.port` in `application.yml`).
- **No JVM flags:** No memory limits, GC tuning, or JMX configuration in `ENTRYPOINT`.
- **Multi-stage build:** Not used. The JAR must be pre-built before `docker build`.

---

### 4.7 Connection Map

```
+---------------------------------------------------------------------+
|                        CodeOps-Server (:8090)                        |
|                    Spring Boot 3.3.0 / Java 21                       |
+----------+--------------+--------------+--------------+--------------+
           |              |              |              |
           v              v              v              v
   +------------+  +--------------+ +---------+  +---------------+
   | PostgreSQL |  |   AWS S3     | | AWS SES |  | MS Teams      |
   | 16-alpine  |  | (prod only)  | | (prod)  |  | Webhook       |
   | :5432      |  | Report/file  | | Email   |  | Notifications |
   |            |  | storage      | | sending |  |               |
   | Dev: local |  |              | |         |  | Outbound HTTP |
   | Prod: env  |  | Dev: local   | | Dev:    |  | via           |
   | Test: H2   |  | filesystem   | | console |  | RestTemplate  |
   +------------+  +--------------+ +---------+  +---------------+

  +-------------------------------------------------------+
  |                   NOT USED                              |
  |  - No Redis (token blacklist is in-memory HashMap)      |
  |  - No Kafka / message broker                            |
  |  - No external auth provider (JWT is self-issued)       |
  |  - No CDN / reverse proxy defined                       |
  +-------------------------------------------------------+
```

**Connection Summary:**

| Service | Protocol | Host:Port | Profile | Auth |
|---------|----------|-----------|---------|------|
| **PostgreSQL** | JDBC (TCP) | `localhost:5432` (dev), `${DATABASE_URL}` (prod), H2 in-memory (test) | All | Username/password |
| **AWS S3** | HTTPS (SDK) | `s3.${region}.amazonaws.com` | Prod only | IAM credentials (SDK default chain) |
| **AWS SES** | HTTPS (SDK) | `email.${region}.amazonaws.com` | Prod only | IAM credentials (SDK default chain) |
| **MS Teams** | HTTPS (Webhook) | Configurable webhook URL | When configured | Webhook URL (no auth header) |
| **Local filesystem** | Disk I/O | `~/.codeops/storage` (dev), `${java.io.tmpdir}/codeops-test-storage` (test) | Dev/Test | N/A |

---

## Section 5: Startup & Runtime Behavior

### 5.1 Entry Point

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/CodeOpsApplication.java`

```java
package com.codeops;

import com.codeops.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class CodeOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeOpsApplication.class, args);
    }
}
```

The entry point is minimal. It bootstraps Spring Boot with component scanning rooted at `com.codeops` and enables `JwtProperties` as a `@ConfigurationProperties` bean.

### 5.2 Startup Initialization

There is exactly **one `@PostConstruct` method** in the entire codebase:

**`JwtTokenProvider.validateSecret()`** (`/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/security/JwtTokenProvider.java`, line 31-37):

```java
@PostConstruct
public void validateSecret() {
    String secret = jwtProperties.getSecret();
    if (secret == null || secret.isBlank() || secret.length() < 32) {
        throw new IllegalStateException(
            "JWT secret must be at least 32 characters. Set the JWT_SECRET environment variable.");
    }
}
```

This is a **fail-fast guard** that prevents the application from starting if the JWT secret is missing or too short (< 32 characters). If the secret is invalid, the application context will fail to initialize and the server will not start.

### 5.3 Scheduled Tasks

There are **no `@Scheduled` methods**, no `CommandLineRunner`, no `ApplicationRunner`, and no `ApplicationReadyEvent` listeners in the codebase. The application has no background jobs, cron tasks, or startup data initialization.

### 5.4 Async Configuration

The `AsyncConfig` class (`/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/config/AsyncConfig.java`) enables `@EnableAsync` and configures a `ThreadPoolTaskExecutor`:

```
Core pool: 5 threads
Max pool: 20 threads
Queue capacity: 100
Thread name prefix: "codeops-async-"
Rejection policy: CallerRunsPolicy
```

This thread pool is used by `AuditLogService.log()` which is annotated with `@Async` for fire-and-forget audit logging.

### 5.5 Health Check Endpoint

**Endpoint:** `GET /api/v1/health` (unauthenticated -- permitted in `SecurityConfig`)

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/config/HealthController.java`

```java
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "codeops-server",
                "timestamp", Instant.now().toString()
        ));
    }
}
```

**Response example:**
```json
{
    "status": "UP",
    "service": "codeops-server",
    "timestamp": "2026-02-14T15:05:00Z"
}
```

**Notes:**
- This is a **custom health endpoint**, not the Spring Boot Actuator health endpoint (`/actuator/health`). Spring Boot Actuator is **not** included as a dependency.
- The health check is **superficial** -- it returns `"UP"` unconditionally without checking database connectivity, S3 availability, or any other downstream dependency. It proves the JVM is running and Tomcat is accepting requests, but nothing more.

### 5.6 Security Filter Chain (Startup-Registered)

On startup, Spring Security registers the following filter chain (in order of execution):

1. **`RateLimitFilter`** (`/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/security/RateLimitFilter.java`) -- In-memory per-IP rate limiting for `/api/v1/auth/**` endpoints. Max 10 requests per minute per IP. Uses `ConcurrentHashMap` (not persisted, resets on restart).

2. **`JwtAuthFilter`** (`/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/security/JwtAuthFilter.java`) -- Extracts Bearer token from `Authorization` header, validates JWT signature and expiration, checks token blacklist, and sets `SecurityContext` with user ID (UUID) as principal and roles as granted authorities.

3. **Spring Security filter chain** (`/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/security/SecurityConfig.java`) -- CSRF disabled (stateless JWT API), CORS configured via `CorsConfigurationSource` bean, session management set to `STATELESS`, security headers (CSP, HSTS, X-Frame-Options DENY, X-Content-Type-Options).

**Unauthenticated paths:** `/api/v1/auth/**`, `/api/v1/health`, `/swagger-ui/**`, `/v3/api-docs/**`

### 5.7 Conditional Beans

| Bean | Condition | Behavior When Disabled |
|------|-----------|----------------------|
| `S3Client` | `codeops.aws.s3.enabled=true` | Not created. `S3StorageService` falls back to local filesystem via `ReportStorageService`. |
| `SesClient` | `codeops.aws.ses.enabled=true` | Not created. `EmailService` logs emails to console instead of sending. |

### 5.8 Startup Sequence Summary

```
1. JVM starts, Spring Boot bootstraps
2. Component scan: com.codeops.**
3. Profile activated: dev (default)
4. DataSource initialized: PostgreSQL on localhost:5432
5. Hibernate ddl-auto=update: auto-create/alter tables
6. @PostConstruct: JwtTokenProvider.validateSecret() -- fail-fast if JWT_SECRET < 32 chars
7. @EnableAsync: ThreadPoolTaskExecutor initialized (5-20 threads)
8. Security filter chain registered: RateLimitFilter -> JwtAuthFilter -> Spring Security
9. Conditional beans: S3Client (skipped in dev), SesClient (skipped in dev)
10. Embedded Tomcat starts on port 8090
11. Swagger UI available at /swagger-ui/index.html
12. Application ready -- accepting requests
```


## Section 6: Entity / Data Model Layer

All entity source files are located under:
`/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/entity/`

---

### === BaseEntity.java ===

```
@MappedSuperclass (abstract — not a table itself)
```

**Primary Key:**
- `id`: UUID `@GeneratedValue(strategy = GenerationType.UUID)`

**Fields:**
- `id`: UUID [@Id, @GeneratedValue(strategy = GenerationType.UUID)]
- `createdAt`: Instant [@Column(name = "created_at", nullable = false, updatable = false)]
- `updatedAt`: Instant [@Column(name = "updated_at")]

**Relationships:** None

**Enum Fields:** None

**Indexes:** None

**Unique Constraints:** None

**Validation:** None (no Jakarta Validation annotations)

**Auditing:**
- `createdAt` set in `@PrePersist` via `onCreate()` to `Instant.now()`
- `updatedAt` set in both `@PrePersist` and `@PreUpdate` to `Instant.now()`

**Lombok:** `@Getter`, `@Setter`

**Notes:** Serves as the base class for most entities. Exceptions: `AuditLog` (Long PK), `SystemSetting` (String PK), `ProjectDirective` (composite PK) do NOT extend `BaseEntity`.

---

### === User.java ===

```
Table: users
```

**Primary Key:** `id`: UUID (inherited from BaseEntity, GenerationType.UUID)

**Fields:**
- `email`: String [@Column(name = "email", nullable = false, unique = true, length = 255)]
- `passwordHash`: String [@Column(name = "password_hash", nullable = false, length = 255)]
- `displayName`: String [@Column(name = "display_name", nullable = false, length = 100)]
- `avatarUrl`: String [@Column(name = "avatar_url", length = 500)] (nullable)
- `isActive`: Boolean [@Column(name = "is_active", nullable = false)] @Builder.Default = true
- `lastLoginAt`: Instant [@Column(name = "last_login_at")] (nullable)

**Relationships:** None declared on this entity (referenced as target by many others)

**Enum Fields:** None

**Indexes:** None (beyond implicit unique index on email)

**Unique Constraints:** `email` (column-level `unique = true`)

**Validation:** None (no Jakarta Validation annotations)

**Auditing:** Inherited from BaseEntity (`createdAt`, `updatedAt`)

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === Team.java ===

```
Table: teams
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `name`: String [@Column(name = "name", nullable = false, length = 100)]
- `description`: String [@Column(name = "description", columnDefinition = "TEXT")] (nullable)
- `teamsWebhookUrl`: String [@Column(name = "teams_webhook_url", length = 500)] (nullable)
- `settingsJson`: String [@Column(name = "settings_json", columnDefinition = "TEXT")] (nullable)

**Relationships:**
- `owner`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "owner_id", nullable = false)]

**Enum Fields:** None

**Indexes:** None explicitly declared

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === TeamMember.java ===

```
Table: team_members
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `role`: TeamRole [@Enumerated(EnumType.STRING), @Column(name = "role", nullable = false)]
- `joinedAt`: Instant [@Column(name = "joined_at", nullable = false)]

**Relationships:**
- `team`: @ManyToOne(fetch = FetchType.LAZY) -> Team [@JoinColumn(name = "team_id", nullable = false)]
- `user`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "user_id", nullable = false)]

**Enum Fields:**
- `role`: TeamRole [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_tm_team_id` on `team_id`
- `idx_tm_user_id` on `user_id`

**Unique Constraints:**
- Composite: `(team_id, user_id)` via @UniqueConstraint

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === Project.java ===

```
Table: projects
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `name`: String [@Column(name = "name", nullable = false, length = 200)]
- `description`: String [@Column(name = "description", columnDefinition = "TEXT")] (nullable)
- `repoUrl`: String [@Column(name = "repo_url", length = 500)] (nullable)
- `repoFullName`: String [@Column(name = "repo_full_name", length = 200)] (nullable)
- `defaultBranch`: String [@Column(name = "default_branch", columnDefinition = "varchar(100) default 'main'")] @Builder.Default = "main"
- `jiraProjectKey`: String [@Column(name = "jira_project_key", length = 20)] (nullable)
- `jiraDefaultIssueType`: String [@Column(name = "jira_default_issue_type", columnDefinition = "varchar(50) default 'Task'")] @Builder.Default = "Task"
- `jiraLabels`: String [@Column(name = "jira_labels", columnDefinition = "TEXT")] (nullable)
- `jiraComponent`: String [@Column(name = "jira_component", length = 100)] (nullable)
- `techStack`: String [@Column(name = "tech_stack", length = 200)] (nullable)
- `healthScore`: Integer [@Column(name = "health_score")] (nullable)
- `lastAuditAt`: Instant [@Column(name = "last_audit_at")] (nullable)
- `settingsJson`: String [@Column(name = "settings_json", columnDefinition = "TEXT")] (nullable)
- `isArchived`: Boolean [@Column(name = "is_archived", nullable = false)] @Builder.Default = false

**Relationships:**
- `team`: @ManyToOne(fetch = FetchType.LAZY) -> Team [@JoinColumn(name = "team_id", nullable = false)]
- `githubConnection`: @ManyToOne(fetch = FetchType.LAZY) -> GitHubConnection [@JoinColumn(name = "github_connection_id", nullable = true)]
- `jiraConnection`: @ManyToOne(fetch = FetchType.LAZY) -> JiraConnection [@JoinColumn(name = "jira_connection_id", nullable = true)]
- `createdBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "created_by", nullable = false)]

**Enum Fields:** None

**Indexes:**
- `idx_project_team_id` on `team_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === QaJob.java ===

```
Table: qa_jobs
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `mode`: JobMode [@Enumerated(EnumType.STRING), @Column(name = "mode", nullable = false)]
- `status`: JobStatus [@Enumerated(EnumType.STRING), @Column(name = "status", nullable = false)]
- `name`: String [@Column(name = "name", length = 200)] (nullable)
- `branch`: String [@Column(name = "branch", length = 100)] (nullable)
- `configJson`: String [@Column(name = "config_json", columnDefinition = "TEXT")] (nullable)
- `summaryMd`: String [@Column(name = "summary_md", columnDefinition = "TEXT")] (nullable)
- `overallResult`: JobResult [@Enumerated(EnumType.STRING), @Column(name = "overall_result")] (nullable)
- `healthScore`: Integer [@Column(name = "health_score")] (nullable)
- `totalFindings`: Integer [@Column(name = "total_findings")] @Builder.Default = 0
- `criticalCount`: Integer [@Column(name = "critical_count")] @Builder.Default = 0
- `highCount`: Integer [@Column(name = "high_count")] @Builder.Default = 0
- `mediumCount`: Integer [@Column(name = "medium_count")] @Builder.Default = 0
- `lowCount`: Integer [@Column(name = "low_count")] @Builder.Default = 0
- `jiraTicketKey`: String [@Column(name = "jira_ticket_key", length = 50)] (nullable)
- `startedAt`: Instant [@Column(name = "started_at")] (nullable)
- `completedAt`: Instant [@Column(name = "completed_at")] (nullable)
- `version`: Long [@Version, @Column(name = "version")]

**Relationships:**
- `project`: @ManyToOne(fetch = FetchType.LAZY) -> Project [@JoinColumn(name = "project_id", nullable = false)]
- `startedBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "started_by", nullable = false)]

**Enum Fields:**
- `mode`: JobMode [@Enumerated(EnumType.STRING)]
- `status`: JobStatus [@Enumerated(EnumType.STRING)]
- `overallResult`: JobResult [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_job_project_id` on `project_id`
- `idx_job_started_by` on `started_by`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Optimistic Locking:** `version` field with `@Version`

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === AgentRun.java ===

```
Table: agent_runs
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `agentType`: AgentType [@Enumerated(EnumType.STRING), @Column(name = "agent_type", nullable = false)]
- `status`: AgentStatus [@Enumerated(EnumType.STRING), @Column(name = "status", nullable = false)]
- `result`: AgentResult [@Enumerated(EnumType.STRING), @Column(name = "result")] (nullable)
- `reportS3Key`: String [@Column(name = "report_s3_key", length = 500)] (nullable)
- `score`: Integer [@Column(name = "score")] (nullable)
- `findingsCount`: Integer [@Column(name = "findings_count")] @Builder.Default = 0
- `criticalCount`: Integer [@Column(name = "critical_count")] @Builder.Default = 0
- `highCount`: Integer [@Column(name = "high_count")] @Builder.Default = 0
- `startedAt`: Instant [@Column(name = "started_at")] (nullable)
- `completedAt`: Instant [@Column(name = "completed_at")] (nullable)
- `version`: Long [@Version, @Column(name = "version")]

**Relationships:**
- `job`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "job_id", nullable = false)]

**Enum Fields:**
- `agentType`: AgentType [@Enumerated(EnumType.STRING)]
- `status`: AgentStatus [@Enumerated(EnumType.STRING)]
- `result`: AgentResult [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_agent_run_job_id` on `job_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Optimistic Locking:** `version` field with `@Version`

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === Finding.java ===

```
Table: findings
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `agentType`: AgentType [@Enumerated(EnumType.STRING), @Column(name = "agent_type", nullable = false)]
- `severity`: Severity [@Enumerated(EnumType.STRING), @Column(name = "severity", nullable = false)]
- `title`: String [@Column(name = "title", nullable = false, length = 500)]
- `description`: String [@Column(name = "description", columnDefinition = "TEXT")] (nullable)
- `filePath`: String [@Column(name = "file_path", length = 500)] (nullable)
- `lineNumber`: Integer [@Column(name = "line_number")] (nullable)
- `recommendation`: String [@Column(name = "recommendation", columnDefinition = "TEXT")] (nullable)
- `evidence`: String [@Column(name = "evidence", columnDefinition = "TEXT")] (nullable)
- `effortEstimate`: Effort [@Enumerated(EnumType.STRING), @Column(name = "effort_estimate")] (nullable)
- `debtCategory`: DebtCategory [@Enumerated(EnumType.STRING), @Column(name = "debt_category")] (nullable)
- `status`: FindingStatus [@Enumerated(EnumType.STRING), @Column(name = "status", columnDefinition = "varchar(20) default 'OPEN'")] @Builder.Default = FindingStatus.OPEN
- `statusChangedAt`: Instant [@Column(name = "status_changed_at")] (nullable)
- `version`: Long [@Version, @Column(name = "version")]

**Relationships:**
- `job`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "job_id", nullable = false)]
- `statusChangedBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "status_changed_by", nullable = true)]

**Enum Fields:**
- `agentType`: AgentType [@Enumerated(EnumType.STRING)]
- `severity`: Severity [@Enumerated(EnumType.STRING)]
- `effortEstimate`: Effort [@Enumerated(EnumType.STRING)]
- `debtCategory`: DebtCategory [@Enumerated(EnumType.STRING)]
- `status`: FindingStatus [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_finding_job_id` on `job_id`
- `idx_finding_status` on `status`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Optimistic Locking:** `version` field with `@Version`

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === RemediationTask.java ===

```
Table: remediation_tasks
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `taskNumber`: Integer [@Column(name = "task_number", nullable = false)]
- `title`: String [@Column(name = "title", nullable = false, length = 500)]
- `description`: String [@Column(name = "description", columnDefinition = "TEXT")] (nullable)
- `promptMd`: String [@Column(name = "prompt_md", columnDefinition = "TEXT")] (nullable)
- `promptS3Key`: String [@Column(name = "prompt_s3_key", length = 500)] (nullable)
- `priority`: Priority [@Enumerated(EnumType.STRING), @Column(name = "priority", nullable = false)]
- `status`: TaskStatus [@Enumerated(EnumType.STRING), @Column(name = "status", columnDefinition = "varchar(20) default 'PENDING'")] @Builder.Default = TaskStatus.PENDING
- `jiraKey`: String [@Column(name = "jira_key", length = 50)] (nullable)
- `version`: Long [@Version, @Column(name = "version")]

**Relationships:**
- `job`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "job_id", nullable = false)]
- `findings`: @ManyToMany(fetch = FetchType.LAZY) -> List<Finding> via join table `remediation_task_findings` (joinColumns = `task_id`, inverseJoinColumns = `finding_id`) @Builder.Default = new ArrayList<>()
- `assignedTo`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "assigned_to", nullable = true)]

**Enum Fields:**
- `priority`: Priority [@Enumerated(EnumType.STRING)]
- `status`: TaskStatus [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_task_job_id` on `job_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Optimistic Locking:** `version` field with `@Version`

**Join Tables:**
- `remediation_task_findings` with columns `task_id` (FK -> remediation_tasks.id) and `finding_id` (FK -> findings.id)

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === Directive.java ===

```
Table: directives
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `name`: String [@Column(name = "name", nullable = false, length = 200)]
- `description`: String [@Column(name = "description", columnDefinition = "TEXT")] (nullable)
- `contentMd`: String [@Column(name = "content_md", nullable = false, columnDefinition = "TEXT")]
- `category`: DirectiveCategory [@Enumerated(EnumType.STRING), @Column(name = "category")] (nullable)
- `scope`: DirectiveScope [@Enumerated(EnumType.STRING), @Column(name = "scope", nullable = false)]
- `version`: Integer [@Column(name = "version", columnDefinition = "integer default 1")] @Builder.Default = 1

**Relationships:**
- `team`: @ManyToOne(fetch = FetchType.LAZY) -> Team [@JoinColumn(name = "team_id", nullable = true)]
- `project`: @ManyToOne(fetch = FetchType.LAZY) -> Project [@JoinColumn(name = "project_id", nullable = true)]
- `createdBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "created_by", nullable = false)]

**Enum Fields:**
- `category`: DirectiveCategory [@Enumerated(EnumType.STRING)]
- `scope`: DirectiveScope [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_directive_team_id` on `team_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

**Note:** The `version` field here is a simple Integer with a default, NOT a JPA `@Version` for optimistic locking.

---

### === ProjectDirective.java ===

```
Table: project_directives
```

**Primary Key:** `id`: ProjectDirectiveId [@EmbeddedId] (composite key)

**Fields:**
- `enabled`: Boolean [@Column(nullable = false, columnDefinition = "boolean default true")] @Builder.Default = true

**Relationships:**
- `project`: @ManyToOne(fetch = FetchType.LAZY) -> Project [@MapsId("projectId"), @JoinColumn(name = "project_id")]
- `directive`: @ManyToOne(fetch = FetchType.LAZY) -> Directive [@MapsId("directiveId"), @JoinColumn(name = "directive_id")]

**Enum Fields:** None

**Indexes:** None explicitly declared

**Unique Constraints:** Implicit via composite primary key (project_id, directive_id)

**Validation:** None

**Auditing:** None (does NOT extend BaseEntity)

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === ProjectDirectiveId.java ===

```
@Embeddable — Composite primary key for ProjectDirective
Implements: Serializable
```

**Fields:**
- `projectId`: UUID [@Column(name = "project_id")]
- `directiveId`: UUID [@Column(name = "directive_id")]

**Methods:**
- `equals(Object o)`: Compares `projectId` and `directiveId`
- `hashCode()`: Uses `Objects.hash(projectId, directiveId)`

**Lombok:** `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`

---

### === Persona.java ===

```
Table: personas
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `name`: String [@Column(name = "name", nullable = false, length = 100)]
- `agentType`: AgentType [@Enumerated(EnumType.STRING), @Column(name = "agent_type", nullable = false)]
- `description`: String [@Column(name = "description", columnDefinition = "TEXT")] (nullable)
- `contentMd`: String [@Column(name = "content_md", nullable = false, columnDefinition = "TEXT")]
- `scope`: Scope [@Enumerated(EnumType.STRING), @Column(name = "scope", nullable = false)]
- `isDefault`: Boolean [@Column(name = "is_default")] @Builder.Default = false
- `version`: Integer [@Column(name = "version", nullable = false)] @Builder.Default = 1

**Relationships:**
- `team`: @ManyToOne(fetch = FetchType.LAZY) -> Team [@JoinColumn(name = "team_id", nullable = true)]
- `createdBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "created_by", nullable = false)]

**Enum Fields:**
- `agentType`: AgentType [@Enumerated(EnumType.STRING)]
- `scope`: Scope [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_persona_team_id` on `team_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

**Note:** The `version` field is a simple Integer, NOT a JPA `@Version` for optimistic locking.

---

### === Invitation.java ===

```
Table: invitations
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `email`: String [@Column(name = "email", nullable = false, length = 255)]
- `role`: TeamRole [@Enumerated(EnumType.STRING), @Column(name = "role", nullable = false)]
- `token`: String [@Column(name = "token", nullable = false, unique = true, length = 100)]
- `status`: InvitationStatus [@Enumerated(EnumType.STRING), @Column(name = "status", nullable = false)]
- `expiresAt`: Instant [@Column(name = "expires_at", nullable = false)]

**Relationships:**
- `team`: @ManyToOne(fetch = FetchType.LAZY) -> Team [@JoinColumn(name = "team_id", nullable = false)]
- `invitedBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "invited_by", nullable = false)]

**Enum Fields:**
- `role`: TeamRole [@Enumerated(EnumType.STRING)]
- `status`: InvitationStatus [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_inv_team_id` on `team_id`
- `idx_inv_email` on `email`

**Unique Constraints:** `token` (column-level `unique = true`)

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === AuditLog.java ===

```
Table: audit_log
```

**Primary Key:** `id`: Long [@Id, @GeneratedValue(strategy = GenerationType.IDENTITY)]

**Fields:**
- `action`: String [@Column(name = "action", nullable = false, length = 50)]
- `entityType`: String [@Column(name = "entity_type", length = 30)] (nullable)
- `entityId`: UUID [@Column(name = "entity_id")] (nullable)
- `details`: String [@Column(name = "details", columnDefinition = "TEXT")] (nullable)
- `ipAddress`: String [@Column(name = "ip_address", length = 45)] (nullable)
- `createdAt`: Instant [@Column(name = "created_at", nullable = false)]

**Relationships:**
- `user`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "user_id", nullable = true)]
- `team`: @ManyToOne(fetch = FetchType.LAZY) -> Team [@JoinColumn(name = "team_id", nullable = true)]

**Enum Fields:** None

**Indexes:**
- `idx_audit_user_id` on `user_id`
- `idx_audit_team_id` on `team_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Manual `createdAt` field (no `@PrePersist` — does NOT extend BaseEntity)

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

**Note:** Does NOT extend BaseEntity. Has its own Long PK with IDENTITY generation, and manages `createdAt` manually.

---

### === SystemSetting.java ===

```
Table: system_settings
```

**Primary Key:** `settingKey`: String [@Id, @Column(name = "key", length = 100)]

**Fields:**
- `settingKey`: String [@Id, @Column(name = "key", length = 100)]
- `value`: String [@Column(name = "value", nullable = false, columnDefinition = "TEXT")]
- `updatedAt`: Instant [@Column(name = "updated_at", nullable = false)]

**Relationships:**
- `updatedBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "updated_by", nullable = true)]

**Enum Fields:** None

**Indexes:** None

**Unique Constraints:** PK on `key` column

**Validation:** None

**Auditing:** Manual `updatedAt` field

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

**Note:** Does NOT extend BaseEntity. Uses a natural String primary key (`key` column). No `createdAt` field.

---

### === GitHubConnection.java ===

```
Table: github_connections
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `name`: String [@Column(name = "name", nullable = false, length = 100)]
- `authType`: GitHubAuthType [@Enumerated(EnumType.STRING), @Column(name = "auth_type", nullable = false)]
- `encryptedCredentials`: String [@Column(name = "encrypted_credentials", nullable = false, columnDefinition = "TEXT")]
- `githubUsername`: String [@Column(name = "github_username", length = 100)] (nullable)
- `isActive`: Boolean [@Column(name = "is_active", nullable = false)] @Builder.Default = true

**Relationships:**
- `team`: @ManyToOne(fetch = FetchType.LAZY) -> Team [@JoinColumn(name = "team_id", nullable = false)]
- `createdBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "created_by", nullable = false)]

**Enum Fields:**
- `authType`: GitHubAuthType [@Enumerated(EnumType.STRING)]

**Indexes:** None explicitly declared

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === JiraConnection.java ===

```
Table: jira_connections
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `name`: String [@Column(name = "name", nullable = false, length = 100)]
- `instanceUrl`: String [@Column(name = "instance_url", nullable = false, length = 500)]
- `email`: String [@Column(name = "email", nullable = false, length = 255)]
- `encryptedApiToken`: String [@Column(name = "encrypted_api_token", nullable = false, columnDefinition = "TEXT")]
- `isActive`: Boolean [@Column(name = "is_active", nullable = false)] @Builder.Default = true

**Relationships:**
- `team`: @ManyToOne(fetch = FetchType.LAZY) -> Team [@JoinColumn(name = "team_id", nullable = false)]
- `createdBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "created_by", nullable = false)]

**Enum Fields:** None

**Indexes:** None explicitly declared

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === BugInvestigation.java ===

```
Table: bug_investigations
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `jiraKey`: String [@Column(name = "jira_key", length = 50)] (nullable)
- `jiraSummary`: String [@Column(name = "jira_summary", columnDefinition = "TEXT")] (nullable)
- `jiraDescription`: String [@Column(name = "jira_description", columnDefinition = "TEXT")] (nullable)
- `jiraCommentsJson`: String [@Column(name = "jira_comments_json", columnDefinition = "TEXT")] (nullable)
- `jiraAttachmentsJson`: String [@Column(name = "jira_attachments_json", columnDefinition = "TEXT")] (nullable)
- `jiraLinkedIssues`: String [@Column(name = "jira_linked_issues", columnDefinition = "TEXT")] (nullable)
- `additionalContext`: String [@Column(name = "additional_context", columnDefinition = "TEXT")] (nullable)
- `rcaMd`: String [@Column(name = "rca_md", columnDefinition = "TEXT")] (nullable)
- `impactAssessmentMd`: String [@Column(name = "impact_assessment_md", columnDefinition = "TEXT")] (nullable)
- `rcaS3Key`: String [@Column(name = "rca_s3_key", length = 500)] (nullable)
- `rcaPostedToJira`: Boolean [@Column(name = "rca_posted_to_jira")] @Builder.Default = false
- `fixTasksCreatedInJira`: Boolean [@Column(name = "fix_tasks_created_in_jira")] @Builder.Default = false

**Relationships:**
- `job`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "job_id", nullable = false)]

**Enum Fields:** None

**Indexes:** None explicitly declared

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === ComplianceItem.java ===

```
Table: compliance_items
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `requirement`: String [@Column(name = "requirement", nullable = false, columnDefinition = "TEXT")]
- `status`: ComplianceStatus [@Enumerated(EnumType.STRING), @Column(name = "status", nullable = false)]
- `evidence`: String [@Column(name = "evidence", columnDefinition = "TEXT")] (nullable)
- `agentType`: AgentType [@Enumerated(EnumType.STRING), @Column(name = "agent_type")] (nullable)
- `notes`: String [@Column(name = "notes", columnDefinition = "TEXT")] (nullable)

**Relationships:**
- `job`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "job_id", nullable = false)]
- `spec`: @ManyToOne(fetch = FetchType.LAZY) -> Specification [@JoinColumn(name = "spec_id", nullable = true)]

**Enum Fields:**
- `status`: ComplianceStatus [@Enumerated(EnumType.STRING)]
- `agentType`: AgentType [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_compliance_job_id` on `job_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === Specification.java ===

```
Table: specifications
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `name`: String [@Column(nullable = false, length = 200)]
- `specType`: SpecType [@Enumerated(EnumType.STRING), @Column(name = "spec_type", nullable = false)]
- `s3Key`: String [@Column(name = "s3_key", nullable = false, length = 500)]

**Relationships:**
- `job`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "job_id", nullable = false)]

**Enum Fields:**
- `specType`: SpecType [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_spec_job_id` on `job_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === DependencyScan.java ===

```
Table: dependency_scans
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `manifestFile`: String [@Column(name = "manifest_file", length = 200)] (nullable)
- `totalDependencies`: Integer [@Column(name = "total_dependencies")] (nullable)
- `outdatedCount`: Integer [@Column(name = "outdated_count")] (nullable)
- `vulnerableCount`: Integer [@Column(name = "vulnerable_count")] (nullable)
- `scanDataJson`: String [@Column(name = "scan_data_json", columnDefinition = "TEXT")] (nullable)

**Relationships:**
- `project`: @ManyToOne(fetch = FetchType.LAZY) -> Project [@JoinColumn(name = "project_id", nullable = false)]
- `job`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "job_id", nullable = true)]

**Enum Fields:** None

**Indexes:**
- `idx_dep_scan_project_id` on `project_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === DependencyVulnerability.java ===

```
Table: dependency_vulnerabilities
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `dependencyName`: String [@Column(name = "dependency_name", nullable = false, length = 200)]
- `currentVersion`: String [@Column(name = "current_version", length = 50)] (nullable)
- `fixedVersion`: String [@Column(name = "fixed_version", length = 50)] (nullable)
- `cveId`: String [@Column(name = "cve_id", length = 30)] (nullable)
- `severity`: Severity [@Enumerated(EnumType.STRING), @Column(name = "severity", nullable = false)]
- `description`: String [@Column(name = "description", columnDefinition = "TEXT")] (nullable)
- `status`: VulnerabilityStatus [@Enumerated(EnumType.STRING), @Column(name = "status", columnDefinition = "varchar(20) default 'OPEN'")] @Builder.Default = VulnerabilityStatus.OPEN

**Relationships:**
- `scan`: @ManyToOne(fetch = FetchType.LAZY) -> DependencyScan [@JoinColumn(name = "scan_id", nullable = false)]

**Enum Fields:**
- `severity`: Severity [@Enumerated(EnumType.STRING)]
- `status`: VulnerabilityStatus [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_vuln_scan_id` on `scan_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === TechDebtItem.java ===

```
Table: tech_debt_items
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `category`: DebtCategory [@Enumerated(EnumType.STRING), @Column(name = "category", nullable = false)]
- `title`: String [@Column(name = "title", nullable = false, length = 500)]
- `description`: String [@Column(name = "description", columnDefinition = "TEXT")] (nullable)
- `filePath`: String [@Column(name = "file_path", length = 500)] (nullable)
- `effortEstimate`: Effort [@Enumerated(EnumType.STRING), @Column(name = "effort_estimate")] (nullable)
- `businessImpact`: BusinessImpact [@Enumerated(EnumType.STRING), @Column(name = "business_impact")] (nullable)
- `status`: DebtStatus [@Enumerated(EnumType.STRING), @Column(name = "status", columnDefinition = "varchar(20) default 'IDENTIFIED'")] @Builder.Default = DebtStatus.IDENTIFIED
- `version`: Long [@Version, @Column(name = "version")]

**Relationships:**
- `project`: @ManyToOne(fetch = FetchType.LAZY) -> Project [@JoinColumn(name = "project_id", nullable = false)]
- `firstDetectedJob`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "first_detected_job_id", nullable = true)]
- `resolvedJob`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "resolved_job_id", nullable = true)]

**Enum Fields:**
- `category`: DebtCategory [@Enumerated(EnumType.STRING)]
- `effortEstimate`: Effort [@Enumerated(EnumType.STRING)]
- `businessImpact`: BusinessImpact [@Enumerated(EnumType.STRING)]
- `status`: DebtStatus [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_tech_debt_project_id` on `project_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Optimistic Locking:** `version` field with `@Version`

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === HealthSchedule.java ===

```
Table: health_schedules
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `scheduleType`: ScheduleType [@Enumerated(EnumType.STRING), @Column(name = "schedule_type", nullable = false)]
- `cronExpression`: String [@Column(name = "cron_expression", length = 50)] (nullable)
- `agentTypes`: String [@Column(name = "agent_types", nullable = false, columnDefinition = "TEXT")]
- `isActive`: Boolean [@Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")] @Builder.Default = true
- `lastRunAt`: Instant [@Column(name = "last_run_at")] (nullable)
- `nextRunAt`: Instant [@Column(name = "next_run_at")] (nullable)

**Relationships:**
- `project`: @ManyToOne(fetch = FetchType.LAZY) -> Project [@JoinColumn(name = "project_id", nullable = false)]
- `createdBy`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "created_by", nullable = false)]

**Enum Fields:**
- `scheduleType`: ScheduleType [@Enumerated(EnumType.STRING)]

**Indexes:**
- `idx_schedule_project_id` on `project_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === HealthSnapshot.java ===

```
Table: health_snapshots
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `healthScore`: Integer [@Column(name = "health_score", nullable = false)]
- `findingsBySeverity`: String [@Column(name = "findings_by_severity", columnDefinition = "TEXT")] (nullable)
- `techDebtScore`: Integer [@Column(name = "tech_debt_score")] (nullable)
- `dependencyScore`: Integer [@Column(name = "dependency_score")] (nullable)
- `testCoveragePercent`: BigDecimal [@Column(name = "test_coverage_percent", precision = 5, scale = 2)] (nullable)
- `capturedAt`: Instant [@Column(name = "captured_at", nullable = false)]

**Relationships:**
- `project`: @ManyToOne(fetch = FetchType.LAZY) -> Project [@JoinColumn(name = "project_id", nullable = false)]
- `job`: @ManyToOne(fetch = FetchType.LAZY) -> QaJob [@JoinColumn(name = "job_id", nullable = true)]

**Enum Fields:** None

**Indexes:**
- `idx_snapshot_project_id` on `project_id`

**Unique Constraints:** None

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### === NotificationPreference.java ===

```
Table: notification_preferences
```

**Primary Key:** `id`: UUID (inherited from BaseEntity)

**Fields:**
- `eventType`: String [@Column(name = "event_type", nullable = false, length = 50)]
- `inApp`: Boolean [@Column(name = "in_app", nullable = false, columnDefinition = "boolean default true")] @Builder.Default = true
- `email`: Boolean [@Column(name = "email", nullable = false, columnDefinition = "boolean default false")] @Builder.Default = false

**Relationships:**
- `user`: @ManyToOne(fetch = FetchType.LAZY) -> User [@JoinColumn(name = "user_id", nullable = false)]

**Enum Fields:** None

**Indexes:**
- `idx_notif_user_id` on `user_id`

**Unique Constraints:**
- Composite: `(user_id, event_type)` via @UniqueConstraint

**Validation:** None

**Auditing:** Inherited from BaseEntity

**Lombok:** `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

---

### Entity Relationship Summary

All `@ManyToOne` relationships with their FK columns:

| Source Entity | Field | FK Column | Target Entity | Nullable |
|---|---|---|---|---|
| Team | owner | owner_id | User | No |
| TeamMember | team | team_id | Team | No |
| TeamMember | user | user_id | User | No |
| Project | team | team_id | Team | No |
| Project | githubConnection | github_connection_id | GitHubConnection | Yes |
| Project | jiraConnection | jira_connection_id | JiraConnection | Yes |
| Project | createdBy | created_by | User | No |
| QaJob | project | project_id | Project | No |
| QaJob | startedBy | started_by | User | No |
| AgentRun | job | job_id | QaJob | No |
| Finding | job | job_id | QaJob | No |
| Finding | statusChangedBy | status_changed_by | User | Yes |
| RemediationTask | job | job_id | QaJob | No |
| RemediationTask | assignedTo | assigned_to | User | Yes |
| Directive | team | team_id | Team | Yes |
| Directive | project | project_id | Project | Yes |
| Directive | createdBy | created_by | User | No |
| ProjectDirective | project | project_id | Project | No (PK) |
| ProjectDirective | directive | directive_id | Directive | No (PK) |
| Persona | team | team_id | Team | Yes |
| Persona | createdBy | created_by | User | No |
| Invitation | team | team_id | Team | No |
| Invitation | invitedBy | invited_by | User | No |
| AuditLog | user | user_id | User | Yes |
| AuditLog | team | team_id | Team | Yes |
| SystemSetting | updatedBy | updated_by | User | Yes |
| GitHubConnection | team | team_id | Team | No |
| GitHubConnection | createdBy | created_by | User | No |
| JiraConnection | team | team_id | Team | No |
| JiraConnection | createdBy | created_by | User | No |
| BugInvestigation | job | job_id | QaJob | No |
| ComplianceItem | job | job_id | QaJob | No |
| ComplianceItem | spec | spec_id | Specification | Yes |
| Specification | job | job_id | QaJob | No |
| DependencyScan | project | project_id | Project | No |
| DependencyScan | job | job_id | QaJob | Yes |
| DependencyVulnerability | scan | scan_id | DependencyScan | No |
| TechDebtItem | project | project_id | Project | No |
| TechDebtItem | firstDetectedJob | first_detected_job_id | QaJob | Yes |
| TechDebtItem | resolvedJob | resolved_job_id | QaJob | Yes |
| HealthSchedule | project | project_id | Project | No |
| HealthSchedule | createdBy | created_by | User | No |
| HealthSnapshot | project | project_id | Project | No |
| HealthSnapshot | job | job_id | QaJob | Yes |
| NotificationPreference | user | user_id | User | No |

**`@ManyToMany` relationships:**

| Source Entity | Field | Join Table | Source FK | Target FK | Target Entity |
|---|---|---|---|---|---|
| RemediationTask | findings | remediation_task_findings | task_id | finding_id | Finding |

**No `@OneToMany` relationships are explicitly declared on any entity.** All relationships are navigated from the child side via `@ManyToOne`.

---

## Section 7: Enum Definitions

All enum source files are located under:
`/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/entity/enums/`

---

### === AgentResult.java ===

**Values:** `PASS`, `WARN`, `FAIL`

**Used By:** `AgentRun.result`

---

### === AgentStatus.java ===

**Values:** `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`

**Used By:** `AgentRun.status`

---

### === AgentType.java ===

**Values:** `SECURITY`, `CODE_QUALITY`, `BUILD_HEALTH`, `COMPLETENESS`, `API_CONTRACT`, `TEST_COVERAGE`, `UI_UX`, `DOCUMENTATION`, `DATABASE`, `PERFORMANCE`, `DEPENDENCY`, `ARCHITECTURE`

**Used By:** `AgentRun.agentType`, `Finding.agentType`, `Persona.agentType`, `ComplianceItem.agentType`

---

### === BusinessImpact.java ===

**Values:** `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

**Used By:** `TechDebtItem.businessImpact`

---

### === ComplianceStatus.java ===

**Values:** `MET`, `PARTIAL`, `MISSING`, `NOT_APPLICABLE`

**Used By:** `ComplianceItem.status`

---

### === DebtCategory.java ===

**Values:** `ARCHITECTURE`, `CODE`, `TEST`, `DEPENDENCY`, `DOCUMENTATION`

**Used By:** `TechDebtItem.category`, `Finding.debtCategory`

---

### === DebtStatus.java ===

**Values:** `IDENTIFIED`, `PLANNED`, `IN_PROGRESS`, `RESOLVED`

**Used By:** `TechDebtItem.status`

---

### === DirectiveCategory.java ===

**Values:** `ARCHITECTURE`, `STANDARDS`, `CONVENTIONS`, `CONTEXT`, `OTHER`

**Used By:** `Directive.category`

---

### === DirectiveScope.java ===

**Values:** `TEAM`, `PROJECT`, `USER`

**Used By:** `Directive.scope`

---

### === Effort.java ===

**Values:** `S`, `M`, `L`, `XL`

**Used By:** `Finding.effortEstimate`, `TechDebtItem.effortEstimate`

---

### === FindingStatus.java ===

**Values:** `OPEN`, `ACKNOWLEDGED`, `FALSE_POSITIVE`, `FIXED`, `WONT_FIX`

**Used By:** `Finding.status`

---

### === GitHubAuthType.java ===

**Values:** `PAT`, `OAUTH`, `SSH`

**Used By:** `GitHubConnection.authType`

---

### === InvitationStatus.java ===

**Values:** `PENDING`, `ACCEPTED`, `EXPIRED`

**Used By:** `Invitation.status`

---

### === JobMode.java ===

**Values:** `AUDIT`, `COMPLIANCE`, `BUG_INVESTIGATE`, `REMEDIATE`, `TECH_DEBT`, `DEPENDENCY`, `HEALTH_MONITOR`

**Used By:** `QaJob.mode`

---

### === JobResult.java ===

**Values:** `PASS`, `WARN`, `FAIL`

**Used By:** `QaJob.overallResult`

---

### === JobStatus.java ===

**Values:** `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`

**Used By:** `QaJob.status`

---

### === Priority.java ===

**Values:** `P0`, `P1`, `P2`, `P3`

**Used By:** `RemediationTask.priority`

---

### === ScheduleType.java ===

**Values:** `DAILY`, `WEEKLY`, `ON_COMMIT`

**Used By:** `HealthSchedule.scheduleType`

---

### === Scope.java ===

**Values:** `SYSTEM`, `TEAM`, `USER`

**Used By:** `Persona.scope`

---

### === Severity.java ===

**Values:** `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`

**Used By:** `Finding.severity`, `DependencyVulnerability.severity`

---

### === SpecType.java ===

**Values:** `OPENAPI`, `MARKDOWN`, `SCREENSHOT`, `FIGMA`

**Used By:** `Specification.specType`

---

### === TaskStatus.java ===

**Values:** `PENDING`, `ASSIGNED`, `EXPORTED`, `JIRA_CREATED`, `COMPLETED`

**Used By:** `RemediationTask.status`

---

### === TeamRole.java ===

**Values:** `OWNER`, `ADMIN`, `MEMBER`, `VIEWER`

**Used By:** `TeamMember.role`, `Invitation.role`

---

### === VulnerabilityStatus.java ===

**Values:** `OPEN`, `UPDATING`, `SUPPRESSED`, `RESOLVED`

**Used By:** `DependencyVulnerability.status`

---

## Section 8: Repository Layer

All repository interfaces are located under:
`/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/repository/`

---

### === UserRepository.java ===

**Extends:** `JpaRepository<User, UUID>`
**Entity:** User

**Custom/Derived Query Methods:**
- `Optional<User> findByEmail(String email)`
- `boolean existsByEmail(String email)`
- `List<User> findByDisplayNameContainingIgnoreCase(String search)`
- `long countByIsActiveTrue()`

---

### === TeamRepository.java ===

**Extends:** `JpaRepository<Team, UUID>`
**Entity:** Team

**Custom/Derived Query Methods:**
- `List<Team> findByOwnerId(UUID ownerId)`

---

### === TeamMemberRepository.java ===

**Extends:** `JpaRepository<TeamMember, UUID>`
**Entity:** TeamMember

**Custom/Derived Query Methods:**
- `List<TeamMember> findByTeamId(UUID teamId)`
- `List<TeamMember> findByUserId(UUID userId)`
- `Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId)`
- `boolean existsByTeamIdAndUserId(UUID teamId, UUID userId)`
- `long countByTeamId(UUID teamId)`
- `void deleteByTeamIdAndUserId(UUID teamId, UUID userId)`

---

### === ProjectRepository.java ===

**Extends:** `JpaRepository<Project, UUID>`
**Entity:** Project

**Custom/Derived Query Methods:**
- `List<Project> findByTeamIdAndIsArchivedFalse(UUID teamId)`
- `List<Project> findByTeamId(UUID teamId)`
- `Page<Project> findByTeamId(UUID teamId, Pageable pageable)`
- `Page<Project> findByTeamIdAndIsArchivedFalse(UUID teamId, Pageable pageable)`
- `Optional<Project> findByTeamIdAndRepoFullName(UUID teamId, String repoFullName)`
- `long countByTeamId(UUID teamId)`

---

### === QaJobRepository.java ===

**Extends:** `JpaRepository<QaJob, UUID>`
**Entity:** QaJob

**Custom/Derived Query Methods:**
- `List<QaJob> findByProjectIdOrderByCreatedAtDesc(UUID projectId)`
- `List<QaJob> findByProjectIdAndMode(UUID projectId, JobMode mode)`
- `List<QaJob> findByStartedById(UUID userId)`
- `Page<QaJob> findByStartedById(UUID userId, Pageable pageable)`
- `Page<QaJob> findByProjectId(UUID projectId, Pageable pageable)`
- `long countByProjectIdAndStatus(UUID projectId, JobStatus status)`

---

### === AgentRunRepository.java ===

**Extends:** `JpaRepository<AgentRun, UUID>`
**Entity:** AgentRun

**Custom/Derived Query Methods:**
- `List<AgentRun> findByJobId(UUID jobId)`
- `List<AgentRun> findByJobIdAndStatus(UUID jobId, AgentStatus status)`
- `Optional<AgentRun> findByJobIdAndAgentType(UUID jobId, AgentType agentType)`

---

### === FindingRepository.java ===

**Extends:** `JpaRepository<Finding, UUID>`
**Entity:** Finding

**Custom/Derived Query Methods:**
- `List<Finding> findByJobId(UUID jobId)`
- `List<Finding> findByJobIdAndAgentType(UUID jobId, AgentType agentType)`
- `List<Finding> findByJobIdAndSeverity(UUID jobId, Severity severity)`
- `List<Finding> findByJobIdAndStatus(UUID jobId, FindingStatus status)`
- `Page<Finding> findByJobId(UUID jobId, Pageable pageable)`
- `Page<Finding> findByJobIdAndSeverity(UUID jobId, Severity severity, Pageable pageable)`
- `Page<Finding> findByJobIdAndAgentType(UUID jobId, AgentType agentType, Pageable pageable)`
- `Page<Finding> findByJobIdAndStatus(UUID jobId, FindingStatus status, Pageable pageable)`
- `long countByJobIdAndSeverity(UUID jobId, Severity severity)`
- `long countByJobIdAndSeverityAndStatus(UUID jobId, Severity severity, FindingStatus status)`

---

### === RemediationTaskRepository.java ===

**Extends:** `JpaRepository<RemediationTask, UUID>`
**Entity:** RemediationTask

**Custom/Derived Query Methods:**
- `List<RemediationTask> findByJobIdOrderByTaskNumberAsc(UUID jobId)`
- `Page<RemediationTask> findByJobId(UUID jobId, Pageable pageable)`
- `List<RemediationTask> findByAssignedToId(UUID userId)`
- `Page<RemediationTask> findByAssignedToId(UUID userId, Pageable pageable)`

---

### === DirectiveRepository.java ===

**Extends:** `JpaRepository<Directive, UUID>`
**Entity:** Directive

**Custom/Derived Query Methods:**
- `List<Directive> findByTeamId(UUID teamId)`
- `List<Directive> findByProjectId(UUID projectId)`
- `List<Directive> findByTeamIdAndScope(UUID teamId, DirectiveScope scope)`

---

### === ProjectDirectiveRepository.java ===

**Extends:** `JpaRepository<ProjectDirective, ProjectDirectiveId>`
**Entity:** ProjectDirective

**Custom/Derived Query Methods:**
- `List<ProjectDirective> findByProjectId(UUID projectId)`
- `List<ProjectDirective> findByProjectIdAndEnabledTrue(UUID projectId)`
- `List<ProjectDirective> findByDirectiveId(UUID directiveId)`
- `void deleteByProjectIdAndDirectiveId(UUID projectId, UUID directiveId)`

---

### === PersonaRepository.java ===

**Extends:** `JpaRepository<Persona, UUID>`
**Entity:** Persona

**Custom/Derived Query Methods:**
- `List<Persona> findByTeamId(UUID teamId)`
- `Page<Persona> findByTeamId(UUID teamId, Pageable pageable)`
- `List<Persona> findByScope(Scope scope)`
- `List<Persona> findByTeamIdAndAgentType(UUID teamId, AgentType agentType)`
- `Optional<Persona> findByTeamIdAndAgentTypeAndIsDefaultTrue(UUID teamId, AgentType agentType)`
- `List<Persona> findByCreatedById(UUID userId)`

---

### === InvitationRepository.java ===

**Extends:** `JpaRepository<Invitation, UUID>`
**Entity:** Invitation

**Custom/Derived Query Methods:**
- `Optional<Invitation> findByToken(String token)`
- `List<Invitation> findByTeamIdAndStatus(UUID teamId, InvitationStatus status)`
- `List<Invitation> findByEmailAndStatus(String email, InvitationStatus status)`
- `List<Invitation> findByTeamIdAndEmailAndStatusForUpdate(UUID teamId, String email, InvitationStatus status)` [@Lock(LockModeType.PESSIMISTIC_WRITE), @Query("SELECT i FROM Invitation i WHERE i.team.id = :teamId AND i.email = :email AND i.status = :status")]

---

### === AuditLogRepository.java ===

**Extends:** `JpaRepository<AuditLog, Long>`
**Entity:** AuditLog

**Custom/Derived Query Methods:**
- `Page<AuditLog> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable)`
- `Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable)`
- `List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId)`

---

### === SystemSettingRepository.java ===

**Extends:** `JpaRepository<SystemSetting, String>`
**Entity:** SystemSetting

**Custom/Derived Query Methods:** None (inherits standard CRUD from JpaRepository)

---

### === GitHubConnectionRepository.java ===

**Extends:** `JpaRepository<GitHubConnection, UUID>`
**Entity:** GitHubConnection

**Custom/Derived Query Methods:**
- `List<GitHubConnection> findByTeamIdAndIsActiveTrue(UUID teamId)`

---

### === JiraConnectionRepository.java ===

**Extends:** `JpaRepository<JiraConnection, UUID>`
**Entity:** JiraConnection

**Custom/Derived Query Methods:**
- `List<JiraConnection> findByTeamIdAndIsActiveTrue(UUID teamId)`

---

### === BugInvestigationRepository.java ===

**Extends:** `JpaRepository<BugInvestigation, UUID>`
**Entity:** BugInvestigation

**Custom/Derived Query Methods:**
- `Optional<BugInvestigation> findByJobId(UUID jobId)`
- `Optional<BugInvestigation> findByJiraKey(String jiraKey)`

---

### === ComplianceItemRepository.java ===

**Extends:** `JpaRepository<ComplianceItem, UUID>`
**Entity:** ComplianceItem

**Custom/Derived Query Methods:**
- `List<ComplianceItem> findByJobId(UUID jobId)`
- `Page<ComplianceItem> findByJobId(UUID jobId, Pageable pageable)`
- `List<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status)`
- `Page<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status, Pageable pageable)`

---

### === SpecificationRepository.java ===

**Extends:** `JpaRepository<Specification, UUID>`
**Entity:** Specification

**Custom/Derived Query Methods:**
- `List<Specification> findByJobId(UUID jobId)`
- `Page<Specification> findByJobId(UUID jobId, Pageable pageable)`

---

### === DependencyScanRepository.java ===

**Extends:** `JpaRepository<DependencyScan, UUID>`
**Entity:** DependencyScan

**Custom/Derived Query Methods:**
- `List<DependencyScan> findByProjectIdOrderByCreatedAtDesc(UUID projectId)`
- `Page<DependencyScan> findByProjectId(UUID projectId, Pageable pageable)`
- `Optional<DependencyScan> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId)`

---

### === DependencyVulnerabilityRepository.java ===

**Extends:** `JpaRepository<DependencyVulnerability, UUID>`
**Entity:** DependencyVulnerability

**Custom/Derived Query Methods:**
- `List<DependencyVulnerability> findByScanId(UUID scanId)`
- `Page<DependencyVulnerability> findByScanId(UUID scanId, Pageable pageable)`
- `List<DependencyVulnerability> findByScanIdAndStatus(UUID scanId, VulnerabilityStatus status)`
- `Page<DependencyVulnerability> findByScanIdAndStatus(UUID scanId, VulnerabilityStatus status, Pageable pageable)`
- `List<DependencyVulnerability> findByScanIdAndSeverity(UUID scanId, Severity severity)`
- `Page<DependencyVulnerability> findByScanIdAndSeverity(UUID scanId, Severity severity, Pageable pageable)`
- `long countByScanIdAndStatus(UUID scanId, VulnerabilityStatus status)`

---

### === TechDebtItemRepository.java ===

**Extends:** `JpaRepository<TechDebtItem, UUID>`
**Entity:** TechDebtItem

**Custom/Derived Query Methods:**
- `List<TechDebtItem> findByProjectId(UUID projectId)`
- `Page<TechDebtItem> findByProjectId(UUID projectId, Pageable pageable)`
- `List<TechDebtItem> findByProjectIdAndStatus(UUID projectId, DebtStatus status)`
- `Page<TechDebtItem> findByProjectIdAndStatus(UUID projectId, DebtStatus status, Pageable pageable)`
- `List<TechDebtItem> findByProjectIdAndCategory(UUID projectId, DebtCategory category)`
- `Page<TechDebtItem> findByProjectIdAndCategory(UUID projectId, DebtCategory category, Pageable pageable)`
- `long countByProjectIdAndStatus(UUID projectId, DebtStatus status)`

---

### === HealthScheduleRepository.java ===

**Extends:** `JpaRepository<HealthSchedule, UUID>`
**Entity:** HealthSchedule

**Custom/Derived Query Methods:**
- `List<HealthSchedule> findByProjectId(UUID projectId)`
- `List<HealthSchedule> findByIsActiveTrue()`

---

### === HealthSnapshotRepository.java ===

**Extends:** `JpaRepository<HealthSnapshot, UUID>`
**Entity:** HealthSnapshot

**Custom/Derived Query Methods:**
- `List<HealthSnapshot> findByProjectIdOrderByCapturedAtDesc(UUID projectId)`
- `Page<HealthSnapshot> findByProjectId(UUID projectId, Pageable pageable)`
- `Optional<HealthSnapshot> findFirstByProjectIdOrderByCapturedAtDesc(UUID projectId)`

---

### === NotificationPreferenceRepository.java ===

**Extends:** `JpaRepository<NotificationPreference, UUID>`
**Entity:** NotificationPreference

**Custom/Derived Query Methods:**
- `List<NotificationPreference> findByUserId(UUID userId)`
- `Optional<NotificationPreference> findByUserIdAndEventType(UUID userId, String eventType)`

---

### Repository Summary

| Repository | Entity | PK Type | Custom Method Count |
|---|---|---|---|
| UserRepository | User | UUID | 4 |
| TeamRepository | Team | UUID | 1 |
| TeamMemberRepository | TeamMember | UUID | 6 |
| ProjectRepository | Project | UUID | 6 |
| QaJobRepository | QaJob | UUID | 6 |
| AgentRunRepository | AgentRun | UUID | 3 |
| FindingRepository | Finding | UUID | 10 |
| RemediationTaskRepository | RemediationTask | UUID | 4 |
| DirectiveRepository | Directive | UUID | 3 |
| ProjectDirectiveRepository | ProjectDirective | ProjectDirectiveId | 4 |
| PersonaRepository | Persona | UUID | 6 |
| InvitationRepository | Invitation | UUID | 4 |
| AuditLogRepository | AuditLog | Long | 3 |
| SystemSettingRepository | SystemSetting | String | 0 |
| GitHubConnectionRepository | GitHubConnection | UUID | 1 |
| JiraConnectionRepository | JiraConnection | UUID | 1 |
| BugInvestigationRepository | BugInvestigation | UUID | 2 |
| ComplianceItemRepository | ComplianceItem | UUID | 4 |
| SpecificationRepository | Specification | UUID | 2 |
| DependencyScanRepository | DependencyScan | UUID | 3 |
| DependencyVulnerabilityRepository | DependencyVulnerability | UUID | 7 |
| TechDebtItemRepository | TechDebtItem | UUID | 7 |
| HealthScheduleRepository | HealthSchedule | UUID | 2 |
| HealthSnapshotRepository | HealthSnapshot | UUID | 3 |
| NotificationPreferenceRepository | NotificationPreference | UUID | 2 |

**Total: 25 repositories, 87 custom/derived query methods**

**Notable patterns:**
- Only `InvitationRepository` uses an explicit `@Query` annotation (with `@Lock(PESSIMISTIC_WRITE)` for concurrent invitation creation)
- All other custom methods use Spring Data derived query method naming conventions
- Most repositories provide both `List` and `Page` return-type overloads for the same query
- `SystemSettingRepository` has zero custom methods (relies entirely on inherited JpaRepository CRUD)
- `FindingRepository` has the most custom methods (10), covering filtering by job, agent type, severity, and status


---

## Section 9: DTO Layer

### Overview

All DTOs in CodeOps-Server are implemented as **Java records** (immutable, no-arg canonical constructor). Request DTOs use Jakarta Validation annotations for input validation. Response DTOs are plain records with no validation annotations.

- **Request DTOs**: 41 records in `com.codeops.dto.request`
- **Response DTOs**: 31 records in `com.codeops.dto.response`

Base path: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/dto/`

---

### 9.1 Request DTOs

File path prefix: `src/main/java/com/codeops/dto/request/`

---

### === AdminUpdateUserRequest.java ===
Type: Request
Fields:
  - `isActive`: `Boolean` (no validation)
Notes: Record. Single optional field. Used by admin endpoints to activate/deactivate users. No `@NotNull` -- allows partial update (null means "don't change").

---

### === AssignDirectiveRequest.java ===
Type: Request
Fields:
  - `projectId`: `UUID` [`@NotNull`]
  - `directiveId`: `UUID` [`@NotNull`]
  - `enabled`: `boolean` (primitive, no validation)
Notes: Record. The `enabled` field is a primitive `boolean` (defaults to `false` if not provided in JSON, not nullable). Used to assign/enable/disable a directive on a project.

---

### === BulkUpdateFindingsRequest.java ===
Type: Request
Fields:
  - `findingIds`: `List<UUID>` [`@NotEmpty`, `@Size(max = 100)`]
  - `status`: `FindingStatus` [`@NotNull`]
Notes: Record. Allows bulk status update for up to 100 findings at once. References enum `FindingStatus`.

---

### === ChangePasswordRequest.java ===
Type: Request
Fields:
  - `currentPassword`: `String` [`@NotBlank`]
  - `newPassword`: `String` [`@NotBlank`, `@Size(min = 8)`]
Notes: Record. Minimum 8-character new password. No maximum length constraint on either field (potential concern -- could accept extremely long strings).

---

### === CreateAgentRunRequest.java ===
Type: Request
Fields:
  - `jobId`: `UUID` [`@NotNull`]
  - `agentType`: `AgentType` [`@NotNull`]
Notes: Record. Minimal creation request -- just links an agent type to a job.

---

### === CreateBugInvestigationRequest.java ===
Type: Request
Fields:
  - `jobId`: `UUID` [`@NotNull`]
  - `jiraKey`: `String` [`@Size(max = 200)`]
  - `jiraSummary`: `String` [`@Size(max = 500)`]
  - `jiraDescription`: `String` [`@Size(max = 50000)`]
  - `jiraCommentsJson`: `String` [`@Size(max = 50000)`]
  - `jiraAttachmentsJson`: `String` [`@Size(max = 50000)`]
  - `jiraLinkedIssues`: `String` [`@Size(max = 50000)`]
  - `additionalContext`: `String` [`@Size(max = 50000)`]
Notes: Record. Only `jobId` is required. All Jira fields are optional and stored as raw strings (JSON fields stored as String, not parsed/validated as JSON). Five fields allow up to 50KB each.

---

### === CreateComplianceItemRequest.java ===
Type: Request
Fields:
  - `jobId`: `UUID` [`@NotNull`]
  - `requirement`: `String` [`@NotBlank`, `@Size(max = 5000)`]
  - `specId`: `UUID` (no validation)
  - `status`: `ComplianceStatus` [`@NotNull`]
  - `evidence`: `String` [`@Size(max = 50000)`]
  - `agentType`: `AgentType` (no validation)
  - `notes`: `String` [`@Size(max = 5000)`]
Notes: Record. References enums `ComplianceStatus` and `AgentType`. The `specId` is optional (links to a specification).

---

### === CreateDependencyScanRequest.java ===
Type: Request
Fields:
  - `projectId`: `UUID` [`@NotNull`]
  - `jobId`: `UUID` (no validation)
  - `manifestFile`: `String` [`@Size(max = 200)`]
  - `totalDependencies`: `Integer` (no validation)
  - `outdatedCount`: `Integer` (no validation)
  - `vulnerableCount`: `Integer` (no validation)
  - `scanDataJson`: `String` [`@Size(max = 50000)`]
Notes: Record. Only `projectId` is required. `jobId` is optional (scan may not be part of a job). Numeric fields have no min/max validation (could accept negative values).

---

### === CreateDirectiveRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@NotBlank`, `@Size(max = 200)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `contentMd`: `String` [`@NotBlank`, `@Size(max = 50000)`]
  - `category`: `DirectiveCategory` (no validation)
  - `scope`: `DirectiveScope` [`@NotNull`]
  - `teamId`: `UUID` (no validation)
  - `projectId`: `UUID` (no validation)
Notes: Record. `scope` is required (GLOBAL, TEAM, or PROJECT). `category` is optional. `teamId` and `projectId` are conditionally required based on scope but not enforced at the DTO level.

---

### === CreateFindingRequest.java ===
Type: Request
Fields:
  - `jobId`: `UUID` [`@NotNull`]
  - `agentType`: `AgentType` [`@NotNull`]
  - `severity`: `Severity` [`@NotNull`]
  - `title`: `String` [`@NotBlank`, `@Size(max = 500)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `filePath`: `String` [`@Size(max = 1000)`]
  - `lineNumber`: `Integer` (no validation)
  - `recommendation`: `String` [`@Size(max = 5000)`]
  - `evidence`: `String` [`@Size(max = 50000)`]
  - `effortEstimate`: `Effort` (no validation)
  - `debtCategory`: `DebtCategory` (no validation)
Notes: Record. References enums `AgentType`, `Severity`, `Effort`, `DebtCategory`. `lineNumber` has no min/max validation.

---

### === CreateGitHubConnectionRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@NotBlank`, `@Size(max = 100)`]
  - `authType`: `GitHubAuthType` [`@NotNull`]
  - `credentials`: `String` [`@NotBlank`, `@Size(max = 10000)`]
  - `githubUsername`: `String` [`@Size(max = 200)`]
Notes: Record. `credentials` holds PAT or app key (encrypted by service layer before storage). Up to 10KB credentials field.

---

### === CreateHealthScheduleRequest.java ===
Type: Request
Fields:
  - `projectId`: `UUID` [`@NotNull`]
  - `scheduleType`: `ScheduleType` [`@NotNull`]
  - `cronExpression`: `String` [`@Size(max = 200)`]
  - `agentTypes`: `List<AgentType>` [`@NotEmpty`, `@Size(max = 20)`]
Notes: Record. Requires at least one agent type, max 20. `cronExpression` is optional (used when `scheduleType` is CRON). No format validation on cron expression at DTO level.

---

### === CreateHealthSnapshotRequest.java ===
Type: Request
Fields:
  - `projectId`: `UUID` [`@NotNull`]
  - `jobId`: `UUID` (no validation)
  - `healthScore`: `Integer` [`@NotNull`]
  - `findingsBySeverity`: `String` [`@Size(max = 5000)`]
  - `techDebtScore`: `Integer` (no validation)
  - `dependencyScore`: `Integer` (no validation)
  - `testCoveragePercent`: `BigDecimal` (no validation)
Notes: Record. `healthScore` is required but has no `@Min`/`@Max` validation (convention is 0-100 but not enforced at DTO level). `testCoveragePercent` has no range constraint.

---

### === CreateJiraConnectionRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@NotBlank`, `@Size(max = 100)`]
  - `instanceUrl`: `String` [`@NotBlank`, `@Size(max = 500)`]
  - `email`: `String` [`@NotBlank`, `@Email`]
  - `apiToken`: `String` [`@NotBlank`, `@Size(max = 10000)`]
Notes: Record. All fields required. `email` has both `@NotBlank` and `@Email` validation. `instanceUrl` has no `@URL` annotation (not validated as a proper URL). `apiToken` encrypted by service layer.

---

### === CreateJobRequest.java ===
Type: Request
Fields:
  - `projectId`: `UUID` [`@NotNull`]
  - `mode`: `JobMode` [`@NotNull`]
  - `name`: `String` [`@Size(max = 200)`]
  - `branch`: `String` [`@Size(max = 200)`]
  - `configJson`: `String` [`@Size(max = 50000)`]
  - `jiraTicketKey`: `String` [`@Size(max = 200)`]
Notes: Record. Only `projectId` and `mode` are required. `configJson` stored as raw string. `name` is optional (may be auto-generated).

---

### === CreatePersonaRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@NotBlank`, `@Size(max = 100)`]
  - `agentType`: `AgentType` (no validation)
  - `description`: `String` [`@Size(max = 5000)`]
  - `contentMd`: `String` [`@NotBlank`, `@Size(max = 50000)`]
  - `scope`: `Scope` [`@NotNull`]
  - `teamId`: `UUID` (no validation)
  - `isDefault`: `Boolean` (no validation)
Notes: Record. `scope` uses `Scope` enum (different from `DirectiveScope` used in directives). `teamId` conditionally required for TEAM scope but not enforced at DTO level. `isDefault` is nullable Boolean.

---

### === CreateProjectRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@NotBlank`, `@Size(max = 200)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `githubConnectionId`: `UUID` (no validation)
  - `repoUrl`: `String` [`@Size(max = 2000)`]
  - `repoFullName`: `String` [`@Size(max = 200)`]
  - `defaultBranch`: `String` [`@Size(max = 200)`]
  - `jiraConnectionId`: `UUID` (no validation)
  - `jiraProjectKey`: `String` [`@Size(max = 50)`]
  - `jiraDefaultIssueType`: `String` [`@Size(max = 200)`]
  - `jiraLabels`: `List<@Size(max = 100) String>` [`@Size(max = 100)`]
  - `jiraComponent`: `String` [`@Size(max = 200)`]
  - `techStack`: `String` [`@Size(max = 5000)`]
Notes: Record. Only `name` is required. `jiraLabels` has nested element-level `@Size(max = 100)` validation on each string plus list-level `@Size(max = 100)` (max 100 labels, each max 100 chars). `repoUrl` has no `@URL` annotation.

---

### === CreateSpecificationRequest.java ===
Type: Request
Fields:
  - `jobId`: `UUID` [`@NotNull`]
  - `name`: `String` [`@NotBlank`, `@Size(max = 200)`]
  - `specType`: `SpecType` (no validation -- optional enum)
  - `s3Key`: `String` [`@NotBlank`, `@Size(max = 1000)`]
Notes: Record. Links a specification document (stored in S3/local filesystem) to a job.

---

### === CreateTaskRequest.java ===
Type: Request
Fields:
  - `jobId`: `UUID` [`@NotNull`]
  - `taskNumber`: `Integer` [`@NotNull`]
  - `title`: `String` [`@NotBlank`, `@Size(max = 500)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `promptMd`: `String` [`@Size(max = 50000)`]
  - `promptS3Key`: `String` [`@Size(max = 1000)`]
  - `findingIds`: `List<UUID>` [`@Size(max = 100)`]
  - `priority`: `Priority` (no validation)
Notes: Record. `taskNumber` is required but has no `@Min` constraint. `findingIds` limited to 100 entries. Both `promptMd` (inline) and `promptS3Key` (S3 reference) are optional -- presumably one or the other is used.

---

### === CreateTeamRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@NotBlank`, `@Size(max = 100)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `teamsWebhookUrl`: `String` [`@Size(max = 500)`]
Notes: Record. Only `name` is required. `teamsWebhookUrl` has no URL format validation.

---

### === CreateTechDebtItemRequest.java ===
Type: Request
Fields:
  - `projectId`: `UUID` [`@NotNull`]
  - `category`: `DebtCategory` [`@NotNull`]
  - `title`: `String` [`@NotBlank`, `@Size(max = 500)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `filePath`: `String` [`@Size(max = 1000)`]
  - `effortEstimate`: `Effort` (no validation)
  - `businessImpact`: `BusinessImpact` (no validation)
  - `firstDetectedJobId`: `UUID` (no validation)
Notes: Record. References enums `DebtCategory`, `Effort`, `BusinessImpact`. Only `projectId`, `category`, and `title` are required.

---

### === CreateVulnerabilityRequest.java ===
Type: Request
Fields:
  - `scanId`: `UUID` [`@NotNull`]
  - `dependencyName`: `String` [`@NotBlank`, `@Size(max = 200)`]
  - `currentVersion`: `String` [`@Size(max = 50)`]
  - `fixedVersion`: `String` [`@Size(max = 50)`]
  - `cveId`: `String` [`@Size(max = 50)`]
  - `severity`: `Severity` [`@NotNull`]
  - `description`: `String` [`@Size(max = 5000)`]
Notes: Record. Requires `scanId`, `dependencyName`, and `severity`. `cveId` is optional (not all vulnerabilities have a CVE). No pattern validation on `cveId` format (e.g., CVE-YYYY-NNNN).

---

### === InviteMemberRequest.java ===
Type: Request
Fields:
  - `email`: `String` [`@NotBlank`, `@Email`]
  - `role`: `TeamRole` [`@NotNull`]
Notes: Record. Both fields required. References enum `TeamRole`.

---

### === LoginRequest.java ===
Type: Request
Fields:
  - `email`: `String` [`@NotBlank`, `@Email`]
  - `password`: `String` [`@NotBlank`]
Notes: Record. Standard login DTO. No `@Size` on password (accepts any length for login attempt).

---

### === PasswordResetRequest.java ===
Type: Request
Fields:
  - `email`: `String` [`@NotBlank`, `@Email`]
Notes: Record. Single-field DTO for initiating password reset.

---

### === RefreshTokenRequest.java ===
Type: Request
Fields:
  - `refreshToken`: `String` [`@NotBlank`]
Notes: Record. Single-field DTO. No `@Size` constraint on token length.

---

### === RegisterRequest.java ===
Type: Request
Fields:
  - `email`: `String` [`@NotBlank`, `@Email`]
  - `password`: `String` [`@NotBlank`, `@Size(min = 8)`]
  - `displayName`: `String` [`@NotBlank`, `@Size(max = 100)`]
Notes: Record. All fields required. Password requires minimum 8 characters (no max -- same note as ChangePasswordRequest). `displayName` has max 100 chars.

---

### === UpdateAgentRunRequest.java ===
Type: Request
Fields:
  - `status`: `AgentStatus` (no validation)
  - `result`: `AgentResult` (no validation)
  - `reportS3Key`: `String` [`@Size(max = 1000)`]
  - `score`: `Integer` (no validation)
  - `findingsCount`: `Integer` (no validation)
  - `criticalCount`: `Integer` (no validation)
  - `highCount`: `Integer` (no validation)
  - `completedAt`: `Instant` (no validation)
  - `startedAt`: `Instant` (no validation)
Notes: Record. All fields optional (partial update pattern). No `@NotNull` on any field. Numeric fields have no min/max constraints.

---

### === UpdateBugInvestigationRequest.java ===
Type: Request
Fields:
  - `rcaMd`: `String` [`@Size(max = 50000)`]
  - `impactAssessmentMd`: `String` [`@Size(max = 50000)`]
  - `rcaS3Key`: `String` [`@Size(max = 1000)`]
  - `rcaPostedToJira`: `Boolean` (no validation)
  - `fixTasksCreatedInJira`: `Boolean` (no validation)
Notes: Record. All fields optional (partial update). RCA = Root Cause Analysis.

---

### === UpdateDirectiveRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@Size(max = 200)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `contentMd`: `String` [`@Size(max = 50000)`]
  - `category`: `DirectiveCategory` (no validation)
Notes: Record. All fields optional (partial update). Does not allow updating `scope`, `teamId`, or `projectId` (those are immutable after creation).

---

### === UpdateFindingStatusRequest.java ===
Type: Request
Fields:
  - `status`: `FindingStatus` [`@NotNull`]
Notes: Record. Single required field. Used for individual finding status updates (vs. `BulkUpdateFindingsRequest` for bulk).

---

### === UpdateJobRequest.java ===
Type: Request
Fields:
  - `status`: `JobStatus` (no validation)
  - `summaryMd`: `String` [`@Size(max = 50000)`]
  - `overallResult`: `JobResult` (no validation)
  - `healthScore`: `Integer` (no validation)
  - `totalFindings`: `Integer` (no validation)
  - `criticalCount`: `Integer` (no validation)
  - `highCount`: `Integer` (no validation)
  - `mediumCount`: `Integer` (no validation)
  - `lowCount`: `Integer` (no validation)
  - `completedAt`: `Instant` (no validation)
  - `startedAt`: `Instant` (no validation)
Notes: Record. All fields optional (partial update). Heavy use for agent callbacks that progressively update job state and metrics. No range constraints on numeric fields.

---

### === UpdateMemberRoleRequest.java ===
Type: Request
Fields:
  - `role`: `TeamRole` [`@NotNull`]
Notes: Record. Single required field. References enum `TeamRole`.

---

### === UpdateNotificationPreferenceRequest.java ===
Type: Request
Fields:
  - `eventType`: `String` [`@NotBlank`, `@Size(max = 200)`]
  - `inApp`: `boolean` (primitive, no validation)
  - `email`: `boolean` (primitive, no validation)
Notes: Record. `eventType` is a free-form string (not an enum). `inApp` and `email` are primitive booleans (default to `false` if not provided).

---

### === UpdatePersonaRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@Size(max = 100)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `contentMd`: `String` [`@Size(max = 50000)`]
  - `isDefault`: `Boolean` (no validation)
Notes: Record. All fields optional (partial update). Cannot change `agentType` or `scope` after creation.

---

### === UpdateProjectRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@Size(max = 200)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `githubConnectionId`: `UUID` (no validation)
  - `repoUrl`: `String` [`@Size(max = 2000)`]
  - `repoFullName`: `String` [`@Size(max = 200)`]
  - `defaultBranch`: `String` [`@Size(max = 200)`]
  - `jiraConnectionId`: `UUID` (no validation)
  - `jiraProjectKey`: `String` [`@Size(max = 50)`]
  - `jiraDefaultIssueType`: `String` [`@Size(max = 200)`]
  - `jiraLabels`: `List<@Size(max = 100) String>` [`@Size(max = 100)`]
  - `jiraComponent`: `String` [`@Size(max = 200)`]
  - `techStack`: `String` [`@Size(max = 5000)`]
  - `isArchived`: `Boolean` (no validation)
Notes: Record. Mirrors `CreateProjectRequest` with all fields optional plus `isArchived`. Same nested `@Size` validation on `jiraLabels`.

---

### === UpdateSystemSettingRequest.java ===
Type: Request
Fields:
  - `key`: `String` [`@NotBlank`, `@Size(max = 200)`]
  - `value`: `String` [`@NotBlank`, `@Size(max = 5000)`]
Notes: Record. Both fields required. The `key` being in the request body is slightly unusual (typically the key is in the URL path).

---

### === UpdateTaskRequest.java ===
Type: Request
Fields:
  - `status`: `TaskStatus` (no validation)
  - `assignedTo`: `UUID` (no validation)
  - `jiraKey`: `String` [`@Size(max = 200)`]
Notes: Record. All fields optional (partial update). Can change status, assign to a user, or link to a Jira ticket.

---

### === UpdateTeamRequest.java ===
Type: Request
Fields:
  - `name`: `String` [`@Size(max = 100)`]
  - `description`: `String` [`@Size(max = 5000)`]
  - `teamsWebhookUrl`: `String` [`@Size(max = 500)`]
Notes: Record. All fields optional (partial update). Mirrors `CreateTeamRequest` without `@NotBlank` on `name`.

---

### === UpdateTechDebtStatusRequest.java ===
Type: Request
Fields:
  - `status`: `DebtStatus` [`@NotNull`]
  - `resolvedJobId`: `UUID` (no validation)
Notes: Record. `status` is required. `resolvedJobId` is optional (provided when marking as resolved).

---

### === UpdateUserRequest.java ===
Type: Request
Fields:
  - `displayName`: `String` [`@Size(max = 100)`]
  - `avatarUrl`: `String` [`@Size(max = 500)`]
Notes: Record. Both fields optional (partial update). Self-service profile update (vs. `AdminUpdateUserRequest` for admin actions). No URL validation on `avatarUrl`.

---

### 9.2 Response DTOs

File path prefix: `src/main/java/com/codeops/dto/response/`

---

### === AgentRunResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `jobId`: `UUID`
  - `agentType`: `AgentType`
  - `status`: `AgentStatus`
  - `result`: `AgentResult`
  - `reportS3Key`: `String`
  - `score`: `Integer`
  - `findingsCount`: `int`
  - `criticalCount`: `int`
  - `highCount`: `int`
  - `startedAt`: `Instant`
  - `completedAt`: `Instant`
Notes: Record. No validation annotations. Mix of primitive `int` (findingsCount, criticalCount, highCount) and boxed `Integer` (score). Score is nullable (may not be computed yet).

---

### === AuditLogResponse.java ===
Type: Response
Fields:
  - `id`: `Long`
  - `userId`: `UUID`
  - `userName`: `String`
  - `teamId`: `UUID`
  - `action`: `String`
  - `entityType`: `String`
  - `entityId`: `UUID`
  - `details`: `String`
  - `ipAddress`: `String`
  - `createdAt`: `Instant`
Notes: Record. Uses `Long` PK (not UUID) -- consistent with AuditLog entity which has a Long PK. `userName` is a denormalized field for display convenience.

---

### === AuthResponse.java ===
Type: Response
Fields:
  - `token`: `String`
  - `refreshToken`: `String`
  - `user`: `UserResponse`
Notes: Record. Contains nested `UserResponse` DTO. Returned from login and register endpoints. Token field names: `token` (access token) and `refreshToken`.

---

### === BugInvestigationResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `jobId`: `UUID`
  - `jiraKey`: `String`
  - `jiraSummary`: `String`
  - `jiraDescription`: `String`
  - `additionalContext`: `String`
  - `rcaMd`: `String`
  - `impactAssessmentMd`: `String`
  - `rcaS3Key`: `String`
  - `rcaPostedToJira`: `boolean`
  - `fixTasksCreatedInJira`: `boolean`
  - `createdAt`: `Instant`
Notes: Record. Omits `jiraCommentsJson`, `jiraAttachmentsJson`, and `jiraLinkedIssues` that exist in the create request -- these are not returned in the response (stored for agent processing but not exposed).

---

### === ComplianceItemResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `jobId`: `UUID`
  - `requirement`: `String`
  - `specId`: `UUID`
  - `specName`: `String`
  - `status`: `ComplianceStatus`
  - `evidence`: `String`
  - `agentType`: `AgentType`
  - `notes`: `String`
  - `createdAt`: `Instant`
Notes: Record. Includes denormalized `specName` (resolved from the `specId` FK). References enums `ComplianceStatus` and `AgentType`.

---

### === DependencyScanResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `projectId`: `UUID`
  - `jobId`: `UUID`
  - `manifestFile`: `String`
  - `totalDependencies`: `int`
  - `outdatedCount`: `int`
  - `vulnerableCount`: `int`
  - `createdAt`: `Instant`
Notes: Record. Uses primitive `int` for counts (never null in response). Omits `scanDataJson` from the create request (large payload not returned in listings).

---

### === DirectiveResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `name`: `String`
  - `description`: `String`
  - `contentMd`: `String`
  - `category`: `DirectiveCategory`
  - `scope`: `DirectiveScope`
  - `teamId`: `UUID`
  - `projectId`: `UUID`
  - `createdBy`: `UUID`
  - `createdByName`: `String`
  - `version`: `int`
  - `createdAt`: `Instant`
  - `updatedAt`: `Instant`
Notes: Record. Includes denormalized `createdByName`. Has `version` field for optimistic concurrency / audit trail.

---

### === ErrorResponse.java ===
Type: Response
Fields:
  - `status`: `int`
  - `message`: `String`
Notes: Record. Generic error response returned by global exception handlers. Minimal fields -- HTTP status code and error message.

---

### === FindingResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `jobId`: `UUID`
  - `agentType`: `AgentType`
  - `severity`: `Severity`
  - `title`: `String`
  - `description`: `String`
  - `filePath`: `String`
  - `lineNumber`: `Integer`
  - `recommendation`: `String`
  - `evidence`: `String`
  - `effortEstimate`: `Effort`
  - `debtCategory`: `DebtCategory`
  - `status`: `FindingStatus`
  - `statusChangedBy`: `UUID`
  - `statusChangedAt`: `Instant`
  - `createdAt`: `Instant`
Notes: Record. Comprehensive finding detail. Includes status tracking fields (`statusChangedBy`, `statusChangedAt`) not present in create request. References five enums.

---

### === GitHubConnectionResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `teamId`: `UUID`
  - `name`: `String`
  - `authType`: `GitHubAuthType`
  - `githubUsername`: `String`
  - `isActive`: `boolean`
  - `createdAt`: `Instant`
Notes: Record. Correctly omits `credentials` field (encrypted PAT/key never returned in API responses per security policy).

---

### === HealthScheduleResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `projectId`: `UUID`
  - `scheduleType`: `ScheduleType`
  - `cronExpression`: `String`
  - `agentTypes`: `List<AgentType>`
  - `isActive`: `boolean`
  - `lastRunAt`: `Instant`
  - `nextRunAt`: `Instant`
  - `createdAt`: `Instant`
Notes: Record. Includes computed scheduling fields (`lastRunAt`, `nextRunAt`, `isActive`) not in create request.

---

### === HealthSnapshotResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `projectId`: `UUID`
  - `jobId`: `UUID`
  - `healthScore`: `int`
  - `findingsBySeverity`: `String`
  - `techDebtScore`: `Integer`
  - `dependencyScore`: `Integer`
  - `testCoveragePercent`: `BigDecimal`
  - `capturedAt`: `Instant`
Notes: Record. Uses primitive `int` for `healthScore` (always present) but boxed `Integer` for optional scores. `capturedAt` instead of `createdAt` (semantic naming for when the snapshot was taken).

---

### === InvitationResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `email`: `String`
  - `role`: `TeamRole`
  - `status`: `InvitationStatus`
  - `invitedByName`: `String`
  - `expiresAt`: `Instant`
  - `createdAt`: `Instant`
Notes: Record. Includes denormalized `invitedByName`. References enums `TeamRole` and `InvitationStatus`.

---

### === JiraConnectionResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `teamId`: `UUID`
  - `name`: `String`
  - `instanceUrl`: `String`
  - `email`: `String`
  - `isActive`: `boolean`
  - `createdAt`: `Instant`
Notes: Record. Correctly omits `apiToken` field (encrypted credential never returned in API responses per security policy).

---

### === JobResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `projectId`: `UUID`
  - `projectName`: `String`
  - `mode`: `JobMode`
  - `status`: `JobStatus`
  - `name`: `String`
  - `branch`: `String`
  - `configJson`: `String`
  - `summaryMd`: `String`
  - `overallResult`: `JobResult`
  - `healthScore`: `Integer`
  - `totalFindings`: `int`
  - `criticalCount`: `int`
  - `highCount`: `int`
  - `mediumCount`: `int`
  - `lowCount`: `int`
  - `jiraTicketKey`: `String`
  - `startedBy`: `UUID`
  - `startedByName`: `String`
  - `startedAt`: `Instant`
  - `completedAt`: `Instant`
  - `createdAt`: `Instant`
Notes: Record. 22 fields -- the largest response DTO. Includes denormalized `projectName` and `startedByName`. Mix of primitive `int` (counts, always present) and boxed `Integer` (healthScore, nullable). References three enums.

---

### === JobSummaryResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `projectName`: `String`
  - `mode`: `JobMode`
  - `status`: `JobStatus`
  - `name`: `String`
  - `overallResult`: `JobResult`
  - `healthScore`: `Integer`
  - `totalFindings`: `int`
  - `criticalCount`: `int`
  - `completedAt`: `Instant`
  - `createdAt`: `Instant`
Notes: Record. Lightweight version of `JobResponse` for listing views. Omits branch, configJson, summaryMd, individual severity counts (except critical), jiraTicketKey, startedBy details.

---

### === NotificationPreferenceResponse.java ===
Type: Response
Fields:
  - `eventType`: `String`
  - `inApp`: `boolean`
  - `email`: `boolean`
Notes: Record. No ID field -- identified by `eventType` string. Matches the structure of `UpdateNotificationPreferenceRequest` exactly.

---

### === PageResponse.java ===
Type: Response (Generic)
Fields:
  - `content`: `List<T>`
  - `page`: `int`
  - `size`: `int`
  - `totalElements`: `long`
  - `totalPages`: `int`
  - `isLast`: `boolean`
Notes: Record. **Generic type parameter `<T>`** -- used as `PageResponse<JobResponse>`, `PageResponse<FindingResponse>`, etc. Standard Spring-style pagination wrapper. Uses primitive types for all pagination metadata.

---

### === PersonaResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `name`: `String`
  - `agentType`: `AgentType`
  - `description`: `String`
  - `contentMd`: `String`
  - `scope`: `Scope`
  - `teamId`: `UUID`
  - `createdBy`: `UUID`
  - `createdByName`: `String`
  - `isDefault`: `boolean`
  - `version`: `int`
  - `createdAt`: `Instant`
  - `updatedAt`: `Instant`
Notes: Record. Includes denormalized `createdByName`. Has `version` field. `isDefault` is primitive `boolean` in response (vs. boxed `Boolean` in create request).

---

### === ProjectDirectiveResponse.java ===
Type: Response
Fields:
  - `projectId`: `UUID`
  - `directiveId`: `UUID`
  - `directiveName`: `String`
  - `category`: `DirectiveCategory`
  - `enabled`: `boolean`
Notes: Record. No `id` field -- composite key (`projectId` + `directiveId`). Includes denormalized `directiveName` and `category` for display without additional lookups.

---

### === ProjectMetricsResponse.java ===
Type: Response
Fields:
  - `projectId`: `UUID`
  - `projectName`: `String`
  - `currentHealthScore`: `Integer`
  - `previousHealthScore`: `Integer`
  - `totalJobs`: `int`
  - `totalFindings`: `int`
  - `openCritical`: `int`
  - `openHigh`: `int`
  - `techDebtItemCount`: `int`
  - `openVulnerabilities`: `int`
  - `lastAuditAt`: `Instant`
Notes: Record. Aggregated metrics response. Both `currentHealthScore` and `previousHealthScore` are boxed `Integer` (nullable when no audits exist). All counts are primitive `int`.

---

### === ProjectResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `teamId`: `UUID`
  - `name`: `String`
  - `description`: `String`
  - `githubConnectionId`: `UUID`
  - `repoUrl`: `String`
  - `repoFullName`: `String`
  - `defaultBranch`: `String`
  - `jiraConnectionId`: `UUID`
  - `jiraProjectKey`: `String`
  - `jiraDefaultIssueType`: `String`
  - `jiraLabels`: `List<String>`
  - `jiraComponent`: `String`
  - `techStack`: `String`
  - `healthScore`: `Integer`
  - `lastAuditAt`: `Instant`
  - `isArchived`: `boolean`
  - `createdAt`: `Instant`
  - `updatedAt`: `Instant`
Notes: Record. 19 fields. Closely mirrors the create/update request shapes plus computed fields (`healthScore`, `lastAuditAt`, `isArchived`, timestamps). `healthScore` is boxed `Integer` (null when never audited).

---

### === SpecificationResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `jobId`: `UUID`
  - `name`: `String`
  - `specType`: `SpecType`
  - `s3Key`: `String`
  - `createdAt`: `Instant`
Notes: Record. Direct mapping from entity. References enum `SpecType`.

---

### === SystemSettingResponse.java ===
Type: Response
Fields:
  - `key`: `String`
  - `value`: `String`
  - `updatedBy`: `UUID`
  - `updatedAt`: `Instant`
Notes: Record. No `id` field -- `key` is the natural identifier (String PK in the entity). Tracks who last updated each setting.

---

### === TaskResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `jobId`: `UUID`
  - `taskNumber`: `int`
  - `title`: `String`
  - `description`: `String`
  - `promptMd`: `String`
  - `promptS3Key`: `String`
  - `findingIds`: `List<UUID>`
  - `priority`: `Priority`
  - `status`: `TaskStatus`
  - `assignedTo`: `UUID`
  - `assignedToName`: `String`
  - `jiraKey`: `String`
  - `createdAt`: `Instant`
Notes: Record. Includes denormalized `assignedToName`. `taskNumber` is primitive `int` in response (boxed `Integer` in create request). References enums `Priority` and `TaskStatus`.

---

### === TeamMemberResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `userId`: `UUID`
  - `displayName`: `String`
  - `email`: `String`
  - `avatarUrl`: `String`
  - `role`: `TeamRole`
  - `joinedAt`: `Instant`
Notes: Record. Combines user profile fields (`displayName`, `email`, `avatarUrl`) with membership fields (`role`, `joinedAt`). `id` is the TeamMember entity ID (not the User ID -- `userId` is separate).

---

### === TeamMetricsResponse.java ===
Type: Response
Fields:
  - `teamId`: `UUID`
  - `totalProjects`: `int`
  - `totalJobs`: `int`
  - `totalFindings`: `int`
  - `averageHealthScore`: `double`
  - `projectsBelowThreshold`: `int`
  - `openCriticalFindings`: `int`
Notes: Record. Aggregated team-level metrics. `averageHealthScore` is primitive `double` (will be 0.0 if no projects have scores, not null).

---

### === TeamResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `name`: `String`
  - `description`: `String`
  - `ownerId`: `UUID`
  - `ownerName`: `String`
  - `teamsWebhookUrl`: `String`
  - `memberCount`: `int`
  - `createdAt`: `Instant`
  - `updatedAt`: `Instant`
Notes: Record. Includes denormalized `ownerName` and computed `memberCount`. `teamsWebhookUrl` is exposed in response (it is not a secret credential).

---

### === TechDebtItemResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `projectId`: `UUID`
  - `category`: `DebtCategory`
  - `title`: `String`
  - `description`: `String`
  - `filePath`: `String`
  - `effortEstimate`: `Effort`
  - `businessImpact`: `BusinessImpact`
  - `status`: `DebtStatus`
  - `firstDetectedJobId`: `UUID`
  - `resolvedJobId`: `UUID`
  - `createdAt`: `Instant`
  - `updatedAt`: `Instant`
Notes: Record. 13 fields. References four enums (`DebtCategory`, `Effort`, `BusinessImpact`, `DebtStatus`). Includes lifecycle tracking (`firstDetectedJobId`, `resolvedJobId`).

---

### === UserResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `email`: `String`
  - `displayName`: `String`
  - `avatarUrl`: `String`
  - `isActive`: `boolean`
  - `lastLoginAt`: `Instant`
  - `createdAt`: `Instant`
Notes: Record. Core user profile response. Does not include password hash or any sensitive auth data. Nested within `AuthResponse`.

---

### === VulnerabilityResponse.java ===
Type: Response
Fields:
  - `id`: `UUID`
  - `scanId`: `UUID`
  - `dependencyName`: `String`
  - `currentVersion`: `String`
  - `fixedVersion`: `String`
  - `cveId`: `String`
  - `severity`: `Severity`
  - `description`: `String`
  - `status`: `VulnerabilityStatus`
  - `createdAt`: `Instant`
Notes: Record. Includes `status` field (`VulnerabilityStatus` enum) not present in the create request (presumably defaults to OPEN on creation). References enums `Severity` and `VulnerabilityStatus`.

---

### 9.3 Summary Statistics

| Metric | Count |
|--------|-------|
| Total Request DTOs | 41 |
| Total Response DTOs | 31 |
| Total DTO files | 72 |
| Generic DTOs | 1 (`PageResponse<T>`) |
| DTOs with nested DTOs | 1 (`AuthResponse` contains `UserResponse`) |
| Composite-key responses (no UUID id) | 3 (`ProjectDirectiveResponse`, `NotificationPreferenceResponse`, `SystemSettingResponse`) |
| Non-UUID PK responses | 1 (`AuditLogResponse` uses `Long`) |

### Validation Annotation Usage Across Request DTOs

| Annotation | Occurrences |
|------------|-------------|
| `@NotNull` | 25 fields |
| `@NotBlank` | 22 fields |
| `@Size` | 67 fields |
| `@Email` | 5 fields |
| `@NotEmpty` | 2 fields |

### Enum Types Referenced by DTOs

| Enum | Used In |
|------|---------|
| `AgentType` | CreateAgentRunRequest, CreateComplianceItemRequest, CreateFindingRequest, CreateHealthScheduleRequest, CreatePersonaRequest, AgentRunResponse, ComplianceItemResponse, FindingResponse, HealthScheduleResponse, PersonaResponse |
| `AgentStatus` | UpdateAgentRunRequest, AgentRunResponse |
| `AgentResult` | UpdateAgentRunRequest, AgentRunResponse |
| `Severity` | CreateFindingRequest, CreateVulnerabilityRequest, FindingResponse, VulnerabilityResponse |
| `FindingStatus` | BulkUpdateFindingsRequest, UpdateFindingStatusRequest, FindingResponse |
| `JobMode` | CreateJobRequest, JobResponse, JobSummaryResponse |
| `JobStatus` | UpdateJobRequest, JobResponse, JobSummaryResponse |
| `JobResult` | UpdateJobRequest, JobResponse, JobSummaryResponse |
| `TeamRole` | InviteMemberRequest, UpdateMemberRoleRequest, InvitationResponse, TeamMemberResponse |
| `DirectiveCategory` | CreateDirectiveRequest, UpdateDirectiveRequest, DirectiveResponse, ProjectDirectiveResponse |
| `DirectiveScope` | CreateDirectiveRequest, DirectiveResponse |
| `ComplianceStatus` | CreateComplianceItemRequest, ComplianceItemResponse |
| `DebtCategory` | CreateFindingRequest, CreateTechDebtItemRequest, FindingResponse, TechDebtItemResponse |
| `DebtStatus` | UpdateTechDebtStatusRequest, TechDebtItemResponse |
| `Effort` | CreateFindingRequest, CreateTechDebtItemRequest, FindingResponse, TechDebtItemResponse |
| `BusinessImpact` | CreateTechDebtItemRequest, TechDebtItemResponse |
| `GitHubAuthType` | CreateGitHubConnectionRequest, GitHubConnectionResponse |
| `InvitationStatus` | InvitationResponse |
| `VulnerabilityStatus` | VulnerabilityResponse |
| `ScheduleType` | CreateHealthScheduleRequest, HealthScheduleResponse |
| `SpecType` | CreateSpecificationRequest, SpecificationResponse |
| `TaskStatus` | UpdateTaskRequest, TaskResponse |
| `Priority` | CreateTaskRequest, TaskResponse |
| `Scope` | CreatePersonaRequest, PersonaResponse |

### 9.4 Observations and Potential Issues

1. **No `@Max`/`@Min` on numeric fields**: Integer fields like `healthScore`, `lineNumber`, `taskNumber`, `score`, and all count fields have no range constraints. `healthScore` convention is 0-100 but this is not enforced at the DTO level.

2. **No `@Size(max=...)` on password fields**: `ChangePasswordRequest.currentPassword`, `ChangePasswordRequest.newPassword`, `LoginRequest.password`, and `RegisterRequest.password` have no maximum length. An attacker could submit extremely long passwords to cause CPU-intensive BCrypt hashing (potential DoS vector).

3. **No URL format validation**: `repoUrl` (CreateProjectRequest, UpdateProjectRequest), `instanceUrl` (CreateJiraConnectionRequest), `avatarUrl` (UpdateUserRequest), and `teamsWebhookUrl` (CreateTeamRequest, UpdateTeamRequest) have `@Size` but no `@URL` or `@Pattern` annotation.

4. **JSON-as-String fields not validated**: `configJson`, `scanDataJson`, `jiraCommentsJson`, `jiraAttachmentsJson`, `jiraLinkedIssues`, and `findingsBySeverity` are stored as raw strings with no JSON format validation at the DTO level.

5. **Primitive vs. boxed type inconsistency**: Some DTOs use primitive `boolean`/`int` (defaults to false/0 if absent from JSON) while others use boxed `Boolean`/`Integer` (can be null). This is intentional for required-vs-optional semantics but could confuse API consumers. For example, `AssignDirectiveRequest.enabled` is primitive `boolean` (defaults false), while `AdminUpdateUserRequest.isActive` is boxed `Boolean` (nullable).

6. **Missing fields in BugInvestigationResponse**: The response omits `jiraCommentsJson`, `jiraAttachmentsJson`, and `jiraLinkedIssues` that are accepted in the create request. This appears intentional (large payloads used for agent processing, not exposed via API).

7. **Credential security is correct**: `GitHubConnectionResponse` omits `credentials` and `JiraConnectionResponse` omits `apiToken`. This is the correct pattern -- encrypted credentials are never returned in API responses.

8. **Denormalized name fields**: Several response DTOs include denormalized name fields (`createdByName`, `startedByName`, `assignedToName`, `ownerName`, `invitedByName`, `specName`, `projectName`, `userName`) to avoid requiring additional API calls for display purposes. This is a good pattern for read-heavy UIs.

9. **Update DTOs use all-optional pattern**: Update request DTOs consistently make all fields optional (no `@NotNull`/`@NotBlank`), supporting partial/PATCH-style updates. The service layer handles null-means-skip-update logic.

10. **`UpdateNotificationPreferenceRequest.eventType` is free-form**: The `eventType` field is a `String` rather than an enum, which allows flexible event types but risks typos and inconsistency. Consider using an enum if the set of event types is fixed.

11. **`UpdateSystemSettingRequest` includes key in body**: The `key` field is `@NotBlank` in the request body, but system settings are typically identified by key in the URL path parameter. Having it in both places could lead to mismatches if the path key and body key differ.


---

## Section 10: Services

**Audit scope:** All 25 files in `src/main/java/com/codeops/service/`

---

### 10.1 AdminService

**File:** `src/main/java/com/codeops/service/AdminService.java`
**Purpose:** System administration operations including user management, system settings, and platform usage statistics.

**Constructor Dependencies:**
- `UserRepository`
- `TeamRepository`
- `ProjectRepository`
- `QaJobRepository`
- `SystemSettingRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `Page<UserResponse>` | `getAllUsers` | `Pageable pageable` | `readOnly = true` | No | No | No |
| `UserResponse` | `getUserById` | `UUID userId` | `readOnly = true` | No | No | No |
| `UserResponse` | `updateUserStatus` | `UUID userId, AdminUpdateUserRequest request` | (class-level) | No | No | **No** |
| `SystemSettingResponse` | `getSystemSetting` | `String key` | `readOnly = true` | No | No | No |
| `SystemSettingResponse` | `updateSystemSetting` | `UpdateSystemSettingRequest request` | (class-level) | No | No | **No** |
| `List<SystemSettingResponse>` | `getAllSettings` | (none) | `readOnly = true` | No | No | No |
| `Map<String, Object>` | `getUsageStats` | (none) | `readOnly = true` | No | No | No |

**Authorization pattern:** `verifyCurrentUserIsAdmin()` calls `SecurityUtils.isAdmin()`. Applied on `getAllUsers`, `getUserById`, `getUsageStats`. **Not applied** on `updateUserStatus`, `updateSystemSetting`, `getSystemSetting`, `getAllSettings`.

---

### 10.2 AgentRunService

**File:** `src/main/java/com/codeops/service/AgentRunService.java`
**Purpose:** CRUD operations for AI agent execution runs within QA jobs.

**Constructor Dependencies:**
- `AgentRunRepository`
- `QaJobRepository`
- `TeamMemberRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `AgentRunResponse` | `createAgentRun` | `CreateAgentRunRequest request` | (class-level) | No | No | No |
| `List<AgentRunResponse>` | `createAgentRuns` | `UUID jobId, List<AgentType> agentTypes` | (class-level) | No | No | No |
| `List<AgentRunResponse>` | `getAgentRuns` | `UUID jobId` | `readOnly = true` | No | No | No |
| `AgentRunResponse` | `getAgentRun` | `UUID agentRunId` | `readOnly = true` | No | No | No |
| `AgentRunResponse` | `updateAgentRun` | `UUID agentRunId, UpdateAgentRunRequest request` | (class-level) | No | No | No |

**Authorization pattern:** `verifyTeamMembership(teamId)` via `TeamMemberRepository.existsByTeamIdAndUserId`. Applied on all methods. Navigation chain: `job.getProject().getTeam().getId()`.

---

### 10.3 AuditLogService

**File:** `src/main/java/com/codeops/service/AuditLogService.java`
**Purpose:** Asynchronous audit trail recording and audit log querying.

**Constructor Dependencies:**
- `AuditLogRepository`
- `UserRepository`
- `TeamRepository`
- `TeamMemberRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `void` | `log` | `UUID userId, UUID teamId, String action, String entityType, UUID entityId, String details` | `@Transactional` | **@Async** | No | N/A (is itself) |
| `Page<AuditLogResponse>` | `getTeamAuditLog` | `UUID teamId, Pageable pageable` | `readOnly = true` | No | No | No |
| `Page<AuditLogResponse>` | `getUserAuditLog` | `UUID userId, Pageable pageable` | `readOnly = true` | No | No | No |

**Authorization pattern:** `getTeamAuditLog` checks team membership. `getUserAuditLog` checks `currentUserId.equals(userId)`.

---

### 10.4 AuthService

**File:** `src/main/java/com/codeops/service/AuthService.java`
**Purpose:** User authentication (register, login, refresh token, change password).

**Constructor Dependencies:**
- `UserRepository`
- `PasswordEncoder`
- `JwtTokenProvider`
- `TeamMemberRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `AuthResponse` | `register` | `RegisterRequest request` | (class-level) | No | No | **No** |
| `AuthResponse` | `login` | `LoginRequest request` | (class-level) | No | No | **No** |
| `AuthResponse` | `refreshToken` | `RefreshTokenRequest request` | (class-level) | No | No | No |
| `void` | `changePassword` | `ChangePasswordRequest request` | (class-level) | No | No | **No** |

**Authorization pattern:** `register`/`login`/`refreshToken` are unauthenticated. `changePassword` uses `SecurityUtils.getCurrentUserId()` implicitly (only self).

---

### 10.5 BugInvestigationService

**File:** `src/main/java/com/codeops/service/BugInvestigationService.java`
**Purpose:** CRUD for bug investigation records linked to QA jobs, including RCA upload.

**Constructor Dependencies:**
- `BugInvestigationRepository`
- `QaJobRepository`
- `TeamMemberRepository`
- `S3StorageService`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `BugInvestigationResponse` | `createInvestigation` | `CreateBugInvestigationRequest request` | (class-level) | No | No | No |
| `BugInvestigationResponse` | `getInvestigation` | `UUID investigationId` | `readOnly = true` | No | No | No |
| `BugInvestigationResponse` | `getInvestigationByJob` | `UUID jobId` | `readOnly = true` | No | No | No |
| `BugInvestigationResponse` | `getInvestigationByJiraKey` | `String jiraKey` | `readOnly = true` | No | No | No |
| `BugInvestigationResponse` | `updateInvestigation` | `UUID investigationId, UpdateBugInvestigationRequest request` | (class-level) | No | No | No |
| `String` | `uploadRca` | `UUID jobId, String rcaMd` | (class-level) | No | No | No |

**Authorization pattern:** `verifyTeamMembership(teamId)` via navigation chain `job.getProject().getTeam().getId()`. Applied on all methods except `uploadRca`.

---

### 10.6 ComplianceService

**File:** `src/main/java/com/codeops/service/ComplianceService.java`
**Purpose:** Manage compliance specifications and compliance check items per QA job.

**Constructor Dependencies:**
- `ComplianceItemRepository`
- `SpecificationRepository`
- `QaJobRepository`
- `TeamMemberRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `SpecificationResponse` | `createSpecification` | `CreateSpecificationRequest request` | (class-level) | No | No | No |
| `PageResponse<SpecificationResponse>` | `getSpecificationsForJob` | `UUID jobId, Pageable pageable` | `readOnly = true` | No | No | No |
| `ComplianceItemResponse` | `createComplianceItem` | `CreateComplianceItemRequest request` | (class-level) | No | No | No |
| `List<ComplianceItemResponse>` | `createComplianceItems` | `List<CreateComplianceItemRequest> requests` | (class-level) | No | No | No |
| `PageResponse<ComplianceItemResponse>` | `getComplianceItemsForJob` | `UUID jobId, Pageable pageable` | `readOnly = true` | No | No | No |
| `PageResponse<ComplianceItemResponse>` | `getComplianceItemsByStatus` | `UUID jobId, ComplianceStatus status, Pageable pageable` | `readOnly = true` | No | No | No |
| `Map<String, Object>` | `getComplianceSummary` | `UUID jobId` | `readOnly = true` | No | No | No |

**Authorization pattern:** `verifyTeamMembership(teamId)` on all methods.

---

### 10.7 DependencyService

**File:** `src/main/java/com/codeops/service/DependencyService.java`
**Purpose:** Manage dependency scans and their associated vulnerability records.

**Constructor Dependencies:**
- `DependencyScanRepository`
- `DependencyVulnerabilityRepository`
- `ProjectRepository`
- `TeamMemberRepository`
- `QaJobRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `DependencyScanResponse` | `createScan` | `CreateDependencyScanRequest request` | (class-level) | No | No | No |
| `DependencyScanResponse` | `getScan` | `UUID scanId` | `readOnly = true` | No | No | No |
| `PageResponse<DependencyScanResponse>` | `getScansForProject` | `UUID projectId, Pageable pageable` | `readOnly = true` | No | No | No |
| `DependencyScanResponse` | `getLatestScan` | `UUID projectId` | `readOnly = true` | No | No | No |
| `VulnerabilityResponse` | `addVulnerability` | `CreateVulnerabilityRequest request` | (class-level) | No | No | No |
| `List<VulnerabilityResponse>` | `addVulnerabilities` | `List<CreateVulnerabilityRequest> requests` | (class-level) | No | No | No |
| `PageResponse<VulnerabilityResponse>` | `getVulnerabilities` | `UUID scanId, Pageable pageable` | `readOnly = true` | No | No | No |
| `PageResponse<VulnerabilityResponse>` | `getVulnerabilitiesBySeverity` | `UUID scanId, Severity severity, Pageable pageable` | `readOnly = true` | No | No | No |
| `PageResponse<VulnerabilityResponse>` | `getOpenVulnerabilities` | `UUID scanId, Pageable pageable` | `readOnly = true` | No | No | No |
| `VulnerabilityResponse` | `updateVulnerabilityStatus` | `UUID vulnerabilityId, VulnerabilityStatus status` | (class-level) | No | No | No |

**Authorization pattern:** `verifyTeamMembership(teamId)` on most methods. **Missing** on `getLatestScan` and `getVulnerabilitiesBySeverity` (no team membership check before query).

---

### 10.8 DirectiveService

**File:** `src/main/java/com/codeops/service/DirectiveService.java`
**Purpose:** CRUD for reusable directives (coding guidelines/rules) and their assignment to projects.

**Constructor Dependencies:**
- `DirectiveRepository`
- `ProjectDirectiveRepository`
- `ProjectRepository`
- `TeamMemberRepository`
- `UserRepository`
- `TeamRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `DirectiveResponse` | `createDirective` | `CreateDirectiveRequest request` | (class-level) | No | No | No |
| `DirectiveResponse` | `getDirective` | `UUID directiveId` | `readOnly = true` | No | No | No |
| `List<DirectiveResponse>` | `getDirectivesForTeam` | `UUID teamId` | `readOnly = true` | No | No | No |
| `List<DirectiveResponse>` | `getDirectivesForProject` | `UUID projectId` | `readOnly = true` | No | No | No |
| `List<DirectiveResponse>` | `getDirectivesByCategory` | `UUID teamId, DirectiveScope scope` | `readOnly = true` | No | No | No |
| `DirectiveResponse` | `updateDirective` | `UUID directiveId, UpdateDirectiveRequest request` | (class-level) | No | No | No |
| `void` | `deleteDirective` | `UUID directiveId` | (class-level) | No | No | No |
| `ProjectDirectiveResponse` | `assignToProject` | `AssignDirectiveRequest request` | (class-level) | No | No | No |
| `void` | `removeFromProject` | `UUID projectId, UUID directiveId` | (class-level) | No | No | No |
| `List<ProjectDirectiveResponse>` | `getProjectDirectives` | `UUID projectId` | `readOnly = true` | No | No | No |
| `List<DirectiveResponse>` | `getEnabledDirectivesForProject` | `UUID projectId` | `readOnly = true` | No | No | No |
| `ProjectDirectiveResponse` | `toggleProjectDirective` | `UUID projectId, UUID directiveId, boolean enabled` | (class-level) | No | No | No |

**Authorization pattern:** Mixed. `createDirective` uses `verifyTeamAdmin`. `updateDirective`/`deleteDirective` use `verifyCreatorOrTeamAdmin`. Project assignment methods use `verifyTeamAdmin`. Read methods use `verifyTeamMembership`. **Missing** on `getEnabledDirectivesForProject` (no auth check).

---

### 10.9 EncryptionService

**File:** `src/main/java/com/codeops/service/EncryptionService.java`
**Purpose:** AES-256-GCM encryption/decryption for sensitive credentials (GitHub PATs, Jira tokens).

**Constructor Dependencies:**
- `@Value("${codeops.encryption.key}") String key` (manual constructor, not `@RequiredArgsConstructor`)

**Class-level annotations:** `@Service`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `String` | `encrypt` | `String plaintext` | No | No | No | No |
| `String` | `decrypt` | `String encryptedBase64` | No | No | No | No |

**Authorization pattern:** None (utility service; callers are responsible for authorization).

---

### 10.10 FindingService

**File:** `src/main/java/com/codeops/service/FindingService.java`
**Purpose:** CRUD for QA findings (issues discovered by agents), with filtering by severity/agent/status and bulk updates.

**Constructor Dependencies:**
- `FindingRepository`
- `QaJobRepository`
- `UserRepository`
- `TeamMemberRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `FindingResponse` | `createFinding` | `CreateFindingRequest request` | (class-level) | No | No | No |
| `List<FindingResponse>` | `createFindings` | `List<CreateFindingRequest> requests` | (class-level) | No | No | No |
| `FindingResponse` | `getFinding` | `UUID findingId` | `readOnly = true` | No | No | No |
| `PageResponse<FindingResponse>` | `getFindingsForJob` | `UUID jobId, Pageable pageable` | `readOnly = true` | No | No | No |
| `PageResponse<FindingResponse>` | `getFindingsByJobAndSeverity` | `UUID jobId, Severity severity, Pageable pageable` | `readOnly = true` | No | No | No |
| `PageResponse<FindingResponse>` | `getFindingsByJobAndAgent` | `UUID jobId, AgentType agentType, Pageable pageable` | `readOnly = true` | No | No | No |
| `PageResponse<FindingResponse>` | `getFindingsByJobAndStatus` | `UUID jobId, FindingStatus status, Pageable pageable` | `readOnly = true` | No | No | No |
| `FindingResponse` | `updateFindingStatus` | `UUID findingId, UpdateFindingStatusRequest request` | (class-level) | No | No | No |
| `List<FindingResponse>` | `bulkUpdateFindingStatus` | `BulkUpdateFindingsRequest request` | (class-level) | No | No | No |
| `Map<Severity, Long>` | `countFindingsBySeverity` | `UUID jobId` | `readOnly = true` | No | No | No |

**Authorization pattern:** `verifyTeamMembership(teamId)` on all methods.

---

### 10.11 GitHubConnectionService

**File:** `src/main/java/com/codeops/service/GitHubConnectionService.java`
**Purpose:** CRUD for GitHub integration connections (encrypted credential storage).

**Constructor Dependencies:**
- `GitHubConnectionRepository`
- `TeamMemberRepository`
- `EncryptionService`
- `TeamRepository`
- `UserRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `GitHubConnectionResponse` | `createConnection` | `UUID teamId, CreateGitHubConnectionRequest request` | (class-level) | No | No | **No** |
| `List<GitHubConnectionResponse>` | `getConnections` | `UUID teamId` | `readOnly = true` | No | No | No |
| `GitHubConnectionResponse` | `getConnection` | `UUID connectionId` | `readOnly = true` | No | No | No |
| `void` | `deleteConnection` | `UUID connectionId` | (class-level) | No | No | **No** |
| `String` | `getDecryptedCredentials` | `UUID connectionId` | (class-level) | No | No | **No** |

**Authorization pattern:** `createConnection`/`deleteConnection` use `verifyTeamAdmin`. `getDecryptedCredentials` checks ADMIN/OWNER inline. Read methods use `verifyTeamMembership`.

---

### 10.12 HealthMonitorService

**File:** `src/main/java/com/codeops/service/HealthMonitorService.java`
**Purpose:** Manage health check schedules and health snapshots for project monitoring over time.

**Constructor Dependencies:**
- `HealthScheduleRepository`
- `HealthSnapshotRepository`
- `ProjectRepository`
- `TeamMemberRepository`
- `UserRepository`
- `QaJobRepository`
- `ObjectMapper`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `HealthScheduleResponse` | `createSchedule` | `CreateHealthScheduleRequest request` | (class-level) | No | No | No |
| `List<HealthScheduleResponse>` | `getSchedulesForProject` | `UUID projectId` | `readOnly = true` | No | No | No |
| `List<HealthScheduleResponse>` | `getActiveSchedules` | (none) | `readOnly = true` | No | No | No |
| `HealthScheduleResponse` | `updateSchedule` | `UUID scheduleId, boolean isActive` | (class-level) | No | No | No |
| `void` | `deleteSchedule` | `UUID scheduleId` | (class-level) | No | No | No |
| `void` | `markScheduleRun` | `UUID scheduleId` | (class-level) | No | No | No |
| `HealthSnapshotResponse` | `createSnapshot` | `CreateHealthSnapshotRequest request` | (class-level) | No | No | No |
| `PageResponse<HealthSnapshotResponse>` | `getSnapshots` | `UUID projectId, Pageable pageable` | `readOnly = true` | No | No | No |
| `HealthSnapshotResponse` | `getLatestSnapshot` | `UUID projectId` | `readOnly = true` | No | No | No |
| `List<HealthSnapshotResponse>` | `getHealthTrend` | `UUID projectId, int limit` | `readOnly = true` | No | No | No |

**Authorization pattern:** Schedule creation/update/delete use `verifyTeamAdmin`. `markScheduleRun` has **no auth check**. `createSnapshot` uses `verifyTeamMembership`. `getLatestSnapshot` has **no auth check**.

---

### 10.13 JiraConnectionService

**File:** `src/main/java/com/codeops/service/JiraConnectionService.java`
**Purpose:** CRUD for Jira integration connections (encrypted API token storage).

**Constructor Dependencies:**
- `JiraConnectionRepository`
- `TeamMemberRepository`
- `EncryptionService`
- `TeamRepository`
- `UserRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `JiraConnectionResponse` | `createConnection` | `UUID teamId, CreateJiraConnectionRequest request` | (class-level) | No | No | **No** |
| `List<JiraConnectionResponse>` | `getConnections` | `UUID teamId` | `readOnly = true` | No | No | No |
| `JiraConnectionResponse` | `getConnection` | `UUID connectionId` | `readOnly = true` | No | No | No |
| `void` | `deleteConnection` | `UUID connectionId` | (class-level) | No | No | **No** |
| `String` | `getDecryptedApiToken` | `UUID connectionId` | (class-level) | No | No | **No** |
| `JiraConnectionDetails` | `getConnectionDetails` | `UUID connectionId` | (class-level) | No | No | **No** |

**Authorization pattern:** Identical to GitHubConnectionService. `createConnection`/`deleteConnection` use `verifyTeamAdmin`. Credential access checks ADMIN/OWNER inline.

---

### 10.14 MetricsService

**File:** `src/main/java/com/codeops/service/MetricsService.java`
**Purpose:** Compute aggregated project and team metrics (health scores, findings, tech debt, vulnerabilities).

**Constructor Dependencies:**
- `ProjectRepository`
- `QaJobRepository`
- `FindingRepository`
- `TechDebtItemRepository`
- `DependencyVulnerabilityRepository`
- `DependencyScanRepository`
- `HealthSnapshotRepository`
- `TeamMemberRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `ProjectMetricsResponse` | `getProjectMetrics` | `UUID projectId` | (class-level readOnly) | No | No | No |
| `TeamMetricsResponse` | `getTeamMetrics` | `UUID teamId` | (class-level readOnly) | No | No | No |
| `List<HealthSnapshotResponse>` | `getHealthTrend` | `UUID projectId, int days` | (class-level readOnly) | No | No | No |

**Authorization pattern:** `verifyTeamMembership(teamId)` on all methods.

---

### 10.15 NotificationService

**File:** `src/main/java/com/codeops/service/NotificationService.java`
**Purpose:** Manage per-user notification preferences (in-app, email) and check if a user should be notified.

**Constructor Dependencies:**
- `NotificationPreferenceRepository`
- `UserRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `List<NotificationPreferenceResponse>` | `getPreferences` | `UUID userId` | `readOnly = true` | No | No | No |
| `NotificationPreferenceResponse` | `updatePreference` | `UUID userId, UpdateNotificationPreferenceRequest request` | (class-level) | No | No | No |
| `List<NotificationPreferenceResponse>` | `updatePreferences` | `UUID userId, List<UpdateNotificationPreferenceRequest> requests` | (class-level) | No | No | No |
| `boolean` | `shouldNotify` | `UUID userId, String eventType, String channel` | `readOnly = true` | No | No | No |

**Authorization pattern:** `verifyCurrentUserAccess(userId)` checks `currentUserId.equals(userId)`. Applied on `getPreferences`, `updatePreference`, `updatePreferences`. **Not applied** on `shouldNotify` (intended for internal use).

---

### 10.16 PersonaService

**File:** `src/main/java/com/codeops/service/PersonaService.java`
**Purpose:** CRUD for AI agent personas (system-wide, team-scoped, user-scoped) with default persona management.

**Constructor Dependencies:**
- `PersonaRepository`
- `TeamMemberRepository`
- `UserRepository`
- `TeamRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `PersonaResponse` | `createPersona` | `CreatePersonaRequest request` | (class-level) | No | No | No |
| `PersonaResponse` | `getPersona` | `UUID personaId` | `readOnly = true` | No | No | No |
| `PageResponse<PersonaResponse>` | `getPersonasForTeam` | `UUID teamId, Pageable pageable` | `readOnly = true` | No | No | No |
| `List<PersonaResponse>` | `getPersonasByAgentType` | `UUID teamId, AgentType agentType` | `readOnly = true` | No | No | No |
| `PersonaResponse` | `getDefaultPersona` | `UUID teamId, AgentType agentType` | `readOnly = true` | No | No | No |
| `List<PersonaResponse>` | `getPersonasByUser` | `UUID userId` | `readOnly = true` | No | No | No |
| `List<PersonaResponse>` | `getSystemPersonas` | (none) | `readOnly = true` | No | No | No |
| `PersonaResponse` | `updatePersona` | `UUID personaId, UpdatePersonaRequest request` | (class-level) | No | No | No |
| `void` | `deletePersona` | `UUID personaId` | (class-level) | No | No | No |
| `PersonaResponse` | `setAsDefault` | `UUID personaId` | (class-level) | No | No | No |
| `PersonaResponse` | `removeDefault` | `UUID personaId` | (class-level) | No | No | No |

**Authorization pattern:** `createPersona` uses `verifyTeamAdmin` for TEAM scope. `updatePersona`/`deletePersona` use `verifyCreatorOrTeamAdmin`. `setAsDefault`/`removeDefault` use `verifyTeamAdmin`. `getPersonasByUser` and `getSystemPersonas` have **no auth check**. `getDefaultPersona` has **no auth check**.

---

### 10.17 ProjectService

**File:** `src/main/java/com/codeops/service/ProjectService.java`
**Purpose:** Full CRUD for projects within teams, including archive/unarchive and health score updates.

**Constructor Dependencies:**
- `ProjectRepository`
- `TeamMemberRepository`
- `UserRepository`
- `TeamRepository`
- `GitHubConnectionRepository`
- `JiraConnectionRepository`
- `ObjectMapper`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `ProjectResponse` | `createProject` | `UUID teamId, CreateProjectRequest request` | (class-level) | No | No | No |
| `ProjectResponse` | `getProject` | `UUID projectId` | `readOnly = true` | No | No | No |
| `List<ProjectResponse>` | `getProjectsForTeam` | `UUID teamId` | `readOnly = true` | No | No | No |
| `PageResponse<ProjectResponse>` | `getAllProjectsForTeam` | `UUID teamId, boolean includeArchived, Pageable pageable` | `readOnly = true` | No | No | No |
| `ProjectResponse` | `updateProject` | `UUID projectId, UpdateProjectRequest request` | (class-level) | No | No | No |
| `void` | `archiveProject` | `UUID projectId` | (class-level) | No | No | No |
| `void` | `unarchiveProject` | `UUID projectId` | (class-level) | No | No | No |
| `void` | `deleteProject` | `UUID projectId` | (class-level) | No | No | No |
| `void` | `updateHealthScore` | `UUID projectId, int score` | (class-level) | No | No | No |

**Authorization pattern:** `createProject`/`updateProject`/`archiveProject`/`unarchiveProject` use `verifyTeamAdmin`. `deleteProject` checks OWNER only (inline). `updateHealthScore` has **no auth check** (called internally by QaJobService).

---

### 10.18 QaJobService

**File:** `src/main/java/com/codeops/service/QaJobService.java`
**Purpose:** Full CRUD for QA jobs (audit runs) associated with projects.

**Constructor Dependencies:**
- `QaJobRepository`
- `AgentRunRepository`
- `FindingRepository`
- `ProjectRepository`
- `UserRepository`
- `TeamMemberRepository`
- `ProjectService`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `JobResponse` | `createJob` | `CreateJobRequest request` | (class-level) | No | No | No |
| `JobResponse` | `getJob` | `UUID jobId` | `readOnly = true` | No | No | No |
| `PageResponse<JobSummaryResponse>` | `getJobsForProject` | `UUID projectId, Pageable pageable` | `readOnly = true` | No | No | No |
| `PageResponse<JobSummaryResponse>` | `getJobsByUser` | `UUID userId, Pageable pageable` | `readOnly = true` | No | No | No |
| `JobResponse` | `updateJob` | `UUID jobId, UpdateJobRequest request` | (class-level) | No | No | No |
| `void` | `deleteJob` | `UUID jobId` | (class-level) | No | No | No |

**Authorization pattern:** `createJob`/`getJob`/`getJobsForProject`/`updateJob` use `verifyTeamMembership`. `deleteJob` uses `verifyTeamAdmin`. `getJobsByUser` has **no auth check** (any authenticated user can query any userId's jobs).

---

### 10.19 RemediationTaskService

**File:** `src/main/java/com/codeops/service/RemediationTaskService.java`
**Purpose:** CRUD for remediation tasks linked to findings, with task prompt upload to S3.

**Constructor Dependencies:**
- `RemediationTaskRepository`
- `QaJobRepository`
- `UserRepository`
- `TeamMemberRepository`
- `FindingRepository`
- `S3StorageService`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `TaskResponse` | `createTask` | `CreateTaskRequest request` | (class-level) | No | No | No |
| `List<TaskResponse>` | `createTasks` | `List<CreateTaskRequest> requests` | (class-level) | No | No | No |
| `PageResponse<TaskResponse>` | `getTasksForJob` | `UUID jobId, Pageable pageable` | `readOnly = true` | No | No | No |
| `TaskResponse` | `getTask` | `UUID taskId` | `readOnly = true` | No | No | No |
| `PageResponse<TaskResponse>` | `getTasksAssignedToUser` | `UUID userId, Pageable pageable` | `readOnly = true` | No | No | No |
| `TaskResponse` | `updateTask` | `UUID taskId, UpdateTaskRequest request` | (class-level) | No | No | No |
| `String` | `uploadTaskPrompt` | `UUID jobId, int taskNumber, String promptMd` | (class-level) | No | No | No |

**Authorization pattern:** `createTask`/`createTasks`/`getTasksForJob`/`getTask`/`updateTask` use `verifyTeamMembership`. `getTasksAssignedToUser` has **no auth check**. `uploadTaskPrompt` has **no auth check**.

---

### 10.20 ReportStorageService

**File:** `src/main/java/com/codeops/service/ReportStorageService.java`
**Purpose:** Facade over S3StorageService for uploading/downloading agent reports and specification files.

**Constructor Dependencies:**
- `S3StorageService`
- `AgentRunRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `String` | `uploadReport` | `UUID jobId, AgentType agentType, String markdownContent` | No | No | No | No |
| `String` | `uploadSummaryReport` | `UUID jobId, String markdownContent` | No | No | No | No |
| `String` | `downloadReport` | `String s3Key` | No | No | No | No |
| `void` | `deleteReportsForJob` | `UUID jobId` | No | No | No | No |
| `String` | `uploadSpecification` | `UUID jobId, String fileName, byte[] fileData, String contentType` | No | No | No | No |
| `byte[]` | `downloadSpecification` | `String s3Key` | No | No | No | No |

**Authorization pattern:** None (internal utility service; callers are responsible for authorization).

---

### 10.21 S3StorageService

**File:** `src/main/java/com/codeops/service/S3StorageService.java`
**Purpose:** Abstracted storage layer -- S3 when enabled, local filesystem fallback in development.

**Constructor Dependencies:**
- `@Value("${codeops.aws.s3.enabled:false}") boolean s3Enabled`
- `@Value("${codeops.aws.s3.bucket:codeops-dev}") String bucket`
- `@Value("${codeops.local-storage.path}") String localStoragePath`
- `@Autowired(required = false) S3Client s3Client`

(Uses `@Autowired`/`@Value` field injection, not `@RequiredArgsConstructor`.)

**Class-level annotations:** `@Service`, `@Slf4j`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `String` | `upload` | `String key, byte[] data, String contentType` | No | No | No | No |
| `byte[]` | `download` | `String key` | No | No | No | No |
| `void` | `delete` | `String key` | No | No | No | No |
| `String` | `generatePresignedUrl` | `String key, Duration expiry` | No | No | No | No |

**Authorization pattern:** None (infrastructure utility).

---

### 10.22 TeamService

**File:** `src/main/java/com/codeops/service/TeamService.java`
**Purpose:** Full team lifecycle -- create, update, delete teams; manage members, invitations, role changes, and ownership transfer.

**Constructor Dependencies:**
- `TeamRepository`
- `TeamMemberRepository`
- `UserRepository`
- `InvitationRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `TeamResponse` | `createTeam` | `CreateTeamRequest request` | (class-level) | No | No | No |
| `TeamResponse` | `getTeam` | `UUID teamId` | `readOnly = true` | No | No | No |
| `List<TeamResponse>` | `getTeamsForUser` | (none) | `readOnly = true` | No | No | No |
| `TeamResponse` | `updateTeam` | `UUID teamId, UpdateTeamRequest request` | (class-level) | No | No | No |
| `void` | `deleteTeam` | `UUID teamId` | (class-level) | No | No | No |
| `List<TeamMemberResponse>` | `getTeamMembers` | `UUID teamId` | `readOnly = true` | No | No | No |
| `TeamMemberResponse` | `updateMemberRole` | `UUID teamId, UUID userId, UpdateMemberRoleRequest request` | (class-level) | No | No | No |
| `void` | `removeMember` | `UUID teamId, UUID userId` | (class-level) | No | No | No |
| `InvitationResponse` | `inviteMember` | `UUID teamId, InviteMemberRequest request` | (class-level) | No | No | No |
| `TeamResponse` | `acceptInvitation` | `String token` | (class-level) | No | No | No |
| `List<InvitationResponse>` | `getTeamInvitations` | `UUID teamId` | `readOnly = true` | No | No | No |
| `void` | `cancelInvitation` | `UUID invitationId` | (class-level) | No | No | No |

**Authorization pattern:** `createTeam` -- any authenticated user. `getTeam`/`getTeamMembers` use `verifyTeamMembership`. `updateTeam`/`updateMemberRole`/`removeMember`/`inviteMember`/`getTeamInvitations`/`cancelInvitation` use `verifyTeamAdmin`. `deleteTeam` checks OWNER only. `acceptInvitation` verifies email match.

---

### 10.23 TechDebtService

**File:** `src/main/java/com/codeops/service/TechDebtService.java`
**Purpose:** CRUD for tech debt items discovered per project, with categorization and summary aggregation.

**Constructor Dependencies:**
- `TechDebtItemRepository`
- `ProjectRepository`
- `TeamMemberRepository`
- `QaJobRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `TechDebtItemResponse` | `createTechDebtItem` | `CreateTechDebtItemRequest request` | (class-level) | No | No | No |
| `List<TechDebtItemResponse>` | `createTechDebtItems` | `List<CreateTechDebtItemRequest> requests` | (class-level) | No | No | No |
| `TechDebtItemResponse` | `getTechDebtItem` | `UUID itemId` | `readOnly = true` | No | No | No |
| `PageResponse<TechDebtItemResponse>` | `getTechDebtForProject` | `UUID projectId, Pageable pageable` | `readOnly = true` | No | No | No |
| `PageResponse<TechDebtItemResponse>` | `getTechDebtByStatus` | `UUID projectId, DebtStatus status, Pageable pageable` | `readOnly = true` | No | No | No |
| `PageResponse<TechDebtItemResponse>` | `getTechDebtByCategory` | `UUID projectId, DebtCategory category, Pageable pageable` | `readOnly = true` | No | No | No |
| `TechDebtItemResponse` | `updateTechDebtStatus` | `UUID itemId, UpdateTechDebtStatusRequest request` | (class-level) | No | No | No |
| `void` | `deleteTechDebtItem` | `UUID itemId` | (class-level) | No | No | No |
| `Map<String, Object>` | `getDebtSummary` | `UUID projectId` | `readOnly = true` | No | No | No |

**Authorization pattern:** `deleteTechDebtItem` uses `verifyTeamAdmin`. All other methods use `verifyTeamMembership`.

---

### 10.24 TokenBlacklistService

**File:** `src/main/java/com/codeops/service/TokenBlacklistService.java`
**Purpose:** In-memory JWT token blacklist for logout/revocation.

**Constructor Dependencies:** None (no injected dependencies).

**Class-level annotations:** `@Service`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `void` | `blacklist` | `String jti, Instant expiry` | No | No | No | No |
| `boolean` | `isBlacklisted` | `String jti` | No | No | No | No |

**Authorization pattern:** None (infrastructure utility).

---

### 10.25 UserService

**File:** `src/main/java/com/codeops/service/UserService.java`
**Purpose:** User profile lookup, search, update, and activation/deactivation.

**Constructor Dependencies:**
- `UserRepository`

**Class-level annotations:** `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`

| Return Type | Method | Parameters | @Transactional | @Async | @PreAuthorize | Calls AuditLogService.log() |
|---|---|---|---|---|---|---|
| `UserResponse` | `getUserById` | `UUID id` | (class-level readOnly) | No | No | No |
| `UserResponse` | `getUserByEmail` | `String email` | (class-level readOnly) | No | No | No |
| `UserResponse` | `getCurrentUser` | (none) | (class-level readOnly) | No | No | No |
| `UserResponse` | `updateUser` | `UUID userId, UpdateUserRequest request` | `@Transactional` | No | No | No |
| `List<UserResponse>` | `searchUsers` | `String query` | (class-level readOnly) | No | No | No |
| `void` | `deactivateUser` | `UUID userId` | `@Transactional` | No | No | **No** |
| `void` | `activateUser` | `UUID userId` | `@Transactional` | No | No | **No** |

**Authorization pattern:** `updateUser` checks `currentUserId.equals(userId) || SecurityUtils.isAdmin()`. `deactivateUser`/`activateUser` have **no auth check** in the service (assumed controller-level protection). `getUserById`/`getUserByEmail`/`searchUsers` have **no auth check** (any authenticated user).

---

## Summary Tables

### Authorization Check Coverage -- Mutating Methods Missing Authorization

| Service | Method | Mutation Type | Risk Level |
|---|---|---|---|
| **AdminService** | `updateUserStatus` | User activation/deactivation | **CRITICAL** -- no admin check |
| **AdminService** | `updateSystemSetting` | System config change | **CRITICAL** -- no admin check |
| **AdminService** | `getSystemSetting` | Read (but admin-only data) | MEDIUM |
| **AdminService** | `getAllSettings` | Read (but admin-only data) | MEDIUM |
| **BugInvestigationService** | `uploadRca` | S3 file write | HIGH -- no team membership check |
| **DependencyService** | `getLatestScan` | Read | LOW -- no team membership check |
| **DependencyService** | `getVulnerabilitiesBySeverity` | Read | LOW -- no team membership check |
| **DirectiveService** | `getEnabledDirectivesForProject` | Read | LOW -- no team membership check |
| **GitHubConnectionService** | `createConnection` | Store encrypted credentials | MEDIUM -- admin-checked, but no audit log |
| **GitHubConnectionService** | `deleteConnection` | Soft-delete connection | MEDIUM -- no audit log |
| **GitHubConnectionService** | `getDecryptedCredentials` | Decrypt secrets | **CRITICAL** -- no audit log |
| **HealthMonitorService** | `markScheduleRun` | Timestamp update | HIGH -- no auth check at all |
| **HealthMonitorService** | `getLatestSnapshot` | Read | LOW -- no team membership check |
| **JiraConnectionService** | `createConnection` | Store encrypted credentials | MEDIUM -- no audit log |
| **JiraConnectionService** | `deleteConnection` | Soft-delete connection | MEDIUM -- no audit log |
| **JiraConnectionService** | `getDecryptedApiToken` | Decrypt secrets | **CRITICAL** -- no audit log |
| **JiraConnectionService** | `getConnectionDetails` | Decrypt secrets | **CRITICAL** -- no audit log |
| **PersonaService** | `getDefaultPersona` | Read | LOW -- no team membership check |
| **PersonaService** | `getPersonasByUser` | Read | LOW -- no auth check on userId |
| **ProjectService** | `updateHealthScore` | Score update | MEDIUM -- no auth check (internal use) |
| **QaJobService** | `getJobsByUser` | Read | MEDIUM -- no check that caller == userId |
| **RemediationTaskService** | `getTasksAssignedToUser` | Read | MEDIUM -- no check that caller == userId |
| **RemediationTaskService** | `uploadTaskPrompt` | S3 file write | HIGH -- no auth check at all |
| **UserService** | `deactivateUser` | Account deactivation | **CRITICAL** -- no auth check |
| **UserService** | `activateUser` | Account activation | **CRITICAL** -- no auth check |

### Audit Logging Gaps -- Mutations Not Logged

**No service in the entire codebase calls `AuditLogService.log()`.** The audit log infrastructure exists (AuditLogService with @Async + @Transactional, AuditLog entity, AuditLogRepository) but is **never invoked by any service method**. This means the `audit_logs` table remains empty during normal application operation.

Security-sensitive operations that should be audit-logged:
1. `AuthService.register` / `login` / `changePassword`
2. `AdminService.updateUserStatus` / `updateSystemSetting`
3. `UserService.deactivateUser` / `activateUser`
4. `TeamService.createTeam` / `deleteTeam` / `updateMemberRole` / `removeMember` / `inviteMember`
5. `GitHubConnectionService.createConnection` / `deleteConnection` / `getDecryptedCredentials`
6. `JiraConnectionService.createConnection` / `deleteConnection` / `getDecryptedApiToken` / `getConnectionDetails`
7. `ProjectService.createProject` / `deleteProject`
8. `DirectiveService.createDirective` / `deleteDirective`

### Performance Concerns

| Service | Method | Concern |
|---|---|---|
| **ComplianceService** | `getComplianceSummary` | Issues 4 separate `findByJobIdAndStatus` queries to count each status, then loads `.size()` on the full result lists. Should use `COUNT` queries or a single grouped query. |
| **MetricsService** | `getTeamMetrics` | **N+1 problem.** Iterates all projects, and for each project executes `findByProjectIdOrderByCreatedAtDesc` (loads all jobs) plus `countByJobIdAndSeverityAndStatus`. For a team with many projects, this produces O(projects) database round-trips. |
| **MetricsService** | `getProjectMetrics` | Loads **all** snapshots via `findByProjectIdOrderByCapturedAtDesc` just to get `snapshots.get(1)`. Also loads all jobs for the project into memory to count findings. |
| **MetricsService** | `getHealthTrend` | Loads **all** snapshots for a project, then filters in memory by date cutoff. Should push the date filter into the query. |
| **HealthMonitorService** | `getActiveSchedules` | Loads **all** active schedules across the system, then filters in memory by team membership. Does not scale with number of teams/schedules. |
| **HealthMonitorService** | `getHealthTrend` | Loads all snapshots ordered by date, slices in memory with `subList`. Should use `Pageable` or LIMIT in query. |
| **TechDebtService** | `getDebtSummary` | Loads **all** tech debt items for a project into memory, then groups/counts with Java streams. Should use grouped `COUNT` queries. |
| **TechDebtService** | `createTechDebtItems` | Potential N+1 inside the `.map()` lambda: each item with `firstDetectedJobId != null` triggers a separate `qaJobRepository.findById()`. |
| **DependencyService** | `addVulnerabilities` | Similar pattern -- validates all items but the builder lambda is executed per item. |
| **FindingService** | `countFindingsBySeverity` | Iterates all `Severity.values()` and issues a separate `countByJobIdAndSeverity` query for each. Should use a single grouped query. |
| **TeamService** | `mapToTeamResponse` | Every call to `mapToTeamResponse` issues `teamMemberRepository.countByTeamId()`. When listing teams for a user, this causes N+1 queries. |
| **PersonaService** | `createPersona` | Calls `personaRepository.findByTeamId(teamId).size()` to count personas, loading all persona entities. Should use a `COUNT` query. |
| **DirectiveService** | `assignToProject` | Calls `projectDirectiveRepository.findByProjectId(projectId).size()` to count assignments, loading all entities. Should use a `COUNT` query. |

### Cross-cutting Patterns

| Pattern | Description | Services Using It |
|---|---|---|
| **Team membership verification** | `verifyTeamMembership(UUID teamId)` checks `teamMemberRepository.existsByTeamIdAndUserId`. Duplicated identically in 16 services. | AgentRun, BugInvestigation, Compliance, Dependency, Directive, Finding, GitHubConnection, HealthMonitor, JiraConnection, Metrics, Notification (variant), Persona, Project, QaJob, RemediationTask, Team, TechDebt |
| **Team admin verification** | `verifyTeamAdmin(UUID teamId)` checks role is OWNER or ADMIN. Duplicated identically in 10 services. | Directive, GitHubConnection, HealthMonitor, JiraConnection, Persona, Project, QaJob, Team, TechDebt |
| **Creator-or-admin pattern** | `verifyCreatorOrTeamAdmin(entity)` allows the original creator or a team admin to modify. | Directive, Persona |
| **Bulk create with same-parent validation** | Pattern: validate all items belong to same parent, look up parent once, then `saveAll`. | Compliance, Dependency, Finding, RemediationTask, TechDebt |
| **PageResponse wrapping** | All paginated queries manually wrap `Page<Entity>` into `PageResponse<DTO>`. Identical boilerplate in every service. | Compliance, Dependency, Finding, HealthMonitor, Persona, Project, QaJob, RemediationTask, TechDebt |
| **Null-check field-by-field updates** | Partial update pattern: `if (request.field() != null) entity.setField(request.field())`. No use of MapStruct or BeanUtils. | AgentRun, BugInvestigation, Directive, Finding, Persona, Project, QaJob, RemediationTask, Team |
| **Entity navigation chain for team ID** | Deep navigation like `job.getProject().getTeam().getId()` to reach team ID. Repeated in many services and may trigger lazy-loading issues. | AgentRun, BugInvestigation, Compliance, Finding, HealthMonitor, QaJob, RemediationTask |
| **Manual DTO mapping** | All services manually construct response DTOs via constructor calls. No MapStruct. Fragile to field additions. | All 25 services |
| **No `@PreAuthorize` usage** | Despite CLAUDE.md stating "Controllers use @PreAuthorize for authorization," no service-level `@PreAuthorize` annotations exist. All authorization is imperative code. | All 25 services |
| **No audit logging** | `AuditLogService.log()` exists but is never called anywhere. | All 25 services |
| **S3 key construction** | S3 keys built via string concatenation with `AppConstants.S3_REPORTS` / `AppConstants.S3_SPECS` prefixes. | BugInvestigation, RemediationTask, ReportStorage |

### Statistics

| Metric | Value |
|---|---|
| Total services | 25 |
| Total public methods | 146 |
| Methods with `@Transactional` (explicit method-level) | 8 |
| Methods with `@Transactional(readOnly = true)` (explicit method-level) | 52 |
| Class-level `@Transactional` (write) | 18 classes |
| Class-level `@Transactional(readOnly = true)` | 2 classes (UserService, MetricsService) |
| Methods with `@Async` | 1 (`AuditLogService.log`) |
| Methods with `@PreAuthorize` | 0 |
| Methods calling `AuditLogService.log()` | 0 |
| Utility/infra services (no auth) | 5 (EncryptionService, ReportStorageService, S3StorageService, TokenBlacklistService, AuditLogService) |
| Services with auth gaps on mutations | 7 (AdminService, BugInvestigationService, HealthMonitorService, ProjectService, RemediationTaskService, UserService, QaJobService) |


---

## Section 11: Controller Layer

All controllers reside under `src/main/java/com/codeops/controller/` unless otherwise noted.
The `HealthController` resides under `src/main/java/com/codeops/config/`.

---

### === AdminController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/AdminController.java`

**Base Path**: `@RequestMapping("/api/v1/admin")`

**Injected Dependencies**:
- `AdminService adminService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/admin")`
- `@RequiredArgsConstructor`
- `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")`
- `@Tag(name = "Admin")`

**Endpoints**:

```
─── GET /api/v1/admin/users
    Method: getAllUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size)
    Auth: Class-level @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: none
    Path Variables: none
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<Page<UserResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/admin/users/{userId}
    Method: getUserById(@PathVariable UUID userId)
    Auth: Class-level @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: none
    Path Variables: UUID userId
    Query Params: none
    Response: ResponseEntity<UserResponse>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/admin/users/{userId}
    Method: updateUserStatus(@PathVariable UUID userId, @Valid @RequestBody AdminUpdateUserRequest request)
    Auth: Class-level @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: AdminUpdateUserRequest
    Path Variables: UUID userId
    Query Params: none
    Response: ResponseEntity<UserResponse>
    Status Codes: 200
    Audit Logged: Yes — "ADMIN_USER_UPDATED"

─── GET /api/v1/admin/settings
    Method: getAllSettings()
    Auth: Class-level @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: none
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<SystemSettingResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/admin/settings/{key}
    Method: getSystemSetting(@PathVariable String key)
    Auth: Class-level @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: none
    Path Variables: String key
    Query Params: none
    Response: ResponseEntity<SystemSettingResponse>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/admin/settings
    Method: updateSystemSetting(@Valid @RequestBody UpdateSystemSettingRequest request)
    Auth: Class-level @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: UpdateSystemSettingRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<SystemSettingResponse>
    Status Codes: 200
    Audit Logged: Yes — "SYSTEM_SETTING_UPDATED"

─── GET /api/v1/admin/usage
    Method: getUsageStats()
    Auth: Class-level @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: none
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<Map<String, Object>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/admin/audit-log/team/{teamId}
    Method: getTeamAuditLog(@PathVariable UUID teamId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size)
    Auth: Class-level @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<Page<AuditLogResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/admin/audit-log/user/{userId}
    Method: getUserAuditLog(@PathVariable UUID userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size)
    Auth: Class-level @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: none
    Path Variables: UUID userId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<Page<AuditLogResponse>>
    Status Codes: 200
    Audit Logged: No
```

---

### === AuthController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/AuthController.java`

**Base Path**: `@RequestMapping("/api/v1/auth")`

**Injected Dependencies**:
- `AuthService authService`
- `AuditLogService auditLogService`
- `JwtTokenProvider jwtTokenProvider`
- `TokenBlacklistService tokenBlacklistService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/auth")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Authentication")`

**Endpoints**:

```
─── POST /api/v1/auth/register
    Method: register(@Valid @RequestBody RegisterRequest request)
    Auth: none (public)
    Request Body: RegisterRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<AuthResponse>
    Status Codes: 201
    Audit Logged: Yes — "USER_REGISTERED"

─── POST /api/v1/auth/login
    Method: login(@Valid @RequestBody LoginRequest request)
    Auth: none (public)
    Request Body: LoginRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<AuthResponse>
    Status Codes: 200
    Audit Logged: Yes — "USER_LOGIN"

─── POST /api/v1/auth/refresh
    Method: refresh(@Valid @RequestBody RefreshTokenRequest request)
    Auth: none (public)
    Request Body: RefreshTokenRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<AuthResponse>
    Status Codes: 200
    Audit Logged: No

─── POST /api/v1/auth/logout
    Method: logout(@RequestHeader("Authorization") String authHeader)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: none
    Headers: Authorization (Bearer token)
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: No

─── POST /api/v1/auth/change-password
    Method: changePassword(@Valid @RequestBody ChangePasswordRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: ChangePasswordRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 200
    Audit Logged: No
```

---

### === ComplianceController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/ComplianceController.java`

**Base Path**: `@RequestMapping("/api/v1/compliance")`

**Injected Dependencies**:
- `ComplianceService complianceService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/compliance")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Compliance")`

**Endpoints**:

```
─── POST /api/v1/compliance/specs
    Method: createSpecification(@Valid @RequestBody CreateSpecificationRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateSpecificationRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<SpecificationResponse>
    Status Codes: 201
    Audit Logged: Yes — "SPECIFICATION_CREATED"

─── GET /api/v1/compliance/specs/job/{jobId}
    Method: getSpecificationsForJob(@PathVariable UUID jobId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<SpecificationResponse>>
    Status Codes: 200
    Audit Logged: No

─── POST /api/v1/compliance/items
    Method: createComplianceItem(@Valid @RequestBody CreateComplianceItemRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateComplianceItemRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<ComplianceItemResponse>
    Status Codes: 201
    Audit Logged: Yes — "COMPLIANCE_ITEM_CREATED"

─── POST /api/v1/compliance/items/batch
    Method: createComplianceItems(@Valid @RequestBody List<CreateComplianceItemRequest> requests)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: List<CreateComplianceItemRequest>
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<ComplianceItemResponse>>
    Status Codes: 201
    Audit Logged: Yes — "COMPLIANCE_ITEM_CREATED" (per item)

─── GET /api/v1/compliance/items/job/{jobId}
    Method: getComplianceItemsForJob(@PathVariable UUID jobId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<ComplianceItemResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/compliance/items/job/{jobId}/status/{status}
    Method: getComplianceItemsByStatus(@PathVariable UUID jobId, @PathVariable ComplianceStatus status, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId, ComplianceStatus status
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<ComplianceItemResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/compliance/summary/job/{jobId}
    Method: getComplianceSummary(@PathVariable UUID jobId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<Map<String, Object>>
    Status Codes: 200
    Audit Logged: No
```

---

### === DependencyController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/DependencyController.java`

**Base Path**: `@RequestMapping("/api/v1/dependencies")`

**Injected Dependencies**:
- `DependencyService dependencyService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/dependencies")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Dependencies")`

**Endpoints**:

```
─── POST /api/v1/dependencies/scans
    Method: createScan(@Valid @RequestBody CreateDependencyScanRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateDependencyScanRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<DependencyScanResponse>
    Status Codes: 201
    Audit Logged: Yes — "DEPENDENCY_SCAN_CREATED"

─── GET /api/v1/dependencies/scans/{scanId}
    Method: getScan(@PathVariable UUID scanId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID scanId
    Query Params: none
    Response: ResponseEntity<DependencyScanResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/dependencies/scans/project/{projectId}
    Method: getScansForProject(@PathVariable UUID projectId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<DependencyScanResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/dependencies/scans/project/{projectId}/latest
    Method: getLatestScan(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<DependencyScanResponse>
    Status Codes: 200
    Audit Logged: No

─── POST /api/v1/dependencies/vulnerabilities
    Method: addVulnerability(@Valid @RequestBody CreateVulnerabilityRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateVulnerabilityRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<VulnerabilityResponse>
    Status Codes: 201
    Audit Logged: Yes — "VULNERABILITY_ADDED"

─── POST /api/v1/dependencies/vulnerabilities/batch
    Method: addVulnerabilities(@Valid @RequestBody List<CreateVulnerabilityRequest> requests)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: List<CreateVulnerabilityRequest>
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<VulnerabilityResponse>>
    Status Codes: 201
    Audit Logged: Yes — "VULNERABILITY_ADDED" (per item)

─── GET /api/v1/dependencies/vulnerabilities/scan/{scanId}
    Method: getVulnerabilities(@PathVariable UUID scanId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID scanId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<VulnerabilityResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/dependencies/vulnerabilities/scan/{scanId}/severity/{severity}
    Method: getVulnerabilitiesBySeverity(@PathVariable UUID scanId, @PathVariable Severity severity, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID scanId, Severity severity
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<VulnerabilityResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/dependencies/vulnerabilities/scan/{scanId}/open
    Method: getOpenVulnerabilities(@PathVariable UUID scanId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID scanId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<VulnerabilityResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/dependencies/vulnerabilities/{vulnerabilityId}/status
    Method: updateVulnerabilityStatus(@PathVariable UUID vulnerabilityId, @RequestParam VulnerabilityStatus status)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID vulnerabilityId
    Query Params: VulnerabilityStatus status
    Response: ResponseEntity<VulnerabilityResponse>
    Status Codes: 200
    Audit Logged: Yes — "VULNERABILITY_STATUS_UPDATED"
```

---

### === DirectiveController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/DirectiveController.java`

**Base Path**: `@RequestMapping("/api/v1/directives")`

**Injected Dependencies**:
- `DirectiveService directiveService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/directives")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Directives")`

**Endpoints**:

```
─── POST /api/v1/directives
    Method: createDirective(@Valid @RequestBody CreateDirectiveRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateDirectiveRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<DirectiveResponse>
    Status Codes: 201
    Audit Logged: Yes — "DIRECTIVE_CREATED"

─── GET /api/v1/directives/{directiveId}
    Method: getDirective(@PathVariable UUID directiveId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID directiveId
    Query Params: none
    Response: ResponseEntity<DirectiveResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/directives/team/{teamId}
    Method: getDirectivesForTeam(@PathVariable UUID teamId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<List<DirectiveResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/directives/project/{projectId}
    Method: getDirectivesForProject(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<List<DirectiveResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/directives/{directiveId}
    Method: updateDirective(@PathVariable UUID directiveId, @Valid @RequestBody UpdateDirectiveRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateDirectiveRequest
    Path Variables: UUID directiveId
    Query Params: none
    Response: ResponseEntity<DirectiveResponse>
    Status Codes: 200
    Audit Logged: Yes — "DIRECTIVE_UPDATED"

─── DELETE /api/v1/directives/{directiveId}
    Method: deleteDirective(@PathVariable UUID directiveId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID directiveId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "DIRECTIVE_DELETED"

─── POST /api/v1/directives/assign
    Method: assignToProject(@Valid @RequestBody AssignDirectiveRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: AssignDirectiveRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<ProjectDirectiveResponse>
    Status Codes: 201
    Audit Logged: Yes — "DIRECTIVE_ASSIGNED"

─── DELETE /api/v1/directives/project/{projectId}/directive/{directiveId}
    Method: removeFromProject(@PathVariable UUID projectId, @PathVariable UUID directiveId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId, UUID directiveId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "DIRECTIVE_REMOVED"

─── GET /api/v1/directives/project/{projectId}/assignments
    Method: getProjectDirectives(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<List<ProjectDirectiveResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/directives/project/{projectId}/enabled
    Method: getEnabledDirectives(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<List<DirectiveResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/directives/project/{projectId}/directive/{directiveId}/toggle
    Method: toggleDirective(@PathVariable UUID projectId, @PathVariable UUID directiveId, @RequestParam boolean enabled)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId, UUID directiveId
    Query Params: boolean enabled
    Response: ResponseEntity<ProjectDirectiveResponse>
    Status Codes: 200
    Audit Logged: No
```

---

### === FindingController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/FindingController.java`

**Base Path**: `@RequestMapping("/api/v1/findings")`

**Injected Dependencies**:
- `FindingService findingService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/findings")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Findings")`

**Endpoints**:

```
─── POST /api/v1/findings
    Method: createFinding(@Valid @RequestBody CreateFindingRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateFindingRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<FindingResponse>
    Status Codes: 201
    Audit Logged: Yes — "FINDING_CREATED"

─── POST /api/v1/findings/batch
    Method: createFindings(@Valid @RequestBody List<CreateFindingRequest> requests)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: List<CreateFindingRequest>
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<FindingResponse>>
    Status Codes: 201
    Audit Logged: Yes — "FINDING_CREATED" (per item)

─── GET /api/v1/findings/{findingId}
    Method: getFinding(@PathVariable UUID findingId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID findingId
    Query Params: none
    Response: ResponseEntity<FindingResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/findings/job/{jobId}
    Method: getFindingsForJob(@PathVariable UUID jobId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<FindingResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/findings/job/{jobId}/severity/{severity}
    Method: getFindingsBySeverity(@PathVariable UUID jobId, @PathVariable Severity severity, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId, Severity severity
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<FindingResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/findings/job/{jobId}/agent/{agentType}
    Method: getFindingsByAgent(@PathVariable UUID jobId, @PathVariable AgentType agentType, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId, AgentType agentType
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<FindingResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/findings/job/{jobId}/status/{status}
    Method: getFindingsByStatus(@PathVariable UUID jobId, @PathVariable FindingStatus status, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId, FindingStatus status
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<FindingResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/findings/job/{jobId}/counts
    Method: getSeverityCounts(@PathVariable UUID jobId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<Map<Severity, Long>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/findings/{findingId}/status
    Method: updateFindingStatus(@PathVariable UUID findingId, @Valid @RequestBody UpdateFindingStatusRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateFindingStatusRequest
    Path Variables: UUID findingId
    Query Params: none
    Response: ResponseEntity<FindingResponse>
    Status Codes: 200
    Audit Logged: Yes — "FINDING_STATUS_UPDATED"

─── PUT /api/v1/findings/bulk-status
    Method: bulkUpdateStatus(@Valid @RequestBody BulkUpdateFindingsRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: BulkUpdateFindingsRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<FindingResponse>>
    Status Codes: 200
    Audit Logged: Yes — "FINDING_STATUS_UPDATED" (per item)
```

---

### === HealthMonitorController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/HealthMonitorController.java`

**Base Path**: `@RequestMapping("/api/v1/health-monitor")`

**Injected Dependencies**:
- `HealthMonitorService healthMonitorService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/health-monitor")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Health Monitor")`

**Endpoints**:

```
─── POST /api/v1/health-monitor/schedules
    Method: createSchedule(@Valid @RequestBody CreateHealthScheduleRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateHealthScheduleRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<HealthScheduleResponse>
    Status Codes: 201
    Audit Logged: Yes — "HEALTH_SCHEDULE_CREATED"

─── GET /api/v1/health-monitor/schedules/project/{projectId}
    Method: getSchedulesForProject(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<List<HealthScheduleResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/health-monitor/schedules/{scheduleId}
    Method: updateSchedule(@PathVariable UUID scheduleId, @RequestParam boolean active)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID scheduleId
    Query Params: boolean active
    Response: ResponseEntity<HealthScheduleResponse>
    Status Codes: 200
    Audit Logged: No

─── DELETE /api/v1/health-monitor/schedules/{scheduleId}
    Method: deleteSchedule(@PathVariable UUID scheduleId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID scheduleId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "HEALTH_SCHEDULE_DELETED"

─── POST /api/v1/health-monitor/snapshots
    Method: createSnapshot(@Valid @RequestBody CreateHealthSnapshotRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateHealthSnapshotRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<HealthSnapshotResponse>
    Status Codes: 201
    Audit Logged: No

─── GET /api/v1/health-monitor/snapshots/project/{projectId}
    Method: getSnapshots(@PathVariable UUID projectId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<HealthSnapshotResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/health-monitor/snapshots/project/{projectId}/latest
    Method: getLatestSnapshot(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<HealthSnapshotResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/health-monitor/snapshots/project/{projectId}/trend
    Method: getHealthTrend(@PathVariable UUID projectId, @RequestParam(defaultValue = "30") int limit)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: limit (int, default 30)
    Response: ResponseEntity<List<HealthSnapshotResponse>>
    Status Codes: 200
    Audit Logged: No
```

---

### === IntegrationController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/IntegrationController.java`

**Base Path**: `@RequestMapping("/api/v1/integrations")`

**Injected Dependencies**:
- `GitHubConnectionService gitHubConnectionService`
- `JiraConnectionService jiraConnectionService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/integrations")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Integrations")`

**Endpoints**:

```
─── POST /api/v1/integrations/github/{teamId}
    Method: createGitHubConnection(@PathVariable UUID teamId, @Valid @RequestBody CreateGitHubConnectionRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateGitHubConnectionRequest
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<GitHubConnectionResponse>
    Status Codes: 201
    Audit Logged: Yes — "GITHUB_CONNECTION_CREATED"

─── GET /api/v1/integrations/github/{teamId}
    Method: getGitHubConnections(@PathVariable UUID teamId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<List<GitHubConnectionResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/integrations/github/{teamId}/{connectionId}
    Method: getGitHubConnection(@PathVariable UUID teamId, @PathVariable UUID connectionId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId, UUID connectionId
    Query Params: none
    Response: ResponseEntity<GitHubConnectionResponse>
    Status Codes: 200
    Audit Logged: No

─── DELETE /api/v1/integrations/github/{teamId}/{connectionId}
    Method: deleteGitHubConnection(@PathVariable UUID teamId, @PathVariable UUID connectionId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId, UUID connectionId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "GITHUB_CONNECTION_DELETED"

─── POST /api/v1/integrations/jira/{teamId}
    Method: createJiraConnection(@PathVariable UUID teamId, @Valid @RequestBody CreateJiraConnectionRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateJiraConnectionRequest
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<JiraConnectionResponse>
    Status Codes: 201
    Audit Logged: Yes — "JIRA_CONNECTION_CREATED"

─── GET /api/v1/integrations/jira/{teamId}
    Method: getJiraConnections(@PathVariable UUID teamId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<List<JiraConnectionResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/integrations/jira/{teamId}/{connectionId}
    Method: getJiraConnection(@PathVariable UUID teamId, @PathVariable UUID connectionId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId, UUID connectionId
    Query Params: none
    Response: ResponseEntity<JiraConnectionResponse>
    Status Codes: 200
    Audit Logged: No

─── DELETE /api/v1/integrations/jira/{teamId}/{connectionId}
    Method: deleteJiraConnection(@PathVariable UUID teamId, @PathVariable UUID connectionId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId, UUID connectionId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "JIRA_CONNECTION_DELETED"
```

---

### === JobController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/JobController.java`

**Base Path**: `@RequestMapping("/api/v1/jobs")`

**Injected Dependencies**:
- `QaJobService qaJobService`
- `AgentRunService agentRunService`
- `BugInvestigationService bugInvestigationService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/jobs")`
- `@RequiredArgsConstructor`
- `@Tag(name = "QA Jobs")`

**Endpoints**:

```
─── POST /api/v1/jobs
    Method: createJob(@Valid @RequestBody CreateJobRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateJobRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<JobResponse>
    Status Codes: 201
    Audit Logged: Yes — "JOB_CREATED"

─── GET /api/v1/jobs/{jobId}
    Method: getJob(@PathVariable UUID jobId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<JobResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/jobs/project/{projectId}
    Method: getJobsForProject(@PathVariable UUID projectId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<JobSummaryResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/jobs/mine
    Method: getMyJobs(@RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<JobSummaryResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/jobs/{jobId}
    Method: updateJob(@PathVariable UUID jobId, @Valid @RequestBody UpdateJobRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateJobRequest
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<JobResponse>
    Status Codes: 200
    Audit Logged: Yes — "JOB_UPDATED"

─── DELETE /api/v1/jobs/{jobId}
    Method: deleteJob(@PathVariable UUID jobId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "JOB_DELETED"

─── POST /api/v1/jobs/{jobId}/agents
    Method: createAgentRun(@PathVariable UUID jobId, @Valid @RequestBody CreateAgentRunRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateAgentRunRequest
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<AgentRunResponse>
    Status Codes: 201
    Audit Logged: Yes — "AGENT_RUN_CREATED"

─── POST /api/v1/jobs/{jobId}/agents/batch
    Method: createAgentRunsBatch(@PathVariable UUID jobId, @RequestBody List<AgentType> agentTypes)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: List<AgentType>
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<List<AgentRunResponse>>
    Status Codes: 201
    Audit Logged: Yes — "AGENT_RUN_CREATED" (per item)

─── GET /api/v1/jobs/{jobId}/agents
    Method: getAgentRuns(@PathVariable UUID jobId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<List<AgentRunResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/jobs/agents/{agentRunId}
    Method: updateAgentRun(@PathVariable UUID agentRunId, @Valid @RequestBody UpdateAgentRunRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateAgentRunRequest
    Path Variables: UUID agentRunId
    Query Params: none
    Response: ResponseEntity<AgentRunResponse>
    Status Codes: 200
    Audit Logged: Yes — "AGENT_RUN_UPDATED"

─── GET /api/v1/jobs/{jobId}/investigation
    Method: getInvestigation(@PathVariable UUID jobId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<BugInvestigationResponse>
    Status Codes: 200
    Audit Logged: No

─── POST /api/v1/jobs/{jobId}/investigation
    Method: createInvestigation(@PathVariable UUID jobId, @Valid @RequestBody CreateBugInvestigationRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateBugInvestigationRequest
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<BugInvestigationResponse>
    Status Codes: 201
    Audit Logged: Yes — "INVESTIGATION_CREATED"

─── PUT /api/v1/jobs/investigations/{investigationId}
    Method: updateInvestigation(@PathVariable UUID investigationId, @Valid @RequestBody UpdateBugInvestigationRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateBugInvestigationRequest
    Path Variables: UUID investigationId
    Query Params: none
    Response: ResponseEntity<BugInvestigationResponse>
    Status Codes: 200
    Audit Logged: Yes — "INVESTIGATION_UPDATED"
```

---

### === MetricsController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/MetricsController.java`

**Base Path**: `@RequestMapping("/api/v1/metrics")`

**Injected Dependencies**:
- `MetricsService metricsService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/metrics")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Metrics")`

**Endpoints**:

```
─── GET /api/v1/metrics/project/{projectId}
    Method: getProjectMetrics(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<ProjectMetricsResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/metrics/team/{teamId}
    Method: getTeamMetrics(@PathVariable UUID teamId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<TeamMetricsResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/metrics/project/{projectId}/trend
    Method: getHealthTrend(@PathVariable UUID projectId, @RequestParam(defaultValue = "30") int days)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: days (int, default 30)
    Response: ResponseEntity<List<HealthSnapshotResponse>>
    Status Codes: 200
    Audit Logged: No
```

---

### === PersonaController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/PersonaController.java`

**Base Path**: `@RequestMapping("/api/v1/personas")`

**Injected Dependencies**:
- `PersonaService personaService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/personas")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Personas")`

**Endpoints**:

```
─── POST /api/v1/personas
    Method: createPersona(@Valid @RequestBody CreatePersonaRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreatePersonaRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<PersonaResponse>
    Status Codes: 201
    Audit Logged: Yes — "PERSONA_CREATED"

─── GET /api/v1/personas/{personaId}
    Method: getPersona(@PathVariable UUID personaId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID personaId
    Query Params: none
    Response: ResponseEntity<PersonaResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/personas/team/{teamId}
    Method: getPersonasForTeam(@PathVariable UUID teamId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<PersonaResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/personas/team/{teamId}/agent/{agentType}
    Method: getPersonasByAgentType(@PathVariable UUID teamId, @PathVariable AgentType agentType)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId, AgentType agentType
    Query Params: none
    Response: ResponseEntity<List<PersonaResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/personas/team/{teamId}/default/{agentType}
    Method: getDefaultPersona(@PathVariable UUID teamId, @PathVariable AgentType agentType)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId, AgentType agentType
    Query Params: none
    Response: ResponseEntity<PersonaResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/personas/mine
    Method: getMyPersonas()
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<PersonaResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/personas/system
    Method: getSystemPersonas()
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<PersonaResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/personas/{personaId}
    Method: updatePersona(@PathVariable UUID personaId, @Valid @RequestBody UpdatePersonaRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdatePersonaRequest
    Path Variables: UUID personaId
    Query Params: none
    Response: ResponseEntity<PersonaResponse>
    Status Codes: 200
    Audit Logged: Yes — "PERSONA_UPDATED"

─── DELETE /api/v1/personas/{personaId}
    Method: deletePersona(@PathVariable UUID personaId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID personaId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "PERSONA_DELETED"

─── PUT /api/v1/personas/{personaId}/set-default
    Method: setAsDefault(@PathVariable UUID personaId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID personaId
    Query Params: none
    Response: ResponseEntity<PersonaResponse>
    Status Codes: 200
    Audit Logged: Yes — "PERSONA_SET_DEFAULT"

─── PUT /api/v1/personas/{personaId}/remove-default
    Method: removeDefault(@PathVariable UUID personaId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID personaId
    Query Params: none
    Response: ResponseEntity<PersonaResponse>
    Status Codes: 200
    Audit Logged: Yes — "PERSONA_REMOVED_DEFAULT"
```

---

### === ProjectController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/ProjectController.java`

**Base Path**: `@RequestMapping("/api/v1/projects")`

**Injected Dependencies**:
- `ProjectService projectService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/projects")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Projects")`

**Endpoints**:

```
─── POST /api/v1/projects/{teamId}
    Method: createProject(@PathVariable UUID teamId, @Valid @RequestBody CreateProjectRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateProjectRequest
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<ProjectResponse>
    Status Codes: 201
    Audit Logged: Yes — "PROJECT_CREATED"

─── GET /api/v1/projects/team/{teamId}
    Method: getProjects(@PathVariable UUID teamId, @RequestParam boolean includeArchived, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: includeArchived (boolean, default false), page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<ProjectResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/projects/{projectId}
    Method: getProject(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<ProjectResponse>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/projects/{projectId}
    Method: updateProject(@PathVariable UUID projectId, @Valid @RequestBody UpdateProjectRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateProjectRequest
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<ProjectResponse>
    Status Codes: 200
    Audit Logged: Yes — "PROJECT_UPDATED"

─── PUT /api/v1/projects/{projectId}/archive
    Method: archiveProject(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 200
    Audit Logged: Yes — "PROJECT_ARCHIVED"

─── PUT /api/v1/projects/{projectId}/unarchive
    Method: unarchiveProject(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 200
    Audit Logged: Yes — "PROJECT_UNARCHIVED"

─── DELETE /api/v1/projects/{projectId}
    Method: deleteProject(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "PROJECT_DELETED"
```

---

### === ReportController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/ReportController.java`

**Base Path**: `@RequestMapping("/api/v1/reports")`

**Injected Dependencies**:
- `ReportStorageService reportStorageService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/reports")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Reports")`

**Endpoints**:

```
─── POST /api/v1/reports/job/{jobId}/agent/{agentType}
    Method: uploadAgentReport(@PathVariable UUID jobId, @PathVariable AgentType agentType, @RequestBody String markdownContent)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: String (raw markdown content)
    Path Variables: UUID jobId, AgentType agentType
    Query Params: none
    Response: ResponseEntity<Map<String, String>>
    Status Codes: 201
    Audit Logged: No

─── POST /api/v1/reports/job/{jobId}/summary
    Method: uploadSummaryReport(@PathVariable UUID jobId, @RequestBody String markdownContent)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: String (raw markdown content)
    Path Variables: UUID jobId
    Query Params: none
    Response: ResponseEntity<Map<String, String>>
    Status Codes: 201
    Audit Logged: No

─── GET /api/v1/reports/download
    Method: downloadReport(@RequestParam String s3Key)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: String s3Key
    Response: ResponseEntity<String> (content-type: text/markdown)
    Status Codes: 200
    Audit Logged: No

─── POST /api/v1/reports/job/{jobId}/spec
    Method: uploadSpecification(@PathVariable UUID jobId, @RequestParam("file") MultipartFile file)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: MultipartFile (form data)
    Path Variables: UUID jobId
    Query Params: file (MultipartFile)
    Response: ResponseEntity<Map<String, String>>
    Status Codes: 201
    Audit Logged: No
    Notes: Max 50MB, allowed content types: PDF, text/plain, text/markdown, text/csv, JSON, XML, PNG, JPEG, GIF

─── GET /api/v1/reports/spec/download
    Method: downloadSpecification(@RequestParam String s3Key)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: String s3Key
    Response: ResponseEntity<byte[]> (content-type: application/octet-stream)
    Status Codes: 200
    Audit Logged: No
```

---

### === TaskController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/TaskController.java`

**Base Path**: `@RequestMapping("/api/v1/tasks")`

**Injected Dependencies**:
- `RemediationTaskService remediationTaskService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/tasks")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Remediation Tasks")`

**Endpoints**:

```
─── POST /api/v1/tasks
    Method: createTask(@Valid @RequestBody CreateTaskRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateTaskRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<TaskResponse>
    Status Codes: 201
    Audit Logged: Yes — "TASK_CREATED"

─── POST /api/v1/tasks/batch
    Method: createTasks(@Valid @RequestBody List<CreateTaskRequest> requests)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: List<CreateTaskRequest>
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<TaskResponse>>
    Status Codes: 201
    Audit Logged: Yes — "TASK_CREATED" (per item)

─── GET /api/v1/tasks/job/{jobId}
    Method: getTasksForJob(@PathVariable UUID jobId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID jobId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<TaskResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/tasks/{taskId}
    Method: getTask(@PathVariable UUID taskId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID taskId
    Query Params: none
    Response: ResponseEntity<TaskResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/tasks/assigned-to-me
    Method: getAssignedTasks(@RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<TaskResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/tasks/{taskId}
    Method: updateTask(@PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateTaskRequest
    Path Variables: UUID taskId
    Query Params: none
    Response: ResponseEntity<TaskResponse>
    Status Codes: 200
    Audit Logged: Yes — "TASK_UPDATED"
```

---

### === TeamController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/TeamController.java`

**Base Path**: `@RequestMapping("/api/v1/teams")`

**Injected Dependencies**:
- `TeamService teamService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/teams")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Teams")`

**Endpoints**:

```
─── POST /api/v1/teams
    Method: createTeam(@Valid @RequestBody CreateTeamRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateTeamRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<TeamResponse>
    Status Codes: 201
    Audit Logged: Yes — "TEAM_CREATED"

─── GET /api/v1/teams
    Method: getTeams()
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<TeamResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/teams/{teamId}
    Method: getTeam(@PathVariable UUID teamId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<TeamResponse>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/teams/{teamId}
    Method: updateTeam(@PathVariable UUID teamId, @Valid @RequestBody UpdateTeamRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateTeamRequest
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<TeamResponse>
    Status Codes: 200
    Audit Logged: Yes — "TEAM_UPDATED"

─── DELETE /api/v1/teams/{teamId}
    Method: deleteTeam(@PathVariable UUID teamId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "TEAM_DELETED"

─── GET /api/v1/teams/{teamId}/members
    Method: getTeamMembers(@PathVariable UUID teamId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<List<TeamMemberResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/teams/{teamId}/members/{userId}/role
    Method: updateMemberRole(@PathVariable UUID teamId, @PathVariable UUID userId, @Valid @RequestBody UpdateMemberRoleRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateMemberRoleRequest
    Path Variables: UUID teamId, UUID userId
    Query Params: none
    Response: ResponseEntity<TeamMemberResponse>
    Status Codes: 200
    Audit Logged: Yes — "MEMBER_ROLE_UPDATED"

─── DELETE /api/v1/teams/{teamId}/members/{userId}
    Method: removeMember(@PathVariable UUID teamId, @PathVariable UUID userId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId, UUID userId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "MEMBER_REMOVED"

─── POST /api/v1/teams/{teamId}/invitations
    Method: inviteMember(@PathVariable UUID teamId, @Valid @RequestBody InviteMemberRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: InviteMemberRequest
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<InvitationResponse>
    Status Codes: 201
    Audit Logged: Yes — "MEMBER_INVITED"

─── GET /api/v1/teams/{teamId}/invitations
    Method: getTeamInvitations(@PathVariable UUID teamId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId
    Query Params: none
    Response: ResponseEntity<List<InvitationResponse>>
    Status Codes: 200
    Audit Logged: No

─── DELETE /api/v1/teams/{teamId}/invitations/{invitationId}
    Method: cancelInvitation(@PathVariable UUID teamId, @PathVariable UUID invitationId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID teamId, UUID invitationId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "INVITATION_CANCELLED"

─── POST /api/v1/teams/invitations/{token}/accept
    Method: acceptInvitation(@PathVariable String token)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: String token
    Query Params: none
    Response: ResponseEntity<TeamResponse>
    Status Codes: 200
    Audit Logged: Yes — "INVITATION_ACCEPTED"
```

---

### === TechDebtController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/TechDebtController.java`

**Base Path**: `@RequestMapping("/api/v1/tech-debt")`

**Injected Dependencies**:
- `TechDebtService techDebtService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/tech-debt")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Tech Debt")`

**Endpoints**:

```
─── POST /api/v1/tech-debt
    Method: createTechDebtItem(@Valid @RequestBody CreateTechDebtItemRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: CreateTechDebtItemRequest
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<TechDebtItemResponse>
    Status Codes: 201
    Audit Logged: No

─── POST /api/v1/tech-debt/batch
    Method: createTechDebtItems(@Valid @RequestBody List<CreateTechDebtItemRequest> requests)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: List<CreateTechDebtItemRequest>
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<List<TechDebtItemResponse>>
    Status Codes: 201
    Audit Logged: No

─── GET /api/v1/tech-debt/{itemId}
    Method: getTechDebtItem(@PathVariable UUID itemId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID itemId
    Query Params: none
    Response: ResponseEntity<TechDebtItemResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/tech-debt/project/{projectId}
    Method: getTechDebtForProject(@PathVariable UUID projectId, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<TechDebtItemResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/tech-debt/project/{projectId}/status/{status}
    Method: getTechDebtByStatus(@PathVariable UUID projectId, @PathVariable DebtStatus status, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId, DebtStatus status
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<TechDebtItemResponse>>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/tech-debt/project/{projectId}/category/{category}
    Method: getTechDebtByCategory(@PathVariable UUID projectId, @PathVariable DebtCategory category, @RequestParam int page, @RequestParam int size)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId, DebtCategory category
    Query Params: page (int, default 0), size (int, default 20)
    Response: ResponseEntity<PageResponse<TechDebtItemResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/tech-debt/{itemId}/status
    Method: updateTechDebtStatus(@PathVariable UUID itemId, @Valid @RequestBody UpdateTechDebtStatusRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateTechDebtStatusRequest
    Path Variables: UUID itemId
    Query Params: none
    Response: ResponseEntity<TechDebtItemResponse>
    Status Codes: 200
    Audit Logged: Yes — "TECH_DEBT_STATUS_UPDATED"

─── DELETE /api/v1/tech-debt/{itemId}
    Method: deleteTechDebtItem(@PathVariable UUID itemId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID itemId
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "TECH_DEBT_DELETED"

─── GET /api/v1/tech-debt/project/{projectId}/summary
    Method: getDebtSummary(@PathVariable UUID projectId)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID projectId
    Query Params: none
    Response: ResponseEntity<Map<String, Object>>
    Status Codes: 200
    Audit Logged: No
```

---

### === UserController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/controller/UserController.java`

**Base Path**: `@RequestMapping("/api/v1/users")`

**Injected Dependencies**:
- `UserService userService`
- `AuditLogService auditLogService`

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/users")`
- `@RequiredArgsConstructor`
- `@Tag(name = "Users")`

**Endpoints**:

```
─── GET /api/v1/users/me
    Method: getCurrentUser()
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<UserResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/users/{id}
    Method: getUserById(@PathVariable UUID id)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: UUID id
    Query Params: none
    Response: ResponseEntity<UserResponse>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/users/{id}
    Method: updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: UpdateUserRequest
    Path Variables: UUID id
    Query Params: none
    Response: ResponseEntity<UserResponse>
    Status Codes: 200
    Audit Logged: No

─── GET /api/v1/users/search
    Method: searchUsers(@RequestParam @NotBlank @Size(min = 2, max = 100) String q)
    Auth: @PreAuthorize("isAuthenticated()")
    Request Body: none
    Path Variables: none
    Query Params: String q (required, min 2, max 100 chars)
    Response: ResponseEntity<List<UserResponse>>
    Status Codes: 200
    Audit Logged: No

─── PUT /api/v1/users/{id}/deactivate
    Method: deactivateUser(@PathVariable UUID id)
    Auth: @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: none
    Path Variables: UUID id
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "USER_DEACTIVATED"

─── PUT /api/v1/users/{id}/activate
    Method: activateUser(@PathVariable UUID id)
    Auth: @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    Request Body: none
    Path Variables: UUID id
    Query Params: none
    Response: ResponseEntity<Void>
    Status Codes: 204
    Audit Logged: Yes — "USER_ACTIVATED"
```

---

### === HealthController.java ===

**File**: `/Users/adamallard/Documents/GitHub/CodeOps-Server/src/main/java/com/codeops/config/HealthController.java`

**Base Path**: `@RequestMapping("/api/v1/health")`

**Injected Dependencies**: none

**Class-Level Annotations**:
- `@RestController`
- `@RequestMapping("/api/v1/health")`
- `@Tag(name = "Health")`

**Endpoints**:

```
─── GET /api/v1/health
    Method: health()
    Auth: none (public)
    Request Body: none
    Path Variables: none
    Query Params: none
    Response: ResponseEntity<Map<String, Object>> — returns {status, service, timestamp}
    Status Codes: 200
    Audit Logged: No
```

---
---

## Section 12: REST API Surface — Complete Endpoint Inventory

Total: **18 controllers**, **141 endpoints**.

| # | METHOD | PATH | AUTH | REQUEST_BODY | RESPONSE_BODY |
|---|--------|------|------|-------------|---------------|
| | **AdminController** | | `hasRole('ADMIN') or hasRole('OWNER')` (class-level) | | |
| 1 | GET | `/api/v1/admin/users` | class-level | - | `Page<UserResponse>` |
| 2 | GET | `/api/v1/admin/users/{userId}` | class-level | - | `UserResponse` |
| 3 | PUT | `/api/v1/admin/users/{userId}` | class-level | `AdminUpdateUserRequest` | `UserResponse` |
| 4 | GET | `/api/v1/admin/settings` | class-level | - | `List<SystemSettingResponse>` |
| 5 | GET | `/api/v1/admin/settings/{key}` | class-level | - | `SystemSettingResponse` |
| 6 | PUT | `/api/v1/admin/settings` | class-level | `UpdateSystemSettingRequest` | `SystemSettingResponse` |
| 7 | GET | `/api/v1/admin/usage` | class-level | - | `Map<String, Object>` |
| 8 | GET | `/api/v1/admin/audit-log/team/{teamId}` | class-level | - | `Page<AuditLogResponse>` |
| 9 | GET | `/api/v1/admin/audit-log/user/{userId}` | class-level | - | `Page<AuditLogResponse>` |
| | **AuthController** | | varies per endpoint | | |
| 10 | POST | `/api/v1/auth/register` | none (public) | `RegisterRequest` | `AuthResponse` |
| 11 | POST | `/api/v1/auth/login` | none (public) | `LoginRequest` | `AuthResponse` |
| 12 | POST | `/api/v1/auth/refresh` | none (public) | `RefreshTokenRequest` | `AuthResponse` |
| 13 | POST | `/api/v1/auth/logout` | `isAuthenticated()` | - | `Void` (204) |
| 14 | POST | `/api/v1/auth/change-password` | `isAuthenticated()` | `ChangePasswordRequest` | `Void` (200) |
| | **ComplianceController** | | `isAuthenticated()` (per endpoint) | | |
| 15 | POST | `/api/v1/compliance/specs` | `isAuthenticated()` | `CreateSpecificationRequest` | `SpecificationResponse` |
| 16 | GET | `/api/v1/compliance/specs/job/{jobId}` | `isAuthenticated()` | - | `PageResponse<SpecificationResponse>` |
| 17 | POST | `/api/v1/compliance/items` | `isAuthenticated()` | `CreateComplianceItemRequest` | `ComplianceItemResponse` |
| 18 | POST | `/api/v1/compliance/items/batch` | `isAuthenticated()` | `List<CreateComplianceItemRequest>` | `List<ComplianceItemResponse>` |
| 19 | GET | `/api/v1/compliance/items/job/{jobId}` | `isAuthenticated()` | - | `PageResponse<ComplianceItemResponse>` |
| 20 | GET | `/api/v1/compliance/items/job/{jobId}/status/{status}` | `isAuthenticated()` | - | `PageResponse<ComplianceItemResponse>` |
| 21 | GET | `/api/v1/compliance/summary/job/{jobId}` | `isAuthenticated()` | - | `Map<String, Object>` |
| | **DependencyController** | | `isAuthenticated()` (per endpoint) | | |
| 22 | POST | `/api/v1/dependencies/scans` | `isAuthenticated()` | `CreateDependencyScanRequest` | `DependencyScanResponse` |
| 23 | GET | `/api/v1/dependencies/scans/{scanId}` | `isAuthenticated()` | - | `DependencyScanResponse` |
| 24 | GET | `/api/v1/dependencies/scans/project/{projectId}` | `isAuthenticated()` | - | `PageResponse<DependencyScanResponse>` |
| 25 | GET | `/api/v1/dependencies/scans/project/{projectId}/latest` | `isAuthenticated()` | - | `DependencyScanResponse` |
| 26 | POST | `/api/v1/dependencies/vulnerabilities` | `isAuthenticated()` | `CreateVulnerabilityRequest` | `VulnerabilityResponse` |
| 27 | POST | `/api/v1/dependencies/vulnerabilities/batch` | `isAuthenticated()` | `List<CreateVulnerabilityRequest>` | `List<VulnerabilityResponse>` |
| 28 | GET | `/api/v1/dependencies/vulnerabilities/scan/{scanId}` | `isAuthenticated()` | - | `PageResponse<VulnerabilityResponse>` |
| 29 | GET | `/api/v1/dependencies/vulnerabilities/scan/{scanId}/severity/{severity}` | `isAuthenticated()` | - | `PageResponse<VulnerabilityResponse>` |
| 30 | GET | `/api/v1/dependencies/vulnerabilities/scan/{scanId}/open` | `isAuthenticated()` | - | `PageResponse<VulnerabilityResponse>` |
| 31 | PUT | `/api/v1/dependencies/vulnerabilities/{vulnerabilityId}/status` | `isAuthenticated()` | - (query param) | `VulnerabilityResponse` |
| | **DirectiveController** | | `isAuthenticated()` (per endpoint) | | |
| 32 | POST | `/api/v1/directives` | `isAuthenticated()` | `CreateDirectiveRequest` | `DirectiveResponse` |
| 33 | GET | `/api/v1/directives/{directiveId}` | `isAuthenticated()` | - | `DirectiveResponse` |
| 34 | GET | `/api/v1/directives/team/{teamId}` | `isAuthenticated()` | - | `List<DirectiveResponse>` |
| 35 | GET | `/api/v1/directives/project/{projectId}` | `isAuthenticated()` | - | `List<DirectiveResponse>` |
| 36 | PUT | `/api/v1/directives/{directiveId}` | `isAuthenticated()` | `UpdateDirectiveRequest` | `DirectiveResponse` |
| 37 | DELETE | `/api/v1/directives/{directiveId}` | `isAuthenticated()` | - | `Void` (204) |
| 38 | POST | `/api/v1/directives/assign` | `isAuthenticated()` | `AssignDirectiveRequest` | `ProjectDirectiveResponse` |
| 39 | DELETE | `/api/v1/directives/project/{projectId}/directive/{directiveId}` | `isAuthenticated()` | - | `Void` (204) |
| 40 | GET | `/api/v1/directives/project/{projectId}/assignments` | `isAuthenticated()` | - | `List<ProjectDirectiveResponse>` |
| 41 | GET | `/api/v1/directives/project/{projectId}/enabled` | `isAuthenticated()` | - | `List<DirectiveResponse>` |
| 42 | PUT | `/api/v1/directives/project/{projectId}/directive/{directiveId}/toggle` | `isAuthenticated()` | - (query param) | `ProjectDirectiveResponse` |
| | **FindingController** | | `isAuthenticated()` (per endpoint) | | |
| 43 | POST | `/api/v1/findings` | `isAuthenticated()` | `CreateFindingRequest` | `FindingResponse` |
| 44 | POST | `/api/v1/findings/batch` | `isAuthenticated()` | `List<CreateFindingRequest>` | `List<FindingResponse>` |
| 45 | GET | `/api/v1/findings/{findingId}` | `isAuthenticated()` | - | `FindingResponse` |
| 46 | GET | `/api/v1/findings/job/{jobId}` | `isAuthenticated()` | - | `PageResponse<FindingResponse>` |
| 47 | GET | `/api/v1/findings/job/{jobId}/severity/{severity}` | `isAuthenticated()` | - | `PageResponse<FindingResponse>` |
| 48 | GET | `/api/v1/findings/job/{jobId}/agent/{agentType}` | `isAuthenticated()` | - | `PageResponse<FindingResponse>` |
| 49 | GET | `/api/v1/findings/job/{jobId}/status/{status}` | `isAuthenticated()` | - | `PageResponse<FindingResponse>` |
| 50 | GET | `/api/v1/findings/job/{jobId}/counts` | `isAuthenticated()` | - | `Map<Severity, Long>` |
| 51 | PUT | `/api/v1/findings/{findingId}/status` | `isAuthenticated()` | `UpdateFindingStatusRequest` | `FindingResponse` |
| 52 | PUT | `/api/v1/findings/bulk-status` | `isAuthenticated()` | `BulkUpdateFindingsRequest` | `List<FindingResponse>` |
| | **HealthMonitorController** | | `isAuthenticated()` (per endpoint) | | |
| 53 | POST | `/api/v1/health-monitor/schedules` | `isAuthenticated()` | `CreateHealthScheduleRequest` | `HealthScheduleResponse` |
| 54 | GET | `/api/v1/health-monitor/schedules/project/{projectId}` | `isAuthenticated()` | - | `List<HealthScheduleResponse>` |
| 55 | PUT | `/api/v1/health-monitor/schedules/{scheduleId}` | `isAuthenticated()` | - (query param) | `HealthScheduleResponse` |
| 56 | DELETE | `/api/v1/health-monitor/schedules/{scheduleId}` | `isAuthenticated()` | - | `Void` (204) |
| 57 | POST | `/api/v1/health-monitor/snapshots` | `isAuthenticated()` | `CreateHealthSnapshotRequest` | `HealthSnapshotResponse` |
| 58 | GET | `/api/v1/health-monitor/snapshots/project/{projectId}` | `isAuthenticated()` | - | `PageResponse<HealthSnapshotResponse>` |
| 59 | GET | `/api/v1/health-monitor/snapshots/project/{projectId}/latest` | `isAuthenticated()` | - | `HealthSnapshotResponse` |
| 60 | GET | `/api/v1/health-monitor/snapshots/project/{projectId}/trend` | `isAuthenticated()` | - | `List<HealthSnapshotResponse>` |
| | **IntegrationController** | | `isAuthenticated()` (per endpoint) | | |
| 61 | POST | `/api/v1/integrations/github/{teamId}` | `isAuthenticated()` | `CreateGitHubConnectionRequest` | `GitHubConnectionResponse` |
| 62 | GET | `/api/v1/integrations/github/{teamId}` | `isAuthenticated()` | - | `List<GitHubConnectionResponse>` |
| 63 | GET | `/api/v1/integrations/github/{teamId}/{connectionId}` | `isAuthenticated()` | - | `GitHubConnectionResponse` |
| 64 | DELETE | `/api/v1/integrations/github/{teamId}/{connectionId}` | `isAuthenticated()` | - | `Void` (204) |
| 65 | POST | `/api/v1/integrations/jira/{teamId}` | `isAuthenticated()` | `CreateJiraConnectionRequest` | `JiraConnectionResponse` |
| 66 | GET | `/api/v1/integrations/jira/{teamId}` | `isAuthenticated()` | - | `List<JiraConnectionResponse>` |
| 67 | GET | `/api/v1/integrations/jira/{teamId}/{connectionId}` | `isAuthenticated()` | - | `JiraConnectionResponse` |
| 68 | DELETE | `/api/v1/integrations/jira/{teamId}/{connectionId}` | `isAuthenticated()` | - | `Void` (204) |
| | **JobController** | | `isAuthenticated()` (per endpoint) | | |
| 69 | POST | `/api/v1/jobs` | `isAuthenticated()` | `CreateJobRequest` | `JobResponse` |
| 70 | GET | `/api/v1/jobs/{jobId}` | `isAuthenticated()` | - | `JobResponse` |
| 71 | GET | `/api/v1/jobs/project/{projectId}` | `isAuthenticated()` | - | `PageResponse<JobSummaryResponse>` |
| 72 | GET | `/api/v1/jobs/mine` | `isAuthenticated()` | - | `PageResponse<JobSummaryResponse>` |
| 73 | PUT | `/api/v1/jobs/{jobId}` | `isAuthenticated()` | `UpdateJobRequest` | `JobResponse` |
| 74 | DELETE | `/api/v1/jobs/{jobId}` | `isAuthenticated()` | - | `Void` (204) |
| 75 | POST | `/api/v1/jobs/{jobId}/agents` | `isAuthenticated()` | `CreateAgentRunRequest` | `AgentRunResponse` |
| 76 | POST | `/api/v1/jobs/{jobId}/agents/batch` | `isAuthenticated()` | `List<AgentType>` | `List<AgentRunResponse>` |
| 77 | GET | `/api/v1/jobs/{jobId}/agents` | `isAuthenticated()` | - | `List<AgentRunResponse>` |
| 78 | PUT | `/api/v1/jobs/agents/{agentRunId}` | `isAuthenticated()` | `UpdateAgentRunRequest` | `AgentRunResponse` |
| 79 | GET | `/api/v1/jobs/{jobId}/investigation` | `isAuthenticated()` | - | `BugInvestigationResponse` |
| 80 | POST | `/api/v1/jobs/{jobId}/investigation` | `isAuthenticated()` | `CreateBugInvestigationRequest` | `BugInvestigationResponse` |
| 81 | PUT | `/api/v1/jobs/investigations/{investigationId}` | `isAuthenticated()` | `UpdateBugInvestigationRequest` | `BugInvestigationResponse` |
| | **MetricsController** | | `isAuthenticated()` (per endpoint) | | |
| 82 | GET | `/api/v1/metrics/project/{projectId}` | `isAuthenticated()` | - | `ProjectMetricsResponse` |
| 83 | GET | `/api/v1/metrics/team/{teamId}` | `isAuthenticated()` | - | `TeamMetricsResponse` |
| 84 | GET | `/api/v1/metrics/project/{projectId}/trend` | `isAuthenticated()` | - | `List<HealthSnapshotResponse>` |
| | **PersonaController** | | `isAuthenticated()` (per endpoint) | | |
| 85 | POST | `/api/v1/personas` | `isAuthenticated()` | `CreatePersonaRequest` | `PersonaResponse` |
| 86 | GET | `/api/v1/personas/{personaId}` | `isAuthenticated()` | - | `PersonaResponse` |
| 87 | GET | `/api/v1/personas/team/{teamId}` | `isAuthenticated()` | - | `PageResponse<PersonaResponse>` |
| 88 | GET | `/api/v1/personas/team/{teamId}/agent/{agentType}` | `isAuthenticated()` | - | `List<PersonaResponse>` |
| 89 | GET | `/api/v1/personas/team/{teamId}/default/{agentType}` | `isAuthenticated()` | - | `PersonaResponse` |
| 90 | GET | `/api/v1/personas/mine` | `isAuthenticated()` | - | `List<PersonaResponse>` |
| 91 | GET | `/api/v1/personas/system` | `isAuthenticated()` | - | `List<PersonaResponse>` |
| 92 | PUT | `/api/v1/personas/{personaId}` | `isAuthenticated()` | `UpdatePersonaRequest` | `PersonaResponse` |
| 93 | DELETE | `/api/v1/personas/{personaId}` | `isAuthenticated()` | - | `Void` (204) |
| 94 | PUT | `/api/v1/personas/{personaId}/set-default` | `isAuthenticated()` | - | `PersonaResponse` |
| 95 | PUT | `/api/v1/personas/{personaId}/remove-default` | `isAuthenticated()` | - | `PersonaResponse` |
| | **ProjectController** | | `isAuthenticated()` (per endpoint) | | |
| 96 | POST | `/api/v1/projects/{teamId}` | `isAuthenticated()` | `CreateProjectRequest` | `ProjectResponse` |
| 97 | GET | `/api/v1/projects/team/{teamId}` | `isAuthenticated()` | - | `PageResponse<ProjectResponse>` |
| 98 | GET | `/api/v1/projects/{projectId}` | `isAuthenticated()` | - | `ProjectResponse` |
| 99 | PUT | `/api/v1/projects/{projectId}` | `isAuthenticated()` | `UpdateProjectRequest` | `ProjectResponse` |
| 100 | PUT | `/api/v1/projects/{projectId}/archive` | `isAuthenticated()` | - | `Void` (200) |
| 101 | PUT | `/api/v1/projects/{projectId}/unarchive` | `isAuthenticated()` | - | `Void` (200) |
| 102 | DELETE | `/api/v1/projects/{projectId}` | `isAuthenticated()` | - | `Void` (204) |
| | **ReportController** | | `isAuthenticated()` (per endpoint) | | |
| 103 | POST | `/api/v1/reports/job/{jobId}/agent/{agentType}` | `isAuthenticated()` | `String` (markdown) | `Map<String, String>` |
| 104 | POST | `/api/v1/reports/job/{jobId}/summary` | `isAuthenticated()` | `String` (markdown) | `Map<String, String>` |
| 105 | GET | `/api/v1/reports/download` | `isAuthenticated()` | - | `String` (text/markdown) |
| 106 | POST | `/api/v1/reports/job/{jobId}/spec` | `isAuthenticated()` | `MultipartFile` | `Map<String, String>` |
| 107 | GET | `/api/v1/reports/spec/download` | `isAuthenticated()` | - | `byte[]` (application/octet-stream) |
| | **TaskController** | | `isAuthenticated()` (per endpoint) | | |
| 108 | POST | `/api/v1/tasks` | `isAuthenticated()` | `CreateTaskRequest` | `TaskResponse` |
| 109 | POST | `/api/v1/tasks/batch` | `isAuthenticated()` | `List<CreateTaskRequest>` | `List<TaskResponse>` |
| 110 | GET | `/api/v1/tasks/job/{jobId}` | `isAuthenticated()` | - | `PageResponse<TaskResponse>` |
| 111 | GET | `/api/v1/tasks/{taskId}` | `isAuthenticated()` | - | `TaskResponse` |
| 112 | GET | `/api/v1/tasks/assigned-to-me` | `isAuthenticated()` | - | `PageResponse<TaskResponse>` |
| 113 | PUT | `/api/v1/tasks/{taskId}` | `isAuthenticated()` | `UpdateTaskRequest` | `TaskResponse` |
| | **TeamController** | | `isAuthenticated()` (per endpoint) | | |
| 114 | POST | `/api/v1/teams` | `isAuthenticated()` | `CreateTeamRequest` | `TeamResponse` |
| 115 | GET | `/api/v1/teams` | `isAuthenticated()` | - | `List<TeamResponse>` |
| 116 | GET | `/api/v1/teams/{teamId}` | `isAuthenticated()` | - | `TeamResponse` |
| 117 | PUT | `/api/v1/teams/{teamId}` | `isAuthenticated()` | `UpdateTeamRequest` | `TeamResponse` |
| 118 | DELETE | `/api/v1/teams/{teamId}` | `isAuthenticated()` | - | `Void` (204) |
| 119 | GET | `/api/v1/teams/{teamId}/members` | `isAuthenticated()` | - | `List<TeamMemberResponse>` |
| 120 | PUT | `/api/v1/teams/{teamId}/members/{userId}/role` | `isAuthenticated()` | `UpdateMemberRoleRequest` | `TeamMemberResponse` |
| 121 | DELETE | `/api/v1/teams/{teamId}/members/{userId}` | `isAuthenticated()` | - | `Void` (204) |
| 122 | POST | `/api/v1/teams/{teamId}/invitations` | `isAuthenticated()` | `InviteMemberRequest` | `InvitationResponse` |
| 123 | GET | `/api/v1/teams/{teamId}/invitations` | `isAuthenticated()` | - | `List<InvitationResponse>` |
| 124 | DELETE | `/api/v1/teams/{teamId}/invitations/{invitationId}` | `isAuthenticated()` | - | `Void` (204) |
| 125 | POST | `/api/v1/teams/invitations/{token}/accept` | `isAuthenticated()` | - | `TeamResponse` |
| | **TechDebtController** | | `isAuthenticated()` (per endpoint) | | |
| 126 | POST | `/api/v1/tech-debt` | `isAuthenticated()` | `CreateTechDebtItemRequest` | `TechDebtItemResponse` |
| 127 | POST | `/api/v1/tech-debt/batch` | `isAuthenticated()` | `List<CreateTechDebtItemRequest>` | `List<TechDebtItemResponse>` |
| 128 | GET | `/api/v1/tech-debt/{itemId}` | `isAuthenticated()` | - | `TechDebtItemResponse` |
| 129 | GET | `/api/v1/tech-debt/project/{projectId}` | `isAuthenticated()` | - | `PageResponse<TechDebtItemResponse>` |
| 130 | GET | `/api/v1/tech-debt/project/{projectId}/status/{status}` | `isAuthenticated()` | - | `PageResponse<TechDebtItemResponse>` |
| 131 | GET | `/api/v1/tech-debt/project/{projectId}/category/{category}` | `isAuthenticated()` | - | `PageResponse<TechDebtItemResponse>` |
| 132 | PUT | `/api/v1/tech-debt/{itemId}/status` | `isAuthenticated()` | `UpdateTechDebtStatusRequest` | `TechDebtItemResponse` |
| 133 | DELETE | `/api/v1/tech-debt/{itemId}` | `isAuthenticated()` | - | `Void` (204) |
| 134 | GET | `/api/v1/tech-debt/project/{projectId}/summary` | `isAuthenticated()` | - | `Map<String, Object>` |
| | **UserController** | | varies per endpoint | | |
| 135 | GET | `/api/v1/users/me` | `isAuthenticated()` | - | `UserResponse` |
| 136 | GET | `/api/v1/users/{id}` | `isAuthenticated()` | - | `UserResponse` |
| 137 | PUT | `/api/v1/users/{id}` | `isAuthenticated()` | `UpdateUserRequest` | `UserResponse` |
| 138 | GET | `/api/v1/users/search` | `isAuthenticated()` | - | `List<UserResponse>` |
| 139 | PUT | `/api/v1/users/{id}/deactivate` | `hasRole('ADMIN') or hasRole('OWNER')` | - | `Void` (204) |
| 140 | PUT | `/api/v1/users/{id}/activate` | `hasRole('ADMIN') or hasRole('OWNER')` | - | `Void` (204) |
| | **HealthController** (in config package) | | | | |
| 141 | GET | `/api/v1/health` | none (public) | - | `Map<String, Object>` |

### Summary Statistics

| Metric | Count |
|--------|-------|
| Total Controllers | 18 |
| Total Endpoints | 141 |
| GET endpoints | 72 |
| POST endpoints | 36 |
| PUT endpoints | 24 |
| DELETE endpoints | 9 |
| Public (unauthenticated) endpoints | 4 (`register`, `login`, `refresh`, `health`) |
| Admin/Owner-only endpoints | 11 (9 AdminController + 2 UserController) |
| Authenticated endpoints | 126 |
| Paginated endpoints | 28 |
| Batch/bulk endpoints | 7 |

### Authorization Breakdown

| Auth Level | Endpoints | Controllers |
|------------|-----------|-------------|
| **Public** (no auth) | 4 | AuthController (3), HealthController (1) |
| **`hasRole('ADMIN') or hasRole('OWNER')`** | 11 | AdminController (9 via class-level), UserController (2) |
| **`isAuthenticated()`** | 126 | All remaining endpoints across 16 controllers |

### Audit Logging Coverage by Controller

| Controller | Total Endpoints | Audited | Not Audited |
|------------|----------------|---------|-------------|
| AdminController | 9 | 2 | 7 |
| AuthController | 5 | 2 | 3 |
| ComplianceController | 7 | 3 | 4 |
| DependencyController | 10 | 4 | 6 |
| DirectiveController | 11 | 5 | 6 |
| FindingController | 10 | 4 | 6 |
| HealthMonitorController | 8 | 2 | 6 |
| IntegrationController | 8 | 4 | 4 |
| JobController | 13 | 8 | 5 |
| MetricsController | 3 | 0 | 3 |
| PersonaController | 11 | 6 | 5 |
| ProjectController | 7 | 5 | 2 |
| ReportController | 5 | 0 | 5 |
| TaskController | 6 | 3 | 3 |
| TeamController | 12 | 7 | 5 |
| TechDebtController | 9 | 2 | 7 |
| UserController | 6 | 2 | 4 |
| HealthController | 1 | 0 | 1 |
| **TOTAL** | **141** | **59** | **82** |


---

## Section 13: Security Architecture

### 13.1 Source Files

| File | Path |
|------|------|
| SecurityConfig.java | `src/main/java/com/codeops/security/SecurityConfig.java` |
| JwtTokenProvider.java | `src/main/java/com/codeops/security/JwtTokenProvider.java` |
| JwtAuthFilter.java | `src/main/java/com/codeops/security/JwtAuthFilter.java` |
| RateLimitFilter.java | `src/main/java/com/codeops/security/RateLimitFilter.java` |
| SecurityUtils.java | `src/main/java/com/codeops/security/SecurityUtils.java` |
| JwtProperties.java | `src/main/java/com/codeops/config/JwtProperties.java` |
| CorsConfig.java | `src/main/java/com/codeops/config/CorsConfig.java` |
| TokenBlacklistService.java | `src/main/java/com/codeops/service/TokenBlacklistService.java` |
| EncryptionService.java | `src/main/java/com/codeops/service/EncryptionService.java` |
| AuthService.java | `src/main/java/com/codeops/service/AuthService.java` |
| AuthController.java | `src/main/java/com/codeops/controller/AuthController.java` |

### 13.2 Authentication Flow (JWT HS256)

The application uses stateless JWT authentication with HS256 signing. There is no session storage; every request must carry a valid bearer token.

#### Token Generation

`JwtTokenProvider.generateToken()` creates an access token with the following claims:

| Claim | Value |
|-------|-------|
| `sub` | User UUID (string) |
| `email` | User email address |
| `roles` | List of team role names (OWNER, ADMIN, MEMBER, VIEWER) |
| `jti` | Random UUID (for blacklist support) |
| `iat` | Issued-at timestamp |
| `exp` | Expiration timestamp |

The signing key is derived from `codeops.jwt.secret` (environment variable `JWT_SECRET`). A `@PostConstruct` validator enforces a minimum length of 32 characters, throwing `IllegalStateException` on startup if violated. The key is converted to bytes and passed to `Keys.hmacShaKeyFor()` for HMAC-SHA256 signing via `Jwts.SIG.HS256`.

#### Token Lifetimes (from JwtProperties and AppConstants)

| Token Type | Default Lifetime | Config Property |
|------------|-----------------|-----------------|
| Access Token | 24 hours | `codeops.jwt.expirationHours` (default: 24) |
| Refresh Token | 30 days | `codeops.jwt.refreshExpirationDays` (default: 30) |

The refresh token is identical in structure except it carries an additional `"type": "refresh"` claim and does not carry `email` or `roles` claims.

#### Token Validation

`JwtTokenProvider.validateToken()` performs the following checks in order:

1. Parses and verifies the HS256 signature against the signing key.
2. Checks expiration (catches `ExpiredJwtException`).
3. Extracts the `jti` claim and checks against the in-memory blacklist via `TokenBlacklistService.isBlacklisted()`.
4. Returns `false` for `UnsupportedJwtException`, `MalformedJwtException`, `SignatureException`, and `IllegalArgumentException`.
5. All validation failures are logged at WARN level but do not expose details to the caller.

#### Token Blacklist

`TokenBlacklistService` maintains a `ConcurrentHashMap.KeySetView<String, Boolean>` of blacklisted JTI values. On logout, the `AuthController.logout()` endpoint extracts the `jti` from the current token and adds it to the blacklist.

**Critical observation**: The blacklist is **in-memory only**. It is lost on application restart, meaning logged-out tokens remain valid until their natural expiration. There is no persistent store (no Redis, no database table) backing the blacklist. The `expiry` parameter passed to `blacklist()` is accepted but **never used** -- there is no expiry-based eviction, meaning the set grows unboundedly for the lifetime of the process.

#### Authentication Flow (Login)

1. `AuthController.login()` receives `LoginRequest` (email + password).
2. `AuthService.login()` looks up the user by email.
3. Verifies the account is active (`isActive == true`).
4. Verifies the password using `BCryptPasswordEncoder.matches()`.
5. Updates `lastLoginAt` timestamp.
6. Fetches user roles from `TeamMemberRepository.findByUserId()` -- roles are team-scoped (`TeamRole` enum values: OWNER, ADMIN, MEMBER, VIEWER).
7. Generates an access token and a refresh token.
8. Returns `AuthResponse` containing both tokens and a `UserResponse`.
9. Audit log entry created asynchronously (`USER_LOGIN`).

#### Registration Flow

1. Checks email uniqueness via `userRepository.existsByEmail()`.
2. Validates password strength (see Section 13.6).
3. Creates user with BCrypt-hashed password.
4. Generates tokens with empty roles list (user has no team memberships yet).
5. Audit log entry created asynchronously (`USER_REGISTERED`).

#### Token Refresh Flow

1. Validates the refresh token signature and expiration.
2. Confirms the token carries the `"type": "refresh"` claim.
3. Looks up the user by `sub` claim UUID.
4. Verifies the account is still active.
5. Issues a new access token (with current roles) and a new refresh token.

### 13.3 Authorization Model

#### Roles

Roles are team-scoped and defined in `TeamRole` enum:

```java
public enum TeamRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER
}
```

Roles are included in the JWT `roles` claim as strings. A user who belongs to multiple teams may have multiple distinct roles in the token (e.g., `["OWNER", "MEMBER"]`).

#### @PreAuthorize Patterns

There are **129 `@PreAuthorize` annotations** across all controllers. Three distinct patterns are used:

| Pattern | Usage | Example |
|---------|-------|---------|
| `@PreAuthorize("isAuthenticated()")` | Majority of endpoints (~126) | All CRUD controllers |
| `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")` | Class-level on `AdminController`, method-level on `UserController.deactivateUser()` | Admin operations |
| No annotation (public) | Auth endpoints (`/api/v1/auth/**`), health check | Login, register, refresh |

The `JwtAuthFilter` prefixes each role with `ROLE_` when creating `SimpleGrantedAuthority` objects, which is the standard Spring Security convention that makes `hasRole('ADMIN')` match against `ROLE_ADMIN`.

#### Service-Level Authorization

Beyond `@PreAuthorize`, most services perform additional authorization checks:

- **Team membership verification**: Services like `TeamService`, `ProjectService`, `JobService` verify the current user is a member of the relevant team via `teamMemberRepository.findByTeamIdAndUserId()`.
- **Owner/Admin checks**: `SecurityUtils.isAdmin()` returns true if the user has either `ROLE_ADMIN` or `ROLE_OWNER`.
- **Self-access checks**: `NotificationService.verifyCurrentUserAccess()` and `AuditLogService.getUserAuditLog()` verify the current user matches the requested `userId`.

### 13.4 Security Filter Chain

The `SecurityConfig.filterChain()` method configures the following (in processing order):

1. **RateLimitFilter** (added before `UsernamePasswordAuthenticationFilter`) -- rate-limits auth endpoints
2. **JwtAuthFilter** (added before `UsernamePasswordAuthenticationFilter`) -- extracts and validates JWT from `Authorization: Bearer` header
3. **Spring Security built-in filters** (CORS, CSRF disabled, authorization)

#### Filter Chain Configuration

```
CSRF:              Disabled (stateless JWT API, no cookie auth)
Session:           STATELESS (no HttpSession created)
CORS:              Configured via CorsConfigurationSource bean
```

#### URL Authorization Rules (evaluated in order)

| Pattern | Access |
|---------|--------|
| `/api/v1/auth/**` | `permitAll()` |
| `/api/v1/health` | `permitAll()` |
| `/swagger-ui/**` | `permitAll()` |
| `/v3/api-docs/**` | `permitAll()` |
| `/v3/api-docs.yaml` | `permitAll()` |
| `/api/**` | `authenticated()` |
| Any other request | `authenticated()` |

#### Authentication Entry Point

Unauthenticated requests to protected endpoints receive `HTTP 401 Unauthorized` via a custom `authenticationEntryPoint` that calls `response.sendError(HttpServletResponse.SC_UNAUTHORIZED)`.

#### Security Headers

| Header | Configuration |
|--------|--------------|
| Content-Security-Policy | `default-src 'self'; frame-ancestors 'none'` |
| X-Frame-Options | `DENY` |
| X-Content-Type-Options | `nosniff` (enabled via empty lambda) |
| Strict-Transport-Security | `max-age=31536000; includeSubDomains` |

#### Rate Limiting (RateLimitFilter)

The `RateLimitFilter` applies only to paths starting with `/api/v1/auth/`:

- **Limit**: 10 requests per 60-second sliding window per client IP.
- **Key**: Client IP extracted from `X-Forwarded-For` header (first IP in chain) or `request.getRemoteAddr()`.
- **Storage**: In-memory `ConcurrentHashMap<String, RateWindow>` with sliding window pattern.
- **Response on limit exceeded**: HTTP 429 with JSON body `{"status":429,"message":"Rate limit exceeded. Try again later."}`.
- **Window reset**: Each new request checks if the window has expired (>60s since windowStart) and resets the counter.

**Observations**:
- The rate limiter is in-memory and per-instance only. In a multi-instance deployment, each instance maintains its own counters, effectively multiplying the allowed rate.
- There is no cleanup mechanism for expired entries in the `ConcurrentHashMap`. Entries for IPs that stop making requests are never evicted.
- The `X-Forwarded-For` header is trusted without validation. Behind a misconfigured proxy, clients could spoof their IP to bypass rate limiting.

### 13.5 CORS Configuration

**Source**: `CorsConfig.java`

| Setting | Value |
|---------|-------|
| Allowed Origins | Configurable via `codeops.cors.allowed-origins` (default: `http://localhost:3000`). Supports comma-separated list. |
| Allowed Methods | `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS` |
| Allowed Headers | `Authorization`, `Content-Type`, `X-Requested-With` |
| Exposed Headers | `Authorization` |
| Allow Credentials | `true` |
| Max Age (preflight cache) | 3600 seconds (1 hour) |
| Applied To | `/**` (all paths) |

### 13.6 Encryption (AES-GCM for Credentials)

**Source**: `EncryptionService.java`

Used for encrypting sensitive integration credentials (GitHub PATs, Jira API tokens) at rest.

#### Key Derivation

- Input: `codeops.encryption.key` property (environment variable).
- Algorithm: PBKDF2 with HMAC-SHA256.
- Salt: **Static hardcoded string** `"codeops-static-salt-v1"` (not randomly generated per-key).
- Iterations: 100,000.
- Output key size: 256 bits.
- Result: AES `SecretKeySpec`.

#### Encryption (AES-256-GCM)

- Cipher: `AES/GCM/NoPadding`.
- IV: 12-byte random (via `SecureRandom`), generated fresh per encryption call.
- Tag length: 128 bits.
- Output format: Base64-encoded `[12-byte IV | ciphertext + GCM tag]`.

#### Decryption

- Splits the Base64-decoded input into the first 12 bytes (IV) and remaining bytes (ciphertext + tag).
- Decrypts using the same derived AES key.

**Observations**:
- The static salt weakens PBKDF2. If two deployments use the same `codeops.encryption.key` value, they derive identical AES keys. A per-deployment random salt stored alongside the config would be stronger.
- Error handling wraps all exceptions in `RuntimeException` with generic messages. This is appropriate for not leaking crypto internals.

### 13.7 Password Policy

**Source**: `AuthService.validatePasswordStrength()`

Passwords are hashed using **BCrypt with strength 12** (2^12 = 4,096 rounds), configured in `SecurityConfig.passwordEncoder()`.

#### Complexity Requirements

| Requirement | Regex / Check |
|-------------|--------------|
| Minimum length | 8 characters (`AppConstants.MIN_PASSWORD_LENGTH`) |
| Uppercase letter | `.*[A-Z].*` |
| Lowercase letter | `.*[a-z].*` |
| Digit | `.*[0-9].*` |
| Special character | `.*[^A-Za-z0-9].*` |

All five checks must pass. Each failure throws an `IllegalArgumentException` with a descriptive message. Validation is applied during both registration and password change.

---

## Section 14: Notification / Messaging Layer

### 14.1 Source Files

| File | Path |
|------|------|
| EmailService.java | `src/main/java/com/codeops/notification/EmailService.java` |
| NotificationDispatcher.java | `src/main/java/com/codeops/notification/NotificationDispatcher.java` |
| TeamsWebhookService.java | `src/main/java/com/codeops/notification/TeamsWebhookService.java` |
| SesConfig.java | `src/main/java/com/codeops/config/SesConfig.java` |
| NotificationService.java | `src/main/java/com/codeops/service/NotificationService.java` |

### 14.2 Architecture Overview

The notification layer has three tiers:

```
NotificationDispatcher (orchestrator, @Async)
  |
  +-- EmailService (AWS SES)
  +-- TeamsWebhookService (Microsoft Teams incoming webhook)
  +-- NotificationService (user preference lookup)
```

`NotificationDispatcher` is the central orchestrator. It is called by service/controller code and runs **asynchronously** (all four public methods are annotated `@Async`). It looks up the relevant team, checks webhook configuration and user notification preferences, then delegates to `EmailService` or `TeamsWebhookService` as appropriate.

### 14.3 Email Service (AWS SES)

#### Configuration

- SES is conditionally enabled via `codeops.aws.ses.enabled` (default: `false`).
- When disabled (dev mode), emails are logged to console at INFO level: `"Email (dev mode): to={}, subject={}"`.
- When enabled, `SesConfig` creates an `SesClient` bean using the region from `codeops.aws.ses.region`.
- The `SesClient` bean is injected with `@Autowired(required = false)` so the application starts even when SES is disabled.
- From address: `codeops.aws.ses.from-email` (default: `noreply@codeops.dev`).

#### Emails Sent

| Method | Trigger | Subject | Content |
|--------|---------|---------|---------|
| `sendInvitationEmail()` | Team member invitation | "CodeOps -- Team Invitation" | HTML with team name, inviter name, accept link |
| `sendCriticalFindingAlert()` | Critical findings detected in a QA job | "CodeOps -- Critical Findings Alert: {project}" | HTML with critical count, project name, review link |
| `sendHealthDigest()` | Weekly health digest | "CodeOps -- Weekly Health Digest: {team}" | HTML table with project name, health score, findings count |
| `sendEmail()` (generic) | Task assignment (via dispatcher) | "CodeOps -- Task Assigned: {title}" | Plain text with project and task info |

#### XSS Protection in Emails

All user-supplied values (team names, project names, inviter names, URLs) are escaped using `HtmlUtils.htmlEscape()` before being embedded in HTML email bodies. This prevents stored XSS if email clients render HTML.

#### Error Handling

- `SesException` is caught and logged at ERROR level: `"Failed to send email to {}: {}"`.
- Failures do not propagate; they are silently swallowed after logging.

### 14.4 Teams Webhook Service

#### Message Format

Uses the legacy **Office 365 Connector MessageCard** format (not Adaptive Cards):

```json
{
  "@type": "MessageCard",
  "@context": "http://schema.org/extensions",
  "summary": "title",
  "themeColor": "0076D7",
  "title": "title",
  "sections": [{ "activityTitle": "subtitle", "facts": [...], "markdown": true }],
  "potentialAction": [{ "@type": "OpenUri", "name": "View in CodeOps", "targets": [...] }]
}
```

#### Webhook Messages Sent

| Method | Trigger | Title |
|--------|---------|-------|
| `postJobCompleted()` | QA job completes | "CodeOps -- Audit Complete" with facts: Project, Branch, Health Score, Critical, High, Run By |
| `postCriticalAlert()` | Critical findings detected | "CodeOps -- Critical Alert" with facts: Project, Critical Findings, Action Required |

#### SSRF Protection

`TeamsWebhookService.validateWebhookUrl()` performs the following checks before sending any HTTP request to a webhook URL:

1. **URL parsing**: Validates the URL is syntactically correct via `new URI(url)`.
2. **Host resolution**: Resolves the hostname via `InetAddress.getByName(host)`.
3. **Internal network blocking**: Rejects addresses that are:
   - Loopback (`127.0.0.0/8`, `::1`)
   - Site-local (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`)
   - Link-local (`169.254.0.0/16`)
4. **HTTPS enforcement**: Rejects any URL not using the `https` scheme.

**Observations**:
- The SSRF protection does not guard against DNS rebinding attacks. A DNS name could resolve to a public IP during validation but resolve to an internal IP during the subsequent HTTP request.
- The `RestTemplate` used for webhook calls is a default instance (from `RestTemplateConfig`) with no connection timeout, read timeout, or redirect-following restrictions configured.

#### Error Handling

- All exceptions in `postMessage()` are caught and logged at ERROR level.
- Failures do not propagate to callers.

### 14.5 Notification Preferences (NotificationService)

Users can configure per-event-type notification preferences with two channels: `inApp` and `email`. The `shouldNotify()` method checks preferences before dispatching:

- If no preference exists for the event type, defaults to `inApp = true`, `email = false`.
- Preference lookup: `NotificationPreferenceRepository.findByUserIdAndEventType()`.

### 14.6 Async Dispatch

All `NotificationDispatcher` methods are `@Async`, executed on the thread pool configured in `AsyncConfig`:

- Core pool size: 5
- Max pool size: 20
- Queue capacity: 100
- Rejection policy: `CallerRunsPolicy` (executes on the calling thread if the queue is full)
- Thread name prefix: `codeops-async-`

Every `@Async` method wraps its entire body in a `try/catch(Exception)` block and logs failures at ERROR level. This prevents async exceptions from being lost silently.

The `AsyncConfig` also configures an `AsyncUncaughtExceptionHandler` that logs uncaught async exceptions at ERROR level with the method name and full stack trace.

---

## Section 15: Error Handling

### 15.1 Source Files

| File | Path |
|------|------|
| GlobalExceptionHandler.java | `src/main/java/com/codeops/config/GlobalExceptionHandler.java` |
| CodeOpsException.java | `src/main/java/com/codeops/exception/CodeOpsException.java` |
| AuthorizationException.java | `src/main/java/com/codeops/exception/AuthorizationException.java` |
| NotFoundException.java | `src/main/java/com/codeops/exception/NotFoundException.java` |
| ValidationException.java | `src/main/java/com/codeops/exception/ValidationException.java` |
| ErrorResponse.java | `src/main/java/com/codeops/dto/response/ErrorResponse.java` |

### 15.2 Exception Hierarchy

```
RuntimeException
  |
  +-- CodeOpsException (base application exception)
        |
        +-- AuthorizationException
        +-- NotFoundException
        +-- ValidationException
```

`CodeOpsException` extends `RuntimeException` and provides two constructors: `(String message)` and `(String message, Throwable cause)`. All subclasses only expose the single `(String message)` constructor.

### 15.3 Error Response Format

```java
public record ErrorResponse(int status, String message) {}
```

All error responses follow this JSON structure:

```json
{
  "status": 404,
  "message": "Resource not found"
}
```

### 15.4 Exception-to-HTTP Mapping

The `GlobalExceptionHandler` (`@RestControllerAdvice`) handles exceptions in the following priority order:

| Exception | HTTP Status | Response Message | Logged? | Detail Level |
|-----------|------------|------------------|---------|--------------|
| `EntityNotFoundException` (JPA) | 404 | `"Resource not found"` (hardcoded) | No | Generic -- no entity details exposed |
| `IllegalArgumentException` | 400 | `"Invalid request"` (hardcoded) | WARN: `"Bad request: {message}"` | Generic -- original message logged but NOT returned to client |
| `AccessDeniedException` (Spring Security) | 403 | `"Access denied"` (hardcoded) | No | Generic |
| `MethodArgumentNotValidException` (Bean Validation) | 400 | Concatenated field errors: `"field1: message1, field2: message2"` | No | Field-level details exposed to client |
| `NotFoundException` (CodeOps) | 404 | `ex.getMessage()` | No | Application exception message exposed |
| `ValidationException` (CodeOps) | 400 | `ex.getMessage()` | No | Application exception message exposed |
| `AuthorizationException` (CodeOps) | 403 | `ex.getMessage()` | No | Application exception message exposed |
| `CodeOpsException` (CodeOps base) | 500 | `"An internal error occurred"` (hardcoded) | ERROR: full message + stack trace | Generic -- internal details hidden |
| `Exception` (catch-all) | 500 | `"An internal error occurred"` (hardcoded) | ERROR: `"Unhandled exception"` + full stack trace | Generic -- no internal details exposed |

### 15.5 What Is Exposed to Clients vs. What Is Logged

**Exposed to clients**:
- HTTP status code and a message string.
- For bean validation errors: field names and validation constraint messages (e.g., `"email: must not be blank"`).
- For CodeOps-specific `NotFoundException`, `ValidationException`, and `AuthorizationException`: the developer-provided exception message (e.g., `"Project not found"`, `"Cannot access another user's notification preferences"`).

**Logged server-side only (never exposed)**:
- `IllegalArgumentException` messages (logged at WARN, client sees `"Invalid request"`).
- `CodeOpsException` messages and stack traces (logged at ERROR, client sees `"An internal error occurred"`).
- Unhandled exception messages and stack traces (logged at ERROR, client sees `"An internal error occurred"`).

**Observations**:
- The `IllegalArgumentException` handler hardcodes a generic message, which is good for security but means clients cannot distinguish between different validation failures when services throw `IllegalArgumentException` (as `AuthService` does for login failures, duplicate emails, password issues, etc.).
- There is an inconsistency: `NotFoundException` exposes its message, but JPA's `EntityNotFoundException` returns a hardcoded generic message. Services that use one versus the other will produce different client-facing error messages for the same logical scenario.

---

## Section 16: Test Coverage

### Test File Count

**57 test files** across the following layers:

| Layer | Test Count | Files |
|-------|-----------|-------|
| Service | 25 | `AdminServiceTest`, `AgentRunServiceTest`, `AuditLogServiceTest`, `AuthServiceTest`, `BugInvestigationServiceTest`, `ComplianceServiceTest`, `DependencyServiceTest`, `DirectiveServiceTest`, `EncryptionServiceTest`, `FindingServiceTest`, `GitHubConnectionServiceTest`, `HealthMonitorServiceTest`, `JiraConnectionServiceTest`, `MetricsServiceTest`, `NotificationServiceTest`, `PersonaServiceTest`, `ProjectServiceTest`, `QaJobServiceTest`, `RemediationTaskServiceTest`, `ReportStorageServiceTest`, `S3StorageServiceTest`, `TeamServiceTest`, `TechDebtServiceTest`, `TokenBlacklistServiceTest`, `UserServiceTest` |
| Controller | 17 | `AdminControllerTest`, `AuthControllerTest`, `ComplianceControllerTest`, `DependencyControllerTest`, `DirectiveControllerTest`, `FindingControllerTest`, `HealthMonitorControllerTest`, `IntegrationControllerTest`, `JobControllerTest`, `MetricsControllerTest`, `PersonaControllerTest`, `ProjectControllerTest`, `ReportControllerTest`, `TaskControllerTest`, `TeamControllerTest`, `TechDebtControllerTest`, `UserControllerTest` |
| Config | 8 | `AppConstantsTest`, `AsyncConfigTest`, `CorsConfigTest`, `GlobalExceptionHandlerTest`, `HealthControllerTest`, `JwtPropertiesTest`, `RestTemplateConfigTest`, `SecurityConfigTest` |
| Security | 4 | `JwtAuthFilterTest`, `JwtTokenProviderTest`, `RateLimitFilterTest`, `SecurityUtilsTest` |
| Notification | 3 | `EmailServiceTest`, `NotificationDispatcherTest`, `TeamsWebhookServiceTest` |

### Test Framework

- **JUnit 5** (via `spring-boot-starter-test`)
- **Mockito 5.21.0** (overridden for Java 25 compatibility, with ByteBuddy 1.18.4)
- **Spring Security Test** (`spring-security-test`)
- **Testcontainers** (`testcontainers:postgresql:1.19.8`, `testcontainers:junit-jupiter:1.19.8`) -- available for integration tests
- **H2** (`com.h2database:h2`) -- in-memory database for unit tests
- **JaCoCo 0.8.14** -- coverage reporting plugin configured in `pom.xml`

### Test Method Count

**777 individual `@Test` methods** across all 57 test files.

### JaCoCo Coverage Report (from `target/site/jacoco/jacoco.csv`)

| Metric | Covered | Total | Coverage |
|--------|---------|-------|----------|
| Instructions | 14,874 | 15,541 | **95.7%** |
| Branches | 586 | 648 | **90.4%** |
| Lines | 2,903 | 2,988 | **97.2%** |
| Methods | 698 | 774 | **90.2%** |

### Per-Package Coverage Breakdown

| Package | Instruction Coverage | Line Coverage |
|---------|---------------------|--------------|
| `com.codeops.controller` | 100.0% | 100.0% |
| `com.codeops.dto.response` | 100.0% | 100.0% |
| `com.codeops.entity.enums` | 100.0% | 100.0% |
| `com.codeops.dto.request` | 99.2% | 97.6% |
| `com.codeops.notification` | 97.7% | 97.3% |
| `com.codeops.service` | 95.5% | 98.5% |
| `com.codeops.config` | 92.1% | 86.0% |
| `com.codeops.exception` | 76.2% | 80.0% |
| `com.codeops.security` | 73.5% | 78.3% |
| `com.codeops.entity` | 50.8% | 50.0% |
| `com.codeops` (app main class) | 0.0% | 0.0% |

### Coverage Assessment

The project has **excellent test coverage** at 95.7% instruction coverage and 97.2% line coverage. All 17 controllers are at 100% coverage. The service layer is at 95.5%+ coverage. The lower coverage in `com.codeops.entity` (50.8%) is expected -- entities are Lombok-generated POJOs where testing getters/setters/builders is not typically prioritized. The `com.codeops.security` package at 73.5% is due to `SecurityConfig` having 0% coverage (Spring Security filter chain configuration is difficult to unit test in isolation). The application main class (`CodeOpsApplication`) at 0% is a standard Spring Boot application entry point.

No integration tests using `@SpringBootTest`, `@DataJpaTest`, or `@WebMvcTest` annotations were found in the current test suite. All tests appear to be pure unit tests with mocked dependencies.

---

## Section 17: Infrastructure & Deployment

### 17.1 Containerization

#### Dockerfile Analysis

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/Dockerfile`

```dockerfile
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY target/codeops-server-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
```

| Aspect | Detail |
|--------|--------|
| **Base image** | `eclipse-temurin:21-jre-alpine` -- JRE-only Alpine image, minimal attack surface |
| **Multi-stage build** | **No** -- requires pre-built JAR in `target/`. Build step: `mvn clean package -DskipTests` must run before `docker build`. |
| **Non-root user** | Yes -- `appuser:appgroup` (system user/group) |
| **Exposed port** | 8090 |
| **JVM tuning** | **None** -- no `-Xmx`, `-Xms`, GC flags, or container-aware settings |
| **Health check** | **Not defined** in Dockerfile (no `HEALTHCHECK` instruction) |
| **Signal handling** | Uses `exec` form for `ENTRYPOINT` (PID 1 is the JVM, receives SIGTERM correctly) |
| **Build arg / env** | None -- profile must be set at runtime via `SPRING_PROFILES_ACTIVE=prod` |

**Build and run:**
```bash
# Build JAR first
mvn clean package -DskipTests

# Build Docker image
docker build -t codeops-server .

# Run with production profile
docker run -p 8090:8090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host:5432/codeops \
  -e DATABASE_USERNAME=codeops \
  -e DATABASE_PASSWORD=secret \
  -e JWT_SECRET=your-32-char-minimum-secret-here \
  -e ENCRYPTION_KEY=your-32-char-minimum-key-here \
  -e CORS_ALLOWED_ORIGINS=https://app.codeops.io \
  -e S3_BUCKET=codeops-prod \
  -e AWS_REGION=us-east-1 \
  -e SES_FROM_EMAIL=noreply@codeops.io \
  codeops-server
```

#### docker-compose.yml Analysis

**File:** `/Users/adamallard/Documents/GitHub/CodeOps-Server/docker-compose.yml`

| Aspect | Detail |
|--------|--------|
| **Services** | 1 -- PostgreSQL only |
| **Image** | `postgres:16-alpine` |
| **Container name** | `codeops-db` |
| **Port mapping** | `127.0.0.1:5432:5432` (localhost-only binding) |
| **Volume** | Named volume `codeops_data` at `/var/lib/postgresql/data` |
| **Credentials** | `POSTGRES_DB=codeops`, `POSTGRES_USER=codeops`, `POSTGRES_PASSWORD=codeops` (hardcoded) |
| **Health check** | Not defined |
| **Networks** | Default bridge (no custom network) |
| **Init scripts** | None mounted |
| **Application container** | **Not included** -- the Spring Boot app runs natively via `mvn spring-boot:run` |

The docker-compose is developer-focused: it provides the PostgreSQL dependency only. The application itself is intended to run outside Docker during development.

### 17.2 CI/CD Configuration

**No CI/CD configuration was found.** The following locations were checked:

| Location | Result |
|----------|--------|
| `.github/workflows/*.yml` | Not found |
| `.github/` directory | Does not exist |
| `Jenkinsfile` | Not found |
| `.gitlab-ci.yml` | Not found |
| `.circleci/` | Does not exist |
| `Makefile` | Not found |
| `buildspec.yml` (AWS CodeBuild) | Not found |

There is no automated build, test, or deployment pipeline configured in the repository.

### 17.3 Database Migration Strategy

- **Development:** `ddl-auto: update` -- Hibernate auto-creates and alters tables. No migration scripts.
- **Production:** `ddl-auto: validate` -- Hibernate only validates the schema against entities. The schema must be created/migrated through an external process.
- **Testing:** `ddl-auto: create-drop` -- H2 in-memory database, schema created fresh each test run.
- **Migration tool:** None. Flyway is explicitly disabled in test config (`spring.flyway.enabled: false`), and is not included as a dependency. There are no SQL migration scripts in the repository.

### 17.4 Infrastructure Dependency Summary

```
Production Deployment Requirements:
+-- Runtime
|   +-- JRE 21+ (Eclipse Temurin 21 in Docker)
|   +-- PostgreSQL 16
|   +-- AWS S3 (report/file storage)
|   +-- AWS SES (transactional email)
|   +-- Network access to MS Teams webhook URLs (optional)
+-- Environment Variables (9 required for prod)
|   +-- DATABASE_URL
|   +-- DATABASE_USERNAME
|   +-- DATABASE_PASSWORD
|   +-- JWT_SECRET (min 32 chars)
|   +-- ENCRYPTION_KEY (min 32 chars)
|   +-- CORS_ALLOWED_ORIGINS
|   +-- S3_BUCKET
|   +-- AWS_REGION
|   +-- SES_FROM_EMAIL
+-- NOT Required
    +-- Redis (token blacklist is in-memory)
    +-- Kafka / message broker
    +-- External auth provider
    +-- CDN / load balancer (not configured)
```

### 17.5 Production Readiness Observations

The following observations are relevant to deployment but are reported without action, per the Surgical Precision directive:

1. **No Maven wrapper** -- The project depends on system-installed Maven. This makes builds non-reproducible across environments. A `mvnw` wrapper would pin the Maven version.

2. **No CI/CD pipeline** -- No automated testing, building, or deployment. All builds and deployments are manual.

3. **No database migrations** -- Production uses `ddl-auto: validate` but there are no migration scripts (Flyway/Liquibase) to create or evolve the schema. The initial schema creation strategy for production is undocumented.

4. **In-memory token blacklist** -- `TokenBlacklistService` uses `ConcurrentHashMap`. Blacklisted tokens are lost on restart, and the map grows unboundedly (no expiry cleanup). In a multi-instance deployment, blacklisting is not shared.

5. **In-memory rate limiting** -- `RateLimitFilter` uses `ConcurrentHashMap`. Rate limit state is lost on restart and not shared across instances. The map grows unboundedly (no cleanup of expired windows).

6. **No JVM container tuning** -- The Dockerfile `ENTRYPOINT` has no memory flags. In a container, the JVM may not correctly detect memory limits without `-XX:MaxRAMPercentage` or explicit `-Xmx`.

7. **No Docker health check** -- Neither the Dockerfile nor docker-compose.yml defines a `HEALTHCHECK`. Container orchestrators (ECS, Kubernetes) would need external health check configuration.

8. **Hardcoded encryption key in dev profile** -- `codeops.encryption.key` in `application-dev.yml` is a hardcoded plaintext string with no environment variable override.

---

## Section 18: Cross-Cutting Patterns & Conventions

### 18.1 Package Structure

```
com.codeops/
  CodeOpsApplication.java          -- Main entry point
  config/                          -- 9 configuration classes
    AppConstants.java              -- Static constants (limits, defaults, S3 prefixes)
    AsyncConfig.java               -- Thread pool for @Async
    CorsConfig.java                -- CORS origin/method/header config
    GlobalExceptionHandler.java    -- @RestControllerAdvice
    HealthController.java          -- /api/v1/health endpoint
    JwtProperties.java            -- JWT secret + expiry config properties
    RestTemplateConfig.java        -- RestTemplate bean
    S3Config.java                  -- AWS S3 client config
    SesConfig.java                 -- AWS SES client config
  security/                        -- 5 security classes
    JwtAuthFilter.java             -- JWT extraction + SecurityContext setup
    JwtTokenProvider.java          -- Token generation + validation
    RateLimitFilter.java           -- Auth endpoint rate limiting
    SecurityConfig.java            -- Spring Security filter chain
    SecurityUtils.java             -- Static helpers (getCurrentUserId, hasRole, isAdmin)
  entity/                          -- 25 JPA entities + enums/
    enums/                         -- 22 enum types
    BaseEntity.java                -- @MappedSuperclass base
  repository/                      -- 25 Spring Data JPA repositories
  dto/
    request/                       -- 33 request DTOs (Java records with Jakarta Validation)
    response/                      -- 27 response DTOs (Java records)
  service/                         -- 25 service classes
  controller/                      -- 17 REST controllers
  notification/                    -- 3 notification classes
  exception/                       -- 4 exception classes
```

### 18.2 Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Entities | Singular noun, PascalCase | `Team`, `QaJob`, `TechDebtItem` |
| Repositories | `{Entity}Repository` | `TeamRepository`, `QaJobRepository` |
| Services | `{Entity}Service` or `{Domain}Service` | `TeamService`, `AuthService`, `MetricsService` |
| Controllers | `{Entity}Controller` or `{Domain}Controller` | `TeamController`, `AuthController` |
| Request DTOs | `{Action}{Entity}Request` | `CreateTeamRequest`, `UpdateJobRequest`, `LoginRequest` |
| Response DTOs | `{Entity}Response` | `TeamResponse`, `AuthResponse`, `PageResponse<T>` |
| Enums | Singular noun, values in UPPER_SNAKE_CASE | `TeamRole.OWNER`, `JobStatus.RUNNING` |
| Config classes | `{Feature}Config` or `{Feature}Properties` | `CorsConfig`, `JwtProperties` |

### 18.3 Base Entity (BaseEntity.java)

```java
@MappedSuperclass
@Getter @Setter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

**Extends BaseEntity**: Most entities (User, Team, TeamMember, Project, QaJob, Finding, Persona, etc.)

**Does NOT extend BaseEntity** (custom PK strategies):

| Entity | PK Type | PK Strategy |
|--------|---------|-------------|
| `AuditLog` | `Long` | `GenerationType.IDENTITY` (auto-increment) |
| `SystemSetting` | `String` (key) | Natural key, no generation |
| `ProjectDirective` | `ProjectDirectiveId` (composite) | `@EmbeddedId` with projectId + directiveId |

### 18.4 Audit Logging Pattern

**Source**: `AuditLogService.java`, `AuditLog.java`

The audit log captures security-relevant and administrative actions. It is implemented as an entity stored in the `audit_log` database table.

#### AuditLog Entity

| Column | Type | Notes |
|--------|------|-------|
| id | Long | Auto-increment PK |
| user_id | UUID (FK) | @ManyToOne to User, nullable |
| team_id | UUID (FK) | @ManyToOne to Team, nullable |
| action | String(50) | Action type (e.g., `USER_LOGIN`, `USER_REGISTERED`) |
| entity_type | String(30) | Entity type (e.g., `USER`, `SYSTEM_SETTING`) |
| entity_id | UUID | ID of affected entity |
| details | TEXT | Additional context |
| ip_address | String(45) | Client IP (field exists but not populated by `AuditLogService.log()`) |
| created_at | Instant | Timestamp |

#### Logging Method

```java
@Async
@Transactional
public void log(UUID userId, UUID teamId, String action, String entityType, UUID entityId, String details)
```

- All calls are fire-and-forget (`@Async`).
- The method looks up the `User` and `Team` entities by ID (nullable).
- Callers include `AuthController` (login, register), `AdminController` (user updates, setting changes), and other controllers.

**Observation**: The `ipAddress` field exists on `AuditLog` but `AuditLogService.log()` does not accept or set it. It is always `null` in the database.

### 18.5 Error Handling Pattern

(Fully documented in Section 15.)

Summary: All controllers delegate to `GlobalExceptionHandler`. Application exceptions extend `CodeOpsException`. JPA `EntityNotFoundException` and Spring `AccessDeniedException` are also handled. 500-level errors hide internal details; 400-level errors from CodeOps exceptions expose their message.

### 18.6 Pagination Pattern

**Source**: `PageResponse.java`

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean isLast
) {}
```

#### Controller-Level Convention

```java
@RequestParam(defaultValue = "0") int page,
@RequestParam(defaultValue = "20") int size
// ...
Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE), Sort.by(...));
```

- Default page size: 20 (`AppConstants.DEFAULT_PAGE_SIZE`)
- Maximum page size: 100 (`AppConstants.MAX_PAGE_SIZE`)
- Size is clamped via `Math.min(size, AppConstants.MAX_PAGE_SIZE)` at the controller level.
- Some controllers use Spring's `Page<T>` directly from JPA repositories (e.g., `AdminController`), while others wrap results in `PageResponse<T>` (e.g., `TechDebtController`, `PersonaController`).

### 18.7 Validation Pattern

Request DTOs use Jakarta Bean Validation annotations:

| Annotation | Usage |
|-----------|-------|
| `@NotBlank` | Required non-empty strings (email, password, displayName) |
| `@Email` | Email format validation |
| `@Size(min=, max=)` | String length constraints (e.g., password min 8, displayName max 100) |
| `@Valid` | Applied to `@RequestBody` in controllers to trigger validation |

Bean validation failures are caught by `GlobalExceptionHandler.handleValidation()` and return HTTP 400 with concatenated field error messages.

Additional programmatic validation is performed in service methods (e.g., `AuthService.validatePasswordStrength()` for password complexity beyond basic length).

### 18.8 Constants (AppConstants.java)

```java
public final class AppConstants {
    private AppConstants() {}  // Utility class, no instantiation

    // Team limits
    MAX_TEAM_MEMBERS         = 50
    MAX_PROJECTS_PER_TEAM    = 100
    MAX_PERSONAS_PER_TEAM    = 50
    MAX_DIRECTIVES_PER_PROJECT = 20

    // File size limits
    MAX_REPORT_SIZE_MB       = 25
    MAX_PERSONA_SIZE_KB      = 100
    MAX_DIRECTIVE_SIZE_KB    = 200
    MAX_SPEC_FILE_SIZE_MB    = 50

    // Auth
    JWT_EXPIRY_HOURS         = 24
    REFRESH_TOKEN_EXPIRY_DAYS = 30
    INVITATION_EXPIRY_DAYS   = 7
    MIN_PASSWORD_LENGTH      = 8

    // Notifications
    HEALTH_DIGEST_DAY        = 1   // Monday
    HEALTH_DIGEST_HOUR       = 8   // 8 AM

    // S3 prefixes
    S3_REPORTS               = "reports/"
    S3_SPECS                 = "specs/"
    S3_PERSONAS              = "personas/"
    S3_RELEASES              = "releases/"

    // QA
    MAX_CONCURRENT_AGENTS    = 5
    AGENT_TIMEOUT_MINUTES    = 15
    DEFAULT_HEALTH_SCORE     = 100

    // Pagination
    DEFAULT_PAGE_SIZE        = 20
    MAX_PAGE_SIZE            = 100
}
```

**Observation**: `JWT_EXPIRY_HOURS` and `REFRESH_TOKEN_EXPIRY_DAYS` in AppConstants are defined as constants but `JwtTokenProvider` reads the actual values from `JwtProperties` (which has its own defaults of 24 and 30 respectively). These constants are only used for documentation context -- they are not the source of truth for token lifetimes.

### 18.9 Async Configuration (AsyncConfig.java)

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    // Core pool:     5 threads
    // Max pool:      20 threads
    // Queue:         100 tasks
    // Rejection:     CallerRunsPolicy
    // Thread prefix: "codeops-async-"
    // Uncaught handler: logs at ERROR level
}
```

Used by: `AuditLogService.log()`, all `NotificationDispatcher` methods.

---

## Section 19: Known Issues, TODOs, and Technical Debt

### 19.1 Explicit TODO/FIXME Markers in Source Code

A comprehensive search for `TODO`, `FIXME`, `HACK`, `XXX`, `WORKAROUND`, and `TEMPORARY` across all Java source files found **1 result**:

| # | File | Line | Full Comment |
|---|------|------|--------------|
| 1 | `src/main/java/com/codeops/service/EncryptionService.java` | 30 | `// TODO: Changing key derivation invalidates existing encrypted data -- requires re-encryption migration` |

This TODO documents that the PBKDF2 key derivation parameters (salt, iterations, algorithm) are baked into the code. Any change to these parameters would render all previously encrypted credentials (GitHub PATs, Jira tokens) undecryptable. A migration tool would need to decrypt all existing data with the old parameters and re-encrypt with new parameters.

### 19.2 Implicit Technical Debt Identified During Audit

The following issues were identified through code analysis even though they are not marked with TODO/FIXME comments:

#### Security

| # | Issue | File(s) | Severity | Description |
|---|-------|---------|----------|-------------|
| 1 | In-memory token blacklist | `TokenBlacklistService.java` | High | Token blacklist is stored in a `ConcurrentHashMap` in memory. Logged-out tokens survive until natural expiry after server restart. No persistence (Redis/DB). No eviction of expired entries, causing unbounded memory growth. |
| 2 | Static PBKDF2 salt | `EncryptionService.java` | Medium | The PBKDF2 salt is a hardcoded string `"codeops-static-salt-v1"`. Different deployments with the same encryption key will derive identical AES keys. |
| 3 | Rate limiter is per-instance only | `RateLimitFilter.java` | Medium | The in-memory rate limiter does not work across multiple application instances. No shared state (Redis) for distributed rate limiting. |
| 4 | Rate limiter memory leak | `RateLimitFilter.java` | Low | Expired `RateWindow` entries are replaced on the next request from the same IP but entries from IPs that never return are never evicted. |
| 5 | X-Forwarded-For trust | `RateLimitFilter.java` | Medium | The `X-Forwarded-For` header is trusted without validation. Without a trusted proxy list, clients can spoof their IP to bypass rate limiting. |
| 6 | Audit log missing IP address | `AuditLogService.java` | Low | The `AuditLog` entity has an `ipAddress` field but `AuditLogService.log()` never sets it. All audit log records have `null` for IP address. |

#### Architecture

| # | Issue | File(s) | Severity | Description |
|---|-------|---------|----------|-------------|
| 7 | No RestTemplate timeouts | `RestTemplateConfig.java` | Medium | The `RestTemplate` bean has no connection timeout or read timeout configured. Webhook calls (TeamsWebhookService) could hang indefinitely if the target server is unresponsive. |
| 8 | DNS rebinding in webhook validation | `TeamsWebhookService.java` | Low | SSRF protection validates the resolved IP at check time but the subsequent HTTP request does a new DNS resolution. A DNS rebinding attack could bypass the internal network check. |
| 9 | Inconsistent pagination response types | Various controllers | Low | Some controllers return `Page<T>` (Spring Data), others return `PageResponse<T>` (custom record). API consumers must handle two different response shapes. |
| 10 | JPA EntityNotFoundException vs CodeOps NotFoundException | `GlobalExceptionHandler.java` | Low | `EntityNotFoundException` returns hardcoded `"Resource not found"`, while `NotFoundException` returns the exception message. Services that use `orElseThrow(() -> new EntityNotFoundException(...))` vs `orElseThrow(() -> new NotFoundException(...))` produce different client-facing error responses for the same scenario. |
| 11 | AppConstants JWT values are documentation-only | `AppConstants.java`, `JwtProperties.java` | Low | `JWT_EXPIRY_HOURS` and `REFRESH_TOKEN_EXPIRY_DAYS` in AppConstants are not referenced by `JwtTokenProvider`. Actual values come from `JwtProperties`. If the property defaults diverge from the constants, behavior will not match documentation. |

---


## Section 20: OpenAPI / API Specification

### Specification Files

| File | Location | Format | Lines |
|------|----------|--------|-------|
| `openapi.yaml` | `docs/openapi.yaml` | OpenAPI 3.0.1 | 5,154 |
| `openapi.json` | `docs/openapi.json` | JSON | Present |

### Spec Details

- **Title:** OpenAPI definition
- **Version:** v0
- **Server:** `http://localhost:8090`
- **API Paths:** 116 unique path patterns
- **HTTP Operations:** 140 total operations (GET, POST, PUT, DELETE, PATCH)
- **Tags (API Sections):** 18 tags:
  - Admin, Authentication, Compliance, Dependencies, Directives, Findings, Health, Health Monitor, Integrations, Metrics, Personas, Projects, QA Jobs, Remediation Tasks, Reports, Teams, Tech Debt, Users

### Runtime Generation (springdoc)

The project includes `springdoc-openapi-starter-webmvc-ui:2.5.0` in `pom.xml`, which means:

- **Swagger UI** is available at runtime: `http://localhost:8090/swagger-ui/index.html`
- **Live OpenAPI spec** is generated at: `http://localhost:8090/v3/api-docs` (JSON) and `/v3/api-docs.yaml` (YAML)
- The static files in `docs/` were likely exported from the runtime-generated spec

### Endpoint Summary by Controller

| Controller | Endpoints | API Prefix |
|-----------|-----------|-----------|
| JobController | 13 | `/api/v1/jobs` |
| TeamController | 12 | `/api/v1/teams` |
| DirectiveController | 11 | `/api/v1/directives` |
| PersonaController | 11 | `/api/v1/personas` |
| DependencyController | 10 | `/api/v1/dependencies` |
| FindingController | 10 | `/api/v1/findings` |
| AdminController | 9 | `/api/v1/admin` |
| TechDebtController | 9 | `/api/v1/tech-debt` |
| HealthMonitorController | 8 | `/api/v1/health-monitor` |
| IntegrationController | 8 | `/api/v1/integrations` |
| ComplianceController | 7 | `/api/v1/compliance` |
| ProjectController | 7 | `/api/v1/projects` |
| TaskController | 6 | `/api/v1/tasks` |
| UserController | 6 | `/api/v1/users` |
| AuthController | 5 | `/api/v1/auth` |
| ReportController | 5 | `/api/v1/reports` |
| MetricsController | 3 | `/api/v1/metrics` |
| HealthController | 1 | `/api/v1/health` |
| **Total** | **141** | |

---

## Section 21: Quality Assessment Scorecard

### 21a: Security (max 20 points)

| # | Check | Evidence | Score |
|---|-------|----------|-------|
| 1 | JWT authentication | `JwtAuthFilter.java`, `JwtTokenProvider.java` present; stateless JWT with HS256; `SecurityFilterChain` requires authentication for `/api/**` | 2/2 |
| 2 | Password hashing | `BCryptPasswordEncoder(12)` in `SecurityConfig.java`; strength factor 12 is above default | 2/2 |
| 3 | CORS configuration | `CorsConfig.java` with configurable `allowed-origins` from properties; restrictive headers list (`Authorization`, `Content-Type`, `X-Requested-With`); credentials enabled with `maxAge(3600)` | 2/2 |
| 4 | Rate limiting | `RateLimitFilter.java` -- limits auth endpoints to 10 req/min per IP; in-memory `ConcurrentHashMap` bucket; returns 429 with JSON response | 2/2 |
| 5 | Input validation | 183 validation annotations (`@Valid`, `@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@Email`) across 55 files; `@Valid` used in 44 controller method parameters; `spring-boot-starter-validation` dependency present | 2/2 |
| 6 | SQL injection prevention | Spring Data JPA parameterized queries; only 1 `@Query` found (JPQL, not native SQL); no string concatenation in queries | 2/2 |
| 7 | Secrets management | `EncryptionService` uses AES-256-GCM with PBKDF2 key derivation (100K iterations); credentials stored encrypted in DB (`encryptedCredentials`, `encryptedApiToken`); decrypted credentials never returned in API responses | 2/2 |
| 8 | Security headers | CSP (`default-src 'self'; frame-ancestors 'none'`), `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, HSTS (1 year, includeSubDomains) all configured in `SecurityConfig.java` | 2/2 |
| 9 | Token blacklist / logout | `TokenBlacklistService.java` exists with in-memory `ConcurrentHashMap`; supports `blacklist(jti, expiry)` and `isBlacklisted(jti)`. However, blacklist is in-memory only -- lost on restart, not shared across instances | 1/2 |
| 10 | HTTPS enforcement | No explicit HTTPS redirect or TLS enforcement in application code; HSTS header is configured but app itself listens on HTTP. Relies on reverse proxy/load balancer for TLS termination | 1/2 |

**Security Score: 18/20**

### 21b: Data Integrity (max 16 points)

| # | Check | Evidence | Score |
|---|-------|----------|-------|
| 1 | Database constraints | 216 `@Column`/`@NotNull`/`nullable` annotations across entity classes; column lengths specified; `nullable = false` on critical fields | 2/2 |
| 2 | Unique constraints | `User.email` has `unique = true`; `TeamMember` has `@UniqueConstraint(team_id, user_id)`; `Invitation.token` has `unique = true`; `NotificationPreference` has `@UniqueConstraint(user_id, event_type)` | 2/2 |
| 3 | Cascade rules | No JPA cascade annotations found; all entity relationships managed explicitly in service layer; no orphan removal configured. Prevents accidental cascade deletes but requires manual cleanup | 1/2 |
| 4 | Transactions | 93 `@Transactional` annotations; services are `@Transactional` at class level with `readOnly = true` for read operations | 2/2 |
| 5 | Audit trail | Full `AuditLog` entity with `AuditLogService` (`@Async` fire-and-forget); logs user actions with userId, teamId, action, entityType, entityId; indexed on `user_id` and `team_id` | 2/2 |
| 6 | Pagination | 176 references to `Pageable`/`PageResponse`/`PageRequest`; generic `PageResponse<T>` record for consistent paginated responses | 2/2 |
| 7 | Soft delete | `isActive` field on `User`, `GitHubConnection`, `JiraConnection`, `HealthSchedule`; connections use soft delete (set `isActive = false`); most other entities use hard delete | 1/2 |
| 8 | BaseEntity | 25 entities with `createdAt`/`updatedAt` via `BaseEntity` superclass (exceptions: `SystemSetting` with String PK, `AuditLog` with Long PK, `ProjectDirective` with composite PK) | 2/2 |

**Data Integrity Score: 14/16**

### 21c: API Quality (max 16 points)

| # | Check | Evidence | Score |
|---|-------|----------|-------|
| 1 | RESTful conventions | 141 HTTP method-mapped endpoints using `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`; proper HTTP verb usage for CRUD operations | 2/2 |
| 2 | Consistent API prefix | All 18 controllers use `/api/v1/{resource}` prefix via `@RequestMapping`; consistent naming convention across all controllers | 2/2 |
| 3 | Global exception handler | `GlobalExceptionHandler.java` present; handles 12+ exception types; returns structured `ErrorResponse` records | 2/2 |
| 4 | Response DTOs | 31 response DTO records in `com.codeops.dto.response`; entities never exposed directly in API responses | 2/2 |
| 5 | OpenAPI documentation | `springdoc-openapi-starter-webmvc-ui:2.5.0`; Swagger UI available; static spec exported to `docs/openapi.yaml` (5,154 lines) and `docs/openapi.json` | 2/2 |
| 6 | Versioned API | All endpoints under `/api/v1/`; consistent versioning across all 18 controllers | 2/2 |
| 7 | HTTP status codes | 321 references to `ResponseEntity`/`HttpStatus`/`ResponseStatus`; proper use of 200, 201, 204, 400, 401, 403, 404, 429 | 2/2 |
| 8 | Request validation | 44 `@Valid` annotations on controller method parameters; request DTOs use Jakarta Validation annotations (`@NotBlank`, `@Size`, `@NotNull`, `@Email`, `@Pattern`, `@Min`, `@Max`) | 2/2 |

**API Quality Score: 16/16**

### 21d: Code Quality (max 16 points)

| # | Check | Evidence | Score |
|---|-------|----------|-------|
| 1 | Package structure | Clean layered architecture: `config/`, `controller/`, `dto/` (with `request/` and `response/`), `entity/` (with `enums/`), `exception/`, `notification/`, `repository/`, `security/`, `service/` | 2/2 |
| 2 | Lombok usage | 152 Lombok annotations (`@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Builder`); eliminates boilerplate; consistent across all entities and services | 2/2 |
| 3 | No System.out.println | Zero `System.out.println` or `System.err.println` calls found in main source code | 2/2 |
| 4 | Logging framework | 14 `@Slf4j` / `LoggerFactory` usages; SLF4J via Logback (Spring Boot default); structured logging with appropriate log levels | 2/2 |
| 5 | Constructor injection | Only 2 `@Autowired` annotations found, both are `@Autowired(required = false)` for optional beans (`SesClient`, `S3Client`); all other services use `@RequiredArgsConstructor` constructor injection | 2/2 |
| 6 | Java records for DTOs | 72 Java records across `dto.request` and `dto.response` packages; immutable, no boilerplate | 2/2 |
| 7 | DRY principle | Common patterns extracted: `BaseEntity` for shared fields, `SecurityUtils` for auth helpers, `EncryptionService` for crypto, `PageResponse<T>` for pagination, `GlobalExceptionHandler` for errors. Some repetition in team membership verification across services (could be extracted to a shared utility) | 1/2 |
| 8 | Consistent naming | 18 controllers, 27 services (incl. notification), 25 repositories; consistent `*Controller`, `*Service`, `*Repository` naming; entity names match table names | 2/2 |

**Code Quality Score: 15/16**

### 21e: Test Quality (max 12 points)

| # | Check | Evidence | Score |
|---|-------|----------|-------|
| 1 | Test count | 57 test files with 777 `@Test` methods; covers all services (25), controllers (17), security (4), config (8), notification (3) | 2/2 |
| 2 | Coverage tooling | JaCoCo 0.8.14 configured in `pom.xml` with `prepare-agent` and `report` goals; CSV report available at `target/site/jacoco/jacoco.csv` | 2/2 |
| 3 | Mock framework | Mockito 5.21.0 with ByteBuddy 1.18.4 (overridden for Java 25 compatibility); Spring Security Test available | 2/2 |
| 4 | Layer coverage | All layers tested: 100% controller coverage, 95.5% service coverage, 97.7% notification coverage. Security layer at 73.5% (SecurityConfig filter chain untested) | 2/2 |
| 5 | Integration tests | No `@SpringBootTest`, `@DataJpaTest`, or `@WebMvcTest` annotations found. Testcontainers (PostgreSQL) is a dependency but not actively used. All tests are unit tests with mocks | 0/2 |
| 6 | Test method count | 777 test methods (avg ~13.6 per test file); naming appears to follow descriptive conventions | 2/2 |

**Test Quality Score: 10/12**

### 21f: Infrastructure (max 12 points)

| # | Check | Evidence | Score |
|---|-------|----------|-------|
| 1 | Docker support | `Dockerfile` (Eclipse Temurin 21 JRE Alpine) and `docker-compose.yml` (PostgreSQL 16 Alpine) both present | 2/2 |
| 2 | Health endpoint | `HealthController.java` at `/api/v1/health`; returns service status | 2/2 |
| 3 | Profile-based config | 3 config files: `application.yml` (base), `application-dev.yml`, `application-prod.yml`; dev defaults for local development, prod uses environment variables | 2/2 |
| 4 | .gitignore | Present; excludes `target/`, `.idea/`, `.vscode/`, `.env`, `*.pem`, `*.key`, `*.log`, `logs/`, `.DS_Store` | 2/2 |
| 5 | Non-root Docker user | `Dockerfile` creates `appuser:appgroup`, sets file ownership, runs as `USER appuser` | 2/2 |
| 6 | CI-ready coverage | JaCoCo configured with `prepare-agent` and `report` goals; `@{argLine}` placeholder in surefire for JaCoCo agent; CSV/HTML reports generated automatically during `test` phase | 2/2 |

**Infrastructure Score: 12/12**

### Quality Assessment Summary

| Category | Score | Max | Percentage |
|----------|-------|-----|-----------|
| 21a: Security | 18 | 20 | 90.0% |
| 21b: Data Integrity | 14 | 16 | 87.5% |
| 21c: API Quality | 16 | 16 | 100.0% |
| 21d: Code Quality | 15 | 16 | 93.8% |
| 21e: Test Quality | 10 | 12 | 83.3% |
| 21f: Infrastructure | 12 | 12 | 100.0% |
| **TOTAL** | **85** | **92** | **92.4%** |

### Overall Grade: **A**

| Grade | Range | Description |
|-------|-------|-------------|
| A+ | 95-100% | Exceptional |
| **A** | **90-94%** | **Excellent** |
| B+ | 85-89% | Very Good |
| B | 80-84% | Good |
| C | 70-79% | Acceptable |
| D | 60-69% | Below Average |
| F | <60% | Failing |

### Key Deductions

1. **Token blacklist is in-memory** (-1): `TokenBlacklistService` uses `ConcurrentHashMap` -- not persisted, not shared across instances. For production, should use Redis with TTL.
2. **No explicit HTTPS enforcement** (-1): Application relies on external reverse proxy for TLS. No `requiresChannel().requiresSecure()` or similar.
3. **No JPA cascade rules** (-1): All relationships managed manually. While this prevents accidental cascades, it risks orphaned records if cleanup is missed.
4. **Some code repetition** (-1): Team membership verification pattern repeated across multiple services.
5. **No integration tests** (-2): Despite having Testcontainers as a dependency, no integration tests exist. All 57 tests are pure unit tests with mocked dependencies.

---

## Section 22: Database -- Live Schema Audit

**Database not available for live audit.**

The Docker container `codeops-db` was not running at the time of this audit. The schema structure is documented from JPA entity annotations:

### Tables (25 tables from entity `@Table` annotations)

| # | Table Name | Entity Class | Primary Key | Notes |
|---|-----------|-------------|-------------|-------|
| 1 | `agent_runs` | `AgentRun` | UUID (BaseEntity) | Indexed on `job_id` |
| 2 | `audit_log` | `AuditLog` | Long | Indexed on `user_id`, `team_id` |
| 3 | `bug_investigations` | `BugInvestigation` | UUID (BaseEntity) | |
| 4 | `compliance_items` | `ComplianceItem` | UUID (BaseEntity) | Indexed on `job_id` |
| 5 | `dependency_scans` | `DependencyScan` | UUID (BaseEntity) | Indexed on `project_id` |
| 6 | `dependency_vulnerabilities` | `DependencyVulnerability` | UUID (BaseEntity) | Indexed on `scan_id` |
| 7 | `directives` | `Directive` | UUID (BaseEntity) | Indexed on `team_id` |
| 8 | `findings` | `Finding` | UUID (BaseEntity) | Indexed on `job_id`, `status` |
| 9 | `github_connections` | `GitHubConnection` | UUID (BaseEntity) | Soft delete via `isActive` |
| 10 | `health_schedules` | `HealthSchedule` | UUID (BaseEntity) | Indexed on `project_id`; soft delete via `isActive` |
| 11 | `health_snapshots` | `HealthSnapshot` | UUID (BaseEntity) | Indexed on `project_id` |
| 12 | `invitations` | `Invitation` | UUID (BaseEntity) | Indexed on `team_id`, `email`; unique `token` |
| 13 | `jira_connections` | `JiraConnection` | UUID (BaseEntity) | Soft delete via `isActive` |
| 14 | `notification_preferences` | `NotificationPreference` | UUID (BaseEntity) | Unique on `(user_id, event_type)`; indexed on `user_id` |
| 15 | `personas` | `Persona` | UUID (BaseEntity) | Indexed on `team_id` |
| 16 | `project_directives` | `ProjectDirective` | Composite (`project_id`, `directive_id`) | Join table |
| 17 | `projects` | `Project` | UUID (BaseEntity) | Indexed on `team_id` |
| 18 | `qa_jobs` | `QaJob` | UUID (BaseEntity) | Indexed on `project_id`, `started_by` |
| 19 | `remediation_tasks` | `RemediationTask` | UUID (BaseEntity) | Indexed on `job_id` |
| 20 | `specifications` | `Specification` | UUID (BaseEntity) | Indexed on `job_id` |
| 21 | `system_settings` | `SystemSetting` | String (key) | Key-value store |
| 22 | `team_members` | `TeamMember` | UUID (BaseEntity) | Unique on `(team_id, user_id)`; indexed on `team_id`, `user_id` |
| 23 | `teams` | `Team` | UUID (BaseEntity) | |
| 24 | `tech_debt_items` | `TechDebtItem` | UUID (BaseEntity) | Indexed on `project_id` |
| 25 | `users` | `User` | UUID (BaseEntity) | Unique `email`; soft delete via `isActive` |

### Indexes (23 defined in entity annotations)

| Index Name | Table | Column(s) |
|-----------|-------|-----------|
| `idx_agent_run_job_id` | `agent_runs` | `job_id` |
| `idx_audit_user_id` | `audit_log` | `user_id` |
| `idx_audit_team_id` | `audit_log` | `team_id` |
| `idx_compliance_job_id` | `compliance_items` | `job_id` |
| `idx_dep_scan_project_id` | `dependency_scans` | `project_id` |
| `idx_directive_team_id` | `directives` | `team_id` |
| `idx_finding_job_id` | `findings` | `job_id` |
| `idx_finding_status` | `findings` | `status` |
| `idx_inv_team_id` | `invitations` | `team_id` |
| `idx_inv_email` | `invitations` | `email` |
| `idx_job_project_id` | `qa_jobs` | `project_id` |
| `idx_job_started_by` | `qa_jobs` | `started_by` |
| `idx_notif_user_id` | `notification_preferences` | `user_id` |
| `idx_persona_team_id` | `personas` | `team_id` |
| `idx_project_team_id` | `projects` | `team_id` |
| `idx_schedule_project_id` | `health_schedules` | `project_id` |
| `idx_snapshot_project_id` | `health_snapshots` | `project_id` |
| `idx_spec_job_id` | `specifications` | `job_id` |
| `idx_task_job_id` | `remediation_tasks` | `job_id` |
| `idx_tech_debt_project_id` | `tech_debt_items` | `project_id` |
| `idx_tm_team_id` | `team_members` | `team_id` |
| `idx_tm_user_id` | `team_members` | `user_id` |
| `idx_vuln_scan_id` | `dependency_vulnerabilities` | `scan_id` |

### Unique Constraints

| Table | Column(s) |
|-------|-----------|
| `users` | `email` |
| `team_members` | `(team_id, user_id)` |
| `invitations` | `token` |
| `notification_preferences` | `(user_id, event_type)` |

### Schema Management

- **DDL strategy:** `hibernate.ddl-auto: update` in dev, `validate` in prod
- **No migration tool:** No Flyway or Liquibase configured
- **Init script:** None found in project (no `init-scripts/` directory)

---

## Section 23: Kafka / Message Broker

### Detection Result

**Kafka is NOT used in this project.**

- No Kafka dependencies in `pom.xml` (no `spring-kafka`, no `kafka-clients`, no Confluent dependencies)
- No Kafka-related Java classes found in source code (no `KafkaTemplate`, `KafkaListener`, `KafkaProducerService`, `KafkaConsumerService`)
- No Kafka configuration in `application.yml`, `application-dev.yml`, or `application-prod.yml`
- No Kafka container in `docker-compose.yml` (only PostgreSQL)

### Asynchronous Processing

The project uses **Spring `@Async`** instead of Kafka for asynchronous operations:

- `AsyncConfig.java` configures a `ThreadPoolTaskExecutor` (core: 5, max: 20, queue: 100)
- Thread name prefix: `codeops-async-`
- Rejected execution policy: `CallerRunsPolicy`
- Uncaught exception handler logs errors via SLF4J

### Async Usage

| Class | Method | Purpose |
|-------|--------|---------|
| `NotificationDispatcher` | `dispatchJobCompleted()` | Teams webhook notification after job completes |
| `NotificationDispatcher` | `dispatchCriticalFinding()` | Teams webhook + email for critical findings |
| `NotificationDispatcher` | `dispatchTaskAssigned()` | Email notification for task assignments |
| `NotificationDispatcher` | `dispatchInvitation()` | Email invitation to team members |
| `AuditLogService` | `log()` | Async audit trail logging (fire-and-forget) |

Note: The parent CLAUDE.md references an EventPublisher/KafkaProducerService pattern, but this applies to the Zevaro-Core project, not CodeOps-Server. CodeOps-Server is a separate, simpler project.

---

## Section 24: Redis / Cache Layer

### Detection Result

**Redis is NOT used in this project.**

- No Redis dependency in `pom.xml` (no `spring-boot-starter-data-redis`, no `spring-data-redis`, no `lettuce`, no `jedis`)
- No Redis configuration in any `application*.yml` file
- No Redis container in `docker-compose.yml`
- The only Redis references found are in audit documentation files (`docs/CodeOps-Server-Audit.md`, `docs/Old-CodeOps-Server-Audit.md`) as recommendations

### Token Blacklist Implementation (In-Memory)

The `TokenBlacklistService` at `src/main/java/com/codeops/service/TokenBlacklistService.java` uses an in-memory `ConcurrentHashMap.KeySetView<String, Boolean>` instead of Redis:

```java
@Service
public class TokenBlacklistService {
    private final ConcurrentHashMap.KeySetView<String, Boolean> blacklistedTokens =
        ConcurrentHashMap.newKeySet();

    public void blacklist(String jti, Instant expiry) {
        if (jti != null) {
            blacklistedTokens.add(jti);
        }
    }

    public boolean isBlacklisted(String jti) {
        return jti != null && blacklistedTokens.contains(jti);
    }
}
```

**Limitations of current implementation:**
1. Blacklist is lost on application restart
2. No TTL -- expired token JTIs accumulate indefinitely (memory leak)
3. Not shared across application instances (horizontal scaling issue)
4. Previous audit recommended Redis-based implementation with TTL per token

### Caching

No Spring Cache (`@Cacheable`, `@CacheEvict`, `@CachePut`) annotations found. No caching layer is implemented.

---

## Section 25: Environment Variable Inventory

### `@Value` Annotations in Java Source

| Variable | File | Default Value | Required In Prod |
|----------|------|---------------|-----------------|
| `${codeops.jwt.secret}` | `application-dev.yml` (via JwtProperties) | `dev-secret-key-minimum-32-characters-long-for-hs256` | Yes (`${JWT_SECRET}`) |
| `${codeops.encryption.key}` | `EncryptionService.java` | `dev-only-encryption-key-minimum-32ch` | Yes (`${ENCRYPTION_KEY}`) |
| `${codeops.cors.allowed-origins}` | `CorsConfig.java` | `http://localhost:3000` | Yes (`${CORS_ALLOWED_ORIGINS}`) |
| `${codeops.aws.s3.enabled}` | `S3StorageService.java` | `false` | Implicit (`true` in prod profile) |
| `${codeops.aws.s3.bucket}` | `S3StorageService.java` | `codeops-dev` | Yes (`${S3_BUCKET}`) |
| `${codeops.aws.s3.region}` | `S3Config.java` | None | Yes (`${AWS_REGION}`) |
| `${codeops.aws.ses.enabled}` | `EmailService.java` | `false` | Implicit (`true` in prod profile) |
| `${codeops.aws.ses.from-email}` | `EmailService.java` | `noreply@codeops.dev` | Yes (`${SES_FROM_EMAIL}`) |
| `${codeops.aws.ses.region}` | `SesConfig.java` | None | Yes (`${AWS_REGION}`) |
| `${codeops.local-storage.path}` | `S3StorageService.java` | `~/.codeops/storage` | No (S3 used in prod) |

### Environment Variables in YAML Configuration

#### `application-dev.yml`

| Variable | Property Path | Default |
|----------|--------------|---------|
| `${DB_USERNAME}` | `spring.datasource.username` | `codeops` |
| `${DB_PASSWORD}` | `spring.datasource.password` | `codeops` |
| `${JWT_SECRET}` | `codeops.jwt.secret` | `dev-secret-key-minimum-32-characters-long-for-hs256` |

#### `application-prod.yml`

| Variable | Property Path | Default | Required |
|----------|--------------|---------|----------|
| `${DATABASE_URL}` | `spring.datasource.url` | None | **YES** |
| `${DATABASE_USERNAME}` | `spring.datasource.username` | None | **YES** |
| `${DATABASE_PASSWORD}` | `spring.datasource.password` | None | **YES** |
| `${JWT_SECRET}` | `codeops.jwt.secret` | None | **YES** |
| `${ENCRYPTION_KEY}` | `codeops.encryption.key` | None | **YES** |
| `${CORS_ALLOWED_ORIGINS}` | `codeops.cors.allowed-origins` | None | **YES** |
| `${S3_BUCKET}` | `codeops.aws.s3.bucket` | None | **YES** |
| `${AWS_REGION}` | `codeops.aws.s3.region` and `codeops.aws.ses.region` | None | **YES** |
| `${SES_FROM_EMAIL}` | `codeops.aws.ses.from-email` | None | **YES** |

#### `docker-compose.yml`

| Variable | Service | Value |
|----------|---------|-------|
| `POSTGRES_DB` | postgres | `codeops` |
| `POSTGRES_USER` | postgres | `codeops` |
| `POSTGRES_PASSWORD` | postgres | `codeops` |

### Dockerfile

| Instruction | Details |
|-------------|---------|
| `FROM` | `eclipse-temurin:21-jre-alpine` |
| `EXPOSE` | `8090` |
| No `ENV` | No environment variables set in Dockerfile |
| No `ARG` | No build arguments |

### Complete Production Environment Variable Checklist

| # | Variable | Purpose | Sensitive |
|---|----------|---------|-----------|
| 1 | `DATABASE_URL` | PostgreSQL JDBC URL | No |
| 2 | `DATABASE_USERNAME` | Database user | Yes |
| 3 | `DATABASE_PASSWORD` | Database password | **Yes** |
| 4 | `JWT_SECRET` | JWT signing key (HS256, min 32 chars) | **Yes** |
| 5 | `ENCRYPTION_KEY` | AES-256 encryption master key | **Yes** |
| 6 | `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | No |
| 7 | `S3_BUCKET` | AWS S3 bucket name | No |
| 8 | `AWS_REGION` | AWS region for S3 and SES | No |
| 9 | `SES_FROM_EMAIL` | Sender email for SES | No |
| 10 | `SPRING_PROFILES_ACTIVE` | Must be `prod` for production | No |

Note: AWS credentials (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`) are expected via the standard AWS SDK credential chain (IAM role, environment variables, or `~/.aws/credentials`) -- not configured in application YAML.

---

## Section 26: Inter-Service Communication Map

### Outbound HTTP Client

The project uses a single `RestTemplate` bean configured in `src/main/java/com/codeops/config/RestTemplateConfig.java`:

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

No custom timeouts, interceptors, or retry logic is configured on the `RestTemplate`. This is a vanilla Spring `RestTemplate` with default settings.

### RestTemplate Usage

| Consumer Class | Method | Target | Protocol |
|---------------|--------|--------|----------|
| `TeamsWebhookService` | `postForEntity(webhookUrl, entity, String.class)` | Microsoft Teams webhook URLs (user-configured per team) | HTTPS (enforced by URL validation) |

### External Service Dependencies

#### 1. Microsoft Teams Webhooks (Outbound)

- **Service:** `TeamsWebhookService.java`
- **Transport:** `RestTemplate.postForEntity()`
- **URL Source:** User-configured `teamsWebhookUrl` field on `Team` entity (stored in database)
- **Security:** URL validation enforces HTTPS, rejects loopback/internal addresses, validates URI format
- **Payload:** MessageCard JSON format
- **Error Handling:** Exceptions caught and logged; webhook failures do not propagate to caller
- **Called from:** `NotificationDispatcher` (async) for job completion and critical finding alerts

#### 2. AWS S3 (Outbound)

- **Service:** `S3StorageService.java`
- **Transport:** AWS SDK v2 `S3Client` (not RestTemplate)
- **Operations:** `putObject`, `getObject`, `deleteObject`
- **Configuration:** Region from `${AWS_REGION}`, bucket from `${S3_BUCKET}`
- **Fallback:** When `codeops.aws.s3.enabled=false`, falls back to local filesystem (`~/.codeops/storage/`)
- **Auth:** AWS SDK default credential chain

#### 3. AWS SES (Outbound)

- **Service:** `EmailService.java`
- **Transport:** AWS SDK v2 `SesClient` (not RestTemplate)
- **Operations:** `sendEmail`
- **Configuration:** Region from `${AWS_REGION}`, from-email from `${SES_FROM_EMAIL}`
- **Fallback:** When `codeops.aws.ses.enabled=false`, emails logged to console instead of sent
- **Auth:** AWS SDK default credential chain

#### 4. GitHub API (Data Storage Only)

- **Service:** `GitHubConnectionService.java`
- **Transport:** None -- CodeOps-Server only stores encrypted GitHub credentials (PATs); it does NOT make outbound GitHub API calls itself
- **Purpose:** Connection management (CRUD); credentials decrypted only when requested by authorized users (ADMIN/OWNER)
- **Note:** Actual GitHub API calls are made by an external agent/consumer that retrieves credentials via `getDecryptedCredentials()`

#### 5. Jira API (Data Storage Only)

- **Service:** `JiraConnectionService.java`
- **Transport:** None -- CodeOps-Server only stores encrypted Jira API tokens; it does NOT make outbound Jira API calls itself
- **Purpose:** Connection management (CRUD); tokens decrypted only when requested by authorized users (ADMIN/OWNER)
- **Note:** Actual Jira API calls are made by an external agent/consumer that retrieves credentials via `getConnectionDetails()`

#### 6. PostgreSQL (Database)

- **Driver:** `org.postgresql.Driver`
- **URL:** `jdbc:postgresql://localhost:5432/codeops` (dev), `${DATABASE_URL}` (prod)
- **Connection:** Spring Data JPA / Hibernate
- **Schema management:** `ddl-auto: update` (dev), `ddl-auto: validate` (prod)

### Service Dependency Map

```
                               +-------------------+
                               | CodeOps-Server    |
                               | (Spring Boot)     |
                               | Port: 8090        |
                               +--------+----------+
                                        |
            +---------------------------+---------------------------+
            |                           |                           |
            v                           v                           v
   +----------------+         +------------------+        +------------------+
   | PostgreSQL 16  |         | AWS S3           |        | AWS SES          |
   | Port: 5432     |         | (Report Storage) |        | (Email Service)  |
   | codeops DB     |         | Disabled in dev  |        | Disabled in dev  |
   +----------------+         +------------------+        +------------------+

            +---------------------------+
            |                           |
            v                           v
   +---------------------+    +---------------------+
   | MS Teams Webhooks   |    | External Agents     |
   | (Notifications)     |    | (consume credentials|
   | User-configured URL |    |  for GitHub/Jira)   |
   +---------------------+    +---------------------+
```

### Communication Summary

| Direction | Source | Target | Protocol | Auth |
|-----------|--------|--------|----------|------|
| Outbound | CodeOps-Server | PostgreSQL | JDBC/TCP | Username/Password |
| Outbound | CodeOps-Server | AWS S3 | HTTPS (SDK) | AWS IAM Credentials |
| Outbound | CodeOps-Server | AWS SES | HTTPS (SDK) | AWS IAM Credentials |
| Outbound | CodeOps-Server | MS Teams | HTTPS (RestTemplate) | Webhook URL (implicit) |
| Inbound | HTTP Clients | CodeOps-Server `:8090` | HTTP/HTTPS | JWT Bearer Token |

### Notable Architectural Observations

1. **No direct GitHub/Jira API calls:** CodeOps-Server is a credentials vault and orchestration layer. It stores encrypted credentials but does not itself call GitHub or Jira APIs. External agents retrieve decrypted credentials and make those calls.
2. **No inter-service HTTP calls:** CodeOps-Server is a monolith with no service-to-service communication. There are no `@FeignClient`, `WebClient`, or `HttpClient` usages.
3. **No service discovery:** No Eureka, Consul, or Kubernetes service discovery. The application is self-contained.
4. **RestTemplate has no resilience:** The `RestTemplate` bean is vanilla with no circuit breaker (Resilience4j), retry, or timeout configuration. A webhook endpoint that is slow or unresponsive could tie up threads.
5. **Async isolation:** All outbound webhook/email calls go through `NotificationDispatcher` with `@Async`, preventing external service failures from blocking API requests.
