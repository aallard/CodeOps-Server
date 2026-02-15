package com.codeops.config;

import com.codeops.entity.*;
import com.codeops.entity.enums.*;
import com.codeops.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Profile("dev")
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final PersonaRepository personaRepository;
    private final DirectiveRepository directiveRepository;
    private final ProjectDirectiveRepository projectDirectiveRepository;
    private final QaJobRepository qaJobRepository;
    private final BugInvestigationRepository bugInvestigationRepository;
    private final AgentRunRepository agentRunRepository;
    private final FindingRepository findingRepository;
    private final RemediationTaskRepository remediationTaskRepository;
    private final SpecificationRepository specificationRepository;
    private final ComplianceItemRepository complianceItemRepository;
    private final TechDebtItemRepository techDebtItemRepository;
    private final DependencyScanRepository dependencyScanRepository;
    private final DependencyVulnerabilityRepository dependencyVulnerabilityRepository;
    private final HealthScheduleRepository healthScheduleRepository;
    private final HealthSnapshotRepository healthSnapshotRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final AuditLogRepository auditLogRepository;

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // Shared references across seed methods
    private User adam, sarah, mike;
    private Team team;
    private Project serverProject, clientProject, locksmithProject;
    private List<Directive> directives;
    private QaJob auditJob1, complianceJob, bugJob, remediateJob, auditJob2, techDebtJob, depJob, runningJob;
    private List<Finding> savedFindings;
    private Specification apiSpec, markdownSpec;
    private DependencyScan serverScan, locksmithScan;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded — skipping");
            return;
        }

        seedUsers();
        seedTeam();
        seedTeamMembers();
        seedProjects();
        seedPersonas();
        seedDirectives();
        seedProjectDirectives();
        seedQaJobs();
        seedBugInvestigation();
        seedAgentRuns();
        seedFindings();
        seedRemediationTasks();
        seedSpecifications();
        seedComplianceItems();
        seedTechDebtItems();
        seedDependencyScans();
        seedDependencyVulnerabilities();
        seedHealthSchedules();
        seedHealthSnapshots();
        seedSystemSettings();
        seedAuditLog();

        log.info("Development data seeded successfully");
    }

    private void seedUsers() {
        String hash = passwordEncoder.encode("pass");
        adam = userRepository.save(User.builder()
                .email("adam@allard.com").passwordHash(hash).displayName("Adam Allard").build());
        sarah = userRepository.save(User.builder()
                .email("sarah@codeops.dev").passwordHash(hash).displayName("Sarah Chen").build());
        mike = userRepository.save(User.builder()
                .email("mike@codeops.dev").passwordHash(hash).displayName("Mike Torres").build());
        log.info("Seeded 3 users");
    }

    private void seedTeam() {
        team = teamRepository.save(Team.builder()
                .name("CodeOps Core")
                .description("Core engineering team for CodeOps platform development")
                .owner(adam)
                .build());
        log.info("Seeded 1 team");
    }

    private void seedTeamMembers() {
        Instant now = Instant.now();
        teamMemberRepository.saveAll(List.of(
                TeamMember.builder().team(team).user(adam).role(TeamRole.OWNER)
                        .joinedAt(now.minus(90, ChronoUnit.DAYS)).build(),
                TeamMember.builder().team(team).user(sarah).role(TeamRole.ADMIN)
                        .joinedAt(now.minus(60, ChronoUnit.DAYS)).build(),
                TeamMember.builder().team(team).user(mike).role(TeamRole.MEMBER)
                        .joinedAt(now.minus(30, ChronoUnit.DAYS)).build()
        ));
        log.info("Seeded 3 team members");
    }

    private void seedProjects() {
        serverProject = projectRepository.save(Project.builder()
                .team(team).name("CodeOps Server")
                .description("Spring Boot backend API for the CodeOps platform")
                .repoUrl("https://github.com/codeops-dev/codeops-server")
                .repoFullName("codeops-dev/codeops-server")
                .techStack("Java 21, Spring Boot 3.3, PostgreSQL 16")
                .healthScore(78).createdBy(adam).build());
        clientProject = projectRepository.save(Project.builder()
                .team(team).name("CodeOps Client")
                .description("Flutter web application for the CodeOps platform")
                .repoUrl("https://github.com/codeops-dev/codeops-client")
                .repoFullName("codeops-dev/codeops-client")
                .techStack("Flutter 3.22, Dart 3.4, Riverpod")
                .healthScore(82).createdBy(adam).build());
        locksmithProject = projectRepository.save(Project.builder()
                .team(team).name("Locksmith Auth Service")
                .description("OAuth2/OIDC authentication microservice")
                .repoUrl("https://github.com/codeops-dev/locksmith")
                .repoFullName("codeops-dev/locksmith")
                .techStack("Go 1.22, Chi Router, PostgreSQL")
                .healthScore(65).createdBy(sarah).build());
        log.info("Seeded 3 projects");
    }

    private void seedPersonas() {
        personaRepository.saveAll(List.of(
                Persona.builder().name("Security Auditor").agentType(AgentType.SECURITY)
                        .description("Identifies security vulnerabilities and OWASP Top 10 risks")
                        .contentMd("# Security Auditor\nYou are a security-focused code reviewer. Analyze code for SQL injection, XSS, CSRF, authentication flaws, and insecure configurations. Reference OWASP guidelines.")
                        .scope(Scope.SYSTEM).createdBy(adam).isDefault(true).version(1).build(),
                Persona.builder().name("Code Quality Inspector").agentType(AgentType.CODE_QUALITY)
                        .description("Reviews code for maintainability, complexity, and best practices")
                        .contentMd("# Code Quality Inspector\nYou review code for cyclomatic complexity, code duplication, naming conventions, SOLID principles, and maintainability. Suggest concrete refactoring steps.")
                        .scope(Scope.SYSTEM).createdBy(adam).isDefault(true).version(1).build(),
                Persona.builder().name("Architecture Reviewer").agentType(AgentType.ARCHITECTURE)
                        .description("Evaluates system architecture and design patterns")
                        .contentMd("# Architecture Reviewer\nYou evaluate architectural decisions, dependency structure, module boundaries, and design patterns. Flag circular dependencies and layering violations.")
                        .scope(Scope.SYSTEM).createdBy(adam).isDefault(true).version(1).build(),
                Persona.builder().name("Test Coverage Analyst").agentType(AgentType.TEST_COVERAGE)
                        .description("Analyzes test coverage and testing strategy")
                        .contentMd("# Test Coverage Analyst\nYou analyze test suites for coverage gaps, flaky tests, missing edge cases, and testing strategy. Recommend specific test cases to add.")
                        .scope(Scope.SYSTEM).createdBy(adam).isDefault(true).version(1).build(),
                Persona.builder().name("Spring Boot Expert").agentType(AgentType.CODE_QUALITY)
                        .description("Specialized reviewer for Spring Boot applications")
                        .contentMd("# Spring Boot Expert\nYou are an expert in Spring Boot best practices. Review for proper use of dependency injection, transaction management, bean scoping, and Spring Security configuration.")
                        .scope(Scope.TEAM).team(team).createdBy(adam).version(1).build(),
                Persona.builder().name("Flutter Specialist").agentType(AgentType.CODE_QUALITY)
                        .description("Specialized reviewer for Flutter/Dart applications")
                        .contentMd("# Flutter Specialist\nYou are a Flutter and Dart expert. Review for proper state management with Riverpod, widget composition, performance anti-patterns, and platform-specific issues.")
                        .scope(Scope.TEAM).team(team).createdBy(sarah).version(1).build()
        ));
        log.info("Seeded 6 personas");
    }

    private void seedDirectives() {
        directives = directiveRepository.saveAll(List.of(
                Directive.builder().name("REST API Standards")
                        .description("Standard conventions for all REST API endpoints")
                        .contentMd("# REST API Standards\n- Use plural nouns for resources\n- Return 201 for successful creation\n- Use pagination for list endpoints\n- Include error codes in error responses\n- Version APIs via URL path prefix")
                        .category(DirectiveCategory.STANDARDS).scope(DirectiveScope.TEAM)
                        .team(team).createdBy(adam).version(1).build(),
                Directive.builder().name("Security Requirements")
                        .description("Mandatory security practices for all projects")
                        .contentMd("# Security Requirements\n- All endpoints must require authentication except health checks\n- Input validation on all request DTOs\n- SQL parameters must use prepared statements\n- Secrets must never appear in logs\n- CORS must be explicitly configured")
                        .category(DirectiveCategory.STANDARDS).scope(DirectiveScope.TEAM)
                        .team(team).createdBy(adam).version(1).build(),
                Directive.builder().name("Code Review Checklist")
                        .description("Standard checklist for code reviews")
                        .contentMd("# Code Review Checklist\n- No TODOs or commented-out code\n- All public methods have Javadoc\n- Error handling covers edge cases\n- No hardcoded credentials or magic numbers\n- Unit test coverage for new code >= 80%")
                        .category(DirectiveCategory.CONVENTIONS).scope(DirectiveScope.TEAM)
                        .team(team).createdBy(sarah).version(1).build(),
                Directive.builder().name("Architecture Context")
                        .description("High-level architecture description for AI agents")
                        .contentMd("# Architecture Context\nThis is a microservices platform with a Spring Boot API server, Flutter web client, and Go auth service. PostgreSQL is the primary database. Services communicate via REST APIs. Authentication uses JWT tokens.")
                        .category(DirectiveCategory.CONTEXT).scope(DirectiveScope.TEAM)
                        .team(team).createdBy(adam).version(1).build()
        ));
        log.info("Seeded 4 directives");
    }

    private void seedProjectDirectives() {
        Directive d0 = directives.get(0), d1 = directives.get(1);
        Directive d2 = directives.get(2), d3 = directives.get(3);
        projectDirectiveRepository.saveAll(List.of(
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(serverProject.getId(), d0.getId()))
                        .project(serverProject).directive(d0).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(serverProject.getId(), d1.getId()))
                        .project(serverProject).directive(d1).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(serverProject.getId(), d3.getId()))
                        .project(serverProject).directive(d3).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(clientProject.getId(), d1.getId()))
                        .project(clientProject).directive(d1).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(clientProject.getId(), d2.getId()))
                        .project(clientProject).directive(d2).enabled(true).build(),
                ProjectDirective.builder()
                        .id(new ProjectDirectiveId(locksmithProject.getId(), d1.getId()))
                        .project(locksmithProject).directive(d1).enabled(true).build()
        ));
        log.info("Seeded 6 project directives");
    }

    private void seedQaJobs() {
        Instant now = Instant.now();
        auditJob1 = qaJobRepository.save(QaJob.builder()
                .project(serverProject).mode(JobMode.AUDIT).status(JobStatus.COMPLETED)
                .name("Server Full Audit").branch("main")
                .overallResult(JobResult.WARN).healthScore(78)
                .totalFindings(8).criticalCount(2).highCount(2).mediumCount(2).lowCount(2)
                .startedBy(adam).startedAt(now.minus(7, ChronoUnit.DAYS))
                .completedAt(now.minus(7, ChronoUnit.DAYS).plus(12, ChronoUnit.MINUTES)).build());
        complianceJob = qaJobRepository.save(QaJob.builder()
                .project(serverProject).mode(JobMode.COMPLIANCE).status(JobStatus.COMPLETED)
                .name("API Contract Compliance").branch("main")
                .overallResult(JobResult.WARN).healthScore(85)
                .totalFindings(2).highCount(1).mediumCount(1)
                .startedBy(sarah).startedAt(now.minus(5, ChronoUnit.DAYS))
                .completedAt(now.minus(5, ChronoUnit.DAYS).plus(8, ChronoUnit.MINUTES)).build());
        bugJob = qaJobRepository.save(QaJob.builder()
                .project(serverProject).mode(JobMode.BUG_INVESTIGATE).status(JobStatus.COMPLETED)
                .name("Auth Token Expiry Bug").branch("fix/token-refresh")
                .overallResult(JobResult.PASS).healthScore(90)
                .totalFindings(3).highCount(1).mediumCount(2)
                .jiraTicketKey("COD-142")
                .startedBy(mike).startedAt(now.minus(4, ChronoUnit.DAYS))
                .completedAt(now.minus(4, ChronoUnit.DAYS).plus(15, ChronoUnit.MINUTES)).build());
        remediateJob = qaJobRepository.save(QaJob.builder()
                .project(serverProject).mode(JobMode.REMEDIATE).status(JobStatus.COMPLETED)
                .name("Security Findings Remediation").branch("main")
                .overallResult(JobResult.PASS)
                .totalFindings(3).highCount(1).mediumCount(2)
                .startedBy(adam).startedAt(now.minus(3, ChronoUnit.DAYS))
                .completedAt(now.minus(3, ChronoUnit.DAYS).plus(20, ChronoUnit.MINUTES)).build());
        auditJob2 = qaJobRepository.save(QaJob.builder()
                .project(clientProject).mode(JobMode.AUDIT).status(JobStatus.COMPLETED)
                .name("Client Full Audit").branch("main")
                .overallResult(JobResult.WARN).healthScore(82)
                .totalFindings(4).highCount(1).mediumCount(2).lowCount(1)
                .startedBy(sarah).startedAt(now.minus(6, ChronoUnit.DAYS))
                .completedAt(now.minus(6, ChronoUnit.DAYS).plus(10, ChronoUnit.MINUTES)).build());
        techDebtJob = qaJobRepository.save(QaJob.builder()
                .project(clientProject).mode(JobMode.TECH_DEBT).status(JobStatus.COMPLETED)
                .name("Client Tech Debt Analysis").branch("main")
                .overallResult(JobResult.WARN)
                .totalFindings(2).mediumCount(1).lowCount(1)
                .startedBy(sarah).startedAt(now.minus(2, ChronoUnit.DAYS))
                .completedAt(now.minus(2, ChronoUnit.DAYS).plus(7, ChronoUnit.MINUTES)).build());
        depJob = qaJobRepository.save(QaJob.builder()
                .project(locksmithProject).mode(JobMode.DEPENDENCY).status(JobStatus.COMPLETED)
                .name("Locksmith Dependency Scan").branch("main")
                .overallResult(JobResult.FAIL).healthScore(65)
                .totalFindings(2).criticalCount(1).highCount(1)
                .startedBy(adam).startedAt(now.minus(1, ChronoUnit.DAYS))
                .completedAt(now.minus(1, ChronoUnit.DAYS).plus(5, ChronoUnit.MINUTES)).build());
        runningJob = qaJobRepository.save(QaJob.builder()
                .project(locksmithProject).mode(JobMode.AUDIT).status(JobStatus.RUNNING)
                .name("Locksmith Security Audit").branch("main")
                .startedBy(mike).startedAt(now.minus(10, ChronoUnit.MINUTES)).build());
        log.info("Seeded 8 QA jobs");
    }

    private void seedBugInvestigation() {
        bugInvestigationRepository.save(BugInvestigation.builder()
                .job(bugJob).jiraKey("COD-142")
                .jiraSummary("JWT refresh token not extending session correctly")
                .jiraDescription("Users are being logged out after 24 hours even though they have a valid refresh token. The refresh endpoint returns a new access token but the expiry is not being updated in the client.")
                .rcaMd("## Root Cause Analysis\n\nThe `refreshToken` endpoint in `AuthService.java` generates a new access token but copies the original `issuedAt` claim from the old token. This means the new token's expiry is calculated from the original login time, not from the refresh time.\n\n### Fix\nUpdate `JwtTokenProvider.refreshAccessToken()` to use `Instant.now()` as the `issuedAt` for the new token.")
                .impactAssessmentMd("## Impact Assessment\n\n**Severity:** HIGH\n**Affected Users:** All users with sessions >24h\n**Data Loss Risk:** None — this is a session management issue\n\n### Mitigation\nUsers can work around this by logging out and back in. No data is lost.")
                .build());
        log.info("Seeded 1 bug investigation");
    }

    private void seedAgentRuns() {
        agentRunRepository.saveAll(List.of(
                // auditJob1: SECURITY, CODE_QUALITY, ARCHITECTURE
                AgentRun.builder().job(auditJob1).agentType(AgentType.SECURITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(72).findingsCount(3).criticalCount(1).highCount(1)
                        .startedAt(auditJob1.getStartedAt())
                        .completedAt(auditJob1.getStartedAt().plus(4, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(auditJob1).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(80).findingsCount(3).criticalCount(1).highCount(1)
                        .startedAt(auditJob1.getStartedAt())
                        .completedAt(auditJob1.getStartedAt().plus(5, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(auditJob1).agentType(AgentType.ARCHITECTURE).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(88).findingsCount(2)
                        .startedAt(auditJob1.getStartedAt())
                        .completedAt(auditJob1.getStartedAt().plus(6, ChronoUnit.MINUTES)).build(),
                // complianceJob: API_CONTRACT, COMPLETENESS
                AgentRun.builder().job(complianceJob).agentType(AgentType.API_CONTRACT).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(85).findingsCount(1).highCount(1)
                        .startedAt(complianceJob.getStartedAt())
                        .completedAt(complianceJob.getStartedAt().plus(3, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(complianceJob).agentType(AgentType.COMPLETENESS).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(90).findingsCount(1)
                        .startedAt(complianceJob.getStartedAt())
                        .completedAt(complianceJob.getStartedAt().plus(4, ChronoUnit.MINUTES)).build(),
                // bugJob: SECURITY, CODE_QUALITY
                AgentRun.builder().job(bugJob).agentType(AgentType.SECURITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(92).findingsCount(1).highCount(1)
                        .startedAt(bugJob.getStartedAt())
                        .completedAt(bugJob.getStartedAt().plus(5, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(bugJob).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(88).findingsCount(2)
                        .startedAt(bugJob.getStartedAt())
                        .completedAt(bugJob.getStartedAt().plus(6, ChronoUnit.MINUTES)).build(),
                // remediateJob: CODE_QUALITY, SECURITY
                AgentRun.builder().job(remediateJob).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(90).findingsCount(2)
                        .startedAt(remediateJob.getStartedAt())
                        .completedAt(remediateJob.getStartedAt().plus(8, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(remediateJob).agentType(AgentType.SECURITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(95).findingsCount(1)
                        .startedAt(remediateJob.getStartedAt())
                        .completedAt(remediateJob.getStartedAt().plus(10, ChronoUnit.MINUTES)).build(),
                // auditJob2: SECURITY, CODE_QUALITY, TEST_COVERAGE
                AgentRun.builder().job(auditJob2).agentType(AgentType.SECURITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(90).findingsCount(1).highCount(1)
                        .startedAt(auditJob2.getStartedAt())
                        .completedAt(auditJob2.getStartedAt().plus(3, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(auditJob2).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(78).findingsCount(2)
                        .startedAt(auditJob2.getStartedAt())
                        .completedAt(auditJob2.getStartedAt().plus(4, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(auditJob2).agentType(AgentType.TEST_COVERAGE).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(65).findingsCount(1)
                        .startedAt(auditJob2.getStartedAt())
                        .completedAt(auditJob2.getStartedAt().plus(5, ChronoUnit.MINUTES)).build(),
                // techDebtJob: ARCHITECTURE, CODE_QUALITY
                AgentRun.builder().job(techDebtJob).agentType(AgentType.ARCHITECTURE).status(AgentStatus.COMPLETED)
                        .result(AgentResult.WARN).score(75).findingsCount(1)
                        .startedAt(techDebtJob.getStartedAt())
                        .completedAt(techDebtJob.getStartedAt().plus(3, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(techDebtJob).agentType(AgentType.CODE_QUALITY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(82).findingsCount(1)
                        .startedAt(techDebtJob.getStartedAt())
                        .completedAt(techDebtJob.getStartedAt().plus(4, ChronoUnit.MINUTES)).build(),
                // depJob: DEPENDENCY, BUILD_HEALTH
                AgentRun.builder().job(depJob).agentType(AgentType.DEPENDENCY).status(AgentStatus.COMPLETED)
                        .result(AgentResult.FAIL).score(45).findingsCount(2).criticalCount(1).highCount(1)
                        .startedAt(depJob.getStartedAt())
                        .completedAt(depJob.getStartedAt().plus(2, ChronoUnit.MINUTES)).build(),
                AgentRun.builder().job(depJob).agentType(AgentType.BUILD_HEALTH).status(AgentStatus.COMPLETED)
                        .result(AgentResult.PASS).score(88)
                        .startedAt(depJob.getStartedAt())
                        .completedAt(depJob.getStartedAt().plus(3, ChronoUnit.MINUTES)).build(),
                // runningJob: SECURITY (running), CODE_QUALITY (pending)
                AgentRun.builder().job(runningJob).agentType(AgentType.SECURITY).status(AgentStatus.RUNNING)
                        .startedAt(runningJob.getStartedAt()).build(),
                AgentRun.builder().job(runningJob).agentType(AgentType.CODE_QUALITY).status(AgentStatus.PENDING).build()
        ));
        log.info("Seeded 18 agent runs");
    }

    private void seedFindings() {
        Instant now = Instant.now();
        List<Finding> findings = new ArrayList<>();

        // auditJob1: 8 findings [indices 0-7]
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.SECURITY).severity(Severity.CRITICAL)
                .title("SQL Injection in search endpoint")
                .description("The /api/v1/projects/search endpoint concatenates user input directly into a SQL query string without parameterization.")
                .filePath("src/main/java/com/codeops/repository/CustomProjectRepository.java").lineNumber(45)
                .recommendation("Use parameterized queries or Spring Data JPA @Query with named parameters.")
                .evidence("String query = \"SELECT * FROM projects WHERE name LIKE '%\" + searchTerm + \"%'\";")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.SECURITY).severity(Severity.CRITICAL)
                .title("Hardcoded JWT secret in application.yml")
                .description("The JWT signing secret is hardcoded as a default value in application-dev.yml and could leak to production if environment variable is not set.")
                .filePath("src/main/resources/application-dev.yml").lineNumber(23)
                .recommendation("Remove default secret value and fail fast if JWT_SECRET env var is not set in production profile.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.SECURITY).severity(Severity.HIGH)
                .title("Missing rate limiting on login endpoint")
                .description("The POST /api/v1/auth/login endpoint has no rate limiting, making it vulnerable to brute-force password attacks.")
                .filePath("src/main/java/com/codeops/controller/AuthController.java").lineNumber(32)
                .recommendation("Add rate limiting using a Redis-based sliding window counter.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.CODE_QUALITY).severity(Severity.HIGH)
                .title("God class: ProjectService has 42 methods")
                .description("ProjectService.java has grown to 42 public methods and 1,200 lines, violating the Single Responsibility Principle.")
                .filePath("src/main/java/com/codeops/service/ProjectService.java").lineNumber(1)
                .recommendation("Extract related methods into focused services: ProjectQueryService, ProjectCommandService.")
                .effortEstimate(Effort.L).debtCategory(DebtCategory.ARCHITECTURE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Duplicate error handling logic across controllers")
                .description("Exception handling try-catch blocks are duplicated in 12 controllers instead of using the global exception handler.")
                .filePath("src/main/java/com/codeops/controller/QaJobController.java").lineNumber(87)
                .recommendation("Remove try-catch blocks from controllers and let GlobalExceptionHandler manage all exceptions.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("N+1 query in job listing endpoint")
                .description("GET /api/v1/jobs loads each job's project relationship lazily, causing N+1 queries when listing jobs.")
                .filePath("src/main/java/com/codeops/service/QaJobService.java").lineNumber(65)
                .recommendation("Use @EntityGraph or a JOIN FETCH query in the repository method.")
                .evidence("Hibernate logs show 51 SELECT queries for a page of 50 jobs")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.ARCHITECTURE).severity(Severity.LOW)
                .title("Unused NotificationPreference entity")
                .description("The NotificationPreference entity and repository exist but are never referenced by any service or controller.")
                .filePath("src/main/java/com/codeops/entity/NotificationPreference.java").lineNumber(1)
                .recommendation("Either implement notification preferences or remove the unused entity.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.ARCHITECTURE).build());
        findings.add(Finding.builder().job(auditJob1).agentType(AgentType.ARCHITECTURE).severity(Severity.LOW)
                .title("Inconsistent DTO naming convention")
                .description("Some DTOs use Request/Response suffix while others use Create/Update prefix.")
                .filePath("src/main/java/com/codeops/dto/request/").lineNumber(0)
                .recommendation("Standardize on one naming convention across all DTOs.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());

        // complianceJob: 2 findings [indices 8-9]
        findings.add(Finding.builder().job(complianceJob).agentType(AgentType.API_CONTRACT).severity(Severity.HIGH)
                .title("Missing pagination on GET /api/v1/findings endpoint")
                .description("The API spec requires pagination for all list endpoints, but GET /api/v1/findings returns all findings without pagination.")
                .filePath("src/main/java/com/codeops/controller/FindingController.java").lineNumber(28)
                .recommendation("Add Pageable parameter and return PageResponse<FindingResponse>.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(complianceJob).agentType(AgentType.COMPLETENESS).severity(Severity.MEDIUM)
                .title("OpenAPI spec missing error response schemas")
                .description("15 endpoints are missing 400/401/403/404 error response definitions in the OpenAPI documentation.")
                .filePath("src/main/java/com/codeops/controller/").lineNumber(0)
                .recommendation("Add @ApiResponse annotations with error schema definitions to all controller methods.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.DOCUMENTATION).build());

        // bugJob: 3 findings [indices 10-12]
        findings.add(Finding.builder().job(bugJob).agentType(AgentType.SECURITY).severity(Severity.HIGH)
                .title("Token refresh reuses original issuedAt timestamp")
                .description("JwtTokenProvider.refreshAccessToken() copies the original issuedAt claim, causing refreshed tokens to expire based on original login time.")
                .filePath("src/main/java/com/codeops/security/JwtTokenProvider.java").lineNumber(89)
                .recommendation("Use Instant.now() for the issuedAt claim in refreshed tokens.")
                .evidence("Token generated at login: iat=1704067200, refreshed token: iat=1704067200 (same)")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(bugJob).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Missing null check in refresh token flow")
                .description("AuthService.refreshToken() does not check if the user account is still active before issuing a new token.")
                .filePath("src/main/java/com/codeops/service/AuthService.java").lineNumber(72)
                .recommendation("Add isActive check after loading user from refresh token claims.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(bugJob).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Refresh token endpoint returns 500 on expired token")
                .description("When a refresh token is expired, the endpoint returns 500 instead of 401 Unauthorized.")
                .filePath("src/main/java/com/codeops/controller/AuthController.java").lineNumber(55)
                .recommendation("Catch ExpiredJwtException and return 401 with a descriptive message.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());

        // remediateJob: 3 findings (FIXED) [indices 13-15]
        findings.add(Finding.builder().job(remediateJob).agentType(AgentType.SECURITY).severity(Severity.HIGH)
                .title("CORS allows wildcard origin in dev")
                .description("CorsConfig sets Access-Control-Allow-Origin to * which would be dangerous if deployed without proper configuration.")
                .filePath("src/main/java/com/codeops/config/CorsConfig.java").lineNumber(15)
                .recommendation("Use explicit allowed origins from configuration.")
                .status(FindingStatus.FIXED).statusChangedBy(adam).statusChangedAt(now.minus(3, ChronoUnit.DAYS))
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(remediateJob).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Unencrypted sensitive data in audit log details")
                .description("AuditLogService logs full request bodies including potentially sensitive fields like passwords and tokens.")
                .filePath("src/main/java/com/codeops/service/AuditLogService.java").lineNumber(34)
                .recommendation("Implement a sanitizer that masks sensitive fields before logging.")
                .status(FindingStatus.FIXED).statusChangedBy(adam).statusChangedAt(now.minus(3, ChronoUnit.DAYS))
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(remediateJob).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Stack trace exposed in production error responses")
                .description("GlobalExceptionHandler includes stack traces in error response bodies regardless of environment.")
                .filePath("src/main/java/com/codeops/config/GlobalExceptionHandler.java").lineNumber(28)
                .recommendation("Only include stack traces when spring.profiles.active=dev.")
                .status(FindingStatus.FIXED).statusChangedBy(adam).statusChangedAt(now.minus(3, ChronoUnit.DAYS))
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());

        // auditJob2: 4 findings [indices 16-19]
        findings.add(Finding.builder().job(auditJob2).agentType(AgentType.SECURITY).severity(Severity.HIGH)
                .title("API token stored in localStorage")
                .description("The Flutter web app stores JWT access tokens in localStorage which is vulnerable to XSS attacks.")
                .filePath("lib/core/storage/token_storage.dart").lineNumber(12)
                .recommendation("Use secure HTTP-only cookies or flutter_secure_storage for token persistence.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob2).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Excessive widget rebuilds in job list")
                .description("JobListScreen rebuilds the entire widget tree on every state change because it watches the full job list provider.")
                .filePath("lib/features/jobs/screens/job_list_screen.dart").lineNumber(45)
                .recommendation("Use select() to watch only the specific fields needed.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob2).agentType(AgentType.CODE_QUALITY).severity(Severity.MEDIUM)
                .title("Missing error boundaries in widget tree")
                .description("No ErrorWidget.builder configured and no error handling widgets around async operations in 8 screens.")
                .filePath("lib/main.dart").lineNumber(1)
                .recommendation("Add ErrorBoundary widgets and configure ErrorWidget.builder for graceful error display.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.CODE).build());
        findings.add(Finding.builder().job(auditJob2).agentType(AgentType.TEST_COVERAGE).severity(Severity.LOW)
                .title("No widget tests for core UI components")
                .description("The shared/widgets/ directory contains 15 reusable components with zero widget tests.")
                .filePath("test/").lineNumber(0)
                .recommendation("Add widget tests for AppShell, SideNav, DataTable, and other core components.")
                .effortEstimate(Effort.L).debtCategory(DebtCategory.TEST).build());

        // techDebtJob: 2 findings [indices 20-21]
        findings.add(Finding.builder().job(techDebtJob).agentType(AgentType.ARCHITECTURE).severity(Severity.MEDIUM)
                .title("Circular dependency between features/auth and core/router")
                .description("The auth feature imports from core/router for navigation, while core/router imports from features/auth for guard logic.")
                .filePath("lib/features/auth/providers/auth_provider.dart").lineNumber(5)
                .recommendation("Extract auth state interface to core/ and have both depend on the abstraction.")
                .effortEstimate(Effort.M).debtCategory(DebtCategory.ARCHITECTURE).build());
        findings.add(Finding.builder().job(techDebtJob).agentType(AgentType.CODE_QUALITY).severity(Severity.LOW)
                .title("Deprecated Flutter APIs used in 6 files")
                .description("Several widgets use deprecated APIs: WillPopScope (use PopScope), FlatButton (use TextButton).")
                .filePath("lib/shared/widgets/").lineNumber(0)
                .recommendation("Replace deprecated APIs with their modern equivalents.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.CODE).build());

        // depJob: 2 findings [indices 22-23]
        findings.add(Finding.builder().job(depJob).agentType(AgentType.DEPENDENCY).severity(Severity.CRITICAL)
                .title("Critical CVE in golang.org/x/crypto")
                .description("golang.org/x/crypto v0.17.0 has CVE-2024-45337 — SSH connection bypass allowing unauthorized access.")
                .filePath("go.mod").lineNumber(8)
                .recommendation("Upgrade to golang.org/x/crypto v0.31.0 or later.")
                .evidence("CVE-2024-45337: CVSS 9.1")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.DEPENDENCY).build());
        findings.add(Finding.builder().job(depJob).agentType(AgentType.DEPENDENCY).severity(Severity.HIGH)
                .title("Outdated chi router with known vulnerability")
                .description("go-chi/chi v5.0.10 has a path traversal vulnerability in static file serving middleware.")
                .filePath("go.mod").lineNumber(12)
                .recommendation("Upgrade to go-chi/chi v5.0.12 or later.")
                .effortEstimate(Effort.S).debtCategory(DebtCategory.DEPENDENCY).build());

        savedFindings = findingRepository.saveAll(findings);
        log.info("Seeded 24 findings");
    }

    private void seedRemediationTasks() {
        remediationTaskRepository.saveAll(List.of(
                RemediationTask.builder().job(auditJob1).taskNumber(1)
                        .title("Fix SQL injection in project search")
                        .description("Replace string concatenation with parameterized query in CustomProjectRepository.")
                        .promptMd("Fix the SQL injection vulnerability by replacing the raw SQL string concatenation with a parameterized @Query annotation.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(0))))
                        .priority(Priority.P0).status(TaskStatus.EXPORTED).assignedTo(adam).build(),
                RemediationTask.builder().job(auditJob1).taskNumber(2)
                        .title("Remove hardcoded JWT secret default")
                        .description("Remove the default JWT secret from application-dev.yml and add startup validation in production profile.")
                        .promptMd("Remove the hardcoded default secret and add a @PostConstruct validator that fails if JWT_SECRET is not set in prod.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(1))))
                        .priority(Priority.P0).status(TaskStatus.COMPLETED).assignedTo(adam).build(),
                RemediationTask.builder().job(auditJob1).taskNumber(3)
                        .title("Add rate limiting to auth endpoints")
                        .description("Implement rate limiting on login and refresh endpoints to prevent brute-force attacks.")
                        .promptMd("Add a Redis-based rate limiter that limits login attempts to 5 per minute per IP address.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(2))))
                        .priority(Priority.P1).status(TaskStatus.ASSIGNED).assignedTo(mike).build(),
                RemediationTask.builder().job(auditJob1).taskNumber(4)
                        .title("Refactor ProjectService and fix N+1 queries")
                        .description("Split ProjectService into focused services and add JOIN FETCH for job listing.")
                        .promptMd("Extract ProjectService methods into ProjectQueryService and ProjectCommandService. Add @EntityGraph to job listing repository method.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(3), savedFindings.get(5))))
                        .priority(Priority.P2).status(TaskStatus.PENDING).build(),
                RemediationTask.builder().job(remediateJob).taskNumber(1)
                        .title("Fix CORS configuration")
                        .description("Replace wildcard CORS origin with explicit allowed origins from configuration.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(13))))
                        .priority(Priority.P1).status(TaskStatus.COMPLETED).assignedTo(adam).build(),
                RemediationTask.builder().job(remediateJob).taskNumber(2)
                        .title("Sanitize audit log data and hide stack traces")
                        .description("Add field sanitizer to AuditLogService and make stack traces conditional on dev profile.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(14), savedFindings.get(15))))
                        .priority(Priority.P1).status(TaskStatus.COMPLETED).assignedTo(adam).build(),
                RemediationTask.builder().job(auditJob2).taskNumber(1)
                        .title("Migrate token storage to secure cookies")
                        .description("Replace localStorage token storage with secure HTTP-only cookies for XSS protection.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(16))))
                        .priority(Priority.P0).status(TaskStatus.PENDING).build(),
                RemediationTask.builder().job(auditJob2).taskNumber(2)
                        .title("Optimize widget rebuilds and add error boundaries")
                        .description("Add select() to providers and implement error boundary widgets.")
                        .findings(new ArrayList<>(List.of(savedFindings.get(17), savedFindings.get(18))))
                        .priority(Priority.P2).status(TaskStatus.PENDING).build()
        ));
        log.info("Seeded 8 remediation tasks");
    }

    private void seedSpecifications() {
        List<Specification> specs = specificationRepository.saveAll(List.of(
                Specification.builder().job(complianceJob)
                        .name("CodeOps API v1 OpenAPI Spec").specType(SpecType.OPENAPI)
                        .s3Key("specs/codeops-api-v1.yaml").build(),
                Specification.builder().job(complianceJob)
                        .name("API Design Guidelines").specType(SpecType.MARKDOWN)
                        .s3Key("specs/api-design-guidelines.md").build()
        ));
        apiSpec = specs.get(0);
        markdownSpec = specs.get(1);
        log.info("Seeded 2 specifications");
    }

    private void seedComplianceItems() {
        complianceItemRepository.saveAll(List.of(
                ComplianceItem.builder().job(complianceJob)
                        .requirement("All list endpoints must support pagination with page and size parameters")
                        .spec(apiSpec).status(ComplianceStatus.PARTIAL).agentType(AgentType.API_CONTRACT)
                        .evidence("22 of 28 list endpoints support pagination. 6 endpoints return unpaginated lists.")
                        .notes("Failing: GET /findings, GET /agent-runs, GET /directives, GET /personas, GET /compliance-items, GET /specifications").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("All endpoints must return consistent error response format with code and message")
                        .spec(apiSpec).status(ComplianceStatus.MET).agentType(AgentType.API_CONTRACT)
                        .evidence("GlobalExceptionHandler produces consistent ErrorResponse record for all exception types.").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("All mutation endpoints must require authentication")
                        .spec(markdownSpec).status(ComplianceStatus.MET).agentType(AgentType.API_CONTRACT)
                        .evidence("SecurityConfig requires authentication for all endpoints except /auth/**, /health, and Swagger UI.").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("Request DTOs must use Jakarta Validation annotations")
                        .spec(markdownSpec).status(ComplianceStatus.PARTIAL).agentType(AgentType.COMPLETENESS)
                        .evidence("28 of 35 request DTOs use validation annotations. 7 DTOs have no validation.")
                        .notes("Missing: UpdateProjectRequest, CreateDirectiveRequest, UpdatePersonaRequest, and 4 others").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("All endpoints must be documented with OpenAPI annotations")
                        .spec(apiSpec).status(ComplianceStatus.MISSING).agentType(AgentType.COMPLETENESS)
                        .evidence("Only 5 of 18 controllers have @Operation annotations. Most endpoints rely on auto-generated docs.")
                        .notes("Priority: AuthController, QaJobController, FindingController").build(),
                ComplianceItem.builder().job(complianceJob)
                        .requirement("Health check endpoint must return service dependencies status")
                        .spec(markdownSpec).status(ComplianceStatus.MET).agentType(AgentType.API_CONTRACT)
                        .evidence("GET /health returns {status, database, timestamp} with actual DB connectivity check.").build()
        ));
        log.info("Seeded 6 compliance items");
    }

    private void seedTechDebtItems() {
        techDebtItemRepository.saveAll(List.of(
                TechDebtItem.builder().project(serverProject).category(DebtCategory.ARCHITECTURE)
                        .title("No database migration tool — relying on Hibernate ddl-auto")
                        .description("Using hibernate.ddl-auto=update in all environments. This is dangerous for production and doesn't support rollbacks.")
                        .effortEstimate(Effort.L).businessImpact(BusinessImpact.HIGH)
                        .status(DebtStatus.PLANNED).firstDetectedJob(auditJob1).build(),
                TechDebtItem.builder().project(serverProject).category(DebtCategory.TEST)
                        .title("Integration test coverage below 20%")
                        .description("Only 4 integration tests exist. Most services and controllers have no integration test coverage.")
                        .effortEstimate(Effort.XL).businessImpact(BusinessImpact.HIGH)
                        .status(DebtStatus.IDENTIFIED).firstDetectedJob(auditJob1).build(),
                TechDebtItem.builder().project(serverProject).category(DebtCategory.CODE)
                        .title("Inconsistent error response format across controllers")
                        .description("Some controllers return custom error maps while others rely on GlobalExceptionHandler. Should standardize.")
                        .effortEstimate(Effort.M).businessImpact(BusinessImpact.MEDIUM)
                        .status(DebtStatus.IN_PROGRESS).firstDetectedJob(auditJob1).build(),
                TechDebtItem.builder().project(clientProject).category(DebtCategory.ARCHITECTURE)
                        .title("No offline support or caching strategy")
                        .description("The Flutter client makes fresh API calls on every navigation. No data caching or offline fallback exists.")
                        .effortEstimate(Effort.XL).businessImpact(BusinessImpact.MEDIUM)
                        .status(DebtStatus.IDENTIFIED).firstDetectedJob(auditJob2).build(),
                TechDebtItem.builder().project(clientProject).category(DebtCategory.CODE)
                        .title("Riverpod providers not using code generation")
                        .description("Half of the providers are manually written instead of using @riverpod code generation, leading to inconsistency.")
                        .effortEstimate(Effort.M).businessImpact(BusinessImpact.LOW)
                        .status(DebtStatus.PLANNED).firstDetectedJob(techDebtJob).build(),
                TechDebtItem.builder().project(clientProject).category(DebtCategory.DOCUMENTATION)
                        .title("No component storybook or design system documentation")
                        .description("Shared widgets have no visual documentation. New developers must read source code to understand available components.")
                        .effortEstimate(Effort.L).businessImpact(BusinessImpact.LOW)
                        .status(DebtStatus.IDENTIFIED).firstDetectedJob(techDebtJob).build(),
                TechDebtItem.builder().project(locksmithProject).category(DebtCategory.DEPENDENCY)
                        .title("Multiple Go dependencies 2+ major versions behind")
                        .description("golang.org/x/crypto, go-chi/chi, and pgx are all behind latest major versions with known CVEs.")
                        .effortEstimate(Effort.M).businessImpact(BusinessImpact.CRITICAL)
                        .status(DebtStatus.IN_PROGRESS).firstDetectedJob(depJob).build(),
                TechDebtItem.builder().project(locksmithProject).category(DebtCategory.TEST)
                        .title("No end-to-end test for OAuth2 flow")
                        .description("The complete OAuth2 authorization code flow has no automated test. Only unit tests for individual handlers exist.")
                        .effortEstimate(Effort.L).businessImpact(BusinessImpact.HIGH)
                        .status(DebtStatus.IDENTIFIED).firstDetectedJob(depJob).build()
        ));
        log.info("Seeded 8 tech debt items");
    }

    private void seedDependencyScans() {
        serverScan = dependencyScanRepository.save(DependencyScan.builder()
                .project(serverProject).manifestFile("pom.xml")
                .totalDependencies(34).outdatedCount(8).vulnerableCount(2).build());
        locksmithScan = dependencyScanRepository.save(DependencyScan.builder()
                .project(locksmithProject).job(depJob).manifestFile("go.mod")
                .totalDependencies(22).outdatedCount(6).vulnerableCount(5).build());
        log.info("Seeded 2 dependency scans");
    }

    private void seedDependencyVulnerabilities() {
        dependencyVulnerabilityRepository.saveAll(List.of(
                DependencyVulnerability.builder().scan(serverScan)
                        .dependencyName("org.postgresql:postgresql").currentVersion("42.7.2").fixedVersion("42.7.4")
                        .cveId("CVE-2024-1597").severity(Severity.HIGH)
                        .description("SQL injection via line comment generation in PostgreSQL JDBC driver.").build(),
                DependencyVulnerability.builder().scan(serverScan)
                        .dependencyName("io.jsonwebtoken:jjwt-impl").currentVersion("0.12.3").fixedVersion("0.12.6")
                        .severity(Severity.MEDIUM)
                        .description("Potential key confusion attack when using RSA with HMAC verification.")
                        .status(VulnerabilityStatus.RESOLVED).build(),
                DependencyVulnerability.builder().scan(serverScan)
                        .dependencyName("org.springdoc:springdoc-openapi-starter-webmvc-ui").currentVersion("2.3.0").fixedVersion("2.5.0")
                        .severity(Severity.LOW)
                        .description("XSS via crafted OpenAPI schema definitions in Swagger UI.")
                        .status(VulnerabilityStatus.UPDATING).build(),
                DependencyVulnerability.builder().scan(locksmithScan)
                        .dependencyName("golang.org/x/crypto").currentVersion("0.17.0").fixedVersion("0.31.0")
                        .cveId("CVE-2024-45337").severity(Severity.CRITICAL)
                        .description("SSH connection bypass allowing unauthorized access via crafted authentication request.").build(),
                DependencyVulnerability.builder().scan(locksmithScan)
                        .dependencyName("github.com/go-chi/chi/v5").currentVersion("5.0.10").fixedVersion("5.0.12")
                        .severity(Severity.HIGH)
                        .description("Path traversal vulnerability in chi.FileServer static file serving middleware.").build(),
                DependencyVulnerability.builder().scan(locksmithScan)
                        .dependencyName("github.com/jackc/pgx/v5").currentVersion("5.5.0").fixedVersion("5.5.4")
                        .cveId("CVE-2024-27304").severity(Severity.HIGH)
                        .description("SQL injection via crafted connection string parameters.").build(),
                DependencyVulnerability.builder().scan(locksmithScan)
                        .dependencyName("golang.org/x/net").currentVersion("0.19.0").fixedVersion("0.23.0")
                        .cveId("CVE-2024-24790").severity(Severity.MEDIUM)
                        .description("HTTP/2 CONTINUATION frames flood causing denial of service.").build()
        ));
        log.info("Seeded 7 dependency vulnerabilities");
    }

    private void seedHealthSchedules() {
        Instant now = Instant.now();
        healthScheduleRepository.saveAll(List.of(
                HealthSchedule.builder().project(serverProject).scheduleType(ScheduleType.DAILY)
                        .cronExpression("0 0 6 * * *")
                        .agentTypes(toJson(List.of("SECURITY", "CODE_QUALITY", "ARCHITECTURE")))
                        .createdBy(adam).nextRunAt(now.plus(1, ChronoUnit.DAYS)).build(),
                HealthSchedule.builder().project(clientProject).scheduleType(ScheduleType.WEEKLY)
                        .cronExpression("0 0 6 * * MON")
                        .agentTypes(toJson(List.of("SECURITY", "CODE_QUALITY", "TEST_COVERAGE")))
                        .createdBy(sarah).nextRunAt(now.plus(7, ChronoUnit.DAYS)).build(),
                HealthSchedule.builder().project(locksmithProject).scheduleType(ScheduleType.ON_COMMIT)
                        .agentTypes(toJson(List.of("SECURITY", "DEPENDENCY")))
                        .createdBy(adam).build()
        ));
        log.info("Seeded 3 health schedules");
    }

    private void seedHealthSnapshots() {
        Instant now = Instant.now();
        healthSnapshotRepository.saveAll(List.of(
                // serverProject
                HealthSnapshot.builder().project(serverProject).job(auditJob1).healthScore(72)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 2, "HIGH", 2, "MEDIUM", 2, "LOW", 2)))
                        .techDebtScore(55).dependencyScore(70).testCoveragePercent(new BigDecimal("42.5"))
                        .capturedAt(now.minus(14, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(serverProject).healthScore(75)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 1, "HIGH", 2, "MEDIUM", 3, "LOW", 2)))
                        .techDebtScore(58).dependencyScore(72).testCoveragePercent(new BigDecimal("45.0"))
                        .capturedAt(now.minus(7, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(serverProject).healthScore(78)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 0, "HIGH", 1, "MEDIUM", 3, "LOW", 2)))
                        .techDebtScore(60).dependencyScore(75).testCoveragePercent(new BigDecimal("48.2"))
                        .capturedAt(now.minus(1, ChronoUnit.DAYS)).build(),
                // clientProject
                HealthSnapshot.builder().project(clientProject).job(auditJob2).healthScore(78)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 0, "HIGH", 1, "MEDIUM", 2, "LOW", 1)))
                        .techDebtScore(65).dependencyScore(88).testCoveragePercent(new BigDecimal("35.0"))
                        .capturedAt(now.minus(14, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(clientProject).healthScore(80)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 0, "HIGH", 1, "MEDIUM", 1, "LOW", 1)))
                        .techDebtScore(68).dependencyScore(90).testCoveragePercent(new BigDecimal("38.5"))
                        .capturedAt(now.minus(7, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(clientProject).healthScore(82)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 0, "HIGH", 0, "MEDIUM", 2, "LOW", 1)))
                        .techDebtScore(70).dependencyScore(90).testCoveragePercent(new BigDecimal("41.0"))
                        .capturedAt(now.minus(1, ChronoUnit.DAYS)).build(),
                // locksmithProject
                HealthSnapshot.builder().project(locksmithProject).healthScore(58)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 2, "HIGH", 3, "MEDIUM", 1, "LOW", 0)))
                        .techDebtScore(40).dependencyScore(35).testCoveragePercent(new BigDecimal("62.0"))
                        .capturedAt(now.minus(14, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(locksmithProject).healthScore(62)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 1, "HIGH", 2, "MEDIUM", 2, "LOW", 0)))
                        .techDebtScore(45).dependencyScore(40).testCoveragePercent(new BigDecimal("62.5"))
                        .capturedAt(now.minus(7, ChronoUnit.DAYS)).build(),
                HealthSnapshot.builder().project(locksmithProject).job(depJob).healthScore(65)
                        .findingsBySeverity(toJson(Map.of("CRITICAL", 1, "HIGH", 1, "MEDIUM", 2, "LOW", 0)))
                        .techDebtScore(48).dependencyScore(45).testCoveragePercent(new BigDecimal("63.0"))
                        .capturedAt(now.minus(1, ChronoUnit.DAYS)).build()
        ));
        log.info("Seeded 9 health snapshots");
    }

    private void seedSystemSettings() {
        Instant now = Instant.now();
        systemSettingRepository.saveAll(List.of(
                SystemSetting.builder().settingKey("app.version").value("0.1.0-SNAPSHOT").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("notification.email.enabled").value("false").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("notification.teams.enabled").value("false").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("agent.max.concurrent.runs").value("3").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("health.snapshot.retention.days").value("90").updatedBy(adam).updatedAt(now).build(),
                SystemSetting.builder().settingKey("jira.sync.enabled").value("false").updatedBy(adam).updatedAt(now).build()
        ));
        log.info("Seeded 6 system settings");
    }

    private void seedAuditLog() {
        Instant now = Instant.now();
        auditLogRepository.saveAll(List.of(
                AuditLog.builder().user(adam).team(team).action("CREATE").entityType("Team").entityId(team.getId())
                        .details("Created team 'CodeOps Core'").ipAddress("127.0.0.1").createdAt(now.minus(90, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(adam).team(team).action("CREATE").entityType("Project").entityId(serverProject.getId())
                        .details("Created project 'CodeOps Server'").ipAddress("127.0.0.1").createdAt(now.minus(89, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(adam).team(team).action("CREATE").entityType("Project").entityId(clientProject.getId())
                        .details("Created project 'CodeOps Client'").ipAddress("127.0.0.1").createdAt(now.minus(88, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(sarah).team(team).action("CREATE").entityType("Project").entityId(locksmithProject.getId())
                        .details("Created project 'Locksmith Auth Service'").ipAddress("127.0.0.1").createdAt(now.minus(60, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(adam).team(team).action("START_JOB").entityType("QaJob").entityId(auditJob1.getId())
                        .details("Started audit job for CodeOps Server").ipAddress("127.0.0.1").createdAt(auditJob1.getStartedAt()).build(),
                AuditLog.builder().user(sarah).team(team).action("START_JOB").entityType("QaJob").entityId(complianceJob.getId())
                        .details("Started compliance job for CodeOps Server").ipAddress("127.0.0.1").createdAt(complianceJob.getStartedAt()).build(),
                AuditLog.builder().user(mike).team(team).action("START_JOB").entityType("QaJob").entityId(bugJob.getId())
                        .details("Started bug investigation for COD-142").ipAddress("127.0.0.1").createdAt(bugJob.getStartedAt()).build(),
                AuditLog.builder().user(adam).team(team).action("UPDATE").entityType("Finding").entityId(savedFindings.get(13).getId())
                        .details("Marked finding as FIXED").ipAddress("127.0.0.1").createdAt(now.minus(3, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(sarah).team(team).action("CREATE").entityType("Directive")
                        .details("Created directive 'Code Review Checklist'").ipAddress("127.0.0.1").createdAt(now.minus(30, ChronoUnit.DAYS)).build(),
                AuditLog.builder().user(mike).team(team).action("START_JOB").entityType("QaJob").entityId(runningJob.getId())
                        .details("Started security audit for Locksmith").ipAddress("127.0.0.1").createdAt(runningJob.getStartedAt()).build()
        ));
        log.info("Seeded 10 audit log entries");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON for seed data", e);
        }
    }
}
