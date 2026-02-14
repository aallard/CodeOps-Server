# CodeOps — Architecture Specification

**Product:** CodeOps
**Version:** 1.0 — Final Architecture
**Date:** 2026-02-13
**Classification:** Internal

---

## 1. Product Definition

CodeOps is an AI-powered software maintenance platform. A native desktop application (Flutter) backed by a cloud microservice (Java 21 / Spring Boot), it gives engineering teams a single tool for codebase health management, QA automation, Jira-driven bug investigation, tech debt tracking, and remediation task generation.

### What CodeOps Does
- **Manages source code** — full GitHub Desktop-equivalent functionality (clone, branch, pull, push, commit, PR)
- **Audits codebases** — multi-agent AI analysis from security, quality, completeness, and other perspectives
- **Investigates bugs** — reads a Jira ticket, analyzes the codebase, produces root cause analysis and fix plans
- **Checks compliance** — compares specifications against implementation
- **Tracks tech debt** — categorizes, scores, and trends technical debt over time
- **Monitors dependencies** — identifies outdated packages and known CVEs
- **Monitors health** — scheduled audits with trend tracking and threshold alerts
- **Generates tasks** — produces Claude Code-ready prompts and creates Jira tickets
- **Collaborates** — shared projects, findings, personas, directives across teams

### What CodeOps Is Not
- Not an IDE — doesn't replace VS Code / IntelliJ
- Not a CI/CD pipeline — doesn't replace GitHub Actions
- Not a project management tool — doesn't replace Jira
- It's the **maintenance command center** that integrates with all of these

### Architecture Summary

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Desktop App | Flutter (macOS, Windows, Linux) | All UI, local agent orchestration, Git, Jira |
| Cloud Service | Java 21 / Spring Boot 3.3 / Docker | Auth, team collaboration, data storage |
| Database | PostgreSQL (AWS RDS) | All persistent data |
| File Storage | AWS S3 | Reports, specs, persona files |
| Update Distribution | AWS S3 + CloudFront | App binaries, version manifest |
| QA Manager | Vera (AI orchestrator persona) | Agent dispatch, consolidation, summaries |
| Agent Engine | Claude Code CLI (local) | AI-powered analysis subprocesses |

---

## 2. Seven Workflows

### Mode 1: Code Audit
**Trigger:** Manual
**Purpose:** Comprehensive codebase health assessment
**Flow:** Select project/repo → configure agents → run → review findings → share
**Output:** Per-agent reports (.md), consolidated summary, severity breakdown, health score

### Mode 2: Specification Compliance
**Trigger:** Manual
**Purpose:** Verify implementation matches specifications
**Flow:** Select repo → upload specs (.md, OpenAPI, screenshots, Figma) → agents compare → gap analysis
**Output:** Compliance matrix, gap report, compliance score

### Mode 3: Bug Investigation (Jira-Driven)
**Trigger:** User enters Jira ticket key
**Purpose:** AI-powered root cause analysis and fix planning
**Flow:**
1. Enter JIRA-456 → app fetches ticket details, comments, attachments, linked issues
2. Auto-identifies relevant codebase from project config
3. Vera dispatches agents to investigate
4. RCA report + impact assessment + fix tasks generated
5. Post RCA to Jira, create fix sub-tasks, assign to team

**Output:** RCA report, impact assessment, fix task prompts, Jira updates

### Mode 4: Remediation Task Generation
**Trigger:** From any audit, compliance check, or bug investigation
**Purpose:** Convert findings into self-contained Claude Code prompts
**Flow:** Select findings → Vera groups into tasks → export as .md, .zip, or Jira tickets
**Output:** Ordered task files, each a complete Claude Code prompt (air-gap methodology)

### Mode 5: Tech Debt Tracking
**Trigger:** Manual or scheduled
**Purpose:** Identify, categorize, score, and trend technical debt
**Flow:** Run debt scan → categorize (architecture, code, test, dependency, documentation) → score → track over time
**Output:** Debt inventory, debt score trends, prioritized remediation backlog

### Mode 6: Dependency Health
**Trigger:** Manual or scheduled
**Purpose:** Monitor dependency versions and known CVEs
**Flow:** Scan manifests → check CVE databases → identify outdated packages → generate update tasks
**Output:** Dependency report, CVE alerts, update task prompts

### Mode 7: Codebase Health Monitor
**Trigger:** Scheduled (daily, weekly, on-commit)
**Purpose:** Continuous quality monitoring with trends and alerts
**Flow:** Scheduled audit → compare to previous → update health score → alert if degraded
**Output:** Trend dashboards, threshold alerts, historical snapshots

---

## 3. GitHub Integration (Full GitHub Desktop Equivalent)

### Authentication
- Personal Access Token (stored in OS keychain via flutter_secure_storage)
- OAuth Device Flow (GitHub App)
- SSH key detection for clone/push

### GitHub API Capabilities

| Capability | API | Usage in CodeOps |
|-----------|-----|-----------------|
| List organizations | REST v3 | Org browser |
| List repositories | REST v3 | Repo browser within org |
| Get repository details | REST v3 | Metadata, languages, topics |
| List branches | REST v3 | Branch picker |
| List pull requests | REST v3 | PR list, status |
| Create pull request | REST v3 | Create PR after remediation |
| Get PR reviews | REST v3 | Review status |
| Merge pull request | REST v3 | Merge from app |
| List releases | REST v3 | Release history |
| Get workflow runs | REST v3 | CI status badge |
| Repository search | REST v3 | Find repos across orgs |
| Get commit history | REST v3 | Recent commits view |

### Local Git Operations (via CLI subprocess)

| Operation | Command | UI Component |
|-----------|---------|-------------|
| Clone | `git clone --branch {b} --progress {url} {dir}` | CloneDialog with progress |
| Pull | `git pull origin {branch}` | Pull button |
| Push | `git push origin {branch}` | Push button |
| Fetch | `git fetch --all --prune` | Background refresh |
| Checkout | `git checkout {branch}` | BranchPicker |
| Create branch | `git checkout -b {name}` | NewBranchDialog |
| Status | `git status --porcelain` | Status indicator |
| Diff | `git diff [--staged] [--stat]` | DiffViewer |
| Log | `git log --format=json -n {n}` | CommitHistory |
| Stash | `git stash [list\|pop\|drop]` | StashManager |
| Commit | `git add {files} && git commit -m "{msg}"` | CommitDialog |
| Merge | `git merge {branch}` | MergeDialog |
| Blame | `git blame {file}` | BlameView |
| Tag | `git tag {name}` | TagDialog |

### VCS Provider Abstraction

Even though Azure DevOps is deferred, the architecture uses a provider interface for clean future extension:

```dart
abstract class VcsProvider {
  String get providerName;
  Future<bool> authenticate(VcsCredentials credentials);
  Future<List<VcsOrganization>> getOrganizations();
  Future<List<VcsRepository>> getRepositories(String orgId);
  Future<List<VcsBranch>> getBranches(String repoId);
  Future<CloneProgress> cloneRepository(VcsRepository repo, String branch, String targetDir);
  Future<List<VcsPullRequest>> getPullRequests(String repoId);
  Future<VcsPullRequest> createPullRequest(CreatePRRequest request);
}

class GitHubProvider implements VcsProvider { ... }
// class AzureDevOpsProvider implements VcsProvider { ... }  // Future
```

### GitHub UI Components

```
lib/widgets/vcs/
├── github_auth_dialog.dart      # PAT or OAuth setup
├── org_browser.dart             # Browse GitHub organizations
├── repo_browser.dart            # Browse repos within org
├── repo_search.dart             # Search repos across orgs
├── clone_dialog.dart            # Clone: URL, branch, target, progress bar
├── branch_picker.dart           # Dropdown with search, current branch indicator
├── new_branch_dialog.dart       # Create branch from current
├── repo_status_bar.dart         # Branch | clean/dirty | ahead/behind | last commit
├── commit_dialog.dart           # Stage files + message + commit + optional push
├── diff_viewer.dart             # Side-by-side diff with syntax highlighting
├── commit_history.dart          # Recent commits timeline
├── pull_request_list.dart       # Open/closed/merged PRs
├── create_pr_dialog.dart        # Create PR: title, description, reviewers, base branch
├── merge_dialog.dart            # Merge branch with strategy selection
├── ci_status_badge.dart         # GitHub Actions workflow status
├── stash_manager.dart           # Stash list, pop, drop
└── tag_dialog.dart              # Create/list tags
```

---

## 4. Jira Integration

### Authentication
- API Token + email (Jira Cloud)
- Stored encrypted in cloud service, distributed to app via secure API

### Jira API Capabilities

| Capability | Endpoint | Usage in CodeOps |
|-----------|----------|-----------------|
| Search issues | `POST /search` (JQL) | Browse backlog, find bugs |
| Get issue | `GET /issue/{key}` | Fetch full ticket details |
| Get comments | `GET /issue/{key}/comment` | Investigation context |
| Get attachments | `GET /attachment/{id}` | Screenshots, logs |
| Get linked issues | `GET /issue/{key}` (links) | Related issue context |
| Post comment | `POST /issue/{key}/comment` | Post RCA, investigation results |
| Create issue | `POST /issue` | Create fix tasks |
| Create sub-task | `POST /issue` (subtask type) | Break fix into sub-tasks |
| Update issue | `PUT /issue/{key}` | Assign, update fields |
| Transition issue | `POST /issue/{key}/transitions` | Move through workflow |
| Get projects | `GET /project` | Project mapping |
| Get sprints | `GET /board/{id}/sprint` | Sprint context |
| Get issue types | `GET /project/{key}/statuses` | Type selection for creation |
| Get users | `GET /user/search` | Assignee picker |

### Jira ↔ Project Mapping

Each CodeOps project can be linked to a Jira project:
```
CodeOps Project: payment-service
  GitHub Repo: github.com/acme/payment-service
  Jira Project Key: PAY
  Jira Default Issue Type: Bug
  Jira Default Labels: ["backend", "payment-team"]
  Jira Component: Payment Processing
```

This mapping enables:
- Auto-detecting which codebase to investigate for a given Jira ticket
- Filtering Jira issues relevant to a specific repo
- Pre-filling Jira fields when creating tickets from tasks

### Jira UI Components

```
lib/widgets/jira/
├── jira_connection_dialog.dart   # Configure Jira instance + auth
├── jira_project_mapper.dart      # Map Jira projects to CodeOps projects
├── issue_search.dart             # JQL search with preset filters
├── issue_browser.dart            # Browse issues with status/priority/assignee filters
├── issue_card.dart               # Compact: key, summary, status, priority, assignee avatar
├── issue_detail_panel.dart       # Full: description, comments, attachments, links
├── issue_picker.dart             # Quick-pick: type issue key, auto-complete
├── rca_post_dialog.dart          # Preview RCA comment before posting to Jira
├── create_issue_dialog.dart      # Create Jira issue from task
├── bulk_create_dialog.dart       # Create multiple Jira issues from task list
├── assignee_picker.dart          # Search and select Jira user
└── sprint_selector.dart          # Select target sprint for created issues
```

### Bug Investigation Flow (Mode 3 — Detailed)

```
┌──────────────────────────────────────────────────┐
│ Investigate a Bug                                 │
├──────────────────────────────────────────────────┤
│ Step 1: Select Bug                               │
│                                                  │
│  Jira Ticket: [PAY-456     ] [🔍 Fetch]         │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ PAY-456: Payment fails > $10,000         │    │
│  │ Status: Open  Priority: High  Sprint: 24 │    │
│  │ Reporter: jane.doe  Assignee: —          │    │
│  │                                          │    │
│  │ Description:                             │    │
│  │ When processing payment over $10,000...  │    │
│  │                                          │    │
│  │ 📎 2 comments | 1 attachment | 3 links   │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  OR: [Browse Jira Backlog] to find the ticket    │
├──────────────────────────────────────────────────┤
│ Step 2: Configure                                │
│                                                  │
│  Project: payment-service (auto-detected)        │
│  Branch: main ▾                                  │
│  Additional context: [                        ]  │
│  Attach files: [+ Add logs/screenshots]          │
│  Agents: ☑ Code Quality ☑ Security ☑ API ...    │
├──────────────────────────────────────────────────┤
│ Step 3: Review & Launch                          │
│                                                  │
│  Investigation will analyze payment-service      │
│  using 5 agents to find root cause of PAY-456.   │
│                                                  │
│              [Cancel]  [Investigate →]            │
└──────────────────────────────────────────────────┘

→ Progress page shows agent grid investigating
→ Completion shows:
  - Root Cause Analysis report
  - Impact Assessment
  - Fix tasks (Claude Code prompts)
  - Actions: [Post RCA to Jira] [Create Sub-Tasks] [Assign] [Export]
```

---

## 5. AI Agent Architecture

### Vera — QA Manager
Orchestrator persona. Does not perform analysis — dispatches agents, deduplicates findings, produces executive summaries, determines pass/fail.

### Specialized Agents (12)

| Agent | Focus | Modes |
|-------|-------|-------|
| **Security** | Auth, injection, secrets, OWASP, CVEs | Audit, Bug, Compliance |
| **Code Quality** | Patterns, complexity, DRY, naming, SOLID | Audit, Bug, Tech Debt |
| **Build Health** | Configs, build stability, CI integration | Audit, Deps |
| **Completeness** | TODOs, stubs, placeholders, dead code | Audit, Compliance, Tech Debt |
| **API Contract** | REST conventions, OpenAPI, request/response | Audit, Compliance, Bug |
| **Test Coverage** | Test presence, quality, gaps, assertions | Audit, Compliance, Tech Debt |
| **UI/UX** | Components, accessibility, responsiveness | Audit, Compliance |
| **Documentation** | README, inline docs, API docs, changelogs | Audit, Tech Debt |
| **Database** | Schema, migrations, queries, indexing | Audit, Bug |
| **Performance** | N+1, memory, blocking calls, resource leaks | Audit, Bug |
| **Dependency** | Outdated versions, CVEs, license compliance | Deps, Audit |
| **Architecture** | Patterns, coupling, layering, modularity | Audit, Tech Debt |

### Persona + Directive Assembly

```
Built-in Persona (bundled with app, per agent type)
  → Overridden by Team Persona (if team has custom for that agent type)
    + Team Directives (coding standards, architecture rules — shared across all projects)
    + Project Directives (project-specific context, tech decisions, conventions)
    + Job Context (mode-specific: Jira ticket data, user notes, spec references)
    = Complete Agent Prompt (sent to Claude Code subprocess)
```

### Agent Execution

```dart
// Agent dispatch — spawns Claude Code as subprocess
final process = await Process.start('claude', [
  '--print',
  '--output-format', 'json',
  '--max-turns', '50',
  '--model', settings.claudeModel,
  '-p', assembledPrompt,
], workingDirectory: projectPath);
```

- Max concurrent agents: configurable, default 3
- Per-agent timeout: configurable, default 15 minutes
- Queue surplus agents if more than max selected
- Monitor via stdout/stderr parsing + process exit detection

### Standardized Report Format

Every agent outputs:
```markdown
# {Agent Name} — CodeOps Report

**Project:** {name}
**Date:** {ISO date}
**Agent:** {type}
**Overall:** PASS | WARN | FAIL
**Score:** {0-100}

## Executive Summary
{2-3 plain-language sentences}

## Findings

### [CRITICAL] {Title}
- **File:** {path}
- **Line:** {number}
- **Description:** {what's wrong}
- **Recommendation:** {how to fix}
- **Effort:** S | M | L | XL
- **Evidence:**
  ```{lang}
  {code}
  ```

## Metrics
| Metric | Value |
|--------|-------|
| Files Reviewed | X |
| Total Findings | Y |
| Critical / High / Medium / Low | a / b / c / d |
| Score | Z/100 |
```

---

## 6. Persona & Directive System

### Custom Personas

Users can create custom agent personas that override built-in ones:

| Feature | Description |
|---------|-------------|
| **Editor** | Markdown editor with live preview side panel |
| **Templates** | Start from any built-in persona as base |
| **Validation** | Required sections: Identity, Focus Areas, Severity Calibration, Output Format |
| **Scope** | SYSTEM (built-in, read-only), TEAM (shared), USER (personal) |
| **Versioning** | History of changes with diff view, revert capability |
| **Sharing** | Share within team, set as team default for an agent type |
| **Import/Export** | Import .md from disk, export to .md |
| **Testing** | "Test Persona" — run against a small code sample, preview output |

### Directives

Reusable .md context files that get injected into agent prompts:

| Feature | Description |
|---------|-------------|
| **Categories** | Architecture, Standards, Conventions, Context, Other |
| **Scope** | Team-wide (all projects) or project-specific |
| **Assignment** | Toggle which projects use which directives |
| **Library** | Browse team's directive library |
| **Bulk apply** | Apply directive to all projects at once |
| **Versioning** | Change history with revert |

### Example: Team Directive — "Java Standards.md"
```markdown
# Java Coding Standards — Acme Engineering

## Required Patterns
- Constructor injection only (no @Autowired on fields)
- All controllers: @PreAuthorize with role checks
- All config values in AppConstants.java — no magic numbers
- All new entities must have created_at and updated_at timestamps

## Naming
- Services: {Domain}Service.java
- Controllers: {Domain}Controller.java
- DTOs: {Domain}Request.java / {Domain}Response.java

## Error Handling
- All controllers return standardized error responses via @ControllerAdvice
- Never expose stack traces in API responses
```

### Example: Project Directive — "Payment Service Architecture.md"
```markdown
# Payment Service — Architecture Context

## Tech Stack
Java 21, Spring Boot 3.3, PostgreSQL, Redis, Kafka

## Critical Modules
- PaymentProcessor: core transaction handling, NEVER modify without senior review
- FraudDetection: ML-based, external service dependency
- ReconciliationEngine: nightly batch, complex state machine

## Known Constraints
- PCI DSS compliance required — no card data in logs
- Amount fields use BigDecimal, NEVER int/long
- All external API calls must have circuit breakers
```

---

## 7. Notification System

### Channels

| Channel | Implementation | Use Cases |
|---------|---------------|-----------|
| **In-app** | Riverpod state + notification panel | All events |
| **Email** | AWS SES via cloud service | Invitations, assignments, threshold alerts, weekly digests |
| **Microsoft Teams** | Teams Incoming Webhook | Job completions, threshold alerts, critical findings |

### Teams Integration

CodeOps posts to Microsoft Teams via Incoming Webhooks (configured per team):

```java
// Cloud service posts adaptive card to Teams webhook
TeamsMessage message = TeamsMessage.builder()
    .title("CodeOps — Audit Complete")
    .subtitle("payment-service | main branch")
    .facts(List.of(
        new Fact("Health Score", "72/100"),
        new Fact("Critical Findings", "3"),
        new Fact("High Findings", "12"),
        new Fact("Run By", "adam.allard")
    ))
    .actionUrl(deepLinkUrl)
    .build();

teamsWebhookClient.post(team.getTeamsWebhookUrl(), message);
```

### Notification Events

| Event | In-App | Email | Teams |
|-------|--------|-------|-------|
| Job completed | ✓ | ✗ | ✓ |
| Critical finding detected | ✓ | ✓ | ✓ |
| Health score dropped below threshold | ✓ | ✓ | ✓ |
| Task assigned to you | ✓ | ✓ | ✗ |
| Team invitation | ✓ | ✓ | ✗ |
| RCA posted to Jira | ✓ | ✗ | ✓ |
| Weekly health digest | ✗ | ✓ | ✓ |
| New CVE detected in dependencies | ✓ | ✓ | ✓ |

---

## 8. Auto-Update System

### Architecture

```
S3 Bucket: codeops-releases/
├── latest.json                    # Version manifest
├── releases/
│   ├── 1.0.0/
│   │   ├── CodeOps-1.0.0-macos-arm64.dmg
│   │   ├── CodeOps-1.0.0-macos-x64.dmg
│   │   ├── CodeOps-1.0.0-windows-x64.msix
│   │   ├── CodeOps-1.0.0-linux-x64.AppImage
│   │   └── checksums.sha256
│   └── 1.1.0/
│       └── ...
└── release-notes/
    ├── 1.0.0.md
    └── 1.1.0.md
```

### Version Manifest (latest.json)
```json
{
  "version": "1.1.0",
  "releaseDate": "2026-03-15",
  "releaseNotesUrl": "https://releases.codeops.dev/release-notes/1.1.0.md",
  "minimumVersion": "1.0.0",
  "rolloutPercent": 100,
  "platforms": {
    "macos-arm64": {
      "url": "https://releases.codeops.dev/releases/1.1.0/CodeOps-1.1.0-macos-arm64.dmg",
      "sha256": "abc123...",
      "size": 85000000
    },
    "macos-x64": {
      "url": "https://releases.codeops.dev/releases/1.1.0/CodeOps-1.1.0-macos-x64.dmg",
      "sha256": "def456...",
      "size": 87000000
    },
    "windows-x64": {
      "url": "https://releases.codeops.dev/releases/1.1.0/CodeOps-1.1.0-windows-x64.msix",
      "sha256": "ghi789...",
      "size": 72000000
    },
    "linux-x64": {
      "url": "https://releases.codeops.dev/releases/1.1.0/CodeOps-1.1.0-linux-x64.AppImage",
      "sha256": "jkl012...",
      "size": 80000000
    }
  }
}
```

### Update Flow
1. App checks `latest.json` on launch and every 4 hours
2. If `version > currentVersion` and within `rolloutPercent`:
   - Non-blocking banner: "CodeOps {version} available — [Update Now] [Release Notes] [Later]"
3. If `currentVersion < minimumVersion`:
   - Blocking dialog: "This version is no longer supported. Please update to continue."
4. "Update Now" → download installer → verify SHA-256 → launch installer → app exits
5. User completes install → relaunches → version confirmed

### Update Service (in Flutter app)
```dart
class UpdateService {
  static const _manifestUrl = 'https://releases.codeops.dev/latest.json';
  static const _checkIntervalHours = 4;

  Future<UpdateInfo?> checkForUpdate() async {
    final manifest = await _fetchManifest();
    if (manifest == null) return null;

    final current = Version.parse(AppConstants.appVersion);
    final latest = Version.parse(manifest.version);
    final minimum = Version.parse(manifest.minimumVersion);

    if (current < minimum) {
      return UpdateInfo(type: UpdateType.required, manifest: manifest);
    } else if (current < latest && _withinRollout(manifest.rolloutPercent)) {
      return UpdateInfo(type: UpdateType.available, manifest: manifest);
    }
    return null;
  }

  Future<void> downloadAndInstall(PlatformRelease release) async {
    final file = await _downloadWithProgress(release.url);
    final verified = await _verifySha256(file, release.sha256);
    if (!verified) throw UpdateVerificationException();
    await _launchInstaller(file);
  }
}
```

---

## 9. Cloud Service — Complete Specification

### Tech Stack
- Java 21
- Spring Boot 3.3
- PostgreSQL (AWS RDS)
- AWS S3 (reports, specs, persona files, release binaries)
- AWS SES (email notifications)
- Docker → AWS ECS (Fargate)
- Flyway (database migrations)
- Self-managed JWT (Cognito deferred)

### API Structure (14 Controllers)

```
/api/v1/auth                  — Register, login, refresh, password reset
/api/v1/users                 — User profile, search users
/api/v1/teams                 — Team CRUD, membership, invitations
/api/v1/projects              — Project CRUD, GitHub mapping, Jira mapping
/api/v1/jobs                  — Job sync (create, update, list, query)
/api/v1/findings              — Finding sync, bulk operations, queries
/api/v1/reports               — Report upload/download (S3)
/api/v1/tasks                 — Task sync, assignment, Jira mapping
/api/v1/personas              — Persona CRUD, sharing, versioning
/api/v1/directives            — Directive CRUD, project assignment
/api/v1/integrations          — GitHub + Jira connection configs
/api/v1/metrics               — Health scores, trends, aggregations
/api/v1/admin                 — User management, system settings, audit log
/api/v1/health                — Health check (no auth)
```

### Security
```java
// Every controller (except health):
@PreAuthorize("hasRole('ADMIN') or hasAuthority('specific:permission')")

// Role hierarchy
// OWNER  → Full control: delete team, manage billing (future)
// ADMIN  → User management, system settings, all CRUD
// MEMBER → Run jobs, manage findings, create personas/directives
// VIEWER → Read-only access to everything
```

### Constants (AppConstants.java)
```java
public final class AppConstants {
    private AppConstants() {}

    // Team limits
    public static final int MAX_TEAM_MEMBERS = 50;
    public static final int MAX_PROJECTS_PER_TEAM = 100;
    public static final int MAX_PERSONAS_PER_TEAM = 50;
    public static final int MAX_DIRECTIVES_PER_PROJECT = 20;

    // File size limits
    public static final int MAX_REPORT_SIZE_MB = 25;
    public static final int MAX_PERSONA_SIZE_KB = 100;
    public static final int MAX_DIRECTIVE_SIZE_KB = 200;
    public static final int MAX_SPEC_FILE_SIZE_MB = 50;

    // Auth
    public static final int JWT_EXPIRY_HOURS = 24;
    public static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
    public static final int INVITATION_EXPIRY_DAYS = 7;

    // Notifications
    public static final int HEALTH_DIGEST_DAY = 1;  // Monday
    public static final int HEALTH_DIGEST_HOUR = 8;  // 8 AM

    // S3 prefixes
    public static final String S3_REPORTS = "reports/";
    public static final String S3_SPECS = "specs/";
    public static final String S3_PERSONAS = "personas/";
    public static final String S3_RELEASES = "releases/";
}
```

### Database Schema (28 Tables)

#### Authentication & Users (4)
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL REFERENCES users(id),
    teams_webhook_url VARCHAR(500),    -- Microsoft Teams incoming webhook
    settings_json JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(team_id, user_id)
);

CREATE TABLE invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    invited_by UUID NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    token VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

#### Integration Connections (2)
```sql
CREATE TABLE github_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    auth_type VARCHAR(20) NOT NULL,      -- PAT, OAUTH, SSH
    encrypted_credentials JSONB NOT NULL,
    github_username VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE jira_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    instance_url VARCHAR(500) NOT NULL,
    email VARCHAR(255) NOT NULL,
    encrypted_api_token TEXT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
```

#### Projects (1)
```sql
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    -- GitHub mapping
    github_connection_id UUID REFERENCES github_connections(id),
    repo_url VARCHAR(500),
    repo_full_name VARCHAR(200),        -- "org/repo-name"
    default_branch VARCHAR(100) DEFAULT 'main',
    -- Jira mapping
    jira_connection_id UUID REFERENCES jira_connections(id),
    jira_project_key VARCHAR(20),
    jira_default_issue_type VARCHAR(50) DEFAULT 'Task',
    jira_labels JSONB DEFAULT '[]',
    jira_component VARCHAR(100),
    -- Metadata
    tech_stack VARCHAR(200),
    health_score INTEGER,
    last_audit_at TIMESTAMP,
    settings_json JSONB DEFAULT '{}',
    is_archived BOOLEAN DEFAULT false,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
```

#### QA & Analysis (5)
```sql
CREATE TABLE qa_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    name VARCHAR(200),
    branch VARCHAR(100),
    config_json JSONB,
    summary_md TEXT,
    overall_result VARCHAR(10),
    health_score INTEGER,
    total_findings INTEGER DEFAULT 0,
    critical_count INTEGER DEFAULT 0,
    high_count INTEGER DEFAULT 0,
    medium_count INTEGER DEFAULT 0,
    low_count INTEGER DEFAULT 0,
    jira_ticket_key VARCHAR(50),
    started_by UUID NOT NULL REFERENCES users(id),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE agent_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES qa_jobs(id) ON DELETE CASCADE,
    agent_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    result VARCHAR(10),
    report_s3_key VARCHAR(500),
    score INTEGER,
    findings_count INTEGER DEFAULT 0,
    critical_count INTEGER DEFAULT 0,
    high_count INTEGER DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE findings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES qa_jobs(id) ON DELETE CASCADE,
    agent_type VARCHAR(30) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    file_path VARCHAR(500),
    line_number INTEGER,
    recommendation TEXT,
    evidence TEXT,
    effort_estimate VARCHAR(10),
    debt_category VARCHAR(30),
    status VARCHAR(20) DEFAULT 'OPEN',
    status_changed_by UUID REFERENCES users(id),
    status_changed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE remediation_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES qa_jobs(id) ON DELETE CASCADE,
    task_number INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    prompt_md TEXT,
    prompt_s3_key VARCHAR(500),
    finding_ids JSONB,
    priority VARCHAR(5),
    status VARCHAR(20) DEFAULT 'PENDING',
    assigned_to UUID REFERENCES users(id),
    jira_key VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE bug_investigations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES qa_jobs(id) ON DELETE CASCADE,
    jira_key VARCHAR(50),
    jira_summary TEXT,
    jira_description TEXT,
    jira_comments_json JSONB,
    jira_attachments_json JSONB,
    jira_linked_issues JSONB,
    additional_context TEXT,
    rca_md TEXT,
    impact_assessment_md TEXT,
    rca_s3_key VARCHAR(500),
    rca_posted_to_jira BOOLEAN DEFAULT false,
    fix_tasks_created_in_jira BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

#### Compliance (2)
```sql
CREATE TABLE specifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES qa_jobs(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    spec_type VARCHAR(20),
    s3_key VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE compliance_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES qa_jobs(id) ON DELETE CASCADE,
    requirement TEXT NOT NULL,
    spec_id UUID REFERENCES specifications(id),
    status VARCHAR(20) NOT NULL,
    evidence TEXT,
    agent_type VARCHAR(30),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

#### Tech Debt & Dependencies (3)
```sql
CREATE TABLE tech_debt_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    file_path VARCHAR(500),
    effort_estimate VARCHAR(10),
    business_impact VARCHAR(10),
    status VARCHAR(20) DEFAULT 'IDENTIFIED',
    first_detected_job_id UUID REFERENCES qa_jobs(id),
    resolved_job_id UUID REFERENCES qa_jobs(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE dependency_scans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    job_id UUID REFERENCES qa_jobs(id),
    manifest_file VARCHAR(200),
    total_dependencies INTEGER,
    outdated_count INTEGER,
    vulnerable_count INTEGER,
    scan_data_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE dependency_vulnerabilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id UUID NOT NULL REFERENCES dependency_scans(id) ON DELETE CASCADE,
    dependency_name VARCHAR(200) NOT NULL,
    current_version VARCHAR(50),
    fixed_version VARCHAR(50),
    cve_id VARCHAR(30),
    severity VARCHAR(10) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

#### Health Monitoring (2)
```sql
CREATE TABLE health_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    schedule_type VARCHAR(20) NOT NULL,
    cron_expression VARCHAR(50),
    agent_types JSONB NOT NULL,
    is_active BOOLEAN DEFAULT true,
    last_run_at TIMESTAMP,
    next_run_at TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE health_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    job_id UUID REFERENCES qa_jobs(id),
    health_score INTEGER NOT NULL,
    findings_by_severity JSONB,
    tech_debt_score INTEGER,
    dependency_score INTEGER,
    test_coverage_percent NUMERIC(5,2),
    captured_at TIMESTAMP NOT NULL DEFAULT now()
);
```

#### Personas & Directives (3)
```sql
CREATE TABLE personas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    agent_type VARCHAR(30),
    description TEXT,
    content_md TEXT NOT NULL,
    scope VARCHAR(10) NOT NULL,
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id),
    is_default BOOLEAN DEFAULT false,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE directives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    content_md TEXT NOT NULL,
    category VARCHAR(50),
    scope VARCHAR(10) NOT NULL,
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id),
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE project_directives (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    directive_id UUID NOT NULL REFERENCES directives(id) ON DELETE CASCADE,
    enabled BOOLEAN DEFAULT true,
    PRIMARY KEY (project_id, directive_id)
);
```

#### System (3)
```sql
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    in_app BOOLEAN DEFAULT true,
    email BOOLEAN DEFAULT false,
    UNIQUE(user_id, event_type)
);

CREATE TABLE system_settings (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    team_id UUID REFERENCES teams(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(30),
    entity_id UUID,
    details JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

**Total: 28 tables**

### Cloud Service Layer (24 Services)

| Service | Purpose |
|---------|---------|
| AuthService | Register, login, JWT issue/refresh, password reset |
| UserService | User CRUD, profile, search |
| TeamService | Team CRUD, membership, multi-team support |
| InvitationService | Create, validate, accept invitations |
| ProjectService | Project CRUD, GitHub/Jira mapping |
| GitHubConnectionService | Credential storage, validation, encryption |
| JiraConnectionService | Credential storage, validation, encryption |
| JobSyncService | Receive/store job results from desktop app |
| FindingService | Finding CRUD, bulk operations, queries |
| ReportStorageService | S3 upload/download |
| TaskService | Task CRUD, assignment tracking |
| PersonaService | Persona CRUD, sharing, versioning |
| DirectiveService | Directive CRUD, project assignment |
| SpecificationService | Spec file storage |
| ComplianceService | Compliance item CRUD |
| BugInvestigationService | Bug/RCA storage |
| TechDebtService | Debt item tracking, trend calculation |
| DependencyService | Scan storage, CVE tracking |
| HealthMonitorService | Schedule management, snapshot storage |
| MetricsService | Aggregate health scores, trends, team metrics |
| NotificationService | Email (SES), Teams webhook, in-app |
| ExportService | Server-side PDF generation, bulk export |
| AdminService | System settings, usage stats |
| AuditLogService | Action logging |

---

## 10. Flutter Desktop App — Complete Specification

### Pages (24)

| # | Page | Route | Section |
|---|------|-------|---------|
| 1 | Login | `/login` | — |
| 2 | Setup Wizard | `/setup` | — |
| 3 | Home | `/` | — |
| 4 | Projects | `/projects` | Source |
| 5 | Project Detail | `/projects/:id` | Source |
| 6 | GitHub Browser | `/repos` | Source |
| 7 | Audit Wizard | `/audit` | Analyze |
| 8 | Compliance Wizard | `/compliance` | Analyze |
| 9 | Dependency Scan | `/dependencies` | Analyze |
| 10 | Bug Investigator | `/bugs` | Maintain |
| 11 | Jira Browser | `/bugs/jira` | Maintain |
| 12 | Task Manager | `/tasks` | Maintain |
| 13 | Tech Debt | `/tech-debt` | Maintain |
| 14 | Health Dashboard | `/health` | Monitor |
| 15 | Job History | `/history` | Monitor |
| 16 | Job Progress | `/jobs/:id` | (shared) |
| 17 | Job Report | `/jobs/:id/report` | (shared) |
| 18 | Findings Explorer | `/jobs/:id/findings` | (shared) |
| 19 | Task List | `/jobs/:id/tasks` | (shared) |
| 20 | Personas | `/personas` | Team |
| 21 | Persona Editor | `/personas/:id/edit` | Team |
| 22 | Directives | `/directives` | Team |
| 23 | Settings | `/settings` | — |
| 24 | Admin Hub | `/admin` | Admin |

### Widget Inventory (~65)

| Category | Widgets | Count |
|----------|---------|-------|
| **Wizard** | wizard_scaffold, source_step, agent_selector_step, threshold_step, spec_upload_step, jira_ticket_step, review_step, finding_filter_step | 8 |
| **Progress** | agent_status_grid, agent_card, job_progress_bar, live_findings_feed, phase_indicator, elapsed_timer | 6 |
| **Reports** | executive_summary_card, severity_chart, agent_report_tab, markdown_renderer, compliance_matrix, health_score_gauge, trend_chart, export_dialog | 8 |
| **Tasks** | task_list, task_card, task_detail, task_export_dialog, jira_create_dialog | 5 |
| **VCS/GitHub** | github_auth_dialog, org_browser, repo_browser, repo_search, clone_dialog, branch_picker, new_branch_dialog, repo_status_bar, commit_dialog, diff_viewer, commit_history, pull_request_list, create_pr_dialog, ci_status_badge, stash_manager | 15 |
| **Jira** | jira_connection_dialog, jira_project_mapper, issue_search, issue_browser, issue_card, issue_detail_panel, issue_picker, rca_post_dialog, create_issue_dialog, bulk_create_dialog, assignee_picker | 11 |
| **Dashboard** | quick_start_cards, recent_activity, project_health_grid, notification_panel | 4 |
| **Findings** | findings_table, finding_detail_panel, severity_filter_bar, finding_status_actions | 4 |
| **Personas** | persona_list, persona_editor, persona_preview, persona_test_runner | 4 |
| **Tech Debt** | debt_inventory, debt_trend_chart, debt_category_breakdown, debt_priority_matrix | 4 |
| **Dependencies** | dep_scan_results, cve_alert_card, dep_update_list, dep_health_gauge | 4 |
| **Admin** | user_table, invite_dialog, audit_log_viewer, usage_charts | 4 |
| **Shared** | loading_overlay, error_panel, empty_state, search_bar, confirm_dialog, notification_toast | 6 |
| **Total** | | **83** |

### Service Layer (26 Services)

```
lib/services/
├── auth/
│   ├── auth_service.dart              # Login, JWT management, refresh
│   └── secure_storage.dart            # OS keychain (tokens, API keys)
│
├── cloud/
│   ├── api_client.dart                # Dio + interceptors + auth headers
│   ├── user_api.dart                  # User endpoints
│   ├── team_api.dart                  # Team/membership endpoints
│   ├── project_api.dart               # Project endpoints
│   ├── job_api.dart                   # Job sync endpoints
│   ├── finding_api.dart               # Finding sync endpoints
│   ├── report_api.dart                # Report upload/download
│   ├── persona_api.dart               # Persona endpoints
│   ├── directive_api.dart             # Directive endpoints
│   ├── metrics_api.dart               # Metrics/trends endpoints
│   └── admin_api.dart                 # Admin endpoints
│
├── vcs/
│   ├── vcs_provider.dart              # Abstract VCS interface
│   ├── github_provider.dart           # GitHub API implementation
│   ├── git_service.dart               # Local git CLI wrapper
│   └── repo_manager.dart              # Local directory management
│
├── jira/
│   ├── jira_service.dart              # Jira REST API client
│   └── jira_mapper.dart               # Jira ↔ CodeOps model mapping
│
├── orchestration/
│   ├── job_orchestrator.dart          # Mode routing, lifecycle management
│   ├── agent_dispatcher.dart          # Spawn Claude Code subprocesses
│   ├── agent_monitor.dart             # Monitor processes, detect completion
│   ├── vera_manager.dart              # Post-analysis consolidation
│   └── progress_aggregator.dart       # Aggregate multi-agent progress
│
├── agent/
│   ├── persona_manager.dart           # Assemble persona + directives → prompt
│   ├── report_parser.dart             # Parse .md → structured findings
│   └── task_generator.dart            # Generate Claude Code prompts
│
├── analysis/
│   ├── tech_debt_tracker.dart         # Track debt items over time
│   ├── dependency_scanner.dart        # Parse manifests, check CVEs
│   └── health_calculator.dart         # Compute composite health scores
│
├── data/
│   ├── local_database.dart            # Drift SQLite (local cache)
│   ├── sync_service.dart              # Cloud ↔ local sync orchestration
│   └── cache_manager.dart             # Cache invalidation
│
├── integration/
│   └── export_service.dart            # PDF, Markdown, ZIP export
│
└── platform/
    ├── process_manager.dart           # Subprocess lifecycle
    ├── file_watcher.dart              # Watch directories for changes
    ├── claude_code_detector.dart      # Detect/validate Claude Code CLI
    ├── update_service.dart            # Auto-update check + download
    └── system_info.dart               # OS, paths, resources
```

### Riverpod Providers (14 files)

```
lib/providers/
├── auth_providers.dart
├── user_providers.dart
├── team_providers.dart
├── project_providers.dart
├── job_providers.dart
├── agent_providers.dart
├── finding_providers.dart
├── task_providers.dart
├── persona_providers.dart
├── directive_providers.dart
├── github_providers.dart
├── jira_providers.dart
├── health_providers.dart
└── settings_providers.dart
```

### Models (15 files)

```
lib/models/
├── user.dart
├── team.dart
├── project.dart
├── qa_job.dart
├── agent_run.dart
├── finding.dart
├── remediation_task.dart
├── specification.dart
├── compliance_item.dart
├── persona.dart
├── directive.dart
├── tech_debt_item.dart
├── dependency_scan.dart
├── health_snapshot.dart
└── enums.dart
```

---

## 11. Full Project Structure

```
codeops/
│
├── app/                                    # Flutter Desktop Application
│   ├── lib/
│   │   ├── main.dart
│   │   ├── app.dart                        # MaterialApp, theme, providers
│   │   ├── router.dart                     # GoRouter config
│   │   ├── models/                         (15 files)
│   │   ├── services/                       (26 files across 8 directories)
│   │   ├── providers/                      (14 files)
│   │   ├── pages/                          (24 files)
│   │   ├── widgets/                        (83 files across 13 directories)
│   │   ├── theme/
│   │   │   ├── app_theme.dart
│   │   │   ├── colors.dart
│   │   │   └── typography.dart
│   │   ├── utils/
│   │   │   ├── constants.dart              # ALL app constants
│   │   │   ├── markdown_utils.dart
│   │   │   ├── date_utils.dart
│   │   │   ├── file_utils.dart
│   │   │   └── string_utils.dart
│   │   └── database/
│   │       ├── database.dart               # Drift definition
│   │       ├── tables.dart                 # Local cache tables
│   │       └── daos/                       # Data access objects
│   │
│   ├── personas/                           # Built-in agent personas
│   │   ├── vera-manager.md
│   │   ├── agent-security.md
│   │   ├── agent-code-quality.md
│   │   ├── agent-build-health.md
│   │   ├── agent-completeness.md
│   │   ├── agent-api-contract.md
│   │   ├── agent-test-coverage.md
│   │   ├── agent-ui-ux.md
│   │   ├── agent-documentation.md
│   │   ├── agent-database.md
│   │   ├── agent-performance.md
│   │   └── agent-architecture.md
│   │
│   ├── templates/
│   │   ├── audit-report-template.md
│   │   ├── compliance-report-template.md
│   │   ├── task-prompt-template.md
│   │   ├── rca-template.md
│   │   └── executive-summary-template.md
│   │
│   ├── assets/
│   │   ├── icons/
│   │   ├── fonts/
│   │   └── images/
│   │
│   ├── test/
│   │   ├── services/
│   │   ├── providers/
│   │   └── widgets/
│   │
│   ├── macos/                              # macOS platform config
│   ├── windows/                            # Windows platform config
│   ├── linux/                              # Linux platform config
│   ├── pubspec.yaml
│   └── claude.md
│
├── service/                                # Cloud Microservice
│   ├── src/main/java/com/codeops/
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CorsConfig.java
│   │   │   ├── S3Config.java
│   │   │   ├── SesConfig.java
│   │   │   ├── JwtConfig.java
│   │   │   └── AppConstants.java
│   │   │
│   │   ├── controller/                     (14 controllers)
│   │   │   ├── AuthController.java
│   │   │   ├── UserController.java
│   │   │   ├── TeamController.java
│   │   │   ├── ProjectController.java
│   │   │   ├── JobController.java
│   │   │   ├── FindingController.java
│   │   │   ├── ReportController.java
│   │   │   ├── TaskController.java
│   │   │   ├── PersonaController.java
│   │   │   ├── DirectiveController.java
│   │   │   ├── IntegrationController.java
│   │   │   ├── MetricsController.java
│   │   │   ├── AdminController.java
│   │   │   └── HealthController.java
│   │   │
│   │   ├── dto/
│   │   │   ├── request/                    (~20 request DTOs)
│   │   │   └── response/                   (~20 response DTOs)
│   │   │
│   │   ├── entity/                         (16 entities)
│   │   │   ├── User.java
│   │   │   ├── Team.java
│   │   │   ├── TeamMember.java
│   │   │   ├── Invitation.java
│   │   │   ├── GitHubConnection.java
│   │   │   ├── JiraConnection.java
│   │   │   ├── Project.java
│   │   │   ├── QaJob.java
│   │   │   ├── AgentRun.java
│   │   │   ├── Finding.java
│   │   │   ├── RemediationTask.java
│   │   │   ├── Persona.java
│   │   │   ├── Directive.java
│   │   │   ├── TechDebtItem.java
│   │   │   ├── HealthSnapshot.java
│   │   │   └── AuditLog.java
│   │   │
│   │   ├── repository/                     (16 repositories)
│   │   ├── service/                        (24 services)
│   │   │
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthFilter.java
│   │   │   └── SecurityUtils.java
│   │   │
│   │   ├── notification/
│   │   │   ├── EmailService.java           # AWS SES
│   │   │   ├── TeamsWebhookService.java    # Microsoft Teams
│   │   │   └── NotificationDispatcher.java # Route events to channels
│   │   │
│   │   └── CodeOpsApplication.java
│   │
   │   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── application-prod.yml
│   │
│   ├── src/test/
│   ├── Dockerfile
│   ├── pom.xml
│   └── claude.md
│
├── infrastructure/                         # AWS deployment
│   ├── terraform/                          # or CDK
│   │   ├── ecs.tf
│   │   ├── rds.tf
│   │   ├── s3.tf
│   │   ├── ses.tf
│   │   ├── cloudfront.tf
│   │   └── networking.tf
│   └── docker-compose.yml                  # Local dev
│
├── docs/
│   ├── architecture.md
│   ├── api-spec.yaml                       # OpenAPI 3.0
│   ├── github-integration.md
│   ├── jira-integration.md
│   ├── deployment.md
│   ├── personas-guide.md
│   └── user-guide.md
│
├── README.md
└── claude.md                               # Repo-level Claude Code config
```

---

## 12. Size Estimates

| Component | Files | Est. LOC |
|-----------|-------|----------|
| **Flutter App** | | |
| Models | 15 | 2,000 |
| Services | 26 | 5,500 |
| Providers | 14 | 2,000 |
| Pages | 24 | 7,500 |
| Widgets | 83 | 15,000 |
| Theme/Utils/DB | 15 | 2,500 |
| **App subtotal** | **177** | **34,500** |
| **Cloud Service** | | |
| Controllers | 14 | 2,500 |
| Services | 24 | 5,500 |
| Entities | 16 | 2,200 |
| DTOs | 40 | 3,000 |
| Config/Security/Notification | 12 | 1,800 |
| **Service subtotal** | **106** | **15,000** |
| **Personas + Templates** | 17 | 4,000 |
| **Infrastructure** | ~10 | 500 |
| **Docs** | 7 | — |
| **Grand total** | **~317** | **~54,000** |

---

## 13. Dependencies

### Flutter App (pubspec.yaml)
```yaml
dependencies:
  flutter:
    sdk: flutter

  # State management
  flutter_riverpod: ^2.5.0
  riverpod_annotation: ^2.3.0

  # Navigation
  go_router: ^14.0.0

  # Database (local cache)
  drift: ^2.16.0
  sqlite3_flutter_libs: ^0.5.0

  # HTTP
  dio: ^5.4.0

  # UI
  flutter_markdown: ^0.7.0
  flutter_highlight: ^0.7.0
  fl_chart: ^0.68.0
  file_picker: ^8.0.0
  desktop_drop: ^0.4.0
  window_manager: ^0.4.0
  split_view: ^3.2.0              # Side-by-side diff panels

  # Utilities
  path: ^1.9.0
  path_provider: ^2.1.0
  uuid: ^4.3.0
  intl: ^0.19.0
  yaml: ^3.1.0
  archive: ^3.6.0                 # ZIP export
  url_launcher: ^6.2.0
  flutter_secure_storage: ^9.0.0  # OS keychain
  crypto: ^3.0.0                  # SHA-256 verification
  package_info_plus: ^8.0.0       # App version detection
  connectivity_plus: ^6.0.0       # Online/offline detection

  # PDF
  pdf: ^3.10.0
  printing: ^5.12.0

dev_dependencies:
  build_runner: ^2.4.0
  drift_dev: ^2.16.0
  riverpod_generator: ^2.4.0
  flutter_test:
    sdk: flutter
  mocktail: ^1.0.0
  integration_test:
    sdk: flutter
```

### Cloud Service (pom.xml key dependencies)
```xml
<!-- Spring Boot 3.3 Starter -->
<spring-boot.version>3.3.0</spring-boot.version>
<java.version>21</java.version>

<!-- Core -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation

<!-- Database -->
postgresql (runtime)
<!-- Flyway added at production cutover, not during dev -->

<!-- AWS -->
aws-java-sdk-s3
aws-java-sdk-ses

<!-- Auth -->
jjwt-api / jjwt-impl / jjwt-jackson (io.jsonwebtoken 0.12.x)

<!-- Utilities -->
lombok
mapstruct
jackson-databind
springdoc-openapi-starter-webmvc-ui

<!-- Testing -->
spring-boot-starter-test
testcontainers (postgresql)
```

---

## 14. Build Sequence (AI-First Velocity)

Based on proven delivery rates (78-screen Talent App in days, 48-screen Client Portal in a day, Elaro over a weekend, REACH in a day), this build uses the established AI-first methodology: Claude Opus for architecture and prompt creation, Claude Code for 100% code generation, human architectural oversight only.

### Day 1: Foundation + Cloud Service (Complete)
**Morning — CodeOps-Server (all of it):**
- Spring Boot skeleton, Docker, docker-compose, security config
- All 16 JPA entities with Hibernate ddl-auto=update (Flyway deferred to production)
- All 28 tables generated automatically from entity annotations
- JWT auth (register, login, refresh, password reset)
- All 14 controllers, all 24 services, all 16 repositories, all DTOs
- S3 integration, SES config, Teams webhook service
- AppConstants.java with all constants
- Health endpoint, OpenAPI spec generation
- Dockerfile, application.yml (dev + prod)

**Afternoon — CodeOps-Client Skeleton:**
- Flutter desktop project, window management, GoRouter (all 24 routes)
- Theme, colors, typography
- Drift local database (all cache tables)
- All 15 models + enums.dart
- API client (Dio) with auth interceptors
- Auth service + secure storage
- Login page, home page with quick-start cards
- Navigation shell with all sidebar sections
- All 14 provider files (stubs wired to API)

### Day 2: GitHub + VCS + Projects
**Morning — GitHub Integration:**
- VCS provider abstraction + GitHubProvider implementation
- Git service (full CLI wrapper — all operations)
- Repo manager (local directory management)
- All 15 VCS widgets (clone, branch, commit, diff, PR, status, etc.)
- GitHub auth dialog, org browser, repo browser

**Afternoon — Projects + Repo Dashboard:**
- Projects page (list, favorites, add new)
- Project detail page (repo dashboard with health, activity, actions)
- Project settings (GitHub mapping, Jira mapping, directives)
- Cloud sync for projects
- Claude Code detector + setup wizard page

### Day 3: Core AI Engine + Audit (Mode 1)
**Morning — Agent Infrastructure (CodeOps-Client):**
- Agent dispatcher (subprocess spawning)
- Agent monitor (process monitoring, completion detection)
- Vera manager (consolidation pass)
- Progress aggregator
- Report parser (markdown → structured findings)
- Persona manager (persona + directive assembly)
- All 12 built-in persona .md files
- All 5 report/task templates

**Afternoon — Audit Workflow + Reports:**
- Audit wizard page (all wizard widgets)
- Job progress page (agent status grid, live findings, timer)
- Job report page (executive summary, agent tabs, markdown renderer)
- Findings explorer page (table, filters, detail panel)
- Export dialog (PDF, MD, ZIP)
- Health score calculator
- Job sync to cloud

### Day 4: Jira + Bug Investigation + Tasks
**Morning — Jira Integration:**
- Jira service (full REST client — all operations)
- Jira mapper
- All 11 Jira widgets (browser, picker, detail, search, create, bulk create)
- Jira browser page
- Jira-project mapping in project settings

**Afternoon — Bug Investigation (Mode 3) + Remediation Tasks (Mode 4):**
- Bug investigator page (full wizard flow)
- RCA generation orchestration
- RCA post to Jira
- Fix task generation from investigations
- Task manager page
- Task list, detail, export
- Jira issue creation from tasks (single + bulk)
- Task assignment to team members

### Day 5: Compliance + Tech Debt + Dependencies + Health
**Morning — Compliance (Mode 2) + Tech Debt (Mode 5):**
- Compliance wizard page
- Spec upload widgets
- Compliance matrix widget
- Gap analysis report
- Tech debt page + all 4 widgets
- Tech debt tracker service
- Debt trend calculations

**Afternoon — Dependencies (Mode 6) + Health Monitor (Mode 7):**
- Dependency scanner service
- Dependency scan page + all 4 widgets
- CVE checking integration
- Health dashboard page
- Health schedule configuration
- Health snapshot service
- Trend charts (fl_chart)
- All metrics API integration

### Day 6: Personas + Directives + Notifications + Admin
**Morning — Personas & Directives:**
- Personas page
- Persona editor page (markdown editor + live preview)
- Persona test runner
- Persona versioning + sharing
- Directives page
- Directive editor
- Project-directive assignment UI

**Afternoon — Notifications + Admin + Auto-Update:**
- Notification panel widget
- Notification preferences in settings
- Teams webhook integration (adaptive cards)
- Email notification templates
- Admin hub page (users, team settings, audit log, usage)
- All 4 admin widgets
- Settings page (all preferences, thresholds, integrations)
- Auto-update service + update banner/dialog

### Day 7: Polish + Packaging
- Integration testing across all workflows
- Error handling and edge cases
- Offline mode verification
- Desktop packaging: macOS DMG, Windows MSIX, Linux AppImage
- Docker production build for cloud service
- Terraform/IaC for AWS deployment
- Documentation pass

**Total: 7 days to production-ready platform**

---

## 15. Key Technical Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Product name | CodeOps | Software maintenance + operations |
| Desktop framework | Flutter | Cross-platform, your team's expertise, native file/process access |
| Cloud backend | Java 21 / Spring Boot | Ecosystem consistency, your team's expertise |
| Database | PostgreSQL (RDS) | Reliable, scalable, JSON support |
| File storage | AWS S3 | Reports, specs, personas at any scale |
| Auth | Self-managed JWT | Simple, Cognito later if needed |
| VCS | GitHub only (v1) | Provider abstraction ready for Azure DevOps later |
| Issue tracking | Jira Cloud | Primary integration, deeply embedded in workflows |
| Agent engine | Claude Code CLI | Local subprocess, air-gap methodology |
| State management | Riverpod | Recommended for Flutter, reactive, testable |
| Local database | Drift (SQLite) | Cache layer, offline capability |
| Notifications | In-app + Email (SES) + Teams | Covers all team communication channels |
| Auto-update | S3 manifest + direct download | No app store dependency, staged rollouts |
| Deployment | Docker → ECS Fargate | Serverless containers, minimal ops |
| DB Migrations | Hibernate ddl-auto (dev), Flyway (prod) | Fast iteration in dev, controlled migrations in prod |
| IaC | Terraform | Standard, well-supported |
| Multi-team | Yes | User can belong to multiple teams |
| Azure DevOps | Deferred | VCS abstraction ready, implement when needed |
