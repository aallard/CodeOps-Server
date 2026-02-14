# CodeOps Server — Claude Code Context

## What This Is
Cloud backend for CodeOps, an AI-powered software maintenance platform. Spring Boot 3.3, Java 21, PostgreSQL.

## Package Structure
- `com.codeops.config` — Configuration classes (AppConstants, CORS, JWT, S3, SES)
- `com.codeops.security` — JWT auth (JwtTokenProvider, JwtAuthFilter, SecurityConfig, SecurityUtils)
- `com.codeops.entity` — 25 JPA entities (BaseEntity superclass, enums in entity/enums/)
- `com.codeops.repository` — 25 Spring Data JPA repositories
- `com.codeops.dto.request` — Request DTOs (Java records with Jakarta Validation)
- `com.codeops.dto.response` — Response DTOs (Java records)
- `com.codeops.service` — Business logic (24 services)
- `com.codeops.controller` — REST controllers (18 controllers, ~140 endpoints)
- `com.codeops.notification` — Email (SES), Teams webhook, notification dispatch

## Conventions
- All entities extend BaseEntity (id, createdAt, updatedAt) except SystemSetting (String key PK), AuditLog (Long PK), and ProjectDirective (composite PK)
- All enums stored as @Enumerated(EnumType.STRING)
- DTOs are Java records — request DTOs use Jakarta Validation, response DTOs are plain records
- Services use @RequiredArgsConstructor for dependency injection
- Controllers use @PreAuthorize for authorization — team membership verified in service layer
- Entity field -> repository method: use {field}Id suffix for @ManyToOne FK lookups (e.g., findByStartedById not findByStartedBy)
- Passwords hashed with BCrypt
- Credentials (GitHub PAT, Jira API token) encrypted with AES-256-GCM via EncryptionService
- NEVER return decrypted credentials in API responses
- S3 disabled in dev -> local filesystem at ~/.codeops/storage/
- SES disabled in dev -> emails logged to console
- PageResponse<T> generic record for paginated responses
- AuditLogService.log() is @Async — fire and forget

## Auth
- JWT (HS256) — 24h access token, 30d refresh token
- Principal stored as UUID in SecurityContext
- SecurityUtils.getCurrentUserId() to get current user
- Roles: OWNER, ADMIN, MEMBER, VIEWER (team-scoped)

## Key Patterns
- All mutations verify team membership or admin/owner status
- Soft delete for connections (isActive=false), hard delete for most entities
- Job lifecycle: PENDING -> RUNNING -> COMPLETED/FAILED/CANCELLED
- Health scores: 0-100, tracked over time via snapshots
- Personas: SYSTEM (read-only built-in), TEAM (shared), USER (personal)
- Directives: assigned to projects via project_directives join table with enabled flag

## Running
```
docker-compose up -d       # Start Postgres
mvn spring-boot:run        # Start server on :8090
```

## API Docs
http://localhost:8090/swagger-ui/index.html
