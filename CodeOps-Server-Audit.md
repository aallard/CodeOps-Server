# CodeOps Server — Codebase Audit

## 1. Project Identity

| Field | Value |
|---|---|
| Project Name | CodeOps Server |
| Repository URL | https://github.com/adamallard/CodeOps-Server |
| Primary Language / Framework | Java 21 / Spring Boot 3.3.0 |
| Build Tool | Maven 3.x (wrapper) |
| Current Branch | main |
| Latest Commit Hash | 13589802441af1a4c710f8d16d566477b9f2ff33 |
| Latest Commit Message | Update |
| Audit Timestamp | 2026-02-20T14:09:33Z |

---

## 2. Directory Structure

Single-module Maven project. Source code under `src/main/java/com/codeops/`.

```
CodeOps-Server/
├── pom.xml
├── docker-compose.yml
├── Dockerfile
├── CLAUDE.md
├── src/
│   ├── main/
│   │   ├── java/com/codeops/
│   │   │   ├── CodeOpsServerApplication.java
│   │   │   ├── config/                    ← 14 configuration classes
│   │   │   ├── controller/                ← 17 REST controllers (~140 endpoints)
│   │   │   ├── dto/
│   │   │   │   ├── request/               ← Request DTOs (Java records, Jakarta Validation)
│   │   │   │   └── response/              ← Response DTOs (Java records)
│   │   │   ├── entity/                    ← 28 JPA entities
│   │   │   │   └── enums/                 ← 25 enums
│   │   │   ├── exception/                 ← 4 custom exceptions + GlobalExceptionHandler
│   │   │   ├── notification/              ← 3 classes (EmailService, TeamsWebhookService, NotificationDispatcher)
│   │   │   ├── repository/                ← 26 Spring Data JPA repositories
│   │   │   ├── security/                  ← 5 classes (JwtAuthFilter, JwtTokenProvider, RateLimitFilter, SecurityConfig, SecurityUtils)
│   │   │   └── service/                   ← 26 service classes
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/codeops/
│           ├── (61 unit test files)
│           └── integration/               ← BaseIntegrationTest + 15 IT classes
└── (infrastructure files)
```

**Config classes (14):** AppConstants, AsyncConfig, CorsConfig, DataSeeder, GlobalExceptionHandler, HealthController, JacksonConfig, JwtProperties, LoggingInterceptor, MailProperties, RequestCorrelationFilter, RestTemplateConfig, S3Config, WebMvcConfig.

**Total:** 229 source files, 78 test files.

---

## 3. Build & Dependency Manifest

**Parent:** `spring-boot-starter-parent:3.3.0`, Java 21.

**Properties:**

| Property | Value |
|---|---|
| jjwt.version | 0.12.6 |
| mapstruct.version | 1.5.5.Final |
| lombok.version | 1.18.42 |
| mockito.version | 5.21.0 |
| byte-buddy.version | 1.18.4 |

**Dependencies:**

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.3.0 (parent) | REST API framework |
| spring-boot-starter-data-jpa | 3.3.0 (parent) | JPA / Hibernate ORM |
| spring-boot-starter-security | 3.3.0 (parent) | Authentication & authorization |
| spring-boot-starter-validation | 3.3.0 (parent) | Jakarta Bean Validation |
| spring-boot-starter-mail | 3.3.0 (parent) | SMTP email sending |
| postgresql | runtime (parent) | PostgreSQL JDBC driver |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT token generation & validation |
| aws-sdk-s3 | 2.25.0 | S3 file storage |
| totp (dev.samstevens.totp) | 1.7.1 | TOTP MFA |
| lombok | 1.18.42 | Boilerplate reduction |
| mapstruct | 1.5.5.Final | DTO mapping (annotation processor) |
| jackson-datatype-jsr310 | parent | Java 8 date/time serialization |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI / OpenAPI generation |
| logstash-logback-encoder | 7.4 | Structured JSON logging |
| spring-boot-starter-test | 3.3.0 (parent) | Test framework |
| spring-security-test | parent | Security test utilities |
| testcontainers-postgresql | 1.19.8 | Integration test database |
| testcontainers-junit-jupiter | 1.19.8 | Testcontainers JUnit 5 |
| h2 | parent | In-memory test database |

**Build Plugins:**

| Plugin | Config |
|---|---|
| spring-boot-maven-plugin | Excludes Lombok |
| maven-compiler-plugin | source/target 21, annotationProcessorPaths: lombok + mapstruct |
| maven-surefire-plugin | `--add-opens` for Java 25, includes `*Test.java` + `*IT.java` |
| jacoco-maven-plugin 0.8.14 | prepare-agent + report |

**Build Commands:**
```bash
Build:    mvn clean package -DskipTests
Test:     mvn test
Run:      mvn spring-boot:run
Package:  mvn clean package
```

---

## 4. Configuration & Infrastructure Summary

### application.yml
Default profile: `dev`. Server port: `8090`.

### application-dev.yml
- **Database:** PostgreSQL `localhost:5432/codeops`, user/pass from `${DB_USERNAME:codeops}/${DB_PASSWORD:codeops}`
- **JPA:** `ddl-auto=update`, `show-sql=true`, `PostgreSQLDialect`, `open-in-view=false`
- **JWT:** secret from `${JWT_SECRET:dev-secret-...}`, 24h access, 30d refresh
- **Encryption:** hardcoded dev key `dev-only-encryption-key-minimum-32ch`
- **CORS:** `localhost:3000`, `localhost:5173`
- **AWS S3:** disabled, bucket=`codeops-dev`
- **Mail:** disabled, from=`noreply@codeops.dev`
- **Local storage:** `${user.home}/.codeops/storage`
- **Logging:** DEBUG for `com.codeops`, `hibernate.SQL`, `security`, `web`

### application-prod.yml
- All secrets from env vars: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `ENCRYPTION_KEY`, `CORS_ALLOWED_ORIGINS`, `S3_BUCKET`, `AWS_REGION`, `MAIL_FROM_EMAIL`
- **JPA:** `ddl-auto=validate`, `show-sql=false`
- **S3:** enabled. **Mail:** enabled.
- **Logging:** INFO for `com.codeops`, WARN for `hibernate`/`security`/`web`

### logback-spring.xml
- Dev profile: console appender with ANSI colors
- Prod profile: JSON structured logging via LogstashEncoder

### docker-compose.yml
| Service | Image | Port | Container Name |
|---|---|---|---|
| PostgreSQL 16 | postgres:16 | 127.0.0.1:5432 | codeops-db |
| Redis 7-alpine | redis:7-alpine | 6379 | codeops-redis |
| Zookeeper | confluentinc/cp-zookeeper | 2181 | — |
| Kafka | confluentinc/cp-kafka | 9092 | codeops-kafka |

All volumes persistent.

### Dockerfile
Multi-stage: Maven build + Eclipse Temurin JRE 21. Non-root user via `addgroup`/`adduser`. Exposes port 8090.

### Connection Map
```
Database:        PostgreSQL, localhost:5432, database=codeops
Cache:           Redis, localhost:6379 (in docker-compose but NOT used by application code)
Message Broker:  Kafka, localhost:9092 (in docker-compose but NOT used by application code)
External APIs:   Microsoft Teams webhooks (outbound, via RestTemplate in TeamsWebhookService)
Cloud Services:  AWS S3 (prod only, disabled in dev)
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

**Entry point:** `com.codeops.CodeOpsServerApplication` (standard `@SpringBootApplication`)

**Startup initialization:**
- **DataSeeder** (`@PostConstruct`): Seeds admin user (`admin@codeops.dev` / `AdminPass123!`), default team ("CodeOps Team"), system personas (6 — one per AgentType), and sample system directives. Only runs if admin user doesn't exist.
- **AsyncConfig**: Configures thread pool (core=5, max=10, queue=100, prefix=`codeops-async-`)
- **RequestCorrelationFilter**: Adds `X-Correlation-ID` (UUID) to every request via MDC

**Health check:** `GET /api/v1/health` returns `{"status": "UP", "service": "codeops-server", "timestamp": "..."}`

No scheduled tasks or background jobs.

---

## 6. Entity / Data Model Layer

### BaseEntity (abstract, @MappedSuperclass)

| Field | Type | Constraints |
|---|---|---|
| id | UUID | `@Id @GeneratedValue(strategy=UUID)`, not null |
| createdAt | Instant | `@Column(nullable=false, updatable=false)` |
| updatedAt | Instant | `@Column(nullable=false)` |

Lifecycle: `@PrePersist` sets both to `Instant.now()`, `@PreUpdate` sets `updatedAt`.

All entities below extend BaseEntity (UUID PK, createdAt, updatedAt) unless noted.

---

### User

**Table:** `users`

| Field | Type | Constraints |
|---|---|---|
| email | String | `nullable=false, unique=true, length=255` |
| password | String | `nullable=false, length=255` |
| displayName | String | `nullable=false, length=100` |
| avatarUrl | String | `length=500` |
| isActive | boolean | `nullable=false, default true` |
| lastLoginAt | Instant | — |
| mfaEnabled | boolean | `nullable=false, default false` |
| mfaSecret | String | `length=500` |
| mfaMethod | MfaMethod | `@Enumerated(STRING)` |
| mfaRecoveryCodes | String | `length=5000` |

Indexes: `@Index(email)`. Unique: `email`.

---

### Team

**Table:** `teams`

| Field | Type | Constraints |
|---|---|---|
| name | String | `nullable=false, length=100` |
| description | String | `length=5000` |
| teamsWebhookUrl | String | `length=500` |

Relationships:
- `owner`: `@ManyToOne` -> User (`nullable=false`)
- `members`: `@OneToMany` -> TeamMember (`mappedBy="team"`, `cascade=ALL`, `orphanRemoval=true`)
- `projects`: `@OneToMany` -> Project (`mappedBy="team"`, `cascade=ALL`, `orphanRemoval=true`)

Indexes: `@Index(owner_id)`.

---

### TeamMember

**Table:** `team_members`

| Field | Type | Constraints |
|---|---|---|
| role | TeamRole | `@Enumerated(STRING), nullable=false` |
| joinedAt | Instant | `nullable=false` |

Relationships:
- `team`: `@ManyToOne` -> Team (`nullable=false`)
- `user`: `@ManyToOne` -> User (`nullable=false`)

Indexes: `@Index(team_id)`, `@Index(user_id)`. Unique: `@UniqueConstraint(team_id, user_id)`.

---

### Project

**Table:** `projects`

| Field | Type | Constraints |
|---|---|---|
| name | String | `nullable=false, length=200` |
| description | String | `length=5000` |
| repoUrl | String | `length=2000` |
| repoFullName | String | `length=200` |
| defaultBranch | String | `length=200` |
| jiraProjectKey | String | `length=50` |
| jiraDefaultIssueType | String | `length=200` |
| jiraLabels | String | `length=5000` (JSON array string) |
| jiraComponent | String | `length=200` |
| techStack | String | `length=5000` |
| healthScore | Integer | — |
| lastAuditAt | Instant | — |
| isArchived | boolean | `nullable=false, default false` |

Relationships:
- `team`: `@ManyToOne` -> Team (`nullable=false`)
- `githubConnection`: `@ManyToOne` -> GitHubConnection
- `jiraConnection`: `@ManyToOne` -> JiraConnection

Indexes: `@Index(team_id)`, `@Index(github_connection_id)`, `@Index(jira_connection_id)`.

---

### SystemSetting

**Table:** `system_settings` — **Does NOT extend BaseEntity.**

| Field | Type | Constraints |
|---|---|---|
| key | String | `@Id, length=200` |
| value | String | `nullable=false, length=5000` |
| updatedAt | Instant | — |

---

### AuditLog

**Table:** `audit_log` — **Does NOT extend BaseEntity.**

| Field | Type | Constraints |
|---|---|---|
| id | Long | `@GeneratedValue(IDENTITY)` |
| action | String | `nullable=false, length=200` |
| entityType | String | `length=100` |
| entityId | String | `length=100` |
| userId | UUID | — |
| teamId | UUID | — |
| details | String | `length=5000` |
| ipAddress | String | `length=50` |
| timestamp | Instant | `nullable=false` |

---

### Directive

**Table:** `directives`

| Field | Type | Constraints |
|---|---|---|
| name | String | `nullable=false, length=200` |
| description | String | `length=5000` |
| contentMd | String | `nullable=false, columnDefinition="TEXT"` |
| category | DirectiveCategory | `@Enumerated(STRING)` |
| scope | DirectiveScope | `@Enumerated(STRING), nullable=false` |
| version | int | `nullable=false, default 1` |

Relationships:
- `team`: `@ManyToOne` -> Team
- `project`: `@ManyToOne` -> Project
- `createdBy`: `@ManyToOne` -> User

Indexes: `@Index(team_id)`, `@Index(created_by_id)`, `@Index(scope)`.

---

### ProjectDirective

**Table:** `project_directives` — **Does NOT extend BaseEntity.** Uses composite PK (`@EmbeddedId ProjectDirectiveId`).

| Field | Type | Constraints |
|---|---|---|
| ProjectDirectiveId.projectId | UUID | `@EmbeddedId` (composite PK) |
| ProjectDirectiveId.directiveId | UUID | `@EmbeddedId` (composite PK) |
| enabled | boolean | `nullable=false, default true` |

Relationships:
- `project`: `@ManyToOne` -> Project
- `directive`: `@ManyToOne` -> Directive

---

### QaJob

**Table:** `qa_jobs`

| Field | Type | Constraints |
|---|---|---|
| mode | JobMode | `@Enumerated(STRING), nullable=false` |
| status | JobStatus | `@Enumerated(STRING), nullable=false` |
| name | String | `length=200` |
| branch | String | `length=200` |
| configJson | String | `columnDefinition="TEXT"` |
| summaryMd | String | `columnDefinition="TEXT"` |
| overallResult | JobResult | `@Enumerated(STRING)` |
| healthScore | Integer | — |
| totalFindings | int | `default 0` |
| criticalCount | int | `default 0` |
| highCount | int | `default 0` |
| mediumCount | int | `default 0` |
| lowCount | int | `default 0` |
| jiraTicketKey | String | `length=200` |
| startedAt | Instant | — |
| completedAt | Instant | — |

Relationships:
- `project`: `@ManyToOne` -> Project (`nullable=false`)
- `startedBy`: `@ManyToOne` -> User

Indexes: `@Index(project_id)`, `@Index(started_by_id)`, `@Index(status)`.

---

### AgentRun

**Table:** `agent_runs`

| Field | Type | Constraints |
|---|---|---|
| agentType | AgentType | `@Enumerated(STRING), nullable=false` |
| status | AgentStatus | `@Enumerated(STRING), nullable=false` |
| result | AgentResult | `@Enumerated(STRING)` |
| reportS3Key | String | `length=1000` |
| score | Integer | — |
| findingsCount | int | `default 0` |
| criticalCount | int | `default 0` |
| highCount | int | `default 0` |
| startedAt | Instant | — |
| completedAt | Instant | — |

Relationships:
- `job`: `@ManyToOne` -> QaJob (`nullable=false`)

Indexes: `@Index(job_id)`, `@Index(agent_type)`.

---

### Finding

**Table:** `findings`

| Field | Type | Constraints |
|---|---|---|
| agentType | AgentType | `@Enumerated(STRING), nullable=false` |
| severity | Severity | `@Enumerated(STRING), nullable=false` |
| title | String | `nullable=false, length=500` |
| description | String | `length=5000` |
| filePath | String | `length=1000` |
| lineNumber | Integer | — |
| recommendation | String | `length=5000` |
| evidence | String | `columnDefinition="TEXT"` |
| effortEstimate | Effort | `@Enumerated(STRING)` |
| debtCategory | DebtCategory | `@Enumerated(STRING)` |
| status | FindingStatus | `@Enumerated(STRING), nullable=false` |
| statusChangedAt | Instant | — |

Relationships:
- `job`: `@ManyToOne` -> QaJob (`nullable=false`)
- `statusChangedBy`: `@ManyToOne` -> User

Indexes: `@Index(job_id)`, `@Index(severity)`, `@Index(status)`, `@Index(agent_type)`.

---

### BugInvestigation

**Table:** `bug_investigations`

| Field | Type | Constraints |
|---|---|---|
| jiraKey | String | `length=200` |
| jiraSummary | String | `length=500` |
| jiraDescription | String | `columnDefinition="TEXT"` |
| jiraCommentsJson | String | `columnDefinition="TEXT"` |
| jiraAttachmentsJson | String | `columnDefinition="TEXT"` |
| jiraLinkedIssues | String | `columnDefinition="TEXT"` |
| additionalContext | String | `columnDefinition="TEXT"` |
| rcaMd | String | `columnDefinition="TEXT"` |
| impactAssessmentMd | String | `columnDefinition="TEXT"` |
| rcaS3Key | String | `length=1000` |
| rcaPostedToJira | boolean | `default false` |
| fixTasksCreatedInJira | boolean | `default false` |

Relationships:
- `job`: `@ManyToOne` -> QaJob (`nullable=false`)

Indexes: `@Index(job_id)`.

---

### Persona

**Table:** `personas`

| Field | Type | Constraints |
|---|---|---|
| name | String | `nullable=false, length=100` |
| agentType | AgentType | `@Enumerated(STRING)` |
| description | String | `length=5000` |
| contentMd | String | `nullable=false, columnDefinition="TEXT"` |
| scope | Scope | `@Enumerated(STRING), nullable=false` |
| isDefault | boolean | `nullable=false, default false` |
| version | int | `nullable=false, default 1` |

Relationships:
- `team`: `@ManyToOne` -> Team
- `createdBy`: `@ManyToOne` -> User

Indexes: `@Index(team_id)`, `@Index(created_by_id)`, `@Index(scope)`, `@Index(agent_type)`.

---

### Specification

**Table:** `specifications`

| Field | Type | Constraints |
|---|---|---|
| name | String | `nullable=false, length=200` |
| specType | SpecType | `@Enumerated(STRING)` |
| s3Key | String | `nullable=false, length=1000` |

Relationships:
- `job`: `@ManyToOne` -> QaJob (`nullable=false`)

Indexes: `@Index(job_id)`.

---

### HealthSnapshot

**Table:** `health_snapshots`

| Field | Type | Constraints |
|---|---|---|
| healthScore | int | `nullable=false` |
| findingsBySeverity | String | `length=5000` (JSON string) |
| techDebtScore | Integer | — |
| dependencyScore | Integer | — |
| testCoveragePercent | BigDecimal | — |
| capturedAt | Instant | `nullable=false` |

Relationships:
- `project`: `@ManyToOne` -> Project (`nullable=false`)
- `job`: `@ManyToOne` -> QaJob

Indexes: `@Index(project_id)`, `@Index(captured_at)`.

---

### HealthSchedule

**Table:** `health_schedules`

| Field | Type | Constraints |
|---|---|---|
| scheduleType | ScheduleType | `@Enumerated(STRING), nullable=false` |
| cronExpression | String | `length=200` |
| agentTypes | String | `nullable=false, length=500` (JSON array string) |
| isActive | boolean | `nullable=false, default true` |
| lastRunAt | Instant | — |
| nextRunAt | Instant | — |

Relationships:
- `project`: `@ManyToOne` -> Project (`nullable=false`)

Indexes: `@Index(project_id)`.

---

### GitHubConnection

**Table:** `github_connections`

| Field | Type | Constraints |
|---|---|---|
| name | String | `nullable=false, length=100` |
| authType | GitHubAuthType | `@Enumerated(STRING), nullable=false` |
| encryptedCredentials | String | `nullable=false, columnDefinition="TEXT"` |
| githubUsername | String | `length=200` |
| isActive | boolean | `nullable=false, default true` |

Relationships:
- `team`: `@ManyToOne` -> Team (`nullable=false`)

Indexes: `@Index(team_id)`.

---

### JiraConnection

**Table:** `jira_connections`

| Field | Type | Constraints |
|---|---|---|
| name | String | `nullable=false, length=100` |
| instanceUrl | String | `nullable=false, length=500` |
| email | String | `nullable=false, length=255` |
| encryptedApiToken | String | `nullable=false, columnDefinition="TEXT"` |
| isActive | boolean | `nullable=false, default true` |

Relationships:
- `team`: `@ManyToOne` -> Team (`nullable=false`)

Indexes: `@Index(team_id)`.

---

### Invitation

**Table:** `invitations`

| Field | Type | Constraints |
|---|---|---|
| email | String | `nullable=false, length=255` |
| role | TeamRole | `@Enumerated(STRING), nullable=false` |
| token | String | `nullable=false, unique=true, length=255` |
| status | InvitationStatus | `@Enumerated(STRING), nullable=false` |
| expiresAt | Instant | `nullable=false` |

Relationships:
- `team`: `@ManyToOne` -> Team (`nullable=false`)
- `invitedBy`: `@ManyToOne` -> User (`nullable=false`)

Indexes: `@Index(team_id)`, `@Index(token)`, `@Index(status)`.

---

### TechDebtItem

**Table:** `tech_debt_items`

| Field | Type | Constraints |
|---|---|---|
| category | DebtCategory | `@Enumerated(STRING), nullable=false` |
| title | String | `nullable=false, length=500` |
| description | String | `length=5000` |
| filePath | String | `length=1000` |
| effortEstimate | Effort | `@Enumerated(STRING)` |
| businessImpact | BusinessImpact | `@Enumerated(STRING)` |
| status | DebtStatus | `@Enumerated(STRING), nullable=false` |

Relationships:
- `project`: `@ManyToOne` -> Project (`nullable=false`)
- `firstDetectedJob`: `@ManyToOne` -> QaJob
- `resolvedJob`: `@ManyToOne` -> QaJob

Indexes: `@Index(project_id)`, `@Index(status)`, `@Index(category)`.

---

### RemediationTask

**Table:** `remediation_tasks`

| Field | Type | Constraints |
|---|---|---|
| taskNumber | int | `nullable=false` |
| title | String | `nullable=false, length=500` |
| description | String | `length=5000` |
| promptMd | String | `columnDefinition="TEXT"` |
| promptS3Key | String | `length=1000` |
| priority | Priority | `@Enumerated(STRING)` |
| status | TaskStatus | `@Enumerated(STRING), nullable=false` |
| jiraKey | String | `length=200` |

Relationships:
- `job`: `@ManyToOne` -> QaJob (`nullable=false`)
- `assignedTo`: `@ManyToOne` -> User
- `findings`: `@ManyToMany` -> Finding (join table: `remediation_task_findings`)

Indexes: `@Index(job_id)`, `@Index(assigned_to_id)`, `@Index(status)`.

---

### DependencyScan

**Table:** `dependency_scans`

| Field | Type | Constraints |
|---|---|---|
| manifestFile | String | `length=200` |
| totalDependencies | int | `default 0` |
| outdatedCount | int | `default 0` |
| vulnerableCount | int | `default 0` |
| scanDataJson | String | `columnDefinition="TEXT"` |

Relationships:
- `project`: `@ManyToOne` -> Project (`nullable=false`)
- `job`: `@ManyToOne` -> QaJob

Indexes: `@Index(project_id)`.

---

### DependencyVulnerability

**Table:** `dependency_vulnerabilities`

| Field | Type | Constraints |
|---|---|---|
| dependencyName | String | `nullable=false, length=200` |
| currentVersion | String | `length=50` |
| fixedVersion | String | `length=50` |
| cveId | String | `length=50` |
| severity | Severity | `@Enumerated(STRING), nullable=false` |
| description | String | `length=5000` |
| status | VulnerabilityStatus | `@Enumerated(STRING), nullable=false` |

Relationships:
- `scan`: `@ManyToOne` -> DependencyScan (`nullable=false`)

Indexes: `@Index(scan_id)`, `@Index(severity)`.

---

### ComplianceItem

**Table:** `compliance_items`

| Field | Type | Constraints |
|---|---|---|
| requirement | String | `nullable=false, length=5000` |
| status | ComplianceStatus | `@Enumerated(STRING), nullable=false` |
| evidence | String | `columnDefinition="TEXT"` |
| agentType | AgentType | `@Enumerated(STRING)` |
| notes | String | `length=5000` |

Relationships:
- `job`: `@ManyToOne` -> QaJob (`nullable=false`)
- `spec`: `@ManyToOne` -> Specification

Indexes: `@Index(job_id)`, `@Index(status)`.

---

### NotificationPreference

**Table:** `notification_preferences`

| Field | Type | Constraints |
|---|---|---|
| emailEnabled | boolean | `nullable=false, default true` |
| teamsEnabled | boolean | `nullable=false, default true` |
| jobCompleted | boolean | `nullable=false, default true` |
| criticalFinding | boolean | `nullable=false, default true` |
| invitationReceived | boolean | `nullable=false, default true` |

Relationships:
- `user`: `@ManyToOne` -> User (`nullable=false`)
- `team`: `@ManyToOne` -> Team (`nullable=false`)

Unique: `@UniqueConstraint(user_id, team_id)`.

---

### MfaEmailCode

**Table:** `mfa_email_codes`

| Field | Type | Constraints |
|---|---|---|
| code | String | `nullable=false, length=10` |
| expiresAt | Instant | `nullable=false` |
| used | boolean | `nullable=false, default false` |
| attempts | int | `nullable=false, default 0` |

Relationships:
- `user`: `@ManyToOne` -> User (`nullable=false`)

Indexes: `@Index(user_id)`, `@Index(expires_at)`.

---

### Entity Relationship Diagram

```
User --[OneToMany]--> TeamMember (via user_id)
User --[OneToMany]--> Team (via owner_id)
Team --[OneToMany]--> TeamMember (via team_id, cascade ALL)
Team --[OneToMany]--> Project (via team_id, cascade ALL)
Team --[OneToMany]--> GitHubConnection (via team_id)
Team --[OneToMany]--> JiraConnection (via team_id)
Team --[OneToMany]--> Invitation (via team_id)
Team --[OneToMany]--> Directive (via team_id)
Team --[OneToMany]--> Persona (via team_id)
Project --[OneToMany]--> QaJob (via project_id)
Project --[OneToMany]--> TechDebtItem (via project_id)
Project --[OneToMany]--> DependencyScan (via project_id)
Project --[OneToMany]--> HealthSnapshot (via project_id)
Project --[OneToMany]--> HealthSchedule (via project_id)
Project --[ManyToOne]--> GitHubConnection (via github_connection_id)
Project --[ManyToOne]--> JiraConnection (via jira_connection_id)
QaJob --[OneToMany]--> AgentRun (via job_id)
QaJob --[OneToMany]--> Finding (via job_id)
QaJob --[OneToMany]--> BugInvestigation (via job_id)
QaJob --[OneToMany]--> RemediationTask (via job_id)
QaJob --[OneToMany]--> Specification (via job_id)
QaJob --[OneToMany]--> ComplianceItem (via job_id)
DependencyScan --[OneToMany]--> DependencyVulnerability (via scan_id)
RemediationTask --[ManyToMany]--> Finding (via remediation_task_findings)
ProjectDirective --[ManyToOne]--> Project + Directive (composite PK)
```

---

## 7. Enum Definitions

| Enum | Values | Used By |
|---|---|---|
| AgentType | CODE_REVIEW, SECURITY, PERFORMANCE, MAINTAINABILITY, TESTING, ARCHITECTURE | AgentRun.agentType, Finding.agentType, ComplianceItem.agentType, Persona.agentType, HealthSchedule.agentTypes |
| AgentStatus | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED | AgentRun.status |
| AgentResult | SUCCESS, PARTIAL_SUCCESS, FAILURE | AgentRun.result |
| BusinessImpact | NONE, LOW, MEDIUM, HIGH, CRITICAL | TechDebtItem.businessImpact |
| ComplianceStatus | PASS, FAIL, NOT_APPLICABLE, PENDING | ComplianceItem.status |
| DebtCategory | CODE_SMELL, ARCHITECTURE, DEPENDENCY, DOCUMENTATION | TechDebtItem.category, Finding.debtCategory |
| DebtStatus | IDENTIFIED, IN_PROGRESS, RESOLVED, ACCEPTED | TechDebtItem.status |
| DirectiveCategory | CODING_STANDARDS, SECURITY, PERFORMANCE, TESTING, ARCHITECTURE, DOCUMENTATION | Directive.category |
| DirectiveScope | SYSTEM, TEAM, PROJECT | Directive.scope |
| Effort | TRIVIAL, SMALL, MEDIUM, LARGE, XLARGE | TechDebtItem.effortEstimate, Finding.effortEstimate |
| FindingStatus | OPEN, IN_PROGRESS, RESOLVED, IGNORED | Finding.status |
| GitHubAuthType | PERSONAL_TOKEN, OAUTH | GitHubConnection.authType |
| InvitationStatus | PENDING, ACCEPTED, REJECTED, EXPIRED | Invitation.status |
| JobMode | FULL, INCREMENTAL, TARGETED | QaJob.mode |
| JobResult | SUCCESS, PARTIAL_SUCCESS, FAILURE | QaJob.overallResult |
| JobStatus | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED | QaJob.status |
| MfaMethod | TOTP, EMAIL | User.mfaMethod |
| Priority | LOW, MEDIUM, HIGH, CRITICAL | RemediationTask.priority |
| ScheduleType | CRON, DAILY, WEEKLY | HealthSchedule.scheduleType |
| Scope | SYSTEM, TEAM, USER | Persona.scope |
| Severity | CRITICAL, HIGH, MEDIUM, LOW | Finding.severity, DependencyVulnerability.severity |
| SpecType | REQUIREMENTS, ARCHITECTURE, API, DATABASE, SECURITY, PERFORMANCE | Specification.specType |
| TaskStatus | OPEN, IN_PROGRESS, COMPLETED, BLOCKED | RemediationTask.status |
| TeamRole | OWNER, ADMIN, MEMBER, VIEWER | TeamMember.role, Invitation.role |
| VulnerabilityStatus | OPEN, RESOLVED, IGNORED | DependencyVulnerability.status |

---

## 8. Repository Layer

All repositories extend `JpaRepository<EntityType, UUID>` unless noted.

### UserRepository
Entity: User
- `Optional<User> findByEmail(String email)`
- `boolean existsByEmail(String email)`
- `List<User> findByDisplayNameContainingIgnoreCase(String name)`

### TeamRepository
Entity: Team — Standard JpaRepository only.

### TeamMemberRepository
Entity: TeamMember
- `List<TeamMember> findByTeamId(UUID teamId)`
- `List<TeamMember> findByUserId(UUID userId)`
- `Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId)`
- `boolean existsByTeamIdAndUserId(UUID teamId, UUID userId)`
- `long countByTeamId(UUID teamId)`
- `void deleteByTeamIdAndUserId(UUID teamId, UUID userId)`

### ProjectRepository
Entity: Project
- `Page<Project> findByTeamId(UUID teamId, Pageable pageable)`
- `List<Project> findByTeamId(UUID teamId)`
- `Page<Project> findByTeamIdAndIsArchivedFalse(UUID teamId, Pageable pageable)`

### SystemSettingRepository
Extends `JpaRepository<SystemSetting, String>`.

### AuditLogRepository
Extends `JpaRepository<AuditLog, Long>`.
- `Page<AuditLog> findByTeamIdOrderByTimestampDesc(UUID teamId, Pageable pageable)`
- `Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable)`
- `Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId, Pageable pageable)`

### DirectiveRepository
Entity: Directive
- `List<Directive> findByTeamId(UUID teamId)`
- `List<Directive> findByScopeAndTeamIdIsNull(DirectiveScope scope)`
- `List<Directive> findByCreatedById(UUID userId)`

### ProjectDirectiveRepository
Entity: ProjectDirective, PK: ProjectDirectiveId
- `List<ProjectDirective> findByIdProjectId(UUID projectId)`
- `Optional<ProjectDirective> findByIdProjectIdAndIdDirectiveId(UUID projectId, UUID directiveId)`
- `void deleteByIdProjectIdAndIdDirectiveId(UUID projectId, UUID directiveId)`
- `List<ProjectDirective> findByIdProjectIdAndEnabledTrue(UUID projectId)`

### QaJobRepository
Entity: QaJob
- `Page<QaJob> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable)`
- `Page<QaJob> findByStartedByIdOrderByCreatedAtDesc(UUID userId, Pageable pageable)`
- `List<QaJob> findByProjectIdAndStatusIn(UUID projectId, List<JobStatus> statuses)`
- `long countByProjectId(UUID projectId)`
- `Optional<QaJob> findTopByProjectIdOrderByCreatedAtDesc(UUID projectId)`

### AgentRunRepository
Entity: AgentRun
- `List<AgentRun> findByJobId(UUID jobId)`
- `Optional<AgentRun> findByJobIdAndAgentType(UUID jobId, AgentType agentType)`
- `void deleteByJobId(UUID jobId)`

### FindingRepository
Entity: Finding
- `Page<Finding> findByJobId(UUID jobId, Pageable pageable)`
- `Page<Finding> findByJobIdAndSeverity(UUID jobId, Severity severity, Pageable pageable)`
- `Page<Finding> findByJobIdAndAgentType(UUID jobId, AgentType agentType, Pageable pageable)`
- `Page<Finding> findByJobIdAndStatus(UUID jobId, FindingStatus status, Pageable pageable)`
- `long countByJobIdAndSeverity(UUID jobId, Severity severity)`
- `List<Finding> findByJobIdAndSeverityIn(UUID jobId, List<Severity> severities)`
- `void deleteByJobId(UUID jobId)`

### BugInvestigationRepository
Entity: BugInvestigation
- `Optional<BugInvestigation> findByJobId(UUID jobId)`
- `void deleteByJobId(UUID jobId)`

### PersonaRepository
Entity: Persona
- `Page<Persona> findByTeamId(UUID teamId, Pageable pageable)`
- `List<Persona> findByTeamIdAndAgentType(UUID teamId, AgentType agentType)`
- `Optional<Persona> findByTeamIdAndAgentTypeAndIsDefaultTrue(UUID teamId, AgentType agentType)`
- `List<Persona> findByCreatedById(UUID userId)`
- `List<Persona> findByScopeAndTeamIdIsNull(Scope scope)`

### SpecificationRepository
Entity: Specification
- `Page<Specification> findByJobId(UUID jobId, Pageable pageable)`
- `void deleteByJobId(UUID jobId)`

### HealthSnapshotRepository
Entity: HealthSnapshot
- `Page<HealthSnapshot> findByProjectIdOrderByCapturedAtDesc(UUID projectId, Pageable pageable)`
- `Optional<HealthSnapshot> findTopByProjectIdOrderByCapturedAtDesc(UUID projectId)`
- `List<HealthSnapshot> findByProjectIdAndCapturedAtAfterOrderByCapturedAtAsc(UUID projectId, Instant after)`

### HealthScheduleRepository
Entity: HealthSchedule
- `List<HealthSchedule> findByProjectId(UUID projectId)`
- `List<HealthSchedule> findByIsActiveTrue()`

### GitHubConnectionRepository
Entity: GitHubConnection
- `List<GitHubConnection> findByTeamId(UUID teamId)`

### JiraConnectionRepository
Entity: JiraConnection
- `List<JiraConnection> findByTeamId(UUID teamId)`

### InvitationRepository
Entity: Invitation
- `Optional<Invitation> findByToken(String token)`
- `List<Invitation> findByTeamIdAndStatus(UUID teamId, InvitationStatus status)`
- `@Lock(PESSIMISTIC_WRITE) @Query("SELECT i FROM Invitation i WHERE i.team.id = :teamId AND i.email = :email AND i.status = :status") Optional<Invitation> findByTeamIdAndEmailAndStatusForUpdate(UUID teamId, String email, InvitationStatus status)`

### TechDebtItemRepository
Entity: TechDebtItem
- `Page<TechDebtItem> findByProjectId(UUID projectId, Pageable pageable)`
- `List<TechDebtItem> findByProjectId(UUID projectId)`
- `Page<TechDebtItem> findByProjectIdAndStatus(UUID projectId, DebtStatus status, Pageable pageable)`
- `Page<TechDebtItem> findByProjectIdAndCategory(UUID projectId, DebtCategory category, Pageable pageable)`
- `void deleteByProjectId(UUID projectId)`

### RemediationTaskRepository
Entity: RemediationTask
- `Page<RemediationTask> findByJobId(UUID jobId, Pageable pageable)`
- `Page<RemediationTask> findByAssignedToId(UUID userId, Pageable pageable)`
- `void deleteByJobId(UUID jobId)`

### DependencyScanRepository
Entity: DependencyScan
- `Page<DependencyScan> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable)`
- `Optional<DependencyScan> findTopByProjectIdOrderByCreatedAtDesc(UUID projectId)`

### DependencyVulnerabilityRepository
Entity: DependencyVulnerability
- `Page<DependencyVulnerability> findByScanId(UUID scanId, Pageable pageable)`
- `Page<DependencyVulnerability> findByScanIdAndSeverity(UUID scanId, Severity severity, Pageable pageable)`
- `Page<DependencyVulnerability> findByScanIdAndStatusNot(UUID scanId, VulnerabilityStatus status, Pageable pageable)`
- `void deleteByScanId(UUID scanId)`

### ComplianceItemRepository
Entity: ComplianceItem
- `Page<ComplianceItem> findByJobId(UUID jobId, Pageable pageable)`
- `Page<ComplianceItem> findByJobIdAndStatus(UUID jobId, ComplianceStatus status, Pageable pageable)`
- `long countByJobIdAndStatus(UUID jobId, ComplianceStatus status)`
- `void deleteByJobId(UUID jobId)`

### MfaEmailCodeRepository
Entity: MfaEmailCode
- `@Query("SELECT c FROM MfaEmailCode c WHERE c.user.id = :userId AND c.used = false AND c.expiresAt > :now ORDER BY c.expiresAt DESC") Optional<MfaEmailCode> findLatestValidCode(UUID userId, Instant now)`
- `@Modifying @Query("UPDATE MfaEmailCode c SET c.used = true WHERE c.user.id = :userId AND c.used = false") void invalidateAllForUser(UUID userId)`
- `void deleteByExpiresAtBefore(Instant cutoff)`

### NotificationPreferenceRepository
Entity: NotificationPreference
- `Optional<NotificationPreference> findByUserIdAndTeamId(UUID userId, UUID teamId)`

---

## 9. Service Layer

All services use `@Service`, `@RequiredArgsConstructor`, `@Transactional`. Read methods use `@Transactional(readOnly=true)`. Team membership verified via `TeamMemberRepository.existsByTeamIdAndUserId()`. Role checks via `findByTeamIdAndUserId().getRole()`.

### AdminService
**Injected:** UserRepository, TeamRepository, QaJobRepository, AuditLogService, TeamMemberRepository, SystemSettingRepository

| Method | Returns | Notes |
|---|---|---|
| `getAllUsers(Pageable)` | `PageResponse<UserResponse>` | Admin only. Paginated user list. |
| `updateUserStatus(UUID userId, boolean isActive)` | `UserResponse` | Admin only. Activate/deactivate user. |
| `getAuditLogs(UUID teamId, Pageable)` | `PageResponse<AuditLogResponse>` | Team membership required. |
| `getUserAuditLogs(UUID userId, Pageable)` | `PageResponse<AuditLogResponse>` | Self or admin. |
| `getEntityAuditLogs(String entityType, String entityId, Pageable)` | `PageResponse<AuditLogResponse>` | Admin only. |
| `getSystemSetting(String key)` | `SystemSettingResponse` | Admin only. |
| `updateSystemSetting(UpdateSystemSettingRequest)` | `SystemSettingResponse` | Admin only. Upserts setting, logs to audit. |

### AgentRunService
**Injected:** AgentRunRepository, QaJobRepository, TeamMemberRepository

| Method | Returns | Notes |
|---|---|---|
| `createAgentRun(UUID jobId, CreateAgentRunRequest)` | `AgentRunResponse` | Team membership on job's project. Creates run with PENDING status. |
| `createAgentRuns(UUID jobId, List<AgentType>)` | `List<AgentRunResponse>` | Batch creates runs for multiple agent types. |
| `getAgentRuns(UUID jobId)` | `List<AgentRunResponse>` | Team membership required. |
| `updateAgentRun(UUID agentRunId, UpdateAgentRunRequest)` | `AgentRunResponse` | Updates status, result, scores, timestamps. |
| `deleteAgentRuns(UUID jobId)` | `void` | Deletes all runs for a job. |

### AuditLogService
**Injected:** AuditLogRepository

| Method | Returns | Notes |
|---|---|---|
| `@Async log(String action, String entityType, String entityId, UUID userId, UUID teamId, String details, String ipAddress)` | `void` | Async fire-and-forget audit logging. |
| `getAuditLogs(UUID teamId, Pageable)` | `Page<AuditLog>` | — |
| `getUserAuditLogs(UUID userId, Pageable)` | `Page<AuditLog>` | — |
| `getEntityAuditLogs(String entityType, String entityId, Pageable)` | `Page<AuditLog>` | — |

### AuthService
**Injected:** UserRepository, JwtTokenProvider, PasswordEncoder, TokenBlacklistService, MfaService

| Method | Returns | Notes |
|---|---|---|
| `register(RegisterRequest)` | `AuthResponse` | Validates email unique, hashes password, creates user, generates JWT tokens. |
| `login(LoginRequest)` | `AuthResponse` | Validates credentials. If MFA enabled, returns `mfaRequired=true` + challengeToken. |
| `refresh(RefreshTokenRequest)` | `AuthResponse` | Validates refresh token, generates new token pair. |
| `logout(String authHeader)` | `void` | Extracts JTI from token, blacklists it. |
| `changePassword(ChangePasswordRequest)` | `void` | Validates current password, updates to new hashed password. |

### BugInvestigationService
**Injected:** BugInvestigationRepository, QaJobRepository, TeamMemberRepository, ReportStorageService

| Method | Returns | Notes |
|---|---|---|
| `createInvestigation(CreateBugInvestigationRequest)` | `BugInvestigationResponse` | Team membership on job's project. |
| `getInvestigation(UUID jobId)` | `BugInvestigationResponse` | Team membership required. |
| `updateInvestigation(UUID investigationId, UpdateBugInvestigationRequest)` | `BugInvestigationResponse` | Updates RCA, impact, S3 key, Jira flags. |
| `deleteInvestigation(UUID jobId)` | `void` | Deletes investigation for job. |
| `getInvestigationsByJobIds(List<UUID>)` | `Map<UUID, BugInvestigationResponse>` | — |
| `uploadRca(UUID investigationId, String rcaMd)` | `String` | Uploads RCA markdown to S3. |

### ComplianceService
**Injected:** ComplianceItemRepository, SpecificationRepository, QaJobRepository, TeamMemberRepository, ReportStorageService

| Method | Returns | Notes |
|---|---|---|
| `createSpecification(CreateSpecificationRequest)` | `SpecificationResponse` | Team membership on job's project. |
| `getSpecificationsForJob(UUID jobId, Pageable)` | `PageResponse<SpecificationResponse>` | — |
| `createComplianceItem(CreateComplianceItemRequest)` | `ComplianceItemResponse` | — |
| `createComplianceItems(List<CreateComplianceItemRequest>)` | `List<ComplianceItemResponse>` | Batch create. |
| `getComplianceItemsForJob(UUID jobId, Pageable)` | `PageResponse<ComplianceItemResponse>` | — |
| `getComplianceItemsByStatus(UUID jobId, ComplianceStatus, Pageable)` | `PageResponse<ComplianceItemResponse>` | — |
| `getComplianceSummary(UUID jobId)` | `Map<String, Object>` | Returns total, pass, fail, notApplicable, pending counts + passRate. |

### DependencyService
**Injected:** DependencyScanRepository, DependencyVulnerabilityRepository, ProjectRepository, QaJobRepository, TeamMemberRepository

| Method | Returns | Notes |
|---|---|---|
| `createScan(CreateDependencyScanRequest)` | `DependencyScanResponse` | Team membership on project. |
| `getScan(UUID scanId)` | `DependencyScanResponse` | — |
| `getScansForProject(UUID projectId, Pageable)` | `PageResponse<DependencyScanResponse>` | — |
| `getLatestScan(UUID projectId)` | `DependencyScanResponse` | — |
| `addVulnerability(CreateVulnerabilityRequest)` | `VulnerabilityResponse` | — |
| `addVulnerabilities(List<CreateVulnerabilityRequest>)` | `List<VulnerabilityResponse>` | Batch. |
| `getVulnerabilities(UUID scanId, Pageable)` | `PageResponse<VulnerabilityResponse>` | — |
| `getVulnerabilitiesBySeverity(UUID scanId, Severity, Pageable)` | `PageResponse<VulnerabilityResponse>` | — |
| `getOpenVulnerabilities(UUID scanId, Pageable)` | `PageResponse<VulnerabilityResponse>` | Filters status != RESOLVED. |
| `updateVulnerabilityStatus(UUID vulnId, VulnerabilityStatus)` | `VulnerabilityResponse` | — |

### DirectiveService
**Injected:** DirectiveRepository, ProjectDirectiveRepository, TeamMemberRepository, ProjectRepository, UserRepository

| Method | Returns | Notes |
|---|---|---|
| `createDirective(CreateDirectiveRequest)` | `DirectiveResponse` | Team membership (TEAM scope) or admin (SYSTEM). |
| `getDirective(UUID)` | `DirectiveResponse` | — |
| `getDirectivesForTeam(UUID teamId)` | `List<DirectiveResponse>` | — |
| `getDirectivesForProject(UUID projectId)` | `List<DirectiveResponse>` | Returns SYSTEM + TEAM + PROJECT directives. |
| `updateDirective(UUID, UpdateDirectiveRequest)` | `DirectiveResponse` | Increments version. |
| `deleteDirective(UUID)` | `void` | OWNER/ADMIN only. |
| `assignToProject(AssignDirectiveRequest)` | `ProjectDirectiveResponse` | — |
| `removeFromProject(UUID projectId, UUID directiveId)` | `void` | — |
| `getProjectDirectives(UUID projectId)` | `List<ProjectDirectiveResponse>` | — |
| `getEnabledDirectives(UUID projectId)` | `List<DirectiveResponse>` | Only enabled assignments. |
| `toggleDirective(UUID projectId, UUID directiveId, boolean enabled)` | `ProjectDirectiveResponse` | — |

### EncryptionService
**Injected:** `@Value("${codeops.encryption.key}")` String key

| Method | Returns | Notes |
|---|---|---|
| `encrypt(String plaintext)` | `String` | AES-256-GCM. Returns Base64(IV + ciphertext + tag). |
| `decrypt(String ciphertext)` | `String` | AES-256-GCM. Extracts IV from first 12 bytes. |

### FindingService
**Injected:** FindingRepository, QaJobRepository, TeamMemberRepository

| Method | Returns | Notes |
|---|---|---|
| `createFinding(CreateFindingRequest)` | `FindingResponse` | Team membership on job's project. |
| `createFindings(List<CreateFindingRequest>)` | `List<FindingResponse>` | Batch. |
| `getFinding(UUID)` | `FindingResponse` | — |
| `getFindingsForJob(UUID jobId, Pageable)` | `PageResponse<FindingResponse>` | — |
| `getFindingsBySeverity(UUID jobId, Severity, Pageable)` | `PageResponse<FindingResponse>` | — |
| `getFindingsByAgent(UUID jobId, AgentType, Pageable)` | `PageResponse<FindingResponse>` | — |
| `getFindingsByStatus(UUID jobId, FindingStatus, Pageable)` | `PageResponse<FindingResponse>` | — |
| `getSeverityCounts(UUID jobId)` | `Map<Severity, Long>` | — |
| `updateFindingStatus(UUID, UpdateFindingStatusRequest)` | `FindingResponse` | Sets statusChangedBy + statusChangedAt. |
| `bulkUpdateStatus(BulkUpdateFindingsRequest)` | `List<FindingResponse>` | Max 100 findings. |

### GitHubConnectionService
**Injected:** GitHubConnectionRepository, TeamMemberRepository, EncryptionService

| Method | Returns | Notes |
|---|---|---|
| `createConnection(UUID teamId, CreateGitHubConnectionRequest)` | `GitHubConnectionResponse` | OWNER/ADMIN. Encrypts credentials. |
| `getConnections(UUID teamId)` | `List<GitHubConnectionResponse>` | Team membership. |
| `getConnection(UUID teamId, UUID connectionId)` | `GitHubConnectionResponse` | — |
| `deleteConnection(UUID teamId, UUID connectionId)` | `void` | Soft delete (isActive=false). |
| `getDecryptedCredentials(UUID connectionId)` | `String` | Internal use. Returns decrypted PAT. |

### HealthMonitorService
**Injected:** HealthSnapshotRepository, HealthScheduleRepository, ProjectRepository, QaJobRepository, TeamMemberRepository

| Method | Returns | Notes |
|---|---|---|
| `createSchedule(CreateHealthScheduleRequest)` | `HealthScheduleResponse` | Team membership. |
| `getSchedulesForProject(UUID projectId)` | `List<HealthScheduleResponse>` | — |
| `updateSchedule(UUID scheduleId, boolean active)` | `HealthScheduleResponse` | — |
| `deleteSchedule(UUID scheduleId)` | `void` | — |
| `createSnapshot(CreateHealthSnapshotRequest)` | `HealthSnapshotResponse` | Updates project.healthScore and project.lastAuditAt. |
| `getSnapshots(UUID projectId, Pageable)` | `PageResponse<HealthSnapshotResponse>` | — |
| `getLatestSnapshot(UUID projectId)` | `HealthSnapshotResponse` | — |
| `getHealthTrend(UUID projectId, int days)` | `List<HealthSnapshotResponse>` | Returns snapshots from last N days. |

### JiraConnectionService
**Injected:** JiraConnectionRepository, TeamMemberRepository, EncryptionService

| Method | Returns | Notes |
|---|---|---|
| `createConnection(UUID teamId, CreateJiraConnectionRequest)` | `JiraConnectionResponse` | OWNER/ADMIN. Encrypts API token. |
| `getConnections(UUID teamId)` | `List<JiraConnectionResponse>` | — |
| `getConnection(UUID teamId, UUID connectionId)` | `JiraConnectionResponse` | — |
| `deleteConnection(UUID teamId, UUID connectionId)` | `void` | Soft delete. |
| `getDecryptedToken(UUID connectionId)` | `String` | Internal use. |

### MetricsService
**Injected:** ProjectRepository, QaJobRepository, FindingRepository, TechDebtItemRepository, DependencyVulnerabilityRepository, DependencyScanRepository, HealthSnapshotRepository, TeamMemberRepository

| Method | Returns | Notes |
|---|---|---|
| `getProjectMetrics(UUID projectId)` | `ProjectMetricsResponse` | Aggregates health scores, finding counts, tech debt, vulnerabilities. |
| `getTeamMetrics(UUID teamId)` | `TeamMetricsResponse` | Aggregates across all team projects. |
| `getHealthTrend(UUID projectId, int days)` | `List<HealthSnapshotResponse>` | — |

### MfaService
**Injected:** UserRepository, JwtTokenProvider, MfaEmailCodeRepository, EmailService

| Method | Returns | Notes |
|---|---|---|
| `setupTotp(MfaSetupRequest)` | `MfaSetupResponse` | Validates password. Generates TOTP secret + QR URI + recovery codes. |
| `verifyTotp(MfaVerifyRequest)` | `MfaStatusResponse` | Verifies TOTP code. Enables MFA, stores encrypted secret + recovery codes. |
| `mfaLogin(MfaLoginRequest)` | `AuthResponse` | Validates challenge token + code (TOTP or recovery). |
| `disableTotp(MfaSetupRequest)` | `MfaStatusResponse` | Validates password. Clears MFA fields. |
| `regenerateRecoveryCodes(MfaSetupRequest)` | `MfaRecoveryResponse` | — |
| `getMfaStatus()` | `MfaStatusResponse` | Returns mfaEnabled, method, remaining recovery codes. |
| `setupEmailMfa(MfaEmailSetupRequest)` | `MfaRecoveryResponse` | Validates password. Sends verification code via email. |
| `verifyEmailSetup(MfaVerifyRequest)` | `MfaStatusResponse` | Verifies email code. Enables EMAIL MFA. |
| `resendMfaCode(MfaResendRequest)` | `void` | Sends new email MFA code. |

### NotificationService
**Injected:** NotificationPreferenceRepository, TeamMemberRepository

| Method | Returns | Notes |
|---|---|---|
| `getPreferences(UUID teamId)` | `NotificationPreferenceResponse` | — |
| `updatePreferences(UUID teamId, UpdateNotificationPreferenceRequest)` | `NotificationPreferenceResponse` | — |
| `notifyJobCompleted(QaJob)` | `void` | Dispatches if jobCompleted preference enabled. |
| `notifyCriticalFinding(Finding)` | `void` | — |
| `notifyInvitation(Invitation)` | `void` | — |

### PersonaService
**Injected:** PersonaRepository, TeamMemberRepository, UserRepository

| Method | Returns | Notes |
|---|---|---|
| `createPersona(CreatePersonaRequest)` | `PersonaResponse` | Team membership for TEAM scope. |
| `getPersona(UUID)` | `PersonaResponse` | — |
| `getPersonasForTeam(UUID teamId, Pageable)` | `PageResponse<PersonaResponse>` | — |
| `getPersonasByAgentType(UUID teamId, AgentType)` | `List<PersonaResponse>` | — |
| `getDefaultPersona(UUID teamId, AgentType)` | `PersonaResponse` | — |
| `getMyPersonas()` | `List<PersonaResponse>` | — |
| `getSystemPersonas()` | `List<PersonaResponse>` | — |
| `updatePersona(UUID, UpdatePersonaRequest)` | `PersonaResponse` | Increments version. |
| `deletePersona(UUID)` | `void` | Cannot delete SYSTEM personas. |
| `setAsDefault(UUID)` | `PersonaResponse` | Unsets previous default for same team+agentType. |
| `removeDefault(UUID)` | `PersonaResponse` | — |

### ProjectService
**Injected:** ProjectRepository, TeamMemberRepository, TeamRepository, QaJobRepository

| Method | Returns | Notes |
|---|---|---|
| `createProject(UUID teamId, CreateProjectRequest)` | `ProjectResponse` | Team membership. |
| `getProjects(UUID teamId, Pageable)` | `PageResponse<ProjectResponse>` | — |
| `getProject(UUID)` | `ProjectResponse` | — |
| `updateProject(UUID, UpdateProjectRequest)` | `ProjectResponse` | — |
| `archiveProject(UUID)` | `void` | Sets isArchived=true. |
| `unarchiveProject(UUID)` | `void` | Sets isArchived=false. |
| `deleteProject(UUID)` | `void` | OWNER/ADMIN only. Hard delete. |

### QaJobService
**Injected:** QaJobRepository, ProjectRepository, TeamMemberRepository, AuditLogService, AgentRunRepository, FindingRepository, BugInvestigationRepository, ReportStorageService, RemediationTaskRepository, ComplianceItemRepository, SpecificationRepository, DependencyScanRepository

| Method | Returns | Notes |
|---|---|---|
| `createJob(CreateJobRequest)` | `JobResponse` | Team membership. Sets status=PENDING, startedBy=currentUser. |
| `getJob(UUID)` | `JobResponse` | — |
| `getJobsForProject(UUID projectId, Pageable)` | `PageResponse<JobSummaryResponse>` | — |
| `getMyJobs(Pageable)` | `PageResponse<JobSummaryResponse>` | — |
| `updateJob(UUID, UpdateJobRequest)` | `JobResponse` | Updates status, summary, scores, timestamps. Logs transitions. |
| `deleteJob(UUID)` | `void` | OWNER/ADMIN. Deletes all child entities + S3 reports. |

### RemediationTaskService
**Injected:** RemediationTaskRepository, QaJobRepository, TeamMemberRepository, FindingRepository, UserRepository, S3StorageService

| Method | Returns | Notes |
|---|---|---|
| `createTask(CreateTaskRequest)` | `TaskResponse` | Team membership. Links findings via ManyToMany. |
| `createTasks(List<CreateTaskRequest>)` | `List<TaskResponse>` | Batch. |
| `getTasksForJob(UUID jobId, Pageable)` | `PageResponse<TaskResponse>` | — |
| `getTask(UUID)` | `TaskResponse` | — |
| `getTasksAssignedToUser(UUID userId, Pageable)` | `PageResponse<TaskResponse>` | — |
| `updateTask(UUID, UpdateTaskRequest)` | `TaskResponse` | Updates status, assignee, jiraKey. Logs transitions. |
| `uploadTaskPrompt(UUID jobId, int taskNumber, String promptMd)` | `String` | Uploads to S3. |

### ReportStorageService
**Injected:** S3StorageService, AgentRunRepository

| Method | Returns | Notes |
|---|---|---|
| `uploadReport(UUID jobId, AgentType, String markdownContent)` | `String` | Key: `reports/{jobId}/{agentType}-report.md` |
| `uploadSummaryReport(UUID jobId, String markdownContent)` | `String` | Key: `reports/{jobId}/summary.md` |
| `downloadReport(String s3Key)` | `String` | — |
| `deleteReportsForJob(UUID jobId)` | `void` | Deletes all agent reports + summary. |
| `uploadSpecification(UUID jobId, String fileName, byte[] data, String contentType)` | `String` | Key: `specs/{jobId}/{fileName}` |
| `downloadSpecification(String s3Key)` | `byte[]` | — |

### S3StorageService
**Injected:** `@Value` s3Enabled, bucket, localStoragePath; `@Autowired(required=false)` S3Client

| Method | Returns | Notes |
|---|---|---|
| `upload(String key, byte[] data, String contentType)` | `String` | S3 if enabled, else local filesystem. |
| `download(String key)` | `byte[]` | S3 if enabled, else local. |
| `delete(String key)` | `void` | — |
| `generatePresignedUrl(String key, Duration expiry)` | `String` | Returns `s3://` or `local://` URI. |

### TeamService
**Injected:** TeamRepository, TeamMemberRepository, UserRepository, InvitationRepository

| Method | Returns | Notes |
|---|---|---|
| `createTeam(CreateTeamRequest)` | `TeamResponse` | Creates team + adds current user as OWNER. |
| `getTeam(UUID)` | `TeamResponse` | Team membership. |
| `getTeamsForUser()` | `List<TeamResponse>` | — |
| `updateTeam(UUID, UpdateTeamRequest)` | `TeamResponse` | OWNER/ADMIN. |
| `deleteTeam(UUID)` | `void` | OWNER only. |
| `getTeamMembers(UUID)` | `List<TeamMemberResponse>` | — |
| `updateMemberRole(UUID teamId, UUID userId, UpdateMemberRoleRequest)` | `TeamMemberResponse` | OWNER/ADMIN. Supports ownership transfer. |
| `removeMember(UUID teamId, UUID userId)` | `void` | Admin or self-removal. Cannot remove OWNER. |
| `inviteMember(UUID teamId, InviteMemberRequest)` | `InvitationResponse` | OWNER/ADMIN. Validates capacity (MAX_TEAM_MEMBERS), no duplicate, no existing member. |
| `acceptInvitation(String token)` | `TeamResponse` | Validates PENDING, not expired, email matches current user. |
| `getTeamInvitations(UUID teamId)` | `List<InvitationResponse>` | OWNER/ADMIN. |
| `cancelInvitation(UUID invitationId)` | `void` | OWNER/ADMIN. Marks as EXPIRED. |

### TechDebtService
**Injected:** TechDebtItemRepository, ProjectRepository, TeamMemberRepository, QaJobRepository

| Method | Returns | Notes |
|---|---|---|
| `createTechDebtItem(CreateTechDebtItemRequest)` | `TechDebtItemResponse` | Team membership. |
| `createTechDebtItems(List<CreateTechDebtItemRequest>)` | `List<TechDebtItemResponse>` | Batch. All must share same projectId. |
| `getTechDebtItem(UUID)` | `TechDebtItemResponse` | — |
| `getTechDebtForProject(UUID projectId, Pageable)` | `PageResponse<TechDebtItemResponse>` | — |
| `getTechDebtByStatus(UUID projectId, DebtStatus, Pageable)` | `PageResponse<TechDebtItemResponse>` | — |
| `getTechDebtByCategory(UUID projectId, DebtCategory, Pageable)` | `PageResponse<TechDebtItemResponse>` | — |
| `updateTechDebtStatus(UUID, UpdateTechDebtStatusRequest)` | `TechDebtItemResponse` | Updates status, links resolvedJob. |
| `deleteTechDebtItem(UUID)` | `void` | OWNER/ADMIN. |
| `getDebtSummary(UUID projectId)` | `Map<String, Object>` | Returns total, open, critical counts + byCategory + byStatus maps. |

### TokenBlacklistService
**Injected:** None (ConcurrentHashMap.KeySetView)

| Method | Returns | Notes |
|---|---|---|
| `blacklist(String jti, Instant expiry)` | `void` | Adds JTI to in-memory blacklist. |
| `isBlacklisted(String jti)` | `boolean` | — |

### UserService
**Injected:** UserRepository

| Method | Returns | Notes |
|---|---|---|
| `getUserById(UUID)` | `UserResponse` | — |
| `getUserByEmail(String)` | `UserResponse` | — |
| `getCurrentUser()` | `UserResponse` | Uses SecurityUtils.getCurrentUserId(). |
| `updateUser(UUID, UpdateUserRequest)` | `UserResponse` | Self or admin. Updates displayName, avatarUrl. |
| `searchUsers(String query)` | `List<UserResponse>` | Case-insensitive partial match on displayName, limit 20. |
| `deactivateUser(UUID)` | `void` | Sets isActive=false. |
| `activateUser(UUID)` | `void` | Sets isActive=true. |

---

## 10. Security Architecture

### Authentication Flow
- JWT (HS256) via jjwt 0.12.6
- **Access token:** 24h expiry, claims: `sub`=userId (UUID), `jti`=UUID, `iat`, `exp`
- **Refresh token:** 30d expiry, claims: `sub`=userId, `jti`=UUID, `type`="refresh", `iat`, `exp`
- **MFA challenge token:** 5min expiry, claims: `sub`=userId, `type`="mfa_challenge", `mfaMethod`, `iat`, `exp`
- **Validation:** signature check -> expiry check -> blacklist check -> extract userId
- **Revocation:** logout extracts JTI, adds to in-memory ConcurrentHashMap blacklist. No TTL-based cleanup.

### Authorization Model
- Roles: OWNER, ADMIN, MEMBER, VIEWER (team-scoped via TeamMember.role)
- No global roles (except implicit admin seed user)
- `@PreAuthorize("isAuthenticated()")` on all controller methods (except public auth endpoints)
- Fine-grained auth in service layer: team membership, OWNER/ADMIN role checks
- Admin-only endpoints: AdminController uses `hasRole('ADMIN')` or `hasRole('OWNER')`
- User controller: deactivate/activate use `hasRole('ADMIN')` or `hasRole('OWNER')`

### Security Filter Chain (order)
1. **RequestCorrelationFilter** -- adds X-Correlation-ID to MDC
2. **RateLimitFilter** -- 10 req/min per IP on `/api/v1/auth/**`
3. **JwtAuthFilter** -- extracts Bearer token, validates, sets SecurityContext
4. **Spring Security filter chain**

### Public Paths (permitAll)
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/mfa/login`
- `POST /api/v1/auth/mfa/resend`
- `GET /api/v1/health`
- `/swagger-ui/**`, `/v3/api-docs/**`

### CORS
Configurable origins from `codeops.cors.allowed-origins`. Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS. Headers: `*`, credentials: true.

### Encryption
AES-256-GCM for credentials (GitHub PAT, Jira API token). Key from `${codeops.encryption.key}`. 12-byte random IV per encryption. Output: `Base64(IV + ciphertext + auth tag)`.

### Password Policy
BCrypt (default strength ~10). Minimum 8 characters (`@Size(min=8)`). No uppercase/special requirements.

### Rate Limiting
ConcurrentHashMap-based. 10 requests per minute per IP on `/api/v1/auth/**`. Returns 429 Too Many Requests on violation.

---

## 11. Notification / Messaging Layer

**EmailService** (`@Slf4j`): Sends via JavaMailSender when `codeops.mail.enabled=true`. 5 templates (job completed, critical finding, invitation, MFA verification code, password change). Console-logged in dev.

**TeamsWebhookService** (`@Slf4j`): Sends Adaptive Cards via RestTemplate POST. SSRF protection: validates webhook URL against loopback, site-local, link-local addresses. Catches and logs errors silently.

**NotificationDispatcher** (`@Slf4j`, `@Async`): Multi-channel dispatch. Checks NotificationPreference per user/team. Dispatches to EmailService and/or TeamsWebhookService based on preferences. Errors logged, never thrown.

No Kafka consumers/producers in application code (Kafka in docker-compose but unused).

---

## 12. Error Handling

| Exception | HTTP Status | Response Body |
|---|---|---|
| NotFoundException | 404 | `{"status": 404, "message": "<custom message>"}` |
| ValidationException | 400 | `{"status": 400, "message": "<custom message>"}` |
| AuthorizationException | 403 | `{"status": 403, "message": "<custom message>"}` |
| CodeOpsException | 500 | `{"status": 500, "message": "An internal error occurred"}` |
| EntityNotFoundException | 404 | `{"status": 404, "message": "Resource not found"}` |
| IllegalArgumentException | 400 | `{"status": 400, "message": "Invalid request"}` |
| AccessDeniedException | 403 | `{"status": 403, "message": "Access denied"}` |
| MethodArgumentNotValidException | 400 | `{"status": 400, "message": "field: error, field: error"}` |
| HttpMessageNotReadableException | 400 | `{"status": 400, "message": "Malformed request body"}` |
| Exception (catch-all) | 500 | `{"status": 500, "message": "An internal error occurred"}` |

Internal exception details logged server-side via `log.error()`. Client receives sanitized `ErrorResponse` record.

---

## 13. Test Coverage

| Metric | Count |
|---|---|
| Unit test files | 61 |
| Integration test files | 16 (1 BaseIntegrationTest + 15 IT classes) |
| Unit @Test methods | 851 |
| Integration @Test methods | 121 |
| **Total @Test methods** | **972** |

**Framework:** JUnit 5, Mockito 5.21.0, AssertJ, Spring MockMvc.

**Integration tests:** Testcontainers (PostgreSQL 1.19.8), H2 fallback. Config: `application-test.yml`, `application-integration.yml`.

**Coverage:** JaCoCo 0.8.14 configured, no report generated.

**Unit tests cover:** all 26 services, all 17 controllers, all 5 security classes, all 3 notification classes, all config classes.

**Integration tests cover:** AuthControllerIT, TeamControllerIT, ProjectControllerIT, JobControllerIT, FindingControllerIT, ComplianceControllerIT, DependencyControllerIT, AdminControllerIT, MfaIT, EmailMfaIT, SecurityIT, ValidationIT, PaginationIT, DataIntegrityIT, AuditLogIT.

---

## 14. Cross-Cutting Patterns & Conventions

- **Naming:** Controllers: verbNoun (`createTeam`, `getProject`). Services: same. DTOs: `CreateXxxRequest`, `UpdateXxxRequest`, `XxxResponse`. Endpoints: `/api/v1/{resource}`.
- **Package structure:** Flat packages by layer (entity, repository, service, controller, dto/request, dto/response, config, security, notification, exception).
- **Base classes:** BaseEntity (id, createdAt, updatedAt). No BaseService or BaseController.
- **Audit logging:** `AuditLogService.log()` called from services (not controllers). Async. Parameters: action, entityType, entityId, userId (from SecurityUtils), teamId, details, ipAddress.
- **Error handling:** Custom exceptions thrown from services. GlobalExceptionHandler in config package catches all.
- **Pagination:** `PageResponse<T>` record wraps Spring `Page<T>`. Controllers accept `@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size`.
- **Validation:** Jakarta Bean Validation on request DTO fields. Service-layer business rule validation (throws custom exceptions).
- **Constants:** `AppConstants.java` with 80+ constants (MAX_TEAM_MEMBERS, INVITATION_EXPIRY_DAYS, S3 key prefixes, rate limits, etc).
- **Documentation:** No Javadoc on classes or public methods (0/96 non-DTO/entity classes documented). **This is a gap.**

---

## 15. Known Issues, TODOs, and Technical Debt

1. `src/main/java/com/codeops/service/EncryptionService.java:56` -- `// TODO: Changing key derivation invalidates existing encrypted data -- requires re-encryption migration`

---

## 16. OpenAPI Specification

See `CodeOps-Server-OpenAPI.yaml` in project root. Generated from source code analysis of 17 controllers, ~140 endpoints.

---

## 17. Database -- Live Schema Audit

27 tables in `public` schema (live PostgreSQL):

```
agent_runs, audit_log, bug_investigations, compliance_items, dependency_scans,
dependency_vulnerabilities, directives, findings, github_connections, health_schedules,
health_snapshots, invitations, jira_connections, mfa_email_codes, notification_preferences,
personas, project_directives, projects, qa_jobs, remediation_task_findings,
remediation_tasks, specifications, system_settings, team_members, teams,
tech_debt_items, users
```

**JPA model (28 entities) vs Database (27 tables):** In sync. `BaseEntity` is `@MappedSuperclass` (no table). `ProjectDirectiveId` is `@Embeddable` (no table). `remediation_task_findings` is a join table from `@ManyToMany`.

---

## 18. Kafka / Message Broker

No Kafka producers or consumers detected in application code. Kafka is defined in `docker-compose.yml` (confluentinc/cp-kafka) but not used by the application. No `KafkaTemplate`, `@KafkaListener`, or `spring.kafka` configuration in source.

---

## 19. Redis / Cache Layer

No Redis or caching layer detected in application code. Redis is defined in `docker-compose.yml` (redis:7-alpine) but not used. No `RedisTemplate`, `@Cacheable`, `@CacheEvict`, `CacheManager`, or `spring.cache` configuration. `TokenBlacklistService` uses in-memory `ConcurrentHashMap` instead of Redis.

---

## 20. Environment Variable Inventory

| Variable | Required | Default | Used By | Purpose |
|---|---|---|---|---|
| DB_USERNAME | No | codeops | application-dev.yml | Database username |
| DB_PASSWORD | No | codeops | application-dev.yml | Database password |
| JWT_SECRET | No | dev-secret-key-... | application-dev.yml | JWT signing key |
| DATABASE_URL | Yes (prod) | None | application-prod.yml | PostgreSQL JDBC URL |
| DATABASE_USERNAME | Yes (prod) | None | application-prod.yml | Database username |
| DATABASE_PASSWORD | Yes (prod) | None | application-prod.yml | Database password |
| ENCRYPTION_KEY | Yes (prod) | None | application-prod.yml | AES-256 encryption key |
| CORS_ALLOWED_ORIGINS | Yes (prod) | None | application-prod.yml | Allowed CORS origins |
| S3_BUCKET | Yes (prod) | None | application-prod.yml | S3 bucket name |
| AWS_REGION | Yes (prod) | None | application-prod.yml | AWS region |
| MAIL_FROM_EMAIL | Yes (prod) | None | application-prod.yml | Sender email address |

**Dangerous defaults:** `JWT_SECRET` has a dev fallback that would be insecure in production. `codeops.encryption.key` has a hardcoded dev value `dev-only-encryption-key-minimum-32ch`. Both are overridden by env vars in prod profile.

---

## 21. Inter-Service Communication Map

**Outbound HTTP:**
- `TeamsWebhookService` -> Microsoft Teams webhook URLs (user-configured per team, via RestTemplate POST)

No other outbound service-to-service HTTP calls. `RestTemplate` is configured as a `@Bean` in `RestTemplateConfig` and only used by `TeamsWebhookService`.

**Inbound:** CodeOps-Client (Flutter desktop app) connects to this server's REST API.
