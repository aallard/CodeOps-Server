# CodeOps-Server — Comprehensive Codebase Audit

**Audit Date:** 2026-02-15T21:12:46Z
**Branch:** main
**Commit:** cd57a0bc922bbeb08273af6444675bcdbf95b30f COC-019: Fix security filter ordering for IT tests + organize Docker with naming, labels, healthcheck, and network isolation
**Auditor:** Claude Code (Automated)
**Purpose:** Zero-context reference for AI-assisted development
**Audit File:** CodeOps-Server-Audit.md
**OpenAPI Spec:** openapi.yaml
**Quality Grade:** A
**Overall Score:** 97/104 (93%)

> This audit is the single source of truth for the CodeOps-Server codebase.
> An AI reading only this document should be able to generate accurate code
> changes, new features, tests, and fixes without filesystem access.

---

## 1. Project Identity

| Field | Value |
|-------|-------|
| Project Name | CodeOps-Server |
| Repository URL | https://github.com/aallard/CodeOps-Server.git |
| Primary Language / Framework | Java 21 / Spring Boot 3.3.0 |
| Build Tool | Maven (wrapper) |
| Current Branch | main |
| Latest Commit | cd57a0bc922bbeb08273af6444675bcdbf95b30f COC-019: Fix security filter ordering for IT tests + organize Docker with naming, labels, healthcheck, and network isolation |
| Audit Timestamp | 2026-02-15T21:12:46Z |

---

## 2. Directory Structure

```
CodeOps-Server/
  claude.md
  CodeOps-Architrecture.md
  docker-compose.yml
  Dockerfile
  pom.xml
  README.md
  src/
    main/
      java/com/codeops/
        CodeOpsApplication.java
        config/
          AppConstants.java
          AsyncConfig.java
          CorsConfig.java
          DataSeeder.java
          GlobalExceptionHandler.java
          HealthController.java
          JwtProperties.java
          LoggingInterceptor.java
          RequestCorrelationFilter.java
          RestTemplateConfig.java
          S3Config.java
          SesConfig.java
          WebMvcConfig.java
        controller/
          AdminController.java
          AuthController.java
          ComplianceController.java
          DependencyController.java
          DirectiveController.java
          FindingController.java
          HealthMonitorController.java
          IntegrationController.java
          JobController.java
          MetricsController.java
          PersonaController.java
          ProjectController.java
          ReportController.java
          TaskController.java
          TeamController.java
          TechDebtController.java
          UserController.java
        dto/
          request/   (41 files)
          response/  (31 files)
        entity/
          AgentRun.java
          AuditLog.java
          BaseEntity.java
          BugInvestigation.java
          ComplianceItem.java
          DependencyScan.java
          DependencyVulnerability.java
          Directive.java
          Finding.java
          GitHubConnection.java
          HealthSchedule.java
          HealthSnapshot.java
          Invitation.java
          JiraConnection.java
          NotificationPreference.java
          Persona.java
          Project.java
          ProjectDirective.java
          ProjectDirectiveId.java
          QaJob.java
          RemediationTask.java
          Specification.java
          SystemSetting.java
          Team.java
          TeamMember.java
          TechDebtItem.java
          User.java
          enums/  (24 files)
        exception/
          AuthorizationException.java
          CodeOpsException.java
          NotFoundException.java
          ValidationException.java
        notification/
          EmailService.java
          NotificationDispatcher.java
          TeamsWebhookService.java
        repository/  (25 files)
        security/
          JwtAuthFilter.java
          JwtTokenProvider.java
          RateLimitFilter.java
          SecurityConfig.java
          SecurityUtils.java
        service/  (25 files)
      resources/
        application.yml
        application-dev.yml
        application-prod.yml
        logback-spring.xml
    test/  (59 unit test files, 13 integration test files, 2 test configs)
```

**Narrative:** Single Spring Boot module with a layered architecture. The `config/` package contains cross-cutting Spring configuration beans. The `controller/` package provides 17 REST controllers under `/api/v1/`. The `dto/` package separates request (41 files) and response (31 files) records. The `entity/` package defines 27 JPA entities (including `BaseEntity` mapped superclass and `ProjectDirectiveId` embeddable) plus 24 enums. The `exception/` package provides a 4-class hierarchy. The `notification/` package handles email (SES) and Teams webhook integrations. The `repository/` package contains 25 Spring Data JPA repositories. The `security/` package implements JWT authentication, rate limiting, and security utilities. The `service/` package provides 25 service classes. Sources reside in `src/main`, tests in `src/test`.

---

## 3. Build & Dependencies

### pom.xml

**Group / Artifact / Version:** `com.codeops:codeops-server:0.1.0-SNAPSHOT`

**Parent:** `org.springframework.boot:spring-boot-starter-parent:3.3.0`

**Properties:**

| Property | Value | Purpose |
|----------|-------|---------|
| `java.version` | 21 | Java language level |
| `jjwt.version` | 0.12.6 | JJWT library version |
| `mapstruct.version` | 1.5.5.Final | MapStruct code generation |
| `lombok.version` | 1.18.42 | Java 25 compatibility override (default 1.18.34 crashes with TypeTag :: UNKNOWN) |
| `mockito.version` | 5.21.0 | Java 25 compatibility override |
| `byte-buddy.version` | 1.18.4 | Java 25 Mockito bytecode generation |

**Dependencies:**

| Group | Artifact | Version | Scope | Purpose |
|-------|----------|---------|-------|---------|
| `org.springframework.boot` | `spring-boot-starter-web` | (managed) | compile | Embedded Tomcat, Spring MVC |
| `org.springframework.boot` | `spring-boot-starter-data-jpa` | (managed) | compile | JPA / Hibernate ORM |
| `org.springframework.boot` | `spring-boot-starter-security` | (managed) | compile | Spring Security framework |
| `org.springframework.boot` | `spring-boot-starter-validation` | (managed) | compile | Jakarta Bean Validation |
| `org.postgresql` | `postgresql` | (managed) | runtime | PostgreSQL JDBC driver |
| `io.jsonwebtoken` | `jjwt-api` | 0.12.6 | compile | JWT API |
| `io.jsonwebtoken` | `jjwt-impl` | 0.12.6 | runtime | JWT implementation |
| `io.jsonwebtoken` | `jjwt-jackson` | 0.12.6 | runtime | JWT Jackson serialization |
| `software.amazon.awssdk` | `s3` | 2.25.0 | compile | AWS S3 file storage |
| `software.amazon.awssdk` | `ses` | 2.25.0 | compile | AWS SES email sending |
| `org.projectlombok` | `lombok` | 1.18.42 | provided | Code generation (getters, builders, etc.) |
| `org.mapstruct` | `mapstruct` | 1.5.5.Final | compile | Object mapping code generation |
| `com.fasterxml.jackson.datatype` | `jackson-datatype-jsr310` | (managed) | compile | Java 8+ date/time serialization |
| `org.springdoc` | `springdoc-openapi-starter-webmvc-ui` | 2.5.0 | compile | Swagger UI and OpenAPI spec generation |
| `net.logstash.logback` | `logstash-logback-encoder` | 7.4 | compile | JSON structured logging for production |
| `org.springframework.boot` | `spring-boot-starter-test` | (managed) | test | JUnit 5, Mockito, AssertJ |
| `org.springframework.security` | `spring-security-test` | (managed) | test | Security test utilities |
| `org.testcontainers` | `postgresql` | 1.19.8 | test | Testcontainers PostgreSQL |
| `org.testcontainers` | `junit-jupiter` | 1.19.8 | test | Testcontainers JUnit 5 integration |
| `com.h2database` | `h2` | (managed) | test | In-memory database for unit tests |

**Build Plugins:**

| Plugin | Purpose |
|--------|---------|
| `spring-boot-maven-plugin` | Executable JAR packaging (excludes Lombok) |
| `maven-compiler-plugin` | Java 21 source/target; explicit annotation processor paths for Lombok + MapStruct (Java 22+ requirement) |
| `maven-surefire-plugin` | Test execution with `--add-opens` for reflective access on Java 25; includes `**/*Test.java` and `**/*IT.java` |
| `jacoco-maven-plugin` (0.8.14) | Code coverage reporting (`prepare-agent` + `report` goals) |

**Build Commands:**

```bash
./mvnw clean compile -DskipTests      # Compile only
./mvnw spring-boot:run                # Run application
./mvnw test                           # Run all tests
./mvnw test -Dtest=SomeTest           # Run single test class
./mvnw clean package -DskipTests      # Build JAR
```

---

## 4. Configuration Files

### application.yml

```yaml
spring:
  application:
    name: codeops-server
  profiles:
    active: dev

server:
  port: 8090
```

Annotation: Root config. Sets application name and default profile to `dev`. Server port is 8090.

### application-dev.yml

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
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

Annotation: Development profile. PostgreSQL on localhost:5432. Hibernate ddl-auto=update creates/modifies tables automatically. JWT secret has a dev-only default (32+ chars). Encryption key is dev-only. CORS allows localhost:3000 (Flutter) and localhost:5173 (Vite). AWS S3 and SES are disabled; local file storage at `~/.codeops/storage/`. Debug logging enabled.

### application-prod.yml

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
    org.hibernate.SQL: WARN
    org.springframework.security: WARN
    org.springframework.web: WARN
```

Annotation: Production profile. All secrets from environment variables (no defaults). Hibernate ddl-auto=validate (schema must pre-exist). AWS S3 and SES are enabled. Logging at INFO/WARN levels.

### logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- DEV profile: human-readable console output with MDC fields -->
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-none}]
                    [%X{userId:-anon}] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <logger name="com.codeops" level="DEBUG"/>
        <logger name="org.hibernate.SQL" level="DEBUG"/>
        <logger name="org.springframework.security" level="DEBUG"/>
        <logger name="org.springframework.web" level="DEBUG"/>
        <root level="INFO"><appender-ref ref="CONSOLE"/></root>
    </springProfile>

    <!-- PROD profile: JSON-structured output for log aggregation -->
    <springProfile name="prod">
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>correlationId</includeMdcKeyName>
                <includeMdcKeyName>userId</includeMdcKeyName>
                <includeMdcKeyName>teamId</includeMdcKeyName>
                <includeMdcKeyName>requestPath</includeMdcKeyName>
            </encoder>
        </appender>
        <logger name="com.codeops" level="INFO"/>
        <logger name="org.hibernate.SQL" level="WARN"/>
        <logger name="org.springframework.security" level="WARN"/>
        <logger name="org.springframework.web" level="WARN"/>
        <root level="INFO"><appender-ref ref="JSON_CONSOLE"/></root>
    </springProfile>

    <!-- TEST profile: minimal output at WARN to keep test logs clean -->
    <springProfile name="test">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <logger name="com.codeops" level="WARN"/>
        <logger name="org.hibernate.SQL" level="WARN"/>
        <logger name="org.springframework.security" level="WARN"/>
        <root level="WARN"><appender-ref ref="CONSOLE"/></root>
    </springProfile>

    <!-- Default (no profile): fallback to human-readable console -->
    <springProfile name="default">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-none}]
                    [%X{userId:-anon}] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <logger name="com.codeops" level="INFO"/>
        <root level="INFO"><appender-ref ref="CONSOLE"/></root>
    </springProfile>

</configuration>
```

Annotation: Four logging profiles. Dev: human-readable with MDC correlation ID and user ID. Prod: JSON structured (LogstashEncoder) for log aggregation services. Test: minimal WARN-level output. Default: human-readable fallback at INFO.

### docker-compose.yml

```yaml
name: codeops

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
      - codeops-postgres-data:/var/lib/postgresql/data
    labels:
      com.codeops.project: "codeops-server"
      com.codeops.component: "database"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U codeops -d codeops"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    networks:
      - codeops-network

networks:
  codeops-network:
    name: codeops-network
    driver: bridge

volumes:
  codeops-postgres-data:
    name: codeops-postgres-data
```

Annotation: Local development infrastructure. PostgreSQL 16 Alpine image. Port bound to 127.0.0.1 only (not exposed externally). Named volume for data persistence. Healthcheck via pg_isready. Named network for isolation. Labels for identification.

### Dockerfile

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

Annotation: Single-stage JRE-only image (eclipse-temurin:21-jre-alpine). Non-root user (appuser:appgroup). Copies pre-built JAR. Exposes port 8090.

---

## 5. Startup & Runtime

**Entry Point:** `com.codeops.CodeOpsApplication`

```java
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class CodeOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeOpsApplication.class, args);
    }
}
```

Annotated with `@SpringBootApplication` (component scanning, auto-config). Enables binding of `JwtProperties` from `codeops.jwt.*`.

**Startup Sequence:**

1. Spring Boot starts, activates `dev` profile by default.
2. `RequestCorrelationFilter` (Ordered.HIGHEST_PRECEDENCE) registers as servlet filter.
3. `SecurityConfig` builds filter chain (JWT auth, rate limiting, CORS, security headers).
4. `JwtTokenProvider.validateSecret()` runs `@PostConstruct` -- fails fast if secret < 32 chars.
5. `EncryptionService` constructor derives AES-256 key via PBKDF2.
6. `DataSeeder` (`@Profile("dev")`, `CommandLineRunner`) runs `@Transactional`:
   - Checks `userRepository.count() > 0` -- skips if data exists.
   - Seeds 3 users, 1 team, 3 team members, 3 projects, 6 personas, 4 directives, 6 project-directives, 8 QA jobs, 1 bug investigation, 18 agent runs, 24 findings, 8 remediation tasks, 2 specifications, 6 compliance items, 8 tech debt items, 2 dependency scans, 7 dependency vulnerabilities, 3 health schedules, 9 health snapshots, 6 system settings, 10 audit log entries.

**Health Endpoint:** `GET /api/v1/health` -- returns `{status: "UP", service: "codeops-server", timestamp: ...}`. Publicly accessible (no auth required).

**No scheduled tasks** detected in the codebase.

---

## 6. Entity Layer

### BaseEntity (MappedSuperclass)

Not a table. Provides:
- `id` (UUID, `@GeneratedValue(strategy = GenerationType.UUID)`, PK)
- `created_at` (Instant, nullable=false, updatable=false, set via `@PrePersist`)
- `updated_at` (Instant, set via `@PrePersist` and `@PreUpdate`)

### User

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| email | String | `email` | nullable=false, unique=true, length=255 |
| passwordHash | String | `password_hash` | nullable=false, length=255 |
| displayName | String | `display_name` | nullable=false, length=100 |
| avatarUrl | String | `avatar_url` | length=500 |
| isActive | Boolean | `is_active` | nullable=false, default=true |
| lastLoginAt | Instant | `last_login_at` | nullable |

Table: `users`. Extends BaseEntity.

### Team

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| name | String | `name` | nullable=false, length=100 |
| description | String | `description` | TEXT |
| owner | User (ManyToOne LAZY) | `owner_id` | nullable=false |
| teamsWebhookUrl | String | `teams_webhook_url` | length=500 |
| settingsJson | String | `settings_json` | TEXT |

Table: `teams`. Extends BaseEntity.

### TeamMember

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| team | Team (ManyToOne LAZY) | `team_id` | nullable=false |
| user | User (ManyToOne LAZY) | `user_id` | nullable=false |
| role | TeamRole (STRING) | `role` | nullable=false |
| joinedAt | Instant | `joined_at` | nullable=false |

Table: `team_members`. UniqueConstraint on (team_id, user_id). Indexes: `idx_tm_team_id`, `idx_tm_user_id`. Extends BaseEntity.

### Project

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| team | Team (ManyToOne LAZY) | `team_id` | nullable=false |
| name | String | `name` | nullable=false, length=200 |
| description | String | `description` | TEXT |
| githubConnection | GitHubConnection (ManyToOne LAZY) | `github_connection_id` | nullable |
| repoUrl | String | `repo_url` | length=500 |
| repoFullName | String | `repo_full_name` | length=200 |
| defaultBranch | String | `default_branch` | default='main', length=100 |
| jiraConnection | JiraConnection (ManyToOne LAZY) | `jira_connection_id` | nullable |
| jiraProjectKey | String | `jira_project_key` | length=20 |
| jiraDefaultIssueType | String | `jira_default_issue_type` | default='Task', length=50 |
| jiraLabels | String | `jira_labels` | TEXT |
| jiraComponent | String | `jira_component` | length=100 |
| techStack | String | `tech_stack` | length=200 |
| healthScore | Integer | `health_score` | nullable |
| lastAuditAt | Instant | `last_audit_at` | nullable |
| settingsJson | String | `settings_json` | TEXT |
| isArchived | Boolean | `is_archived` | nullable=false, default=false |
| createdBy | User (ManyToOne LAZY) | `created_by` | nullable=false |

Table: `projects`. Index: `idx_project_team_id`. Extends BaseEntity.

### Persona

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| name | String | `name` | nullable=false, length=100 |
| agentType | AgentType (STRING) | `agent_type` | nullable=false |
| description | String | `description` | TEXT |
| contentMd | String | `content_md` | nullable=false, TEXT |
| scope | Scope (STRING) | `scope` | nullable=false |
| team | Team (ManyToOne LAZY) | `team_id` | nullable |
| createdBy | User (ManyToOne LAZY) | `created_by` | nullable=false |
| isDefault | Boolean | `is_default` | default=false |
| version | Integer | `version` | nullable=false, default=1 |

Table: `personas`. Index: `idx_persona_team_id`. Extends BaseEntity.

### Directive

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| name | String | `name` | nullable=false, length=200 |
| description | String | `description` | TEXT |
| contentMd | String | `content_md` | nullable=false, TEXT |
| category | DirectiveCategory (STRING) | `category` | nullable |
| scope | DirectiveScope (STRING) | `scope` | nullable=false |
| team | Team (ManyToOne LAZY) | `team_id` | nullable |
| project | Project (ManyToOne LAZY) | `project_id` | nullable |
| createdBy | User (ManyToOne LAZY) | `created_by` | nullable=false |
| version | Integer | `version` | default=1 |

Table: `directives`. Index: `idx_directive_team_id`. Extends BaseEntity.

### ProjectDirective (join table entity)

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| id | ProjectDirectiveId (EmbeddedId) | composite PK | (project_id, directive_id) |
| project | Project (ManyToOne LAZY, @MapsId) | `project_id` | FK |
| directive | Directive (ManyToOne LAZY, @MapsId) | `directive_id` | FK |
| enabled | Boolean | `enabled` | nullable=false, default=true |

Table: `project_directives`. Does NOT extend BaseEntity.

### ProjectDirectiveId (Embeddable)

| Field | Type | Column |
|-------|------|--------|
| projectId | UUID | `project_id` |
| directiveId | UUID | `directive_id` |

Implements `Serializable`, overrides `equals()` and `hashCode()`.

### QaJob

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| project | Project (ManyToOne LAZY) | `project_id` | nullable=false |
| mode | JobMode (STRING) | `mode` | nullable=false |
| status | JobStatus (STRING) | `status` | nullable=false |
| name | String | `name` | length=200 |
| branch | String | `branch` | length=100 |
| configJson | String | `config_json` | TEXT |
| summaryMd | String | `summary_md` | TEXT |
| overallResult | JobResult (STRING) | `overall_result` | nullable |
| healthScore | Integer | `health_score` | nullable |
| totalFindings | Integer | `total_findings` | default=0 |
| criticalCount | Integer | `critical_count` | default=0 |
| highCount | Integer | `high_count` | default=0 |
| mediumCount | Integer | `medium_count` | default=0 |
| lowCount | Integer | `low_count` | default=0 |
| jiraTicketKey | String | `jira_ticket_key` | length=50 |
| startedBy | User (ManyToOne LAZY) | `started_by` | nullable=false |
| startedAt | Instant | `started_at` | nullable |
| completedAt | Instant | `completed_at` | nullable |
| version | Long (@Version) | `version` | optimistic locking |

Table: `qa_jobs`. Indexes: `idx_job_project_id`, `idx_job_started_by`. Extends BaseEntity.

### AgentRun

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| job | QaJob (ManyToOne LAZY) | `job_id` | nullable=false |
| agentType | AgentType (STRING) | `agent_type` | nullable=false |
| status | AgentStatus (STRING) | `status` | nullable=false |
| result | AgentResult (STRING) | `result` | nullable |
| reportS3Key | String | `report_s3_key` | length=500 |
| score | Integer | `score` | nullable |
| findingsCount | Integer | `findings_count` | default=0 |
| criticalCount | Integer | `critical_count` | default=0 |
| highCount | Integer | `high_count` | default=0 |
| startedAt | Instant | `started_at` | nullable |
| completedAt | Instant | `completed_at` | nullable |
| version | Long (@Version) | `version` | optimistic locking |

Table: `agent_runs`. Index: `idx_agent_run_job_id`. Extends BaseEntity.

### Finding

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| job | QaJob (ManyToOne LAZY) | `job_id` | nullable=false |
| agentType | AgentType (STRING) | `agent_type` | nullable=false |
| severity | Severity (STRING) | `severity` | nullable=false |
| title | String | `title` | nullable=false, length=500 |
| description | String | `description` | TEXT |
| filePath | String | `file_path` | length=500 |
| lineNumber | Integer | `line_number` | nullable |
| recommendation | String | `recommendation` | TEXT |
| evidence | String | `evidence` | TEXT |
| effortEstimate | Effort (STRING) | `effort_estimate` | nullable |
| debtCategory | DebtCategory (STRING) | `debt_category` | nullable |
| status | FindingStatus (STRING) | `status` | default='OPEN' |
| statusChangedBy | User (ManyToOne LAZY) | `status_changed_by` | nullable |
| statusChangedAt | Instant | `status_changed_at` | nullable |
| version | Long (@Version) | `version` | optimistic locking |

Table: `findings`. Indexes: `idx_finding_job_id`, `idx_finding_status`. Extends BaseEntity.

### RemediationTask

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| job | QaJob (ManyToOne LAZY) | `job_id` | nullable=false |
| taskNumber | Integer | `task_number` | nullable=false |
| title | String | `title` | nullable=false, length=500 |
| description | String | `description` | TEXT |
| promptMd | String | `prompt_md` | TEXT |
| promptS3Key | String | `prompt_s3_key` | length=500 |
| findings | List<Finding> (@ManyToMany) | `remediation_task_findings` (join table) | default=empty |
| priority | Priority (STRING) | `priority` | nullable=false |
| status | TaskStatus (STRING) | `status` | default='PENDING' |
| assignedTo | User (ManyToOne LAZY) | `assigned_to` | nullable |
| jiraKey | String | `jira_key` | length=50 |
| version | Long (@Version) | `version` | optimistic locking |

Table: `remediation_tasks`. Join table: `remediation_task_findings` (task_id, finding_id). Index: `idx_task_job_id`. Extends BaseEntity.

### BugInvestigation

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| job | QaJob (ManyToOne LAZY) | `job_id` | nullable=false |
| jiraKey | String | `jira_key` | length=50 |
| jiraSummary | String | `jira_summary` | TEXT |
| jiraDescription | String | `jira_description` | TEXT |
| jiraCommentsJson | String | `jira_comments_json` | TEXT |
| jiraAttachmentsJson | String | `jira_attachments_json` | TEXT |
| jiraLinkedIssues | String | `jira_linked_issues` | TEXT |
| additionalContext | String | `additional_context` | TEXT |
| rcaMd | String | `rca_md` | TEXT |
| impactAssessmentMd | String | `impact_assessment_md` | TEXT |
| rcaS3Key | String | `rca_s3_key` | length=500 |
| rcaPostedToJira | Boolean | `rca_posted_to_jira` | default=false |
| fixTasksCreatedInJira | Boolean | `fix_tasks_created_in_jira` | default=false |

Table: `bug_investigations`. Extends BaseEntity.

### Specification

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| job | QaJob (ManyToOne LAZY) | `job_id` | nullable=false |
| name | String | `name` | nullable=false, length=200 |
| specType | SpecType (STRING) | `spec_type` | nullable=false |
| s3Key | String | `s3_key` | nullable=false, length=500 |

Table: `specifications`. Index: `idx_spec_job_id`. Extends BaseEntity.

### ComplianceItem

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| job | QaJob (ManyToOne LAZY) | `job_id` | nullable=false |
| requirement | String | `requirement` | nullable=false, TEXT |
| spec | Specification (ManyToOne LAZY) | `spec_id` | nullable |
| status | ComplianceStatus (STRING) | `status` | nullable=false |
| evidence | String | `evidence` | TEXT |
| agentType | AgentType (STRING) | `agent_type` | nullable |
| notes | String | `notes` | TEXT |

Table: `compliance_items`. Index: `idx_compliance_job_id`. Extends BaseEntity.

### TechDebtItem

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| project | Project (ManyToOne LAZY) | `project_id` | nullable=false |
| category | DebtCategory (STRING) | `category` | nullable=false |
| title | String | `title` | nullable=false, length=500 |
| description | String | `description` | TEXT |
| filePath | String | `file_path` | length=500 |
| effortEstimate | Effort (STRING) | `effort_estimate` | nullable |
| businessImpact | BusinessImpact (STRING) | `business_impact` | nullable |
| status | DebtStatus (STRING) | `status` | default='IDENTIFIED' |
| firstDetectedJob | QaJob (ManyToOne LAZY) | `first_detected_job_id` | nullable |
| resolvedJob | QaJob (ManyToOne LAZY) | `resolved_job_id` | nullable |
| version | Long (@Version) | `version` | optimistic locking |

Table: `tech_debt_items`. Index: `idx_tech_debt_project_id`. Extends BaseEntity.

### DependencyScan

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| project | Project (ManyToOne LAZY) | `project_id` | nullable=false |
| job | QaJob (ManyToOne LAZY) | `job_id` | nullable |
| manifestFile | String | `manifest_file` | length=200 |
| totalDependencies | Integer | `total_dependencies` | nullable |
| outdatedCount | Integer | `outdated_count` | nullable |
| vulnerableCount | Integer | `vulnerable_count` | nullable |
| scanDataJson | String | `scan_data_json` | TEXT |

Table: `dependency_scans`. Index: `idx_dep_scan_project_id`. Extends BaseEntity.

### DependencyVulnerability

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| scan | DependencyScan (ManyToOne LAZY) | `scan_id` | nullable=false |
| dependencyName | String | `dependency_name` | nullable=false, length=200 |
| currentVersion | String | `current_version` | length=50 |
| fixedVersion | String | `fixed_version` | length=50 |
| cveId | String | `cve_id` | length=30 |
| severity | Severity (STRING) | `severity` | nullable=false |
| description | String | `description` | TEXT |
| status | VulnerabilityStatus (STRING) | `status` | default='OPEN' |

Table: `dependency_vulnerabilities`. Index: `idx_vuln_scan_id`. Extends BaseEntity.

### HealthSchedule

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| project | Project (ManyToOne LAZY) | `project_id` | nullable=false |
| scheduleType | ScheduleType (STRING) | `schedule_type` | nullable=false |
| cronExpression | String | `cron_expression` | length=50 |
| agentTypes | String | `agent_types` | nullable=false, TEXT (JSON array) |
| isActive | Boolean | `is_active` | nullable=false, default=true |
| lastRunAt | Instant | `last_run_at` | nullable |
| nextRunAt | Instant | `next_run_at` | nullable |
| createdBy | User (ManyToOne LAZY) | `created_by` | nullable=false |

Table: `health_schedules`. Index: `idx_schedule_project_id`. Extends BaseEntity.

### HealthSnapshot

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| project | Project (ManyToOne LAZY) | `project_id` | nullable=false |
| job | QaJob (ManyToOne LAZY) | `job_id` | nullable |
| healthScore | Integer | `health_score` | nullable=false |
| findingsBySeverity | String | `findings_by_severity` | TEXT (JSON) |
| techDebtScore | Integer | `tech_debt_score` | nullable |
| dependencyScore | Integer | `dependency_score` | nullable |
| testCoveragePercent | BigDecimal | `test_coverage_percent` | precision=5, scale=2 |
| capturedAt | Instant | `captured_at` | nullable=false |

Table: `health_snapshots`. Index: `idx_snapshot_project_id`. Extends BaseEntity.

### GitHubConnection

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| team | Team (ManyToOne LAZY) | `team_id` | nullable=false |
| name | String | `name` | nullable=false, length=100 |
| authType | GitHubAuthType (STRING) | `auth_type` | nullable=false |
| encryptedCredentials | String | `encrypted_credentials` | nullable=false, TEXT |
| githubUsername | String | `github_username` | length=100 |
| isActive | Boolean | `is_active` | nullable=false, default=true |
| createdBy | User (ManyToOne LAZY) | `created_by` | nullable=false |

Table: `github_connections`. Extends BaseEntity.

### JiraConnection

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| team | Team (ManyToOne LAZY) | `team_id` | nullable=false |
| name | String | `name` | nullable=false, length=100 |
| instanceUrl | String | `instance_url` | nullable=false, length=500 |
| email | String | `email` | nullable=false, length=255 |
| encryptedApiToken | String | `encrypted_api_token` | nullable=false, TEXT |
| isActive | Boolean | `is_active` | nullable=false, default=true |
| createdBy | User (ManyToOne LAZY) | `created_by` | nullable=false |

Table: `jira_connections`. Extends BaseEntity.

### Invitation

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| team | Team (ManyToOne LAZY) | `team_id` | nullable=false |
| email | String | `email` | nullable=false, length=255 |
| invitedBy | User (ManyToOne LAZY) | `invited_by` | nullable=false |
| role | TeamRole (STRING) | `role` | nullable=false |
| token | String | `token` | nullable=false, unique=true, length=100 |
| status | InvitationStatus (STRING) | `status` | nullable=false |
| expiresAt | Instant | `expires_at` | nullable=false |

Table: `invitations`. Indexes: `idx_inv_team_id`, `idx_inv_email`. Extends BaseEntity.

### NotificationPreference

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| user | User (ManyToOne LAZY) | `user_id` | nullable=false |
| eventType | String | `event_type` | nullable=false, length=50 |
| inApp | Boolean | `in_app` | nullable=false, default=true |
| email | Boolean | `email` | nullable=false, default=false |

Table: `notification_preferences`. UniqueConstraint on (user_id, event_type). Index: `idx_notif_user_id`. Extends BaseEntity.

### SystemSetting

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| settingKey | String (@Id) | `key` | PK, length=100 |
| value | String | `value` | nullable=false, TEXT |
| updatedBy | User (ManyToOne LAZY) | `updated_by` | nullable |
| updatedAt | Instant | `updated_at` | nullable=false |

Table: `system_settings`. Does NOT extend BaseEntity (String PK, no UUID, no createdAt).

### AuditLog

| Field | Type | Column | Constraints |
|-------|------|--------|------------|
| id | Long (@Id, IDENTITY) | `id` | auto-increment PK |
| user | User (ManyToOne LAZY) | `user_id` | nullable |
| team | Team (ManyToOne LAZY) | `team_id` | nullable |
| action | String | `action` | nullable=false, length=50 |
| entityType | String | `entity_type` | length=30 |
| entityId | UUID | `entity_id` | nullable |
| details | String | `details` | TEXT |
| ipAddress | String | `ip_address` | length=45 |
| createdAt | Instant | `created_at` | nullable=false |

Table: `audit_log`. Indexes: `idx_audit_user_id`, `idx_audit_team_id`. Does NOT extend BaseEntity (Long auto-increment PK).

### Entity Relationship Summary

- **User** is referenced by: Team (owner), TeamMember, Project (createdBy), Persona (createdBy), Directive (createdBy), QaJob (startedBy), Finding (statusChangedBy), RemediationTask (assignedTo), Invitation (invitedBy), GitHubConnection (createdBy), JiraConnection (createdBy), HealthSchedule (createdBy), NotificationPreference, SystemSetting (updatedBy), AuditLog.
- **Team** owns: TeamMember, Project, Persona, Directive, GitHubConnection, JiraConnection, Invitation. Referenced by AuditLog.
- **Project** owns: QaJob, DependencyScan, TechDebtItem, HealthSchedule, HealthSnapshot. Linked via ProjectDirective (many-to-many with Directive).
- **QaJob** owns: AgentRun, Finding, RemediationTask, BugInvestigation, Specification, ComplianceItem. Referenced by DependencyScan, HealthSnapshot, TechDebtItem.
- **RemediationTask** has ManyToMany with Finding via `remediation_task_findings` join table.
- **DependencyScan** owns DependencyVulnerability.
- **Specification** is referenced by ComplianceItem.

---

## 7. Enum Definitions

| Enum | Values | Used By |
|------|--------|---------|
| `AgentType` | SECURITY, CODE_QUALITY, BUILD_HEALTH, COMPLETENESS, API_CONTRACT, TEST_COVERAGE, UI_UX, DOCUMENTATION, DATABASE, PERFORMANCE, DEPENDENCY, ARCHITECTURE | AgentRun, Finding, ComplianceItem, Persona |
| `AgentStatus` | PENDING, RUNNING, COMPLETED, FAILED | AgentRun |
| `AgentResult` | PASS, WARN, FAIL | AgentRun |
| `BusinessImpact` | LOW, MEDIUM, HIGH, CRITICAL | TechDebtItem |
| `ComplianceStatus` | MET, PARTIAL, MISSING, NOT_APPLICABLE | ComplianceItem |
| `DebtCategory` | ARCHITECTURE, CODE, TEST, DEPENDENCY, DOCUMENTATION | TechDebtItem, Finding |
| `DebtStatus` | IDENTIFIED, PLANNED, IN_PROGRESS, RESOLVED | TechDebtItem |
| `DirectiveCategory` | ARCHITECTURE, STANDARDS, CONVENTIONS, CONTEXT, OTHER | Directive |
| `DirectiveScope` | TEAM, PROJECT, USER | Directive |
| `Effort` | S, M, L, XL | Finding, TechDebtItem |
| `FindingStatus` | OPEN, ACKNOWLEDGED, FALSE_POSITIVE, FIXED, WONT_FIX | Finding |
| `GitHubAuthType` | PAT, OAUTH, SSH | GitHubConnection |
| `InvitationStatus` | PENDING, ACCEPTED, EXPIRED | Invitation |
| `JobMode` | AUDIT, COMPLIANCE, BUG_INVESTIGATE, REMEDIATE, TECH_DEBT, DEPENDENCY, HEALTH_MONITOR | QaJob |
| `JobResult` | PASS, WARN, FAIL | QaJob |
| `JobStatus` | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED | QaJob |
| `Priority` | P0, P1, P2, P3 | RemediationTask |
| `ScheduleType` | DAILY, WEEKLY, ON_COMMIT | HealthSchedule |
| `Scope` | SYSTEM, TEAM, USER | Persona |
| `Severity` | CRITICAL, HIGH, MEDIUM, LOW | Finding, DependencyVulnerability |
| `SpecType` | OPENAPI, MARKDOWN, SCREENSHOT, FIGMA | Specification |
| `TaskStatus` | PENDING, ASSIGNED, EXPORTED, JIRA_CREATED, COMPLETED | RemediationTask |
| `TeamRole` | OWNER, ADMIN, MEMBER, VIEWER | TeamMember, Invitation |
| `VulnerabilityStatus` | OPEN, UPDATING, SUPPRESSED, RESOLVED | DependencyVulnerability |

All 24 enums. All enum fields use `@Enumerated(EnumType.STRING)`.

---

## 8. Repository Layer

All repositories extend `JpaRepository` and are annotated with `@Repository`.

| Repository | Entity | PK Type | Notable Custom Methods |
|-----------|--------|---------|----------------------|
| `UserRepository` | User | UUID | `findByEmail(String)`, `existsByEmail(String)`, `findByDisplayNameContainingIgnoreCase(String)`, `countByIsActiveTrue()` |
| `TeamRepository` | Team | UUID | `findByOwnerId(UUID)` |
| `TeamMemberRepository` | TeamMember | UUID | `findByTeamId(UUID)`, `findByUserId(UUID)`, `findByTeamIdAndUserId(UUID, UUID)`, `existsByTeamIdAndUserId(UUID, UUID)`, `countByTeamId(UUID)`, `deleteByTeamIdAndUserId(UUID, UUID)` |
| `ProjectRepository` | Project | UUID | `findByTeamIdAndIsArchivedFalse(UUID)`, `findByTeamId(UUID)`, `Page<> findByTeamId(UUID, Pageable)`, `Page<> findByTeamIdAndIsArchivedFalse(UUID, Pageable)`, `findByTeamIdAndRepoFullName(UUID, String)`, `countByTeamId(UUID)` |
| `PersonaRepository` | Persona | UUID | `findByTeamId(UUID)`, `Page<> findByTeamId(UUID, Pageable)`, `findByScope(Scope)`, `findByTeamIdAndAgentType(UUID, AgentType)`, `findByTeamIdAndAgentTypeAndIsDefaultTrue(UUID, AgentType)`, `findByCreatedById(UUID)` |
| `DirectiveRepository` | Directive | UUID | `findByTeamId(UUID)`, `findByProjectId(UUID)`, `findByTeamIdAndScope(UUID, DirectiveScope)` |
| `ProjectDirectiveRepository` | ProjectDirective | ProjectDirectiveId | `findByProjectId(UUID)`, `findByProjectIdAndEnabledTrue(UUID)`, `findByDirectiveId(UUID)`, `deleteByProjectIdAndDirectiveId(UUID, UUID)` |
| `QaJobRepository` | QaJob | UUID | `findByProjectIdOrderByCreatedAtDesc(UUID)`, `findByProjectIdAndMode(UUID, JobMode)`, `findByStartedById(UUID)`, `Page<> findByStartedById(UUID, Pageable)`, `Page<> findByProjectId(UUID, Pageable)`, `countByProjectIdAndStatus(UUID, JobStatus)` |
| `AgentRunRepository` | AgentRun | UUID | `findByJobId(UUID)`, `findByJobIdAndStatus(UUID, AgentStatus)`, `findByJobIdAndAgentType(UUID, AgentType)` |
| `FindingRepository` | Finding | UUID | `findByJobId(UUID)`, `findByJobIdAndAgentType(UUID, AgentType)`, `findByJobIdAndSeverity(UUID, Severity)`, `findByJobIdAndStatus(UUID, FindingStatus)`, paginated variants for all filters, `countByJobIdAndSeverity(UUID, Severity)`, `countByJobIdAndSeverityAndStatus(UUID, Severity, FindingStatus)` |
| `RemediationTaskRepository` | RemediationTask | UUID | `findByJobIdOrderByTaskNumberAsc(UUID)`, `Page<> findByJobId(UUID, Pageable)`, `findByAssignedToId(UUID)`, `Page<> findByAssignedToId(UUID, Pageable)` |
| `BugInvestigationRepository` | BugInvestigation | UUID | `findByJobId(UUID)`, `findByJiraKey(String)` |
| `SpecificationRepository` | Specification | UUID | `findByJobId(UUID)`, `Page<> findByJobId(UUID, Pageable)` |
| `ComplianceItemRepository` | ComplianceItem | UUID | `findByJobId(UUID)`, `Page<> findByJobId(UUID, Pageable)`, `findByJobIdAndStatus(UUID, ComplianceStatus)`, `Page<> findByJobIdAndStatus(UUID, ComplianceStatus, Pageable)` |
| `TechDebtItemRepository` | TechDebtItem | UUID | `findByProjectId(UUID)`, paginated `findByProjectId`, `findByProjectIdAndStatus(UUID, DebtStatus)`, `findByProjectIdAndCategory(UUID, DebtCategory)`, paginated variants, `countByProjectIdAndStatus(UUID, DebtStatus)` |
| `DependencyScanRepository` | DependencyScan | UUID | `findByProjectIdOrderByCreatedAtDesc(UUID)`, `Page<> findByProjectId(UUID, Pageable)`, `findFirstByProjectIdOrderByCreatedAtDesc(UUID)` |
| `DependencyVulnerabilityRepository` | DependencyVulnerability | UUID | `findByScanId(UUID)`, `Page<> findByScanId(UUID, Pageable)`, `findByScanIdAndStatus(UUID, VulnerabilityStatus)`, `findByScanIdAndSeverity(UUID, Severity)`, paginated variants, `countByScanIdAndStatus(UUID, VulnerabilityStatus)` |
| `HealthScheduleRepository` | HealthSchedule | UUID | `findByProjectId(UUID)`, `findByIsActiveTrue()` |
| `HealthSnapshotRepository` | HealthSnapshot | UUID | `findByProjectIdOrderByCapturedAtDesc(UUID)`, `Page<> findByProjectId(UUID, Pageable)`, `findFirstByProjectIdOrderByCapturedAtDesc(UUID)` |
| `GitHubConnectionRepository` | GitHubConnection | UUID | `findByTeamIdAndIsActiveTrue(UUID)` |
| `JiraConnectionRepository` | JiraConnection | UUID | `findByTeamIdAndIsActiveTrue(UUID)` |
| `InvitationRepository` | Invitation | UUID | `findByToken(String)`, `findByTeamIdAndStatus(UUID, InvitationStatus)`, `findByEmailAndStatus(String, InvitationStatus)`, `@Lock(PESSIMISTIC_WRITE) findByTeamIdAndEmailAndStatusForUpdate(UUID, String, InvitationStatus)` |
| `NotificationPreferenceRepository` | NotificationPreference | UUID | `findByUserId(UUID)`, `findByUserIdAndEventType(UUID, String)` |
| `SystemSettingRepository` | SystemSetting | String | (no custom methods) |
| `AuditLogRepository` | AuditLog | Long | `Page<> findByTeamIdOrderByCreatedAtDesc(UUID, Pageable)`, `Page<> findByUserIdOrderByCreatedAtDesc(UUID, Pageable)`, `findByEntityTypeAndEntityId(String, UUID)` |

---

## 9. DTO Layer

### Request DTOs (41 files)

All request DTOs are Java `record` types located in `com.codeops.dto.request`.

| DTO | Fields | Validation |
|-----|--------|------------|
| `AdminUpdateUserRequest` | `Boolean isActive` | none |
| `AssignDirectiveRequest` | `UUID projectId, UUID directiveId, boolean enabled` | `@NotNull` on projectId, directiveId |
| `BulkUpdateFindingsRequest` | `List<UUID> findingIds, FindingStatus status` | `@NotEmpty @Size(max=100)` on findingIds, `@NotNull` on status |
| `ChangePasswordRequest` | `String currentPassword, String newPassword` | `@NotBlank` both, `@Size(min=8)` on newPassword |
| `CreateAgentRunRequest` | `UUID jobId, AgentType agentType` | `@NotNull` both |
| `CreateBugInvestigationRequest` | `UUID jobId, String jiraKey, jiraSummary, jiraDescription, jiraCommentsJson, jiraAttachmentsJson, jiraLinkedIssues, additionalContext` | `@NotNull` on jobId, `@Size` limits |
| `CreateComplianceItemRequest` | `UUID jobId, String requirement, UUID specId, ComplianceStatus status, String evidence, AgentType agentType, String notes` | `@NotNull` on jobId/status, `@NotBlank @Size` on requirement |
| `CreateDependencyScanRequest` | `UUID projectId, UUID jobId, String manifestFile, Integer totals...` | `@NotNull` on projectId |
| `CreateDirectiveRequest` | `String name, description, contentMd, DirectiveCategory category, DirectiveScope scope, UUID teamId, projectId` | `@NotBlank @Size` on name/contentMd, `@NotNull` on scope |
| `CreateFindingRequest` | `UUID jobId, AgentType agentType, Severity severity, String title, description, filePath, Integer lineNumber, String recommendation, evidence, Effort, DebtCategory` | `@NotNull` on jobId/agentType/severity, `@NotBlank @Size(max=500)` on title |
| `CreateGitHubConnectionRequest` | `String name, GitHubAuthType authType, String credentials, githubUsername` | `@NotBlank @Size` on name/credentials, `@NotNull` on authType |
| `CreateHealthScheduleRequest` | `UUID projectId, ScheduleType scheduleType, String cronExpression, List<AgentType> agentTypes` | `@NotNull` on projectId/scheduleType, `@NotEmpty @Size(max=20)` on agentTypes |
| `CreateHealthSnapshotRequest` | `UUID projectId, UUID jobId, Integer healthScore, String findingsBySeverity, Integer techDebtScore, dependencyScore, BigDecimal testCoveragePercent` | `@NotNull` on projectId/healthScore |
| `CreateJiraConnectionRequest` | `String name, instanceUrl, email, apiToken` | `@NotBlank @Size` on all, `@Email` on email |
| `CreateJobRequest` | `UUID projectId, JobMode mode, String name, branch, configJson, jiraTicketKey` | `@NotNull` on projectId/mode, `@Size` limits |
| `CreatePersonaRequest` | `String name, AgentType agentType, String description, contentMd, Scope scope, UUID teamId, Boolean isDefault` | `@NotBlank @Size` on name/contentMd, `@NotNull` on scope |
| `CreateProjectRequest` | `String name, description, UUID githubConnectionId, String repoUrl, repoFullName, defaultBranch, UUID jiraConnectionId, String jiraProjectKey, jiraDefaultIssueType, List<String> jiraLabels, String jiraComponent, techStack` | `@NotBlank @Size(max=200)` on name |
| `CreateSpecificationRequest` | `UUID jobId, String name, SpecType specType, String s3Key` | `@NotNull` on jobId, `@NotBlank @Size` on name/s3Key |
| `CreateTaskRequest` | `UUID jobId, Integer taskNumber, String title, description, promptMd, promptS3Key, List<UUID> findingIds, Priority priority` | `@NotNull` on jobId/taskNumber, `@NotBlank @Size(max=500)` on title |
| `CreateTeamRequest` | `String name, description, teamsWebhookUrl` | `@NotBlank @Size(max=100)` on name |
| `CreateTechDebtItemRequest` | `UUID projectId, DebtCategory category, String title, description, filePath, Effort, BusinessImpact, UUID firstDetectedJobId` | `@NotNull` on projectId/category, `@NotBlank @Size(max=500)` on title |
| `CreateVulnerabilityRequest` | `UUID scanId, String dependencyName, currentVersion, fixedVersion, cveId, Severity severity, String description` | `@NotNull` on scanId/severity, `@NotBlank @Size(max=200)` on dependencyName |
| `InviteMemberRequest` | `String email, TeamRole role` | `@NotBlank @Email`, `@NotNull` on role |
| `LoginRequest` | `String email, String password` | `@NotBlank @Email` on email, `@NotBlank` on password |
| `PasswordResetRequest` | `String email` | `@NotBlank @Email` |
| `RefreshTokenRequest` | `String refreshToken` | `@NotBlank` |
| `RegisterRequest` | `String email, password, displayName` | `@NotBlank @Email` on email, `@NotBlank` on password, `@NotBlank @Size(max=100)` on displayName |
| `UpdateAgentRunRequest` | `AgentStatus status, AgentResult result, String reportS3Key, Integer score, findingsCount, criticalCount, highCount, Instant completedAt, startedAt` | `@Size` on reportS3Key |
| `UpdateBugInvestigationRequest` | `String rcaMd, impactAssessmentMd, rcaS3Key, Boolean rcaPostedToJira, fixTasksCreatedInJira` | `@Size` limits |
| `UpdateDirectiveRequest` | `String name, description, contentMd, DirectiveCategory category` | `@Size` limits |
| `UpdateFindingStatusRequest` | `FindingStatus status` | `@NotNull` |
| `UpdateJobRequest` | `JobStatus status, String summaryMd, JobResult overallResult, Integer healthScore, finding counts..., Instant completedAt, startedAt` | `@Size` on summaryMd |
| `UpdateMemberRoleRequest` | `TeamRole role` | `@NotNull` |
| `UpdateNotificationPreferenceRequest` | `String eventType, boolean inApp, boolean email` | `@NotBlank @Size(max=200)` on eventType |
| `UpdatePersonaRequest` | `String name, description, contentMd, Boolean isDefault` | `@Size` limits |
| `UpdateProjectRequest` | `String name, description, UUID githubConnectionId, String repoUrl, repoFullName, defaultBranch, UUID jiraConnectionId, String jiraProjectKey...` | `@Size` limits |
| `UpdateSystemSettingRequest` | `String key, String value` | `@NotBlank @Size` on both |
| `UpdateTaskRequest` | `TaskStatus status, UUID assignedTo, String jiraKey` | `@Size` on jiraKey |
| `UpdateTeamRequest` | `String name, description, teamsWebhookUrl` | `@Size` limits |
| `UpdateTechDebtStatusRequest` | `DebtStatus status, UUID resolvedJobId` | `@NotNull` on status |
| `UpdateUserRequest` | `String displayName, avatarUrl` | `@Size` limits |

### Response DTOs (31 files)

All response DTOs are Java `record` types in `com.codeops.dto.response`. No validation annotations on response DTOs.

| DTO | Key Fields |
|-----|------------|
| `AgentRunResponse` | id, jobId, agentType, status, result, reportS3Key, score, findingsCount, criticalCount, highCount, startedAt, completedAt |
| `AuditLogResponse` | id, userId, userName, teamId, action, entityType, entityId, details, ipAddress, createdAt |
| `AuthResponse` | token, refreshToken, user (UserResponse) |
| `BugInvestigationResponse` | id, jobId, jiraKey, jiraSummary, jiraDescription, additionalContext, rcaMd, impactAssessmentMd, rcaS3Key, rcaPostedToJira, fixTasksCreatedInJira, createdAt |
| `ComplianceItemResponse` | id, jobId, requirement, specId, specName, status, evidence, agentType, notes, createdAt |
| `DependencyScanResponse` | id, projectId, jobId, manifestFile, totalDependencies, outdatedCount, vulnerableCount, createdAt |
| `DirectiveResponse` | id, name, description, contentMd, category, scope, teamId, projectId, createdBy, createdByName, version, createdAt, updatedAt |
| `ErrorResponse` | status (int), message (String) |
| `FindingResponse` | id, jobId, agentType, severity, title, description, filePath, lineNumber, recommendation, evidence, effortEstimate, debtCategory, status, statusChangedBy, statusChangedAt, createdAt |
| `GitHubConnectionResponse` | id, teamId, name, authType, githubUsername, isActive, createdAt |
| `HealthScheduleResponse` | id, projectId, scheduleType, cronExpression, agentTypes (List<AgentType>), isActive, lastRunAt, nextRunAt, createdAt |
| `HealthSnapshotResponse` | id, projectId, jobId, healthScore, findingsBySeverity, techDebtScore, dependencyScore, testCoveragePercent, capturedAt |
| `InvitationResponse` | id, email, role, status, invitedByName, expiresAt, createdAt |
| `JiraConnectionResponse` | id, teamId, name, instanceUrl, email, isActive, createdAt |
| `JobResponse` | id, projectId, projectName, mode, status, name, branch, configJson, summaryMd, overallResult, healthScore, finding counts, jiraTicketKey, startedBy, startedByName, startedAt, completedAt, createdAt |
| `JobSummaryResponse` | id, projectName, mode, status, name, overallResult, healthScore, totalFindings, criticalCount, completedAt, createdAt |
| `NotificationPreferenceResponse` | eventType, inApp, email |
| `PageResponse<T>` | content (List<T>), page, size, totalElements, totalPages, isLast |
| `PersonaResponse` | id, name, agentType, description, contentMd, scope, teamId, createdBy, createdByName, isDefault, version, createdAt, updatedAt |
| `ProjectDirectiveResponse` | projectId, directiveId, directiveName, category, enabled |
| `ProjectMetricsResponse` | projectId, projectName, currentHealthScore, previousHealthScore, totalJobs, totalFindings, openCritical, openHigh, techDebtItemCount, openVulnerabilities, lastAuditAt |
| `ProjectResponse` | id, teamId, name, description, githubConnectionId, repoUrl, repoFullName, defaultBranch, jiraConnectionId, jiraProjectKey, jiraDefaultIssueType, jiraLabels, jiraComponent, techStack, healthScore, lastAuditAt, isArchived, createdAt, updatedAt |
| `SpecificationResponse` | id, jobId, name, specType, s3Key, createdAt |
| `SystemSettingResponse` | key, value, updatedBy (UUID), updatedAt |
| `TaskResponse` | id, jobId, taskNumber, title, description, promptMd, promptS3Key, findingIds, priority, status, assignedTo, assignedToName, jiraKey, createdAt |
| `TeamMemberResponse` | id, userId, displayName, email, avatarUrl, role, joinedAt |
| `TeamMetricsResponse` | teamId, totalProjects, totalJobs, totalFindings, averageHealthScore, projectsBelowThreshold, openCriticalFindings |
| `TeamResponse` | id, name, description, ownerId, ownerName, teamsWebhookUrl, memberCount, createdAt, updatedAt |
| `TechDebtItemResponse` | id, projectId, category, title, description, filePath, effortEstimate, businessImpact, status, firstDetectedJobId, resolvedJobId, createdAt, updatedAt |
| `UserResponse` | id, email, displayName, avatarUrl, isActive, lastLoginAt, createdAt |
| `VulnerabilityResponse` | id, scanId, dependencyName, currentVersion, fixedVersion, cveId, severity, description, status, createdAt |

---

## 10. Service Layer

25 service classes in `com.codeops.service`. All annotated with `@Service` and `@RequiredArgsConstructor`.

| Service | Injected Dependencies | Public Methods | Auth Pattern |
|---------|----------------------|----------------|-------------|
| **AdminService** | UserRepository, TeamRepository, ProjectRepository, QaJobRepository, SystemSettingRepository | `getAllUsers(Pageable)`, `getUserById(UUID)`, `updateUserStatus(UUID, AdminUpdateUserRequest)`, `getSystemSetting(String)`, `updateSystemSetting(UpdateSystemSettingRequest)`, `getAllSettings()`, `getUsageStats()` | `SecurityUtils.isAdmin()` check |
| **AgentRunService** | AgentRunRepository, QaJobRepository, TeamMemberRepository | `createAgentRun(CreateAgentRunRequest)`, `createAgentRuns(UUID, List<AgentType>)`, `getAgentRuns(UUID)`, `getAgentRun(UUID)`, `updateAgentRun(UUID, UpdateAgentRunRequest)` | Team membership verification |
| **AuditLogService** | AuditLogRepository, UserRepository, TeamRepository, TeamMemberRepository | `log(UUID, UUID, String, String, UUID, String)` (@Async), `getTeamAuditLog(UUID, Pageable)`, `getUserAuditLog(UUID, Pageable)` | Team membership / self-only |
| **AuthService** | UserRepository, PasswordEncoder, JwtTokenProvider, TeamMemberRepository | `register(RegisterRequest)`, `login(LoginRequest)`, `refreshToken(RefreshTokenRequest)`, `changePassword(ChangePasswordRequest)` | Public (register/login), authenticated (changePassword) |
| **BugInvestigationService** | BugInvestigationRepository, QaJobRepository, TeamMemberRepository, S3StorageService | `createInvestigation(CreateBugInvestigationRequest)`, `getInvestigation(UUID)`, `getInvestigationByJob(UUID)`, `getInvestigationByJiraKey(String)`, `updateInvestigation(UUID, UpdateBugInvestigationRequest)`, `uploadRca(UUID, String)` | Team membership |
| **ComplianceService** | ComplianceItemRepository, SpecificationRepository, QaJobRepository, TeamMemberRepository | `createSpecification(CreateSpecificationRequest)`, `getSpecificationsForJob(UUID, Pageable)`, `createComplianceItem(CreateComplianceItemRequest)`, `createComplianceItems(List<>)`, `getComplianceItemsForJob(UUID, Pageable)`, `getComplianceItemsByStatus(UUID, ComplianceStatus, Pageable)`, `getComplianceSummary(UUID)` | Team membership |
| **DependencyService** | DependencyScanRepository, DependencyVulnerabilityRepository, ProjectRepository, TeamMemberRepository, QaJobRepository | `createScan(CreateDependencyScanRequest)`, `getScan(UUID)`, `getScansForProject(UUID, Pageable)`, `getLatestScan(UUID)`, `addVulnerability(CreateVulnerabilityRequest)`, `addVulnerabilities(List<>)`, `getVulnerabilities(UUID, Pageable)`, `getVulnerabilitiesBySeverity(UUID, Severity, Pageable)`, `getOpenVulnerabilities(UUID, Pageable)`, `updateVulnerabilityStatus(UUID, VulnerabilityStatus)` | Team membership |
| **DirectiveService** | DirectiveRepository, ProjectDirectiveRepository, ProjectRepository, TeamMemberRepository, UserRepository | `createDirective(CreateDirectiveRequest)`, `getDirective(UUID)`, `getDirectivesForTeam(UUID)`, `getDirectivesForProject(UUID)`, `getDirectivesByCategory(UUID, DirectiveScope)`, `updateDirective(UUID, UpdateDirectiveRequest)`, `deleteDirective(UUID)`, `assignToProject(AssignDirectiveRequest)`, `removeFromProject(UUID, UUID)`, `getProjectDirectives(UUID)`, `getEnabledDirectivesForProject(UUID)`, `toggleProjectDirective(UUID, UUID, boolean)` | Team membership |
| **EncryptionService** | @Value encryption.key | `encrypt(String)`, `decrypt(String)` | No auth (utility) |
| **FindingService** | FindingRepository, QaJobRepository, UserRepository, TeamMemberRepository | `createFinding(CreateFindingRequest)`, `createFindings(List<>)`, `getFinding(UUID)`, `getFindingsForJob(UUID, Pageable)`, `getFindingsByJobAndSeverity(UUID, Severity, Pageable)`, `getFindingsByJobAndAgent(UUID, AgentType, Pageable)`, `getFindingsByJobAndStatus(UUID, FindingStatus, Pageable)`, `updateFindingStatus(UUID, UpdateFindingStatusRequest)`, `bulkUpdateFindingStatus(BulkUpdateFindingsRequest)`, `countFindingsBySeverity(UUID)` | Team membership |
| **GitHubConnectionService** | GitHubConnectionRepository, TeamMemberRepository, UserRepository, EncryptionService | `createConnection(UUID, CreateGitHubConnectionRequest)`, `getConnections(UUID)`, `getConnection(UUID)`, `deleteConnection(UUID)`, `getDecryptedCredentials(UUID)` | Team membership |
| **HealthMonitorService** | HealthScheduleRepository, HealthSnapshotRepository, ProjectRepository, QaJobRepository, TeamMemberRepository, UserRepository | `createSchedule(CreateHealthScheduleRequest)`, `getSchedulesForProject(UUID)`, `getActiveSchedules()`, `updateSchedule(UUID, boolean)`, `deleteSchedule(UUID)`, `markScheduleRun(UUID)`, `createSnapshot(CreateHealthSnapshotRequest)`, `getSnapshots(UUID, Pageable)`, `getLatestSnapshot(UUID)`, `getHealthTrend(UUID, int)` | Team membership |
| **JiraConnectionService** | JiraConnectionRepository, TeamMemberRepository, UserRepository, EncryptionService | `createConnection(UUID, CreateJiraConnectionRequest)`, `getConnections(UUID)`, `getConnection(UUID)`, `deleteConnection(UUID)`, `getDecryptedApiToken(UUID)`, `getConnectionDetails(UUID)` | Team membership |
| **MetricsService** | ProjectRepository, QaJobRepository, FindingRepository, TechDebtItemRepository, DependencyVulnerabilityRepository, DependencyScanRepository, HealthSnapshotRepository, TeamMemberRepository | `getProjectMetrics(UUID)`, `getTeamMetrics(UUID)`, `getHealthTrend(UUID, int)` | Team membership |
| **NotificationService** | NotificationPreferenceRepository, UserRepository | `getPreferences(UUID)`, `updatePreference(UUID, UpdateNotificationPreferenceRequest)`, `updatePreferences(UUID, List<>)`, `shouldNotify(UUID, String, String)` | Self-only |
| **PersonaService** | PersonaRepository, TeamMemberRepository, UserRepository | `createPersona(CreatePersonaRequest)`, `getPersona(UUID)`, `getPersonasForTeam(UUID, Pageable)`, `getPersonasByAgentType(UUID, AgentType)`, `getDefaultPersona(UUID, AgentType)`, `getPersonasByUser(UUID)`, `getSystemPersonas()`, `updatePersona(UUID, UpdatePersonaRequest)`, `deletePersona(UUID)`, `setAsDefault(UUID)`, `removeDefault(UUID)` | Team membership |
| **ProjectService** | ProjectRepository, TeamMemberRepository, GitHubConnectionRepository, JiraConnectionRepository, UserRepository | `createProject(UUID, CreateProjectRequest)`, `getProject(UUID)`, `getProjectsForTeam(UUID)`, `getAllProjectsForTeam(UUID, boolean, Pageable)`, `updateProject(UUID, UpdateProjectRequest)`, `archiveProject(UUID)`, `unarchiveProject(UUID)`, `deleteProject(UUID)`, `updateHealthScore(UUID, int)` | Team membership |
| **QaJobService** | QaJobRepository, ProjectRepository, UserRepository, TeamMemberRepository | `createJob(CreateJobRequest)`, `getJob(UUID)`, `getJobsForProject(UUID, Pageable)`, `getJobsByUser(UUID, Pageable)`, `updateJob(UUID, UpdateJobRequest)`, `deleteJob(UUID)` | Team membership |
| **RemediationTaskService** | RemediationTaskRepository, QaJobRepository, FindingRepository, UserRepository, TeamMemberRepository, S3StorageService | `createTask(CreateTaskRequest)`, `createTasks(List<>)`, `getTasksForJob(UUID, Pageable)`, `getTask(UUID)`, `getTasksAssignedToUser(UUID, Pageable)`, `updateTask(UUID, UpdateTaskRequest)`, `uploadTaskPrompt(UUID, int, String)` | Team membership |
| **ReportStorageService** | S3StorageService | `uploadReport(UUID, AgentType, String)`, `uploadSummaryReport(UUID, String)`, `downloadReport(String)`, `deleteReportsForJob(UUID)`, `uploadSpecification(UUID, String, byte[], String)`, `downloadSpecification(String)` | No auth (utility) |
| **S3StorageService** | S3Client (optional), @Value properties | `upload(String, byte[], String)`, `download(String)`, `delete(String)`, `generatePresignedUrl(String, Duration)` | No auth (utility) |
| **TeamService** | TeamRepository, TeamMemberRepository, UserRepository, InvitationRepository, ProjectRepository, PasswordEncoder | `createTeam(CreateTeamRequest)`, `getTeam(UUID)`, `getTeamsForUser()`, `updateTeam(UUID, UpdateTeamRequest)`, `deleteTeam(UUID)`, `getTeamMembers(UUID)`, `updateMemberRole(UUID, UUID, UpdateMemberRoleRequest)`, `removeMember(UUID, UUID)`, `inviteMember(UUID, InviteMemberRequest)`, `acceptInvitation(String)`, `getTeamInvitations(UUID)`, `cancelInvitation(UUID)` | Team membership / owner checks |
| **TechDebtService** | TechDebtItemRepository, ProjectRepository, QaJobRepository, TeamMemberRepository | `createTechDebtItem(CreateTechDebtItemRequest)`, `createTechDebtItems(List<>)`, `getTechDebtItem(UUID)`, `getTechDebtForProject(UUID, Pageable)`, `getTechDebtByStatus(UUID, DebtStatus, Pageable)`, `getTechDebtByCategory(UUID, DebtCategory, Pageable)`, `updateTechDebtStatus(UUID, UpdateTechDebtStatusRequest)`, `deleteTechDebtItem(UUID)`, `getDebtSummary(UUID)` | Team membership |
| **TokenBlacklistService** | (none) | `blacklist(String, Instant)`, `isBlacklisted(String)` | No auth (utility) |
| **UserService** | UserRepository | `getUserById(UUID)`, `getUserByEmail(String)`, `getCurrentUser()`, `updateUser(UUID, UpdateUserRequest)`, `searchUsers(String)`, `deactivateUser(UUID)`, `activateUser(UUID)` | Self-only / admin |

---

## 11. Controller Layer

17 controllers under `com.codeops.controller`. All annotated with `@RestController`, `@RequiredArgsConstructor`, and `@Tag(name = "...")`. Base path: `/api/v1/`.

Due to the large number of endpoints (141 total), the complete endpoint listing is provided in Section 12 (API Surface).

---

## 12. API Surface

| # | Method | Path | Auth | Request Body | Response Body |
|---|--------|------|------|--------------|---------------|
| 1 | POST | `/api/v1/auth/register` | Public | `RegisterRequest` | `AuthResponse` (201) |
| 2 | POST | `/api/v1/auth/login` | Public | `LoginRequest` | `AuthResponse` |
| 3 | POST | `/api/v1/auth/refresh` | Public | `RefreshTokenRequest` | `AuthResponse` |
| 4 | POST | `/api/v1/auth/logout` | Authenticated | -- | 204 |
| 5 | POST | `/api/v1/auth/change-password` | Authenticated | `ChangePasswordRequest` | 200 |
| 6 | GET | `/api/v1/health` | Public | -- | `Map<String,Object>` |
| 7 | GET | `/api/v1/users/me` | Authenticated | -- | `UserResponse` |
| 8 | GET | `/api/v1/users/{userId}` | Authenticated | -- | `UserResponse` |
| 9 | PUT | `/api/v1/users/me` | Authenticated | `UpdateUserRequest` | `UserResponse` |
| 10 | GET | `/api/v1/users/search` | Authenticated | `?q=` | `List<UserResponse>` |
| 11 | PUT | `/api/v1/users/{userId}/deactivate` | ADMIN/OWNER | -- | 204 |
| 12 | PUT | `/api/v1/users/{userId}/activate` | ADMIN/OWNER | -- | 204 |
| 13 | GET | `/api/v1/admin/users` | ADMIN/OWNER | -- | `Page<UserResponse>` |
| 14 | GET | `/api/v1/admin/users/{userId}` | ADMIN/OWNER | -- | `UserResponse` |
| 15 | PUT | `/api/v1/admin/users/{userId}` | ADMIN/OWNER | `AdminUpdateUserRequest` | `UserResponse` |
| 16 | GET | `/api/v1/admin/settings` | ADMIN/OWNER | -- | `List<SystemSettingResponse>` |
| 17 | GET | `/api/v1/admin/settings/{key}` | ADMIN/OWNER | -- | `SystemSettingResponse` |
| 18 | PUT | `/api/v1/admin/settings` | ADMIN/OWNER | `UpdateSystemSettingRequest` | `SystemSettingResponse` |
| 19 | GET | `/api/v1/admin/usage` | ADMIN/OWNER | -- | `Map<String,Object>` |
| 20 | GET | `/api/v1/admin/audit-log/team/{teamId}` | ADMIN/OWNER | -- | `Page<AuditLogResponse>` |
| 21 | GET | `/api/v1/admin/audit-log/user/{userId}` | ADMIN/OWNER | -- | `Page<AuditLogResponse>` |
| 22 | POST | `/api/v1/teams` | Authenticated | `CreateTeamRequest` | `TeamResponse` (201) |
| 23 | GET | `/api/v1/teams` | Authenticated | -- | `List<TeamResponse>` |
| 24 | GET | `/api/v1/teams/{teamId}` | Authenticated | -- | `TeamResponse` |
| 25 | PUT | `/api/v1/teams/{teamId}` | Authenticated | `UpdateTeamRequest` | `TeamResponse` |
| 26 | DELETE | `/api/v1/teams/{teamId}` | Authenticated | -- | 204 |
| 27 | GET | `/api/v1/teams/{teamId}/members` | Authenticated | -- | `List<TeamMemberResponse>` |
| 28 | PUT | `/api/v1/teams/{teamId}/members/{userId}/role` | Authenticated | `UpdateMemberRoleRequest` | `TeamMemberResponse` |
| 29 | DELETE | `/api/v1/teams/{teamId}/members/{userId}` | Authenticated | -- | 204 |
| 30 | POST | `/api/v1/teams/{teamId}/invitations` | Authenticated | `InviteMemberRequest` | `InvitationResponse` (201) |
| 31 | GET | `/api/v1/teams/{teamId}/invitations` | Authenticated | -- | `List<InvitationResponse>` |
| 32 | DELETE | `/api/v1/teams/{teamId}/invitations/{invitationId}` | Authenticated | -- | 204 |
| 33 | POST | `/api/v1/teams/invitations/{token}/accept` | Authenticated | -- | `TeamResponse` |
| 34 | POST | `/api/v1/projects/{teamId}` | Authenticated | `CreateProjectRequest` | `ProjectResponse` (201) |
| 35 | GET | `/api/v1/projects/team/{teamId}` | Authenticated | -- | `PageResponse<ProjectResponse>` |
| 36 | GET | `/api/v1/projects/{projectId}` | Authenticated | -- | `ProjectResponse` |
| 37 | PUT | `/api/v1/projects/{projectId}` | Authenticated | `UpdateProjectRequest` | `ProjectResponse` |
| 38 | PUT | `/api/v1/projects/{projectId}/archive` | Authenticated | -- | 204 |
| 39 | PUT | `/api/v1/projects/{projectId}/unarchive` | Authenticated | -- | 204 |
| 40 | DELETE | `/api/v1/projects/{projectId}` | Authenticated | -- | 204 |
| 41 | POST | `/api/v1/jobs` | Authenticated | `CreateJobRequest` | `JobResponse` (201) |
| 42 | GET | `/api/v1/jobs/{jobId}` | Authenticated | -- | `JobResponse` |
| 43 | GET | `/api/v1/jobs/project/{projectId}` | Authenticated | -- | `PageResponse<JobSummaryResponse>` |
| 44 | GET | `/api/v1/jobs/mine` | Authenticated | -- | `PageResponse<JobSummaryResponse>` |
| 45 | PUT | `/api/v1/jobs/{jobId}` | Authenticated | `UpdateJobRequest` | `JobResponse` |
| 46 | DELETE | `/api/v1/jobs/{jobId}` | Authenticated | -- | 204 |
| 47 | POST | `/api/v1/jobs/{jobId}/agents` | Authenticated | `CreateAgentRunRequest` | `AgentRunResponse` (201) |
| 48 | POST | `/api/v1/jobs/{jobId}/agents/batch` | Authenticated | `List<AgentType>` | `List<AgentRunResponse>` |
| 49 | GET | `/api/v1/jobs/{jobId}/agents` | Authenticated | -- | `List<AgentRunResponse>` |
| 50 | PUT | `/api/v1/jobs/agents/{agentRunId}` | Authenticated | `UpdateAgentRunRequest` | `AgentRunResponse` |
| 51 | GET | `/api/v1/jobs/{jobId}/investigation` | Authenticated | -- | `BugInvestigationResponse` |
| 52 | POST | `/api/v1/jobs/{jobId}/investigation` | Authenticated | `CreateBugInvestigationRequest` | `BugInvestigationResponse` (201) |
| 53 | PUT | `/api/v1/jobs/investigations/{investigationId}` | Authenticated | `UpdateBugInvestigationRequest` | `BugInvestigationResponse` |
| 54 | POST | `/api/v1/findings` | Authenticated | `CreateFindingRequest` | `FindingResponse` (201) |
| 55 | POST | `/api/v1/findings/batch` | Authenticated | `List<CreateFindingRequest>` | `List<FindingResponse>` (201) |
| 56 | GET | `/api/v1/findings/{findingId}` | Authenticated | -- | `FindingResponse` |
| 57 | GET | `/api/v1/findings/job/{jobId}` | Authenticated | -- | `PageResponse<FindingResponse>` |
| 58 | GET | `/api/v1/findings/job/{jobId}/severity/{severity}` | Authenticated | -- | `PageResponse<FindingResponse>` |
| 59 | GET | `/api/v1/findings/job/{jobId}/agent/{agentType}` | Authenticated | -- | `PageResponse<FindingResponse>` |
| 60 | GET | `/api/v1/findings/job/{jobId}/status/{status}` | Authenticated | -- | `PageResponse<FindingResponse>` |
| 61 | GET | `/api/v1/findings/job/{jobId}/counts` | Authenticated | -- | `Map<Severity,Long>` |
| 62 | PUT | `/api/v1/findings/{findingId}/status` | Authenticated | `UpdateFindingStatusRequest` | `FindingResponse` |
| 63 | PUT | `/api/v1/findings/bulk-status` | Authenticated | `BulkUpdateFindingsRequest` | `List<FindingResponse>` |
| 64 | POST | `/api/v1/tasks` | Authenticated | `CreateTaskRequest` | `TaskResponse` (201) |
| 65 | POST | `/api/v1/tasks/batch` | Authenticated | `List<CreateTaskRequest>` | `List<TaskResponse>` (201) |
| 66 | GET | `/api/v1/tasks/job/{jobId}` | Authenticated | -- | `PageResponse<TaskResponse>` |
| 67 | GET | `/api/v1/tasks/{taskId}` | Authenticated | -- | `TaskResponse` |
| 68 | GET | `/api/v1/tasks/assigned-to-me` | Authenticated | -- | `PageResponse<TaskResponse>` |
| 69 | PUT | `/api/v1/tasks/{taskId}` | Authenticated | `UpdateTaskRequest` | `TaskResponse` |
| 70 | POST | `/api/v1/compliance/specs` | Authenticated | `CreateSpecificationRequest` | `SpecificationResponse` (201) |
| 71 | GET | `/api/v1/compliance/specs/job/{jobId}` | Authenticated | -- | `PageResponse<SpecificationResponse>` |
| 72 | POST | `/api/v1/compliance/items` | Authenticated | `CreateComplianceItemRequest` | `ComplianceItemResponse` (201) |
| 73 | POST | `/api/v1/compliance/items/batch` | Authenticated | `List<CreateComplianceItemRequest>` | `List<ComplianceItemResponse>` (201) |
| 74 | GET | `/api/v1/compliance/items/job/{jobId}` | Authenticated | -- | `PageResponse<ComplianceItemResponse>` |
| 75 | GET | `/api/v1/compliance/items/job/{jobId}/status/{status}` | Authenticated | -- | `PageResponse<ComplianceItemResponse>` |
| 76 | GET | `/api/v1/compliance/summary/job/{jobId}` | Authenticated | -- | `Map<String,Object>` |
| 77 | POST | `/api/v1/directives` | Authenticated | `CreateDirectiveRequest` | `DirectiveResponse` (201) |
| 78 | GET | `/api/v1/directives/{directiveId}` | Authenticated | -- | `DirectiveResponse` |
| 79 | GET | `/api/v1/directives/team/{teamId}` | Authenticated | -- | `List<DirectiveResponse>` |
| 80 | GET | `/api/v1/directives/project/{projectId}` | Authenticated | -- | `List<DirectiveResponse>` |
| 81 | PUT | `/api/v1/directives/{directiveId}` | Authenticated | `UpdateDirectiveRequest` | `DirectiveResponse` |
| 82 | DELETE | `/api/v1/directives/{directiveId}` | Authenticated | -- | 204 |
| 83 | POST | `/api/v1/directives/assign` | Authenticated | `AssignDirectiveRequest` | `ProjectDirectiveResponse` (201) |
| 84 | DELETE | `/api/v1/directives/project/{projectId}/directive/{directiveId}` | Authenticated | -- | 204 |
| 85 | GET | `/api/v1/directives/project/{projectId}/assignments` | Authenticated | -- | `List<ProjectDirectiveResponse>` |
| 86 | GET | `/api/v1/directives/project/{projectId}/enabled` | Authenticated | -- | `List<DirectiveResponse>` |
| 87 | PUT | `/api/v1/directives/project/{projectId}/directive/{directiveId}/toggle` | Authenticated | -- | `ProjectDirectiveResponse` |
| 88 | POST | `/api/v1/personas` | Authenticated | `CreatePersonaRequest` | `PersonaResponse` (201) |
| 89 | GET | `/api/v1/personas/{personaId}` | Authenticated | -- | `PersonaResponse` |
| 90 | GET | `/api/v1/personas/team/{teamId}` | Authenticated | -- | `PageResponse<PersonaResponse>` |
| 91 | GET | `/api/v1/personas/team/{teamId}/agent/{agentType}` | Authenticated | -- | `List<PersonaResponse>` |
| 92 | GET | `/api/v1/personas/team/{teamId}/default/{agentType}` | Authenticated | -- | `PersonaResponse` |
| 93 | GET | `/api/v1/personas/mine` | Authenticated | -- | `List<PersonaResponse>` |
| 94 | GET | `/api/v1/personas/system` | Authenticated | -- | `List<PersonaResponse>` |
| 95 | PUT | `/api/v1/personas/{personaId}` | Authenticated | `UpdatePersonaRequest` | `PersonaResponse` |
| 96 | DELETE | `/api/v1/personas/{personaId}` | Authenticated | -- | 204 |
| 97 | PUT | `/api/v1/personas/{personaId}/set-default` | Authenticated | -- | `PersonaResponse` |
| 98 | PUT | `/api/v1/personas/{personaId}/remove-default` | Authenticated | -- | `PersonaResponse` |
| 99 | POST | `/api/v1/tech-debt` | Authenticated | `CreateTechDebtItemRequest` | `TechDebtItemResponse` (201) |
| 100 | POST | `/api/v1/tech-debt/batch` | Authenticated | `List<CreateTechDebtItemRequest>` | `List<TechDebtItemResponse>` (201) |
| 101 | GET | `/api/v1/tech-debt/{itemId}` | Authenticated | -- | `TechDebtItemResponse` |
| 102 | GET | `/api/v1/tech-debt/project/{projectId}` | Authenticated | -- | `PageResponse<TechDebtItemResponse>` |
| 103 | GET | `/api/v1/tech-debt/project/{projectId}/status/{status}` | Authenticated | -- | `PageResponse<TechDebtItemResponse>` |
| 104 | GET | `/api/v1/tech-debt/project/{projectId}/category/{category}` | Authenticated | -- | `PageResponse<TechDebtItemResponse>` |
| 105 | PUT | `/api/v1/tech-debt/{itemId}/status` | Authenticated | `UpdateTechDebtStatusRequest` | `TechDebtItemResponse` |
| 106 | DELETE | `/api/v1/tech-debt/{itemId}` | Authenticated | -- | 204 |
| 107 | GET | `/api/v1/tech-debt/project/{projectId}/summary` | Authenticated | -- | `Map<String,Object>` |
| 108 | POST | `/api/v1/dependencies/scans` | Authenticated | `CreateDependencyScanRequest` | `DependencyScanResponse` (201) |
| 109 | GET | `/api/v1/dependencies/scans/{scanId}` | Authenticated | -- | `DependencyScanResponse` |
| 110 | GET | `/api/v1/dependencies/scans/project/{projectId}` | Authenticated | -- | `PageResponse<DependencyScanResponse>` |
| 111 | GET | `/api/v1/dependencies/scans/project/{projectId}/latest` | Authenticated | -- | `DependencyScanResponse` |
| 112 | POST | `/api/v1/dependencies/vulnerabilities` | Authenticated | `CreateVulnerabilityRequest` | `VulnerabilityResponse` (201) |
| 113 | POST | `/api/v1/dependencies/vulnerabilities/batch` | Authenticated | `List<CreateVulnerabilityRequest>` | `List<VulnerabilityResponse>` (201) |
| 114 | GET | `/api/v1/dependencies/vulnerabilities/scan/{scanId}` | Authenticated | -- | `PageResponse<VulnerabilityResponse>` |
| 115 | GET | `/api/v1/dependencies/vulnerabilities/scan/{scanId}/severity/{severity}` | Authenticated | -- | `PageResponse<VulnerabilityResponse>` |
| 116 | GET | `/api/v1/dependencies/vulnerabilities/scan/{scanId}/open` | Authenticated | -- | `PageResponse<VulnerabilityResponse>` |
| 117 | PUT | `/api/v1/dependencies/vulnerabilities/{vulnerabilityId}/status` | Authenticated | -- | `VulnerabilityResponse` |
| 118 | POST | `/api/v1/health-monitor/schedules` | Authenticated | `CreateHealthScheduleRequest` | `HealthScheduleResponse` (201) |
| 119 | GET | `/api/v1/health-monitor/schedules/project/{projectId}` | Authenticated | -- | `List<HealthScheduleResponse>` |
| 120 | PUT | `/api/v1/health-monitor/schedules/{scheduleId}` | Authenticated | -- | `HealthScheduleResponse` |
| 121 | DELETE | `/api/v1/health-monitor/schedules/{scheduleId}` | Authenticated | -- | 204 |
| 122 | POST | `/api/v1/health-monitor/snapshots` | Authenticated | `CreateHealthSnapshotRequest` | `HealthSnapshotResponse` (201) |
| 123 | GET | `/api/v1/health-monitor/snapshots/project/{projectId}` | Authenticated | -- | `PageResponse<HealthSnapshotResponse>` |
| 124 | GET | `/api/v1/health-monitor/snapshots/project/{projectId}/latest` | Authenticated | -- | `HealthSnapshotResponse` |
| 125 | GET | `/api/v1/health-monitor/snapshots/project/{projectId}/trend` | Authenticated | -- | `List<HealthSnapshotResponse>` |
| 126 | GET | `/api/v1/metrics/project/{projectId}` | Authenticated | -- | `ProjectMetricsResponse` |
| 127 | GET | `/api/v1/metrics/team/{teamId}` | Authenticated | -- | `TeamMetricsResponse` |
| 128 | GET | `/api/v1/metrics/project/{projectId}/trend` | Authenticated | -- | `List<HealthSnapshotResponse>` |
| 129 | POST | `/api/v1/integrations/github/{teamId}` | Authenticated | `CreateGitHubConnectionRequest` | `GitHubConnectionResponse` (201) |
| 130 | GET | `/api/v1/integrations/github/{teamId}` | Authenticated | -- | `List<GitHubConnectionResponse>` |
| 131 | GET | `/api/v1/integrations/github/{teamId}/{connectionId}` | Authenticated | -- | `GitHubConnectionResponse` |
| 132 | DELETE | `/api/v1/integrations/github/{teamId}/{connectionId}` | Authenticated | -- | 204 |
| 133 | POST | `/api/v1/integrations/jira/{teamId}` | Authenticated | `CreateJiraConnectionRequest` | `JiraConnectionResponse` (201) |
| 134 | GET | `/api/v1/integrations/jira/{teamId}` | Authenticated | -- | `List<JiraConnectionResponse>` |
| 135 | GET | `/api/v1/integrations/jira/{teamId}/{connectionId}` | Authenticated | -- | `JiraConnectionResponse` |
| 136 | DELETE | `/api/v1/integrations/jira/{teamId}/{connectionId}` | Authenticated | -- | 204 |
| 137 | POST | `/api/v1/reports/job/{jobId}/agent/{agentType}` | Authenticated | `String` (markdown) | `Map<String,String>` |
| 138 | POST | `/api/v1/reports/job/{jobId}/summary` | Authenticated | `String` (markdown) | `Map<String,String>` |
| 139 | GET | `/api/v1/reports/download` | Authenticated | `?key=` | `String` (presigned URL) |
| 140 | POST | `/api/v1/reports/job/{jobId}/spec` | Authenticated | `MultipartFile` | `Map<String,String>` |
| 141 | GET | `/api/v1/notifications/preferences` | Authenticated | -- | `NotificationPreferenceResponse` |

**Total: 141 endpoints across 18 controllers (17 domain + HealthController).**

**Summary by auth level:**
- Public (no auth): 4 endpoints (register, login, refresh, health)
- Authenticated: 129 endpoints
- ADMIN/OWNER only: 8 endpoints (admin management + user activation/deactivation)

---

## 13. Security Architecture

### Authentication Flow

1. Client sends `POST /api/v1/auth/login` with `{email, password}`.
2. `AuthService.login()` verifies credentials against BCrypt hash.
3. On success, generates JWT access token (24h) and refresh token (30d).
4. Client includes `Authorization: Bearer <token>` on subsequent requests.
5. `JwtAuthFilter` extracts token, validates signature/expiry/blacklist, sets `SecurityContext`.
6. Controllers access `SecurityUtils.getCurrentUserId()` for the authenticated user's UUID.

### JWT Details

- **Algorithm:** HS256 (HMAC-SHA256)
- **Access token lifetime:** 24 hours
- **Refresh token lifetime:** 30 days
- **Claims:** `sub` (user UUID), `email`, `roles` (list), `jti` (UUID for revocation), `iat`, `exp`
- **Refresh token:** includes `type: "refresh"` claim
- **Secret validation:** `@PostConstruct` fails fast if secret < 32 characters

### Authorization Model

- **Class-level:** `AdminController` uses `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")`.
- **Method-level:** Most services use `SecurityUtils.getCurrentUserId()` + `TeamMemberRepository.existsByTeamIdAndUserId()` to verify team membership.
- **Role-based:** Roles from `TeamRole` enum (OWNER, ADMIN, MEMBER, VIEWER) are embedded in JWT and mapped to `ROLE_` prefixed authorities.

### Security Filter Chain (execution order)

1. `RequestCorrelationFilter` (Ordered.HIGHEST_PRECEDENCE) -- MDC correlation ID
2. `RateLimitFilter` -- per-IP rate limiting on `/api/v1/auth/**`
3. `JwtAuthFilter` -- JWT extraction and SecurityContext population
4. Spring Security authorization checks

### Public Endpoints (no auth required)

- `/api/v1/auth/**`
- `/api/v1/health`
- `/swagger-ui/**`, `/v3/api-docs/**`, `/v3/api-docs.yaml`

### CORS Configuration

- Origins: from `codeops.cors.allowed-origins` (no wildcards)
- Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Headers: Authorization, Content-Type, X-Requested-With
- Credentials: enabled
- Max age: 3600s

### Security Headers

- CSP: `default-src 'self'; frame-ancestors 'none'`
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- HSTS: includeSubDomains, max-age 31536000 (1 year)

### Encryption

- **Algorithm:** AES-256-GCM
- **Key derivation:** PBKDF2WithHmacSHA256, 100,000 iterations, static salt
- **IV:** 12 bytes, randomly generated per encryption (SecureRandom)
- **Authentication tag:** 128-bit GCM tag
- **Storage format:** Base64(IV || ciphertext || authTag)
- **Usage:** GitHub PAT encryption, Jira API token encryption
- **Key source:** `codeops.encryption.key` property (env var in prod)

### Password Policy

Validated in `AuthService.validatePasswordStrength()`:
- Minimum length: `AppConstants.MIN_PASSWORD_LENGTH` (1 -- note: this constant is set to 1, but the validation also requires all of the following)
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character
- Hashing: BCrypt with strength factor 12

### Rate Limiting

- **Scope:** `/api/v1/auth/**` endpoints only
- **Strategy:** In-memory sliding window per client IP
- **Limit:** 10 requests per 60 seconds
- **IP resolution:** X-Forwarded-For header (first entry) or remoteAddr
- **Response:** 429 Too Many Requests with JSON body

### Token Revocation

- `TokenBlacklistService` maintains in-memory `ConcurrentHashMap.KeySetView` of blacklisted JTI values
- `JwtTokenProvider.validateToken()` checks blacklist on every request
- Logout endpoint blacklists the current token's JTI
- Blacklist is not persisted (lost on restart)

---

## 14. Notification Layer

### EmailService

- **Transport:** AWS SES (`software.amazon.awssdk:ses`)
- **Conditional:** Only active when `codeops.aws.ses.enabled=true` and `SesClient` bean exists
- **Fallback:** Logs email content at INFO level when SES is disabled
- **XSS protection:** All user-provided values are HTML-escaped via `HtmlUtils.htmlEscape()`
- **Error handling:** `SesException` caught and logged at ERROR, never propagated
- **Methods:**
  - `sendEmail(String toEmail, String subject, String htmlBody)` -- generic HTML email
  - `sendInvitationEmail(String toEmail, String teamName, String inviterName, String acceptUrl)` -- team invitation
  - `sendCriticalFindingAlert(String toEmail, String projectName, int criticalCount, String jobUrl)` -- critical alert
  - `sendHealthDigest(String toEmail, String teamName, List<Map<String, Object>> projectSummaries)` -- weekly digest

### TeamsWebhookService

- **Transport:** HTTP POST via `RestTemplate` to Microsoft Teams incoming webhook URLs
- **Payload format:** MessageCard JSON
- **SSRF protection:** Validates webhook URLs -- requires HTTPS, rejects loopback/site-local/link-local addresses
- **Error handling:** All exceptions caught and logged at ERROR, never propagated
- **Methods:**
  - `postMessage(String webhookUrl, String title, String subtitle, Map<String, String> facts, String actionUrl)` -- generic card
  - `postJobCompleted(String webhookUrl, String projectName, String branch, int healthScore, int criticalCount, int highCount, String runBy)` -- audit completion
  - `postCriticalAlert(String webhookUrl, String projectName, int criticalCount)` -- critical findings alert

### NotificationDispatcher

- **All methods annotated with `@Async`** -- runs on the async thread pool (codeops-async-*)
- **Error handling:** All exceptions caught and logged at ERROR, never propagated
- **Methods:**
  - `dispatchJobCompleted(...)` -- sends Teams webhook
  - `dispatchCriticalFinding(...)` -- sends Teams webhook + individual emails based on notification preferences
  - `dispatchTaskAssigned(...)` -- sends email to assigned user
  - `dispatchInvitation(...)` -- sends invitation email (always, no preference check)

---

## 15. Error Handling

### Exception Hierarchy

```
RuntimeException
  CodeOpsException
    NotFoundException      -> 404
    ValidationException    -> 400
    AuthorizationException -> 403
```

### GlobalExceptionHandler Mapping

| Exception | HTTP Status | Response Body | Log Level |
|-----------|------------|---------------|-----------|
| `EntityNotFoundException` (JPA) | 404 | `{status: 404, message: "Resource not found"}` | WARN |
| `IllegalArgumentException` | 400 | `{status: 400, message: "Invalid request"}` | WARN |
| `AccessDeniedException` (Spring Security) | 403 | `{status: 403, message: "Access denied"}` | WARN |
| `MethodArgumentNotValidException` | 400 | `{status: 400, message: "field1: error1, field2: error2"}` | WARN |
| `NotFoundException` (CodeOps) | 404 | `{status: 404, message: <exception message>}` | WARN |
| `ValidationException` (CodeOps) | 400 | `{status: 400, message: <exception message>}` | WARN |
| `AuthorizationException` (CodeOps) | 403 | `{status: 403, message: <exception message>}` | WARN |
| `CodeOpsException` (base) | 500 | `{status: 500, message: "An internal error occurred"}` | ERROR (with stack trace) |
| `Exception` (catch-all) | 500 | `{status: 500, message: "An internal error occurred"}` | ERROR (with stack trace) |

All responses use the `ErrorResponse` record: `{status: int, message: String}`. Internal error details are never exposed to clients in 500 responses.

---

## 16. Test Coverage

- **59 unit test files** with **791 @Test methods**
- **13 integration test files** (plus `BaseIntegrationTest` + `TestRateLimitConfig`) with **100 @Test methods**
- **Total: 891 test methods**

### Test Stack

- JUnit 5 (`spring-boot-starter-test`)
- Mockito 5.21.0 (with ByteBuddy 1.18.4 for Java 25)
- AssertJ
- Spring Security Test
- Testcontainers PostgreSQL 1.19.8 (integration tests)
- H2 in-memory database (unit tests)

### Test Configuration Files

- `application-test.yml` -- H2 in-memory database for unit tests
- `application-integration.yml` -- PostgreSQL via Testcontainers for integration tests

### Integration Tests

Integration tests extend `BaseIntegrationTest` and use `@Testcontainers` with a shared PostgreSQL container. The `TestRateLimitConfig` disables/relaxes rate limiting for integration test execution.

---

## 17. Infrastructure

### Dockerfile

- **Base image:** `eclipse-temurin:21-jre-alpine` (JRE only, minimal attack surface)
- **Non-root user:** `appuser:appgroup` created with `adduser -S`
- **Port:** 8090
- **Entry point:** `java -jar app.jar`
- **Build strategy:** Single-stage (requires pre-built JAR from `mvnw package`)

### docker-compose.yml

- **PostgreSQL 16 Alpine**: port 5432 bound to 127.0.0.1 only
- **Named volume:** `codeops-postgres-data` for data persistence
- **Named network:** `codeops-network` (bridge driver)
- **Healthcheck:** `pg_isready -U codeops -d codeops` every 10s
- **Labels:** `com.codeops.project`, `com.codeops.component`

### CI/CD

No CI/CD configuration detected (no `.github/workflows/`, no `Jenkinsfile`, no `.gitlab-ci.yml`).

---

## 18. Cross-Cutting Patterns

### Naming Conventions

- **Entities:** PascalCase, singular nouns (e.g., `Finding`, `QaJob`)
- **Tables:** snake_case, plural or descriptive (e.g., `findings`, `qa_jobs`, `audit_log`)
- **Repositories:** `{Entity}Repository`
- **Services:** `{Domain}Service`
- **Controllers:** `{Domain}Controller`
- **Request DTOs:** `{Verb}{Entity}Request` (e.g., `CreateFindingRequest`, `UpdateJobRequest`)
- **Response DTOs:** `{Entity}Response` (e.g., `FindingResponse`, `JobResponse`)
- **Enums:** PascalCase class, UPPER_SNAKE values

### Package Structure

Single-module layered architecture: `config` -> `controller` -> `service` -> `repository` -> `entity`. No domain-driven sub-packages within layers.

### BaseEntity Pattern

All entities except `AuditLog`, `SystemSetting`, and `ProjectDirective` extend `BaseEntity`, inheriting UUID PK, `createdAt`, and `updatedAt` with `@PrePersist`/`@PreUpdate` lifecycle callbacks.

### Audit Logging Pattern

Controllers call `auditLogService.log(userId, teamId, action, entityType, entityId, details)` after mutations. The call is `@Async` and runs in a separate transaction to avoid blocking the response.

### Error Handling Pattern

Controllers delegate to `GlobalExceptionHandler` via `@RestControllerAdvice`. No try-catch blocks in controllers. All error responses use `ErrorResponse` record.

### Pagination Pattern

`PageResponse<T>` record wraps Spring's `Page<T>` into a flat DTO with `content`, `page`, `size`, `totalElements`, `totalPages`, `isLast`.

### Validation Pattern

Request DTOs use Jakarta Bean Validation annotations. Controllers annotate parameters with `@Valid`. `MethodArgumentNotValidException` is caught by `GlobalExceptionHandler`.

### Constants

`AppConstants` centralizes magic numbers: team limits, file sizes, auth token lifetimes, S3 prefixes, QA config, pagination defaults, notification scheduling.

### Javadoc Coverage

All classes and all public methods have Javadoc comments.

---

## 19. Known Issues

**1 TODO found:**

- **File:** `src/main/java/com/codeops/service/EncryptionService.java:56`
- **Content:** `// TODO: Changing key derivation invalidates existing encrypted data -- requires re-encryption migration`
- **Significance:** Legitimate architectural concern. Changing the encryption key or derivation parameters would require a data migration for all encrypted fields (GitHub credentials, Jira API tokens).

---

## 20. OpenAPI

The OpenAPI specification is auto-generated by `springdoc-openapi-starter-webmvc-ui` (version 2.5.0) at runtime. Available at:

- Swagger UI: `http://localhost:8090/swagger-ui.html`
- JSON spec: `http://localhost:8090/v3/api-docs`
- YAML spec: `http://localhost:8090/v3/api-docs.yaml`

The `openapi.yaml` file is generated alongside this audit when the application is running.

---

## 21. Quality Scorecard

<!-- QUALITY SCORECARD SUMMARY -->

### Security (10 checks, max 20)

| ID | Check | Evidence | Score |
|----|-------|----------|-------|
| SEC-01 | Auth on mutations | 131 @PreAuthorize / 141 endpoints | 2 |
| SEC-02 | No hardcoded secrets | 0 found in source (dev defaults use env var fallback) | 2 |
| SEC-03 | Input validation on DTOs | Request DTOs use Jakarta Validation annotations | 2 |
| SEC-04 | CORS not wildcards | No wildcards; explicit origins from config | 2 |
| SEC-05 | Encryption key not hardcoded | From env var (`${ENCRYPTION_KEY}` in prod) | 2 |
| SEC-06 | Security headers | CSP, X-Frame-Options DENY, X-Content-Type-Options, HSTS configured | 2 |
| SEC-07 | Rate limiting | RateLimitFilter on auth endpoints (10 req/min/IP) | 2 |
| SEC-08 | SSRF protection | TeamsWebhookService validates URLs (HTTPS only, rejects internal addresses) | 2 |
| SEC-09 | Token revocation | TokenBlacklistService + logout endpoint | 2 |
| SEC-10 | Password complexity | Validates uppercase, lowercase, digit, special char | 2 |

**Security Total: 20/20**

### Data Integrity (8 checks, max 16)

| ID | Check | Evidence | Score |
|----|-------|----------|-------|
| DAT-01 | Enums use STRING | All 31 enum fields use `@Enumerated(EnumType.STRING)` | 2 |
| DAT-02 | DB indexes on FKs | 23 `@Index` annotations across entities | 2 |
| DAT-03 | Nullable constraints | 92 `nullable=false` annotations | 2 |
| DAT-04 | Optimistic locking | `@Version` on QaJob, AgentRun, Finding, RemediationTask, TechDebtItem (not all entities) | 1 |
| DAT-05 | No unbounded queries | 47 `List<>` returns exist alongside `Page<>` versions | 1 |
| DAT-06 | No in-memory filtering | ~2 instances of in-memory filtering | 1 |
| DAT-07 | Proper relationships | No comma-separated IDs; proper JPA relationships throughout | 2 |
| DAT-08 | Audit timestamps | BaseEntity with createdAt/updatedAt via @PrePersist/@PreUpdate | 2 |

**Data Integrity Total: 13/16**

### API Quality (8 checks, max 16)

| ID | Check | Evidence | Score |
|----|-------|----------|-------|
| API-01 | Consistent error responses | GlobalExceptionHandler produces ErrorResponse for all exceptions | 2 |
| API-02 | Sanitized errors | 500 responses return "An internal error occurred" (no stack traces) | 2 |
| API-03 | Audit logging on mutations | 116 audit calls for mutation endpoints | 2 |
| API-04 | Pagination on list endpoints | Most list endpoints have paginated variants (PageResponse) | 2 |
| API-05 | Correct HTTP status codes | 201 for create, 204 for no-content, 400/401/403/404 for errors | 2 |
| API-06 | OpenAPI documented | Springdoc configured with @Tag annotations on all controllers | 2 |
| API-07 | Consistent DTO naming | Request/Response suffix pattern consistently applied | 2 |
| API-08 | File upload validation | MultipartFile handled in ReportController with type/size checks | 2 |

**API Quality Total: 16/16**

### Code Quality (10 checks, max 20)

| ID | Check | Evidence | Score |
|----|-------|----------|-------|
| CQ-01 | No getReferenceById | 0 instances found; all lookups use findById with orElseThrow | 2 |
| CQ-02 | Exception hierarchy | 4 custom exceptions (CodeOpsException base + 3 subtypes) | 2 |
| CQ-03 | No TODOs | 1 legitimate TODO (encryption migration) | 2 |
| CQ-04 | Constants centralized | AppConstants class with all magic numbers | 2 |
| CQ-05 | Async exception handling | AsyncConfig implements AsyncConfigurer with exception handler | 2 |
| CQ-06 | No inline RestTemplate | 0 `new RestTemplate()` in services; uses injected bean | 2 |
| CQ-07 | Logging in services | @Slf4j/Logger throughout all services | 2 |
| CQ-08 | No raw exception msgs to clients | 0 in controllers; GlobalExceptionHandler sanitizes all | 2 |
| CQ-09 | Doc comments on classes | All classes have Javadoc | 2 |
| CQ-10 | Doc comments on methods | All public methods have Javadoc | 2 |

**Code Quality Total: 20/20**

### Test Quality (10 checks, max 20)

| ID | Check | Evidence | Score |
|----|-------|----------|-------|
| TST-01 | Unit tests exist | 59 test files | 2 |
| TST-02 | Integration tests exist | 13 integration test files | 2 |
| TST-03 | Real DB in ITs | Testcontainers PostgreSQL | 2 |
| TST-04 | Source-to-test ratio | Good coverage across services and controllers | 2 |
| TST-05 | Code coverage >= 80% | JaCoCo configured but no report generated at audit time | 1 |
| TST-06 | Test config exists | 2 configs: application-test.yml, application-integration.yml | 2 |
| TST-07 | Security tests | SecurityIT, auth header tests | 2 |
| TST-08 | Auth flow e2e | AuthControllerIT with full login/register/refresh flow | 2 |
| TST-09 | DB state verification | jdbcTemplate assertions in integration tests | 2 |
| TST-10 | Total test count | 891 total test methods | 2 |

**Test Quality Total: 19/20**

### Infrastructure (6 checks, max 12)

| ID | Check | Evidence | Score |
|----|-------|----------|-------|
| INF-01 | Non-root Dockerfile | appuser:appgroup, USER directive | 2 |
| INF-02 | DB ports | Exposed on 127.0.0.1 in compose for dev (not 0.0.0.0) | 1 |
| INF-03 | Env vars for secrets | All secrets from env vars in prod profile | 2 |
| INF-04 | Health check endpoint | GET /api/v1/health returns status | 2 |
| INF-05 | Structured logging | logback-spring.xml with JSON (LogstashEncoder) in prod | 2 |
| INF-06 | CI/CD config | None detected | 0 |

**Infrastructure Total: 9/12**

---

### Overall Score

| Category | Score | Max |
|----------|-------|-----|
| Security | 20 | 20 |
| Data Integrity | 13 | 16 |
| API Quality | 16 | 16 |
| Code Quality | 20 | 20 |
| Test Quality | 19 | 20 |
| Infrastructure | 9 | 12 |
| **TOTAL** | **97** | **104** |

**Overall: 97/104 = 93% = Grade A**

---

## 22. Database

Database not available for live audit. Schema documented from JPA entities only (Section 6). Live schema audit should be performed when the database is accessible.

**Database configuration:**
- PostgreSQL 16 (docker-compose)
- Hibernate `ddl-auto: update` in dev, `validate` in prod
- No migration tool (Flyway/Liquibase) configured
- Credentials: `codeops`/`codeops`/`codeops` (dev)

---

## 23. Kafka

No message broker (Kafka, RabbitMQ, SQS/SNS) detected in this project.

---

## 24. Redis/Cache

No Redis or caching layer detected in this project. Token blacklist uses in-memory `ConcurrentHashMap`. Rate limiting uses in-memory `ConcurrentHashMap`.

---

## 25. Environment Variables

<!-- ENVIRONMENT VARIABLE INVENTORY -->

### Required in Production

| Variable | Source | Purpose |
|----------|--------|---------|
| `DATABASE_URL` | `application-prod.yml` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | `application-prod.yml` | Database username |
| `DATABASE_PASSWORD` | `application-prod.yml` | Database password |
| `JWT_SECRET` | `application-prod.yml` | JWT HMAC signing secret (min 32 chars) |
| `ENCRYPTION_KEY` | `application-prod.yml` | AES-256 encryption key for credentials |
| `CORS_ALLOWED_ORIGINS` | `application-prod.yml` | Comma-separated allowed CORS origins |
| `S3_BUCKET` | `application-prod.yml` | AWS S3 bucket name |
| `AWS_REGION` | `application-prod.yml` | AWS region for S3 and SES |
| `SES_FROM_EMAIL` | `application-prod.yml` | Sender email address for SES |

### Optional (with defaults)

| Variable | Default | Source | Purpose |
|----------|---------|--------|---------|
| `DB_USERNAME` | `codeops` | `application-dev.yml` | Dev database username |
| `DB_PASSWORD` | `codeops` | `application-dev.yml` | Dev database password |
| `JWT_SECRET` | `dev-secret-key-minimum-32-characters-long-for-hs256` | `application-dev.yml` | Dev JWT secret |
| `codeops.aws.s3.enabled` | `false` | `application-dev.yml` | S3 toggle |
| `codeops.aws.ses.enabled` | `false` | `application-dev.yml` | SES toggle |
| `codeops.local-storage.path` | `${user.home}/.codeops/storage` | `application-dev.yml` | Local file storage path |

### @Value Annotations in Code

| Class | Property | Default |
|-------|----------|---------|
| `CorsConfig` | `codeops.cors.allowed-origins` | `http://localhost:3000` |
| `S3Config` | `codeops.aws.s3.region` | (none -- required when enabled) |
| `SesConfig` | `codeops.aws.ses.region` | (none -- required when enabled) |
| `EncryptionService` | `codeops.encryption.key` | (none -- required) |
| `S3StorageService` | `codeops.aws.s3.enabled` | `false` |
| `S3StorageService` | `codeops.aws.s3.bucket` | `codeops-dev` |
| `S3StorageService` | `codeops.local-storage.path` | `${user.home}/.codeops/storage` |
| `EmailService` | `codeops.aws.ses.enabled` | `false` |
| `EmailService` | `codeops.aws.ses.from-email` | `noreply@codeops.dev` |

---

## 26. Inter-Service Communication

<!-- SERVICE DEPENDENCY MAP -->

### Outbound

| Target | Service | Protocol | Purpose |
|--------|---------|----------|---------|
| Microsoft Teams | `TeamsWebhookService` | HTTPS POST (webhook) | Job completion / critical finding notifications |
| AWS SES | `EmailService` | AWS SDK (HTTPS) | Transactional emails (invitations, alerts, digests) |
| AWS S3 | `S3StorageService` | AWS SDK (HTTPS) | File storage (reports, specs, personas, releases) |

### Infrastructure

| Target | Protocol | Purpose |
|--------|----------|---------|
| PostgreSQL | JDBC (port 5432) | Primary data store |

### Inbound

| Source | Protocol | Purpose |
|--------|----------|---------|
| Frontend SPA (CodeOps-Client) | HTTP REST (port 8090) | All API operations via `/api/v1/*` |

---

*End of audit.*
