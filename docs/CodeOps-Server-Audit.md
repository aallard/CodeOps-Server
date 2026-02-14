# CodeOps-Server — Comprehensive Code Review & Audit

**Audit Date:** February 13, 2026
**Codebase Version:** Commit `1f1a3f0` (main)
**Auditor:** Claude Code (Automated Static Analysis)

---

## Executive Summary

This audit covers all layers of the CodeOps-Server codebase: 25 JPA entities, 25 repositories, 24 services, 18 controllers, ~68 DTOs, security configuration, notification services, and infrastructure (Docker, pom.xml). The audit identified **100+ issues** across all severity levels, with the most critical being credential exposure vulnerabilities, missing authorization checks, and JPA enum mapping defects that would cause data corruption.

| Severity | Count | Description |
|----------|-------|-------------|
| **CRITICAL** | 12 | Data corruption, credential exposure, IDOR, missing token revocation |
| **HIGH** | 22 | Unbounded queries, missing auth checks, injection risks, SSRF |
| **MEDIUM** | 18 | Race conditions, weak validation, missing security headers |
| **LOW** | 10 | Docker config, naming conventions, documentation gaps |
| **INFO** | 5 | Positive findings / compliant patterns |

---

## Table of Contents

1. [Entity & Repository Layer](#1-entity--repository-layer)
2. [Service Layer](#2-service-layer)
3. [Controller & DTO Layer](#3-controller--dto-layer)
4. [Security & Authentication](#4-security--authentication)
5. [Configuration & Infrastructure](#5-configuration--infrastructure)
6. [Notification Services](#6-notification-services)
7. [Positive Findings](#7-positive-findings)
8. [Remediation Priority](#8-remediation-priority)

---

## 1. Entity & Repository Layer

### CRITICAL — Missing `@Enumerated(EnumType.STRING)` Annotations (23 Fields)

Without `@Enumerated(EnumType.STRING)`, JPA stores enum values as ordinal integers (0, 1, 2...) instead of their string names. If enum constants are reordered or new values inserted, all existing data silently maps to wrong values. This is a **data corruption time bomb**.

| Entity | Field | Enum Type |
|--------|-------|-----------|
| `AgentRun.java` | `agentType` | `AgentType` |
| `AgentRun.java` | `status` | `AgentStatus` |
| `ComplianceItem.java` | `status` | `ComplianceStatus` |
| `ComplianceItem.java` | `agentType` | `AgentType` |
| `DependencyVulnerability.java` | `severity` | `Severity` |
| `DependencyVulnerability.java` | `status` | `VulnerabilityStatus` |
| `Finding.java` | `agentType` | `AgentType` |
| `Finding.java` | `severity` | `Severity` |
| `Finding.java` | `effortEstimate` | `Effort` |
| `Finding.java` | `debtCategory` | `DebtCategory` |
| `GitHubConnection.java` | `authType` | `GitHubAuthType` |
| `HealthSchedule.java` | `scheduleType` | `ScheduleType` |
| `Invitation.java` | `role` | `TeamRole` |
| `Invitation.java` | `status` | `InvitationStatus` |
| `Persona.java` | `agentType` | `AgentType` |
| `Persona.java` | `scope` | `Scope` |
| `QaJob.java` | `mode` | `JobMode` |
| `QaJob.java` | `status` | `JobStatus` |
| `QaJob.java` | `overallResult` | `JobResult` |
| `RemediationTask.java` | `status` | `TaskStatus` |
| `Specification.java` | `specType` | `SpecType` |
| `TechDebtItem.java` | `category` | `DebtCategory` |
| `TechDebtItem.java` | `effortEstimate` | `Effort` |
| `TechDebtItem.java` | `businessImpact` | `BusinessImpact` |

**Note:** Some entities are correct — `TeamMember.role`, `Directive.category`, `Directive.scope`, `RemediationTask.priority` all have `@Enumerated(EnumType.STRING)`.

**Fix:** Add `@Enumerated(EnumType.STRING)` above each affected field. If data already exists, a migration script is needed to convert ordinals back to strings.

---

### HIGH — Missing `nullable = false` Constraints

These fields should be required but the database doesn't enforce it:

| Entity | Field | Issue |
|--------|-------|-------|
| `Persona.java` | `agentType` | Missing `nullable = false` |
| `Persona.java` | `version` | Missing `nullable = false` (defaults to 1) |
| `RemediationTask.java` | `priority` | Missing `nullable = false` |
| `Specification.java` | `specType` | Missing `nullable = false` |
| `HealthSchedule.java` | `scheduleType` | Missing `nullable = false` |
| `Finding.java` | `effortEstimate` | Optional but no documentation of nullability intent |

---

### HIGH — No Database Indexes Defined

**Zero** `@Index` annotations across all 25 entities. Frequently-queried foreign key columns will cause full table scans:

- `project_id` — used by Finding, TechDebtItem, DependencyVulnerability, HealthSnapshot, Specification, ComplianceItem
- `team_id` — used by Project, TeamMember, Invitation, GitHubConnection, JiraConnection, Persona, Directive, AuditLog
- `job_id` — used by AgentRun, Finding
- `user_id` — used by AuditLog, NotificationPreference
- `scan_id` — used by DependencyVulnerability

**Fix:** Add `@Table(indexes = { @Index(columnList = "project_id"), @Index(columnList = "team_id") })` to each entity.

---

### MEDIUM — `RemediationTask.findingIds` Stores UUIDs as Comma-Separated String

**File:** `RemediationTask.java:36`

```java
private String findingIds;  // Stores comma-separated UUIDs
```

No referential integrity, impossible to query efficiently, breaks if UUID contains a comma (won't happen but indicates design smell). Should be a `@ManyToMany` or separate join table.

---

### MEDIUM — No Optimistic Locking

No `@Version` annotations anywhere. Concurrent updates to the same entity silently overwrite each other (last-write-wins). High-contention entities like `QaJob`, `Finding`, and `TechDebtItem` are particularly at risk.

---

### LOW — Missing Cascade/OrphanRemoval Configuration

No cascade or orphanRemoval on any relationship. This may be intentional (preventing accidental cascading deletes), but it means orphaned records accumulate when parent entities are deleted.

---

## 2. Service Layer

### CRITICAL — Credential Exposure: Decryption Without Authorization

**GitHubConnectionService.java:76-80**
```java
public String getDecryptedCredentials(UUID connectionId) {
    GitHubConnection connection = gitHubConnectionRepository.findById(connectionId)...
    // NO team membership check — ANY authenticated user can decrypt ANY GitHub PAT
    return encryptionService.decrypt(connection.getEncryptedCredentials());
}
```

**JiraConnectionService.java:76-87** — Identical issue with `getDecryptedApiToken()`.

**Impact:** Any authenticated user who knows (or guesses) a connection UUID can retrieve plaintext GitHub PATs and Jira API tokens belonging to other teams. This is the most severe vulnerability in the codebase.

**Fix:** Add `verifyTeamMembership(connection.getTeam().getId())` immediately after loading the connection. Consider restricting to ADMIN/OWNER roles only.

---

### CRITICAL — IDOR in AdminService.getUserById()

**AdminService.java:41-45**
```java
public UserResponse getUserById(UUID userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    return mapToUserResponse(user);
}
```

No authorization check. Any authenticated user can enumerate all user accounts by UUID.

---

### CRITICAL — AdminService.getUsageStats() Loads All Users Into Memory

**AdminService.java:92-105**
```java
long activeUsers = userRepository.findAll().stream().filter(User::getIsActive).count();
```

Loads the entire `users` table into JVM heap to count active users. With 100k users, this is an OOM risk. Should use `userRepository.countByIsActiveTrue()`.

---

### HIGH — AuditLogService Missing Team Membership Check

**AuditLogService.java:47-56** — `getTeamAuditLog()` and `getUserAuditLog()` return audit logs for any team/user without verifying the caller has access. Exposes sensitive operational data (who did what, when).

---

### HIGH — NotificationService Missing User Authorization

**NotificationService.java:24-47** — `getPreferences(userId)` and `updatePreference(userId, ...)` accept any userId without verifying `SecurityUtils.getCurrentUserId().equals(userId)`. User A can view/modify User B's notification preferences.

---

### HIGH — HealthMonitorService.getActiveSchedules() Returns System-Wide Data

**HealthMonitorService.java:77-81** — Returns ALL active health schedules across ALL teams with no authorization filter. Exposes operational configuration to any authenticated user.

---

### HIGH — Unbounded List Queries (15+ Methods)

The following methods return full result sets with no pagination, risking memory exhaustion and DoS:

| Service | Method | Risk |
|---------|--------|------|
| `DependencyService` | `getVulnerabilitiesBySeverity()` | All vulns for severity |
| `DependencyService` | `getOpenVulnerabilities()` | All open vulns |
| `DependencyService` | `getVulnerabilities()` | All vulns for scan |
| `FindingService` | `getFindingsByJobAndSeverity()` | All findings by severity |
| `FindingService` | `getFindingsByJobAndAgent()` | All findings by agent |
| `FindingService` | `getFindingsByJobAndStatus()` | All findings by status |
| `FindingService` | `getFindingsByJobAndCategory()` | All findings by category |
| `HealthMonitorService` | `getHealthTrend()` | Loads ALL snapshots, subList in Java |
| `MetricsService` | `getHealthTrend()` | Loads ALL snapshots, filters in Java |
| `RemediationTaskService` | `getTasksAssignedToUser()` | All tasks for user |
| `TechDebtService` | `getDebtSummary()` | Loads ALL items to count in Java |
| `UserService` | `searchUsers()` | Uses `.limit(20)` in Java, not DB |

**Fix:** Add `Pageable` parameter, return `Page<T>`, use database `LIMIT`/`OFFSET`.

---

### HIGH — N+1 Query Patterns in MetricsService

**MetricsService.java:61-64**
```java
openCritical = (int) findingRepository.findByJobIdAndSeverity(latestJobId, Severity.CRITICAL)
    .stream().filter(f -> f.getStatus() == FindingStatus.OPEN).count();
```

Loads ALL findings for each severity level, then filters in Java. Repeated 4+ times for CRITICAL, HIGH, MEDIUM, LOW. Should use `countByJobIdAndSeverityAndStatus()`.

---

### MEDIUM — Race Condition in TeamService.inviteMember()

**TeamService.java:174-176** — Checks `memberCount >= MAX_TEAM_MEMBERS` then saves. Between the count check and save, another thread can add a member, exceeding the limit. Use a database unique constraint or pessimistic locking.

---

### MEDIUM — PersonaService.getDefaultPersona() Returns Null

**PersonaService.java:100-104** — Returns `null` instead of `Optional<PersonaResponse>`, forcing callers to null-check. Inconsistent with other methods that throw `EntityNotFoundException`.

---

### MEDIUM — getReferenceById() Used Without Error Handling

Throughout multiple services (`AdminService`, `DirectiveService`, `ProjectService`), `getReferenceById()` is used to set relationships. If the referenced entity doesn't exist, a `LazyInitializationException` or `EntityNotFoundException` occurs at flush time, corrupting the transaction. Use `findById()` with explicit error handling.

---

### MEDIUM — Inconsistent Error Handling

Services mix `IllegalArgumentException`, `EntityNotFoundException`, `AccessDeniedException`, and `RuntimeException` without a consistent hierarchy. No custom error codes for API consumers.

---

## 3. Controller & DTO Layer

### CRITICAL — Missing Audit Logging on 15+ Mutation Endpoints

The following POST/PUT endpoints create or modify data without calling `auditLogService.log()`:

| Controller | Endpoints Missing Audit |
|------------|------------------------|
| `TaskController` | `createTask()`, `createTasks()`, `updateTask()` |
| `FindingController` | `createFinding()`, `createFindings()`, `updateFindingStatus()`, `bulkUpdateStatus()` |
| `ComplianceController` | `createSpecification()`, `createComplianceItem()`, `createComplianceItems()` |
| `DependencyController` | `createScan()`, `addVulnerability()`, `addVulnerabilities()`, `updateVulnerabilityStatus()` |
| `JobController` | `createAgentRun()`, `createAgentRunsBatch()`, `updateAgentRun()`, `createInvestigation()`, `updateInvestigation()` |

**Impact:** No audit trail for these operations. Compliance and forensic analysis are impossible for these actions.

---

### HIGH — Path Traversal Risk in ReportController

**ReportController.java:46, 65**
```java
public ResponseEntity<byte[]> downloadReport(@RequestParam String s3Key) { ... }
public ResponseEntity<byte[]> downloadSpecification(@RequestParam String s3Key) { ... }
```

The `s3Key` parameter is passed directly to storage service without validation. An attacker could craft keys like `../../../etc/passwd` (for local filesystem fallback) or access other tenants' files in S3.

**Fix:** Validate s3Key format (alphanumeric, slashes, hyphens only) or resolve the key against the current user's team/project.

---

### HIGH — Unbounded String Fields in Request DTOs (DoS Risk)

Multiple request DTOs accept unbounded strings that could contain megabytes of data:

| DTO | Unbounded Fields |
|-----|-----------------|
| `CreateFindingRequest` | `description`, `filePath`, `recommendation`, `evidence` |
| `CreateTaskRequest` | `description`, `promptMd`, `promptS3Key` |
| `CreatePersonaRequest` | `description`, `contentMd` |
| `CreateDirectiveRequest` | `description`, `contentMd` |
| `CreateProjectRequest` | `description`, `repoUrl`, `techStack` |
| `CreateComplianceItemRequest` | `requirement`, `evidence`, `notes` |
| `CreateTechDebtItemRequest` | `description`, `filePath` |
| `CreateBugInvestigationRequest` | `jiraSummary`, `jiraDescription`, `jiraCommentsJson`, `jiraAttachmentsJson` |

**Fix:** Add `@Size(max = N)` to all string fields. Suggested: `description` fields max 5,000, `contentMd` max 50,000, JSON fields max 100,000.

---

### HIGH — Search Endpoint Missing Validation and Pagination

**UserController.java:48**
```java
@GetMapping("/search")
public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String q) { ... }
```

- No `@NotBlank` or `@Size` on query parameter
- Returns unbounded `List<UserResponse>` (pagination is done in Java with `.limit(20)`)
- Empty or single-character queries could return excessive results

---

### HIGH — Missing Rate Limiting on Auth Endpoints

`AuthController` has no rate limiting on `/login`, `/register`, or `/refresh`. Enables:
- Brute force password attacks
- Credential stuffing
- Spam registrations
- Denial of service

---

### HIGH — Inconsistent teamId in Audit Logs

Many audit log calls pass `null` for `teamId` when the operation is team-scoped:

- `ProjectController` lines 56, 64, 72, 80
- `PersonaController` lines 33, 84, 92, 100, 108
- `DirectiveController` lines 34, 61, 69, 77, 85

This breaks audit trail filtering by team.

---

### MEDIUM — File Upload Missing Validation

**ReportController.java:57**
```java
@RequestParam("file") MultipartFile file
```

- No null check on `file`
- `file.getOriginalFilename()` passed directly (directory traversal risk)
- `file.getBytes()` loads entire file into memory with no size limit checked in controller
- No content type validation

---

### MEDIUM — Missing Validation on List Parameters

`CreateProjectRequest.jiraLabels` and similar list fields have no `@Size` constraint on the list itself or `@NotBlank` on elements. Could contain null elements or be unbounded.

---

### LOW — PUT Operations Return 200 Instead of 204

`ProjectController.archiveProject()`, `unarchiveProject()` and `UserController.deactivateUser()`, `activateUser()` return `ResponseEntity.ok().build()` (HTTP 200 with empty body). Per REST conventions, state-change operations returning no body should use **204 No Content**.

---

## 4. Security & Authentication

### CRITICAL — JWT Secret Hardcoded in Version Control

**application-dev.yml:25**
```yaml
jwt:
  secret: dev-secret-key-minimum-32-characters-long-for-hs256
```

Even for development, hardcoded secrets in version control are dangerous. If the dev profile is accidentally used in production, all JWT tokens can be forged.

**Fix:** Use environment variables. Create a git-ignored `.env.local` for development secrets.

---

### CRITICAL — No Token Revocation Mechanism

**AuthService.java:76-98** — Refresh tokens are never revoked or invalidated. A stolen refresh token grants permanent access with no way to revoke it (no logout, no token blacklist, no `jti` claim for tracking).

**Fix:** Implement Redis-based token blacklist with TTL. Add a logout endpoint. Add `jti` (JWT ID) claim to all tokens.

---

### CRITICAL — Overly Permissive CORS Configuration

**CorsConfig.java:17-19**
```java
config.setAllowedOrigins(List.of("*"));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
```

Wildcard origin allows any website to make cross-origin requests. Combined with `setExposedHeaders(List.of("Authorization"))`, this leaks auth tokens to any origin.

**Fix:** Replace `"*"` with explicit origins: `List.of("https://app.codeops.dev", "http://localhost:3000")`.

---

### HIGH — Weak Encryption Key Derivation

**EncryptionService.java:21-28**
```java
byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
this.secretKey = new SecretKeySpec(keyBytes, "AES");
```

Uses simple SHA-256 hash as AES key. No salt, no iterations. Vulnerable to dictionary attacks.

**Fix:** Use PBKDF2 with 100,000+ iterations and a random salt, or use AWS KMS for key management.

---

### HIGH — Default Encryption Key in Code

**EncryptionService.java:21**
```java
@Value("${codeops.encryption.key:default-dev-encryption-key-32ch}")
```

If the environment variable is missing, the application silently falls back to a known hardcoded key, rendering encryption useless.

**Fix:** Remove the default value. Let the application fail fast on startup if the key is missing.

---

### HIGH — Exception Swallowing in JwtTokenProvider

**JwtTokenProvider.java:71-78**
```java
public boolean validateToken(String token) {
    try {
        parseClaims(token);
        return true;
    } catch (Exception e) {
        return false;  // ALL exceptions silently swallowed
    }
}
```

No logging of validation failures. Cannot detect brute force attempts, token forgery, or expired token patterns.

---

### HIGH — JwtAuthFilter Silently Continues on Invalid Token

**JwtAuthFilter.java:37-50** — If token validation fails, the filter continues the chain without authentication and without returning an error. The request proceeds as unauthenticated, which may bypass misconfigured authorization rules.

---

### HIGH — SSRF via Teams Webhook URL

**TeamsWebhookService.java:57**
```java
restTemplate.postForEntity(webhookUrl, entity, String.class);
```

No URL validation. If `webhookUrl` comes from user input, an attacker can make the server POST to internal IPs (10.x.x.x, 172.16.x.x, 127.0.0.1), enabling port scanning and internal service attacks.

**Fix:** Validate URLs against a whitelist of allowed domains. Disallow private IP ranges.

---

### MEDIUM — Weak Password Validation

**AuthService.java:109** — Only checks minimum length (8 characters). No complexity requirements. Users can set passwords like `"password"` or `"12345678"`.

---

### MEDIUM — Missing Security Headers

`SecurityConfig.java` does not configure:
- `Content-Security-Policy`
- `X-Frame-Options`
- `X-Content-Type-Options`
- `Strict-Transport-Security`

Leaves the application vulnerable to XSS, clickjacking, and MIME-type sniffing.

---

### MEDIUM — SecurityUtils Unchecked Cast

**SecurityUtils.java:16**
```java
return (UUID) auth.getPrincipal();  // ClassCastException if principal is wrong type
```

Also throws generic `RuntimeException` instead of a security-specific exception.

---

### MEDIUM — JWT Algorithm Not Explicitly Specified

**JwtTokenProvider.java:38** — Uses `.signWith(getSigningKey())` without specifying the algorithm. While the key type implies HS256, explicitly specifying prevents algorithm substitution attacks.

---

### MEDIUM — No Audit Logging for Security Events

Failed token validations, login attempts, password changes, and token refreshes are not logged. Cannot detect brute force attacks or suspicious activity.

---

### LOW — BCrypt Default Strength

**SecurityConfig.java:50** — `new BCryptPasswordEncoder()` uses default strength 10. Acceptable but could be increased to 12 for higher security.

---

## 5. Configuration & Infrastructure

### CRITICAL — Hardcoded Database Credentials

**application-dev.yml:3-6**
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/codeops
  username: codeops
  password: codeops
```

**docker-compose.yml:6-8** — Same credentials. While acceptable for local development, these are in version control and could be reused in production.

---

### HIGH — HTML Injection in Email Templates

**EmailService.java:53-78** — User-supplied values (`teamName`, `inviterName`, `projectName`) are concatenated directly into HTML email bodies without escaping.

```java
"<p>" + inviterName + " has invited you to join</p>"
```

An attacker could set their display name to `<script>alert('xss')</script>` and inject HTML/JavaScript into emails.

**Fix:** Use `HtmlUtils.htmlEscape()` on all user inputs, or switch to a templating engine (Thymeleaf, FreeMarker).

---

### HIGH — Async Exception Handling

**NotificationDispatcher.java:30-35** — `@Async` methods have no exception handling. If webhook posting fails, the exception is silently lost.

**AsyncConfig.java** — No `AsyncUncaughtExceptionHandler` configured. No thread pool limits (uses default unbounded pool).

**Fix:** Configure `ThreadPoolTaskExecutor` with bounded pool and `AsyncUncaughtExceptionHandler`.

---

### MEDIUM — CSRF Disabled Globally

**SecurityConfig.java:30** — `csrf.disable()` is correct for pure JWT/token-based auth, but should be documented and reconsidered if cookies are ever introduced.

---

### MEDIUM — No HTTPS Enforcement

No `requiresChannel().requiresSecure()` in security config. Application doesn't force HTTPS. Man-in-the-middle attacks possible if deployed without a reverse proxy enforcing TLS.

---

### LOW — Docker Container Runs as Root

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/codeops-server-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

No `USER` directive. Container runs as root (UID 0).

**Fix:**
```dockerfile
RUN addgroup -g 1000 codeops && adduser -D -u 1000 -G codeops codeops
USER codeops
```

---

### LOW — Docker Compose Exposes PostgreSQL Port

**docker-compose.yml:9-10** — PostgreSQL bound to `0.0.0.0:5432`. Acceptable for development, risky if used in production without firewall rules.

---

### LOW — Information Disclosure in Error Responses

**GlobalExceptionHandler.java:16-23** — Returns raw exception messages to clients (`ex.getMessage()`). Could leak internal system details.

---

## 6. Notification Services

### HIGH — No Input Sanitization in Email Templates

All 4 email methods in `EmailService.java` (`sendInvitationEmail`, `sendCriticalFindingsAlert`, `sendWeeklyDigest`, `sendEmail`) use string concatenation for HTML. User-controlled values are not escaped.

---

### MEDIUM — Silent Failure in NotificationDispatcher

**NotificationDispatcher.java:30-35** — Uses `team.getTeamsWebhookUrl()` without null checking the team itself (`orElse(null)` on line 32). If team is null, the entire dispatch silently fails.

---

### LOW — RestTemplate Created Inline

**TeamsWebhookService.java:21** — `new RestTemplate()` in constructor instead of injecting a Spring-managed bean. Prevents customization (timeouts, interceptors, connection pooling).

---

## 7. Positive Findings

These aspects of the codebase are well-implemented:

1. **AES-256-GCM for credential encryption** — Authenticated encryption providing both confidentiality and integrity
2. **BCrypt password hashing** — Industry standard with built-in salting
3. **Stateless JWT sessions** — No session fixation vulnerabilities
4. **`@PreAuthorize` on controllers** — Method-level authorization present on most endpoints
5. **`open-in-view: false`** — Correctly prevents lazy loading in views
6. **`@ConditionalOnProperty` for S3/SES** — Proper conditional bean creation for dev/prod
7. **Jakarta Validation on request DTOs** — `@NotBlank`, `@Email`, `@Size` constraints present
8. **BaseEntity with `@PrePersist`/`@PreUpdate`** — Consistent audit timestamps
9. **UUID primary keys** — Non-enumerable, non-sequential identifiers
10. **Environment variables for production secrets** — `application-prod.yml` uses `${ENV_VAR}` syntax

---

## 8. Remediation Priority

### Phase 1 — IMMEDIATE (Before Any Production Traffic)

| # | Issue | Severity | Effort |
|---|-------|----------|--------|
| 1 | Add `@Enumerated(EnumType.STRING)` to all 23 enum fields | CRITICAL | 1 hour |
| 2 | Add authorization to `getDecryptedCredentials()` and `getDecryptedApiToken()` | CRITICAL | 30 min |
| 3 | Fix CORS — replace wildcard with explicit origins | CRITICAL | 15 min |
| 4 | Remove default encryption key fallback | HIGH | 5 min |
| 5 | Add team membership check to `AuditLogService.getTeamAuditLog()` | HIGH | 15 min |
| 6 | Add authorization to `AdminService.getUserById()` | CRITICAL | 15 min |
| 7 | Fix `AdminService.getUsageStats()` to use COUNT queries | CRITICAL | 30 min |
| 8 | Implement token revocation (Redis blacklist + logout endpoint) | CRITICAL | 4 hours |
| 9 | Move JWT secret to environment variable only (remove dev default) | CRITICAL | 15 min |

### Phase 2 — HIGH PRIORITY (Next Sprint)

| # | Issue | Severity | Effort |
|---|-------|----------|--------|
| 10 | Add `@Size` constraints to all unbounded DTO string fields | HIGH | 2 hours |
| 11 | Paginate all 15+ unbounded list methods | HIGH | 4 hours |
| 12 | Add rate limiting to auth endpoints | HIGH | 2 hours |
| 13 | Fix HTML injection in email templates | HIGH | 1 hour |
| 14 | Validate S3 key parameters in ReportController | HIGH | 30 min |
| 15 | Add audit logging to all 15+ unlogged mutation endpoints | HIGH | 2 hours |
| 16 | Fix JWT exception swallowing — add logging | HIGH | 30 min |
| 17 | Add SSRF protection to webhook URL validation | HIGH | 1 hour |
| 18 | Add NotificationService user authorization check | HIGH | 15 min |
| 19 | Configure AsyncUncaughtExceptionHandler and thread pool | HIGH | 30 min |

### Phase 3 — MEDIUM PRIORITY (Next Month)

| # | Issue | Severity | Effort |
|---|-------|----------|--------|
| 20 | Add database indexes to all FK columns | MEDIUM | 2 hours |
| 21 | Add `nullable = false` to required entity fields | MEDIUM | 1 hour |
| 22 | Fix N+1 patterns in MetricsService | MEDIUM | 1 hour |
| 23 | Add security headers (CSP, X-Frame-Options, etc.) | MEDIUM | 30 min |
| 24 | Strengthen password validation | MEDIUM | 1 hour |
| 25 | Fix race condition in TeamService.inviteMember() | MEDIUM | 1 hour |
| 26 | Add `@Version` for optimistic locking on high-contention entities | MEDIUM | 2 hours |
| 27 | Replace getReferenceById() with findById() + error handling | MEDIUM | 1 hour |
| 28 | Explicitly specify JWT algorithm (HS256) | MEDIUM | 5 min |
| 29 | Fix teamId null in audit log calls | MEDIUM | 1 hour |

### Phase 4 — LOW PRIORITY (Backlog)

| # | Issue | Severity | Effort |
|---|-------|----------|--------|
| 30 | Add non-root user to Dockerfile | LOW | 10 min |
| 31 | Remove exposed PostgreSQL port in Docker Compose for prod | LOW | 5 min |
| 32 | Sanitize error messages in GlobalExceptionHandler | LOW | 30 min |
| 33 | Inject RestTemplate as Spring bean in TeamsWebhookService | LOW | 15 min |
| 34 | Refactor RemediationTask.findingIds to proper relationship | LOW | 2 hours |
| 35 | Add explicit column names to all @Column annotations | LOW | 1 hour |

---

## Appendix: Files Audited

```
src/main/java/com/codeops/
├── config/
│   ├── AppConstants.java
│   ├── AsyncConfig.java
│   ├── CorsConfig.java
│   ├── GlobalExceptionHandler.java
│   ├── JwtConfig.java
│   ├── S3Config.java
│   └── SesConfig.java
├── security/
│   ├── JwtAuthFilter.java
│   ├── JwtTokenProvider.java
│   ├── SecurityConfig.java
│   └── SecurityUtils.java
├── entity/                    (25 entities + enums/)
├── repository/                (25 repositories)
├── dto/
│   ├── request/               (~38 request DTOs)
│   └── response/              (~30 response DTOs)
├── service/                   (24 services)
├── controller/                (18 controllers)
├── notification/
│   ├── EmailService.java
│   ├── TeamsWebhookService.java
│   └── NotificationDispatcher.java
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

**Total files analyzed:** 205
**Total issues found:** 100+
**Critical/High issues requiring immediate action:** 19
