# CodeOps-Server — Comprehensive Security & Code Audit

**Audit Date:** February 13, 2026
**Auditor:** Claude Code (Automated Static Analysis — 3 parallel agents)
**Scope:** Full codebase — 25 entities, 25 repositories, 24 services, 18 controllers, ~68 DTOs, security config, notification services, infrastructure (Docker, pom.xml)
**Total Files Analyzed:** 208

---

## Overall Verdict

```
╔═══════════════════════════════════════════════════════════════╗
║                      VERDICT: FAIL                           ║
║                                                              ║
║  The codebase is NOT production-ready.                       ║
║                                                              ║
║  9 CRITICAL issues must be resolved before any production    ║
║  traffic. 18 HIGH issues should be resolved within the       ║
║  first sprint. The application has exploitable               ║
║  vulnerabilities (path traversal, rate limit bypass,         ║
║  token blacklist memory leak) and data integrity risks       ║
║  (race conditions, missing cascade deletes).                 ║
║                                                              ║
║  Positive: Strong foundations (AES-256-GCM encryption,       ║
║  BCrypt hashing, UUID PKs, stateless JWT, proper             ║
║  @PreAuthorize usage, Hibernate open-in-view disabled).      ║
╚═══════════════════════════════════════════════════════════════╝
```

---

## Summary Statistics

| Severity | Count | Description |
|----------|-------|-------------|
| **CRITICAL** | 9 | Exploitable vulnerabilities, data corruption risks, credential exposure |
| **HIGH** | 18 | Missing authorization, unbounded queries, no tests, DoS vectors |
| **MEDIUM** | 20 | Race conditions, missing observability, incomplete configs |
| **LOW** | 11 | Validation gaps, naming inconsistencies, minor Docker issues |
| **INFO** | 5 | Positive findings / compliant patterns |
| **Total** | **63** | Unique findings after deduplication across 3 audit agents |

---

## Audit Scope & Methodology

Three specialized agents audited the codebase in parallel:

| Agent | Focus Areas | Findings |
|-------|-------------|----------|
| **Security** | Authentication, authorization, input validation, secrets management, OWASP Top 10 | 15 |
| **Data Layer** | Entities, repositories, services, DTOs, JPA configuration, transactional boundaries | 24 |
| **Infrastructure/Ops** | API design, async config, Docker, observability, resilience, deployment readiness | 30 |

Overlapping findings were merged (e.g., token blacklist identified by both Security and Infra agents). Each finding includes location, description, impact, and suggested fix.

---

## Table of Contents

1. [CRITICAL Findings (9)](#critical-findings)
2. [HIGH Findings (18)](#high-findings)
3. [MEDIUM Findings (20)](#medium-findings)
4. [LOW Findings (11)](#low-findings)
5. [INFO — Positive Findings (5)](#info--positive-findings)
6. [Remediation Priority & Phasing](#remediation-priority--phasing)

---

## CRITICAL Findings

### C-1: Path Traversal Vulnerability in File Upload

**Agents:** Security, Infrastructure
**Location:** `ReportController.java:75`, `ReportStorageService.java:53-54`

**Description:** The `uploadSpecification` endpoint uses `file.getOriginalFilename()` directly without sanitization, concatenating it into an S3 key path. An attacker can upload a file named `../../evil.txt` to write files outside the intended directory.

**Impact:** Arbitrary file write in S3 or local filesystem (dev fallback). Could overwrite critical files or upload malicious payloads.

**Fix:**
```java
String safeName = UUID.randomUUID() + "_" +
    fileName.replaceAll("[^a-zA-Z0-9._-]", "");
```

---

### C-2: IP Spoofing Bypasses Rate Limiting

**Agents:** Security, Infrastructure
**Location:** `RateLimitingFilter.java:46-51`

**Description:** The rate limiter trusts the `X-Forwarded-For` header without validation. Any client can spoof arbitrary IPs to bypass the 10-requests-per-minute limit on authentication endpoints.

**Impact:** Complete bypass of brute-force protection. Unlimited credential guessing attacks against `/api/v1/auth/login`.

**Fix:**
- Only trust `X-Forwarded-For` when behind a known proxy (configure trusted proxies via property)
- Add per-username rate limiting in addition to IP-based limiting
- Consider using Spring's `ForwardedHeaderFilter` with explicit proxy configuration

---

### C-3: Token Blacklist — Memory Leak and No Persistence

**Agents:** Security, Infrastructure
**Location:** `TokenBlacklistService.java:9-22`

**Description:** Two compounding issues:
1. **Memory leak:** Tokens are added to `ConcurrentHashMap.newKeySet()` but never removed. Over time (30+ day refresh tokens), the set grows unbounded.
2. **No persistence:** On server restart, all blacklisted tokens become valid again. In a distributed deployment, other instances won't know about blacklisted tokens.

**Impact:** Memory exhaustion (eventual OOM crash). Logout is not durable — restarting the server un-revokes all tokens. Attackers can accelerate exhaustion by generating many refresh tokens.

**Fix:**
```java
// 1. Use Map with expiry tracking
private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

// 2. Add scheduled cleanup
@Scheduled(fixedRate = 3600000) // Every hour
public void cleanExpiredTokens() {
    Instant now = Instant.now();
    blacklistedTokens.entrySet().removeIf(e -> e.getValue().isBefore(now));
}

// 3. For production: Replace with Redis SET with TTL per token
```

---

### C-4: Default JWT Secret in Dev Config

**Agents:** Security, Infrastructure
**Location:** `application-dev.yml:25`

**Description:**
```yaml
codeops:
  jwt:
    secret: ${JWT_SECRET:dev-secret-key-minimum-32-characters-long-for-hs256}
```
If `JWT_SECRET` environment variable is not set, the application silently uses a hardcoded default. Anyone who reads this file can forge valid JWT tokens.

**Impact:** Complete authentication bypass. Token forgery allows impersonation of any user.

**Fix:** Remove the default value. Fail fast on startup if `JWT_SECRET` is not set:
```yaml
codeops:
  jwt:
    secret: ${JWT_SECRET}  # No default — required
```

---

### C-5: Hardcoded Database Credentials in docker-compose.yml

**Agents:** Infrastructure
**Location:** `docker-compose.yml:5-8`

**Description:**
```yaml
POSTGRES_DB: codeops
POSTGRES_USER: codeops
POSTGRES_PASSWORD: codeops
```
Credentials are identical and hardcoded in version control. Any fork of the repo exposes them.

**Impact:** If the repo becomes public, database credentials are exposed. Even in private repos, credentials should not be in version control.

**Fix:** Use a `.env` file (add to `.gitignore`) or secrets management (Kubernetes Secrets, AWS Secrets Manager).

---

### C-6: Race Condition on Persona Default Assignment

**Agent:** Data Layer
**Location:** `PersonaService.java:68-70, 140-144, 168-169, 185-190`

**Description:** The `clearExistingDefault()` method reads a persona, sets `isDefault=false`, and saves — with no locking mechanism. Concurrent requests to set different personas as default can result in multiple personas marked as default simultaneously.

**Impact:** Data integrity violation — multiple "default" personas for the same team/agent type, producing unpredictable behavior.

**Fix:** Use a unique partial index in PostgreSQL:
```sql
CREATE UNIQUE INDEX idx_persona_default
ON personas (team_id, agent_type)
WHERE is_default = true;
```
Or use pessimistic locking: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the query.

---

### C-7: Missing Cascade Delete on RemediationTask-Finding Join Table

**Agent:** Data Layer
**Location:** `RemediationTask.java:41-48`

**Description:** `RemediationTask` has a `@ManyToMany` relationship with `Finding` using a join table, but no `cascade` configuration. Deleting a remediation task leaves orphaned records in the `remediation_task_findings` join table.

**Impact:** Data bloat and potential referential integrity issues over time.

**Fix:**
```java
@ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
@JoinTable(...)
private List<Finding> findings = new ArrayList<>();
```
Note: Never use `CascadeType.REMOVE` for ManyToMany.

---

### C-8: No Orphan Deletion Strategy for ProjectDirective

**Agent:** Data Layer
**Location:** `ProjectDirective.java:18-26`, `DirectiveService.java:132`

**Description:** When a Directive is deleted, `DirectiveService` manually deletes `ProjectDirective` records. But when a Project is deleted, orphan `ProjectDirective` records remain — no cascade delete is configured from the Project side.

**Impact:** Orphaned mapping records accumulate, wasting storage and producing stale data in queries.

**Fix:** Add cascade delete to the Project entity's relationship, or add cleanup logic in `ProjectService.deleteProject()`.

---

### C-9: Encryption Error Handling — Silent Misconfiguration

**Agent:** Data Layer
**Location:** `GitHubConnectionService.java:76-87`, `EncryptionService.java:21-28`

**Description:** `getDecryptedCredentials()` doesn't validate the encryption key configuration. If `codeops.encryption.key` is unset or empty, the `EncryptionService` throws a generic `RuntimeException`. Callers cannot distinguish between "credentials don't exist" vs "encryption is broken."

**Impact:** Silent encryption failures. Credentials may appear to not exist when in reality decryption is misconfigured.

**Fix:**
```java
public String getDecryptedCredentials(UUID connectionId) {
    // ... existing validation ...
    try {
        return encryptionService.decrypt(connection.getEncryptedCredentials());
    } catch (RuntimeException e) {
        throw new EncryptionException("Failed to decrypt credentials for connection " + connectionId, e);
    }
}
```

---

## HIGH Findings

### H-1: Unsafe Token Parsing in Logout Endpoint

**Agent:** Security
**Location:** `AuthController.java:54-62`

**Description:** The logout endpoint calls `jwtTokenProvider.parseClaims(token)` without first calling `validateToken()`. If the token is expired or malformed, `parseClaims()` throws an exception that may leak information.

**Impact:** Information disclosure via error messages. Potential for unexpected exceptions on malformed tokens.

**Fix:** Validate before parsing:
```java
if (!jwtTokenProvider.validateToken(token)) {
    throw new IllegalArgumentException("Invalid token");
}
Claims claims = jwtTokenProvider.parseClaims(token);
```

---

### H-2: Information Disclosure in Error Responses

**Agents:** Security, Infrastructure
**Location:** `GlobalExceptionHandler.java:27, 43-47`

**Description:** The `IllegalArgumentException` handler returns `ex.getMessage()` directly to the client. Messages like `"Email already registered"`, `"User not found"`, and `"Not a member of this team"` leak internal logic and enable account enumeration.

**Impact:** Attackers can enumerate valid accounts, learn business logic, and gather information for targeted attacks.

**Fix:** Log the actual message internally and return generic messages:
```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException ex) {
    log.warn("Validation error: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
}
```

---

### H-3: Missing File Upload Authorization Check

**Agent:** Security
**Location:** `ReportController.java:69-77`

**Description:** The specification upload endpoint accepts files without verifying the current user has access to the target `jobId`. File type is not validated (only the `Content-Type` header is trusted). No file size validation at the controller level.

**Impact:** Attackers can upload files to jobs they don't own. Arbitrary file types can be uploaded by spoofing MIME types.

**Fix:**
- Validate jobId ownership before accepting the file
- Restrict file extensions to an allowlist (`.pdf`, `.docx`, `.txt`, `.md`)
- Validate actual file content (magic bytes), not just headers
- Enforce `AppConstants.MAX_REPORT_SIZE_MB` at the controller level

---

### H-4: No Request Body Size Validation on Report Uploads

**Agent:** Security
**Location:** `ReportController.java:40-46, 50-57`

**Description:** Report upload endpoints accept `@RequestBody String markdownContent` without size validation. `AppConstants.MAX_REPORT_SIZE_MB = 25` exists but is not enforced for string uploads. An attacker can submit extremely large payloads.

**Impact:** Memory exhaustion (OOM), storage exhaustion, potential XSS through stored malicious markdown.

**Fix:**
```java
if (markdownContent.length() > AppConstants.MAX_REPORT_SIZE_MB * 1024 * 1024) {
    throw new IllegalArgumentException("Report exceeds maximum size");
}
```

---

### H-5: Missing Batch Endpoint Size Validation

**Agent:** Infrastructure
**Location:** `JobController.java:106-108`, `FindingController.java:46-52`, `TaskController.java:41-47`, `TechDebtController.java:42-46`, `DependencyController.java:75-81`, `ComplianceController.java:63-69`

**Description:** Batch endpoints accept `List<T>` request bodies without size constraints. No batches are bounded to `AppConstants.MAX_PAGE_SIZE` or similar limits. Clients can POST thousands of items in a single request.

**Impact:** Memory exhaustion, database performance degradation, timeout cascades. Common DoS vector.

**Fix:**
```java
// On batch request DTOs:
@NotEmpty
@Size(max = 100, message = "Batch size cannot exceed 100 items")
private List<CreateFindingRequest> findings;
```

---

### H-6: Async Error Handling — Silent Audit Log Failures

**Agent:** Infrastructure
**Location:** `AsyncConfig.java:16-38`, `AuditLogService.java`

**Description:** `AuditLogService.log()` is `@Async` (fire-and-forget). The `AsyncConfig` exception handler only logs errors but doesn't persist them or alert. If audit log persistence fails, data loss is silent. `CallerRunsPolicy` will block the main thread if the queue overflows at 100 items.

**Impact:** Silent audit trail gaps violate compliance requirements. Under load, async rejection degrades request performance.

**Fix:**
1. Add a persistent dead-letter queue for failed audit logs
2. Make critical audit events synchronous (USER_LOGIN, ADMIN_ACTION)
3. Increase queue capacity or use `AbortPolicy` with monitoring

---

### H-7: Dockerfile Missing JVM Memory Configuration

**Agent:** Infrastructure
**Location:** `Dockerfile:1-8`

**Description:**
```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```
No JVM arguments for memory limits, GC tuning, or container-aware settings. The JVM will consume all available memory before OOM.

**Impact:** In Docker/Kubernetes, unbounded JVM memory causes container crashes and unstable behavior.

**Fix:**
```dockerfile
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0",
  "-Xms256m", "-Xmx1g", "-jar", "app.jar"]
```

---

### H-8: Missing Actuator Security Configuration

**Agent:** Infrastructure
**Location:** `SecurityConfig.java:34-39`

**Description:** `/api/v1/health` is correctly public for liveness probes. However, there is no explicit restriction on actuator endpoints. `/actuator/env`, `/actuator/metrics`, and `/actuator/beans` may expose sensitive configuration.

**Impact:** Actuator endpoints can leak system information, environment variables, and database credentials.

**Fix:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

---

### H-9: No Request Tracing / Correlation ID

**Agent:** Infrastructure
**Location:** Security and controller layers (globally absent)

**Description:** No correlation ID or `X-Request-ID` header is propagated through requests. Async operations and downstream calls cannot be traced end-to-end.

**Impact:** Production debugging is extremely difficult. Cannot correlate logs across async boundaries.

**Fix:** Add a servlet filter to generate/propagate `X-Request-ID` and include it in MDC for all logs.

---

### H-10: No Database Connection Pool Configuration

**Agent:** Infrastructure
**Location:** `application-dev.yml:2-15`, `application-prod.yml:2-10`

**Description:** No HikariCP pool configuration specified. Spring Boot defaults (max 10 connections) will exhaust under concurrent load. No retry policy or circuit breaker for database queries.

**Impact:** Under concurrent load, the app exhausts the connection pool and rejects requests.

**Fix:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

### H-11: No Timeout Configuration for External Services

**Agent:** Infrastructure
**Location:** `RestTemplateConfig.java:7-14`

**Description:**
```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();  // No timeouts
}
```
Calls to GitHub/Jira APIs will block indefinitely if those services hang.

**Impact:** Hanging requests exhaust the thread pool. Cascading failures across the application.

**Fix:**
```java
HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
factory.setConnectTimeout(5000);
factory.setReadTimeout(10000);
return new RestTemplate(factory);
```

---

### H-12: No Test Files Found

**Agent:** Infrastructure
**Location:** `src/test/` — empty

**Description:** The codebase has 208 Java source files but zero unit, integration, or security tests.

**Impact:** No verification that endpoints work. No regression safety net. No security boundary testing (IDOR, privilege escalation). Changes carry high risk of undetected breakage.

**Fix:** Implement in priority order:
1. Unit tests for services (mock repositories)
2. Integration tests with TestContainers for PostgreSQL
3. Security tests for auth bypass and IDOR
4. Load tests to verify connection pooling and async config

---

### H-13: Missing Index on Foreign Key Columns

**Agents:** Data Layer, Infrastructure
**Location:** `Team.java:21-23` (and 20+ other entities)

**Description:** No `@Index` annotations across all 25 entities. Frequently-queried FK columns (`project_id`, `team_id`, `job_id`, `user_id`, `scan_id`) will cause full table scans.

**Impact:** Query performance degradation as tables grow. Critical for `project_id` (used by 6+ entities) and `team_id` (used by 8+ entities).

**Fix:**
```java
@Entity
@Table(name = "teams", indexes = {
    @Index(name = "idx_team_owner_id", columnList = "owner_id")
})
```
Apply to all entities with FK columns used in queries.

---

### H-14: Transactional Boundary Issue in TeamService.acceptInvitation()

**Agent:** Data Layer
**Location:** `TeamService.java:210-244`

**Description:** If an exception occurs after creating the `TeamMember` (line 238) but before updating the invitation status (line 240-241), the user becomes a team member but the invitation remains PENDING. On retry, duplicate team members could be created.

**Impact:** Data integrity — team membership created without corresponding invitation state update.

**Fix:** Update invitation status FIRST, then create team member:
```java
invitation.setStatus(InvitationStatus.ACCEPTED);
invitationRepository.save(invitation);  // Commit invitation state first
TeamMember member = TeamMember.builder()...build();
teamMemberRepository.save(member);
```

---

### H-15: Missing Unique Constraint on User.email

**Agent:** Data Layer
**Location:** `User.java:17`

**Description:** `User.email` has `unique = true` at the column level but no corresponding `@UniqueConstraint` in the `@Table` annotation. While the column constraint works, this is inconsistent and fragile across DB migrations.

**Impact:** Potential constraint issues during schema migration or DB engine changes.

**Fix:**
```java
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_email", columnNames = "email")
})
```

---

### H-16: Version Field Used Inconsistently Across Entities

**Agent:** Data Layer
**Location:** `Directive.java:49-50` vs `QaJob.java:87-89`

**Description:** `Directive` uses a manual `version` field (plain Integer, incremented in service code). `QaJob`, `Finding`, `TechDebtItem`, and `RemediationTask` use `@Version` for Hibernate optimistic locking. The manual version in Directive is not thread-safe (read-modify-write without atomicity).

**Impact:** Concurrent Directive updates can set the same version number. Mixed approach creates confusion.

**Fix:** Either add `@Version` to Directive for ORM concurrency control, or use a DB trigger for the manual version.

---

### H-17: Unbounded User Search — Multiple Compounding Issues

**Agents:** Security, Data Layer, Infrastructure
**Location:** `UserController.java:46-50`, `UserService.java:61-65`

**Description:** Three overlapping issues:
1. **No input validation** — `@RequestParam String q` has no `@Size` or `@NotBlank` constraint
2. **Loads all results into memory** — `findByDisplayNameContainingIgnoreCase()` returns ALL matches, then `.limit(20)` is applied in Java
3. **No team isolation** — any authenticated user can search all users system-wide (privacy violation, user enumeration)

**Impact:** Memory exhaustion with wildcard-like queries. Privacy violation. User enumeration attack vector.

**Fix:**
```java
// 1. Add validation
@GetMapping("/search")
public ResponseEntity<List<UserResponse>> searchUsers(
    @RequestParam @Size(min=2, max=100) String q) { ... }

// 2. Use database pagination
Page<User> findByDisplayNameContainingIgnoreCase(String search, Pageable pageable);

// 3. Restrict to same-team users
```

---

### H-18: Directive.version Manual Increment Not Thread-Safe

**Agent:** Data Layer
**Location:** `DirectiveService.java:120`

**Description:** Version is incremented with `directive.setVersion(directive.getVersion() + 1)` — a read-modify-write that is not atomic. Two concurrent updates could both read version N and both write version N+1.

**Impact:** Version tracking loses fidelity; audit trail becomes inaccurate.

**Fix:** Use `@Version` annotation for automatic optimistic locking, or use a database sequence.

---

## MEDIUM Findings

### M-1: Rate Limiting Only Covers Auth Endpoints

**Agents:** Security, Infrastructure
**Location:** `RateLimitingFilter.java:26`

**Description:** Rate limiting applies only to `/api/v1/auth/**`. Search endpoints, data-heavy queries, and administrative operations are completely unprotected. Combined with the user search issues (H-17), this enables rapid enumeration.

**Fix:** Extend rate limiting with tiered limits:
- Auth endpoints: 10/min
- Search endpoints: 30/min
- General API: 100/min

---

### M-2: Default Encryption Key in Application Config

**Agent:** Security
**Location:** `application-dev.yml:28-30`

**Description:** Default encryption key `dev-only-encryption-key-minimum-32ch` is in version control. If environment variable is not set in production, all credential encryption uses a known key.

**Fix:** Remove default value. Fail fast if missing:
```yaml
codeops:
  encryption:
    key: ${ENCRYPTION_KEY}  # No default
```

---

### M-3: Encryption Key Not Rotatable

**Agent:** Data Layer
**Location:** `EncryptionService.java:21-28`

**Description:** The encryption key is derived from a single property via SHA-256 digest. If the key is compromised, there's no mechanism to re-encrypt stored credentials with a new key.

**Impact:** Key rotation is a critical operational requirement; lack of support is a security gap.

**Fix:** Implement key versioning. Store the key version alongside encrypted data. Add a background migration task for re-encryption.

---

### M-4: Docker Compose Missing Health Checks

**Agent:** Infrastructure
**Location:** `docker-compose.yml:1-15`

**Description:** Neither the PostgreSQL container nor the application has a `healthcheck` directive. The app may start before PostgreSQL is ready, causing connection failures.

**Fix:**
```yaml
postgres:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U codeops"]
    interval: 10s
    timeout: 5s
    retries: 5
```

---

### M-5: Thread Pool Configuration Undersized

**Agent:** Infrastructure
**Location:** `AsyncConfig.java:22-25`

**Description:** Core pool=5, max=20, queue=100. Under concurrent load, only 125 operations can be queued. The 126th triggers `CallerRunsPolicy`, blocking the calling thread.

**Fix:** Increase to core=10, max=50, queue=500 for production. Make configurable via environment variables.

---

### M-6: No Graceful Shutdown Configuration

**Agent:** Infrastructure
**Location:** Global configuration (absent)

**Description:** No `server.shutdown` or `spring.lifecycle.timeout-per-shutdown-phase` configured. When the container receives SIGTERM, active requests may be killed mid-operation.

**Fix:**
```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

---

### M-7: Minimal Structured Logging

**Agent:** Infrastructure
**Location:** Across codebase (only 11 explicit logger calls found)

**Description:** Most operations are audited via the async `AuditLogService` but don't produce real-time logs. No JSON logging format. No MDC for request correlation.

**Fix:** Add structured JSON logging for all significant operations. Use MDC to correlate requests.

---

### M-8: No Metrics Endpoint

**Agent:** Infrastructure
**Location:** Configuration (absent)

**Description:** No Spring Boot Micrometer metrics exposed. Cannot monitor request latencies, connection pool stats, or JVM health.

**Fix:** Enable Micrometer and expose via `/actuator/metrics` (restricted to authorized users).

---

### M-9: CORS May Be Too Permissive in Production

**Agent:** Infrastructure
**Location:** `CorsConfig.java:15-32`

**Description:** Dev config uses `localhost` origins. No runtime validation prevents wildcard (`*`) in production. No startup check to reject wildcard origins.

**Fix:** Add startup validation to reject wildcard `*` in non-dev profiles.

---

### M-10: Inconsistent HTTP Status Codes

**Agents:** Infrastructure
**Location:** `UserController.java:57,65`, `ProjectController.java:74,83`, `AuthController.java:68`

**Description:** State-change operations (deactivate, activate, archive, password change) return `200 OK` with empty body instead of `204 No Content`. Inconsistent across the API.

**Fix:** Replace `ResponseEntity.ok().build()` with `ResponseEntity.noContent().build()` for all state changes that return no body.

---

### M-11: Improper Route Ordering in JobController

**Agent:** Infrastructure
**Location:** `JobController.java:61, 70`

**Description:** `@GetMapping("/mine")` is defined after `@GetMapping("/{jobId}")`. Spring may match "mine" as a job ID before reaching the `/mine` endpoint.

**Fix:** Reorder route definitions so specific paths (`/mine`) appear before generic ones (`/{jobId}`).

---

### M-12: AuditLog Missing @PrePersist for createdAt

**Agent:** Data Layer
**Location:** `AuditLog.java`

**Description:** `AuditLog` sets `createdAt` manually in `AuditLogService.log()`, but has no `@PrePersist` fallback. If a code path saves an `AuditLog` without setting `createdAt`, it will be null.

**Fix:**
```java
@PrePersist
protected void onCreate() {
    if (createdAt == null) {
        createdAt = Instant.now();
    }
}
```

---

### M-13: HealthSchedule.nextRunAt Nullable Without Documentation

**Agent:** Data Layer
**Location:** `HealthSchedule.java:41-42`, `HealthMonitorService.java:202`

**Description:** `nextRunAt` is nullable, and `calculateNextRun()` returns null for `ON_COMMIT` schedule types. Queries filtering by `nextRunAt IS NOT NULL` will exclude `ON_COMMIT` schedules unexpectedly.

**Fix:** Document null as valid for `ON_COMMIT`, or always calculate a time and use a boolean flag for commit-triggered schedules.

---

### M-14: Missing Pagination in AdminService.getAllSettings()

**Agent:** Data Layer
**Location:** `AdminService.java:94-97`

**Description:** Returns all system settings without pagination. Large deployments could have thousands of settings loaded into memory.

**Fix:** Add `Pageable` parameter and return `Page<SystemSettingResponse>`.

---

### M-15: No Validation on Project.healthScore Range

**Agent:** Data Layer
**Location:** `Project.java:64`

**Description:** `healthScore` is an `Integer` with no range constraints. Values outside 0-100 can be stored, breaking health scoring semantics.

**Fix:**
```java
@Column(name = "health_score", columnDefinition = "integer default 0")
@Min(0)
@Max(100)
private Integer healthScore;
```

---

### M-16: Missing ComplianceItem agent_type Index

**Agent:** Data Layer
**Location:** `ComplianceItem.java:9-11`

**Description:** Only `job_id` is indexed, but queries by `agent_type` perform full table scans.

**Fix:** Add `@Index(name = "idx_compliance_agent_type", columnList = "agent_type")`.

---

### M-17: Race Condition on QaJob Status Updates

**Agent:** Data Layer
**Location:** `QaJobService.java:95-118`

**Description:** QaJob has `@Version` for optimistic locking, but the update method doesn't handle `OptimisticLockException`. Concurrent status updates are silently lost or cause unhandled exceptions.

**Fix:** Wrap updates in try-catch for `OptimisticLockException` with retry or graceful error.

---

### M-18: Large Credential Size Limits in DTOs

**Agent:** Security
**Location:** `CreateGitHubConnectionRequest.java`, `CreateJiraConnectionRequest.java`

**Description:** `@Size` limits allow up to 10,000 characters for credentials and API tokens. A 10KB credential could result in ~100KB encrypted payload.

**Fix:** Reduce to realistic values: 2000 for credentials, 1000 for API tokens.

---

### M-19: Dockerfile Non-Root User Incomplete

**Agent:** Infrastructure
**Location:** `Dockerfile:2, 6`

**Description:** While a non-root user `appuser` is created, the `WORKDIR /app` permissions are not restricted. Other containers on the same host could read/write the application directory.

**Fix:**
```dockerfile
RUN chown appuser:appgroup /app && chmod 750 /app
RUN chmod 640 app.jar
```

---

### M-20: AWS SDK Version Not Monitored

**Agent:** Infrastructure
**Location:** `pom.xml:76-84`

**Description:** AWS SDK pinned to `2.25.0`. No automated dependency scanning (no Dependabot, no GitHub Actions workflow for CVE detection).

**Fix:** Enable Dependabot or Maven Security Scanner. Schedule monthly dependency updates.

---

## LOW Findings

### L-1: Missing Content-Type Enforcement on Reports

**Agent:** Infrastructure
**Location:** `ReportController.java:40-57`

**Description:** POST endpoints for markdown/text uploads don't declare `consumes` or validate Content-Type. Raw String body is accepted without constraints.

**Fix:** Add `consumes = {"text/markdown", "text/plain"}` to `@PostMapping`.

---

### L-2: Missing Swagger Annotations on Admin Endpoints

**Agent:** Infrastructure
**Location:** `AdminController.java:36-90`

**Description:** `@Tag(name = "Admin")` is present, but individual methods lack `@Operation`, `@ApiResponse`, and `@Parameter` annotations.

**Fix:** Add comprehensive Swagger annotations documenting success/error responses.

---

### L-3: Missing @Transactional(readOnly=true)

**Agent:** Infrastructure
**Location:** Service layer classes (inconsistent)

**Description:** Not all read-only methods are marked `@Transactional(readOnly = true)`. This misses Hibernate dirty-checking optimizations.

**Fix:** Apply `@Transactional(readOnly = true)` to all service methods that don't mutate data.

---

### L-4: Audit Log Actions Use Plain Strings

**Agent:** Infrastructure
**Location:** All controllers (e.g., `ProjectController.java:37`)

**Description:** `auditLogService.log(..., "PROJECT_CREATED", "PROJECT", ...)` uses raw strings. Typos go undetected at compile time.

**Fix:** Create enums: `enum AuditAction { PROJECT_CREATED, ... }` and `enum AuditEntityType { PROJECT, ... }`.

---

### L-5: DependencyScan Fields Not Validated

**Agent:** Data Layer
**Location:** `DependencyScan.java:28-29`

**Description:** `totalDependencies`, `outdatedCount`, `vulnerableCount` have no constraints. Negative values or inconsistent values (outdated > total) can be stored.

**Fix:** Add `@Min(0)` to all count fields.

---

### L-6: UserResponse.lastLoginAt Could Be Null

**Agent:** Data Layer
**Location:** `UserResponse.java`

**Description:** `lastLoginAt` (Instant) is nullable in the entity but this isn't documented or handled in the response DTO.

**Fix:** Document nullability in the DTO or use `Optional<Instant>`.

---

### L-7: Finding.lineNumber Has No Validation

**Agent:** Data Layer
**Location:** `Finding.java:42-43`

**Description:** `lineNumber` is `Integer` with no constraints. Negative or zero line numbers could be stored.

**Fix:** Add `@Min(1)`.

---

### L-8: Team.settingsJson No Schema Validation

**Agent:** Data Layer
**Location:** `Team.java:28-29`

**Description:** `settingsJson` is stored as plain TEXT with no JSON schema validation. Invalid JSON causes runtime deserialization errors.

**Fix:** Add service-level JSON validation before saving. Consider `@Column(columnDefinition = "jsonb")` for PostgreSQL.

---

### L-9: HSTS includeSubDomains May Be Premature

**Agent:** Security
**Location:** `SecurityConfig.java:49-52`

**Description:** HSTS configured with `includeSubDomains(true)`, which could make non-HTTPS subdomains inaccessible.

**Fix:** Verify all subdomains support HTTPS before enabling. Consider adding `preload = true` for HSTS preload list.

---

### L-10: CSRF Disabled Without Documentation

**Agents:** Security, Infrastructure
**Location:** `SecurityConfig.java:31`

**Description:** CSRF is disabled globally. Acceptable for stateless JWT APIs (no session cookies), but the decision isn't documented in code.

**Fix:** Add explanatory comment:
```java
// CSRF disabled: stateless JWT auth means no session cookies.
// Tokens sent in Authorization header are immune to CSRF.
.csrf(csrf -> csrf.disable())
```

---

### L-11: Limited Error Response Detail

**Agent:** Infrastructure
**Location:** `GlobalExceptionHandler.java:43-47`

**Description:** Generic exception handler returns `"Internal server error"` without an error code or correlation ID. Clients cannot reference specific errors in support tickets.

**Fix:** Return a structured error with a unique error ID:
```java
String errorId = UUID.randomUUID().toString();
log.error("Error {}: {}", errorId, ex.getMessage(), ex);
return ResponseEntity.status(500).body(
    Map.of("error", "Internal server error", "errorId", errorId));
```

---

## INFO — Positive Findings

The following aspects of the codebase are well-implemented:

| # | Finding | Details |
|---|---------|---------|
| I-1 | **AES-256-GCM for credential encryption** | Authenticated encryption providing confidentiality and integrity |
| I-2 | **BCrypt password hashing with salting** | Industry standard, built-in salt |
| I-3 | **UUID primary keys** | Non-enumerable, non-sequential identifiers prevent IDOR enumeration |
| I-4 | **Consistent DI pattern** | All services use `@RequiredArgsConstructor` — clean constructor injection |
| I-5 | **Proper lazy loading** | All `@ManyToOne` relationships use `FetchType.LAZY`, preventing N+1 queries. `open-in-view: false` correctly prevents lazy loading in views |

Additional positive patterns observed:
- `@PreAuthorize` on controllers — method-level authorization present on most endpoints
- `@ConditionalOnProperty` for S3/SES — proper conditional bean creation for dev/prod
- Jakarta Validation on request DTOs — `@NotBlank`, `@Email`, `@Size` constraints present
- `BaseEntity` with `@PrePersist`/`@PreUpdate` — consistent audit timestamps
- Environment variables for production secrets — `application-prod.yml` uses `${ENV_VAR}` syntax

---

## Remediation Priority & Phasing

### Phase 1 — IMMEDIATE (Before Any Production Traffic)

| # | Finding | Severity | Est. Effort |
|---|---------|----------|-------------|
| C-4 | Remove default JWT secret | CRITICAL | 15 min |
| C-5 | Move DB credentials to .env file | CRITICAL | 15 min |
| M-2 | Remove default encryption key | MEDIUM | 15 min |
| C-9 | Add encryption error handling | CRITICAL | 30 min |
| C-1 | Sanitize file upload filenames | CRITICAL | 30 min |
| C-2 | Fix IP spoofing in rate limiter | CRITICAL | 1 hour |
| C-3 | Fix token blacklist (cleanup + persistence) | CRITICAL | 2 hours |
| C-6 | Fix persona default race condition | CRITICAL | 1 hour |
| C-7 | Add cascade config to RemediationTask-Finding | CRITICAL | 30 min |
| C-8 | Add ProjectDirective orphan cleanup | CRITICAL | 30 min |

**Phase 1 total: ~6.5 hours**

### Phase 2 — HIGH PRIORITY (First Sprint)

| # | Finding | Severity | Est. Effort |
|---|---------|----------|-------------|
| H-2 | Sanitize error messages | HIGH | 30 min |
| H-3 | Add file upload authorization | HIGH | 1 hour |
| H-4 | Enforce report body size limit | HIGH | 30 min |
| H-5 | Add batch endpoint size limits | HIGH | 1 hour |
| H-6 | Fix async error handling | HIGH | 1 hour |
| H-7 | Add JVM memory config to Dockerfile | HIGH | 15 min |
| H-8 | Restrict actuator endpoints | HIGH | 15 min |
| H-10 | Configure HikariCP connection pool | HIGH | 30 min |
| H-11 | Add RestTemplate timeouts | HIGH | 30 min |
| H-13 | Add database indexes to FK columns | HIGH | 2 hours |
| H-14 | Fix acceptInvitation transactional order | HIGH | 30 min |
| H-17 | Fix user search (validation + pagination + team isolation) | HIGH | 2 hours |
| H-1 | Validate token before parsing in logout | HIGH | 15 min |
| H-9 | Add X-Request-ID correlation | HIGH | 1 hour |
| H-15 | Add unique constraint on User.email | HIGH | 15 min |
| H-16 | Standardize version field usage | HIGH | 1 hour |
| H-18 | Fix directive version thread safety | HIGH | 30 min |
| M-1 | Extend rate limiting to all endpoints | MEDIUM | 1 hour |

**Phase 2 total: ~14 hours**

### Phase 3 — MEDIUM PRIORITY (Next Month)

| # | Finding | Severity | Est. Effort |
|---|---------|----------|-------------|
| H-12 | Write tests (unit, integration, security) | HIGH | 40+ hours |
| M-3 | Implement encryption key rotation | MEDIUM | 4 hours |
| M-4 | Add Docker health checks | MEDIUM | 30 min |
| M-5 | Tune async thread pool | MEDIUM | 30 min |
| M-6 | Configure graceful shutdown | MEDIUM | 15 min |
| M-7 | Add structured JSON logging | MEDIUM | 2 hours |
| M-8 | Enable Micrometer metrics | MEDIUM | 1 hour |
| M-9 | Add CORS startup validation | MEDIUM | 30 min |
| M-10 | Fix HTTP status codes (204 for state changes) | MEDIUM | 30 min |
| M-11 | Fix route ordering in JobController | MEDIUM | 15 min |
| M-12 | Add @PrePersist to AuditLog | MEDIUM | 15 min |
| M-13 | Document/fix HealthSchedule.nextRunAt | MEDIUM | 15 min |
| M-14 | Paginate AdminService.getAllSettings | MEDIUM | 30 min |
| M-15 | Validate Project.healthScore range | MEDIUM | 15 min |
| M-16 | Add ComplianceItem agent_type index | MEDIUM | 15 min |
| M-17 | Handle QaJob OptimisticLockException | MEDIUM | 30 min |
| M-18 | Reduce credential size limits | MEDIUM | 15 min |
| M-19 | Tighten Dockerfile permissions | MEDIUM | 15 min |
| M-20 | Enable dependency vulnerability scanning | MEDIUM | 1 hour |

**Phase 3 total: ~52+ hours (driven by test coverage)**

### Phase 4 — BACKLOG

All LOW findings (L-1 through L-11). Estimated total: ~4 hours.

---

## Files Audited

```
src/main/java/com/codeops/
├── config/
│   ├── AppConstants.java
│   ├── AsyncConfig.java
│   ├── CorsConfig.java
│   ├── GlobalExceptionHandler.java
│   ├── JwtConfig.java
│   ├── RestTemplateConfig.java
│   ├── S3Config.java
│   └── SesConfig.java
├── security/
│   ├── JwtAuthFilter.java
│   ├── JwtTokenProvider.java
│   ├── RateLimitingFilter.java
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

**Total files analyzed:** 208
**Total unique findings:** 63 (after deduplication across 3 agents)
**Critical + High requiring immediate action:** 27
