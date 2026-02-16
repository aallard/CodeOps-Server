# CodeOps Database Schema Audit

Generated: 2026-01-31
Database: PostgreSQL 16
Schema: `core`

---

## Tables Overview

| Table | Description |
|-------|-------------|
| tenants | Multi-tenant organizations |
| roles | Role definitions with permissions |
| permissions | Available permissions |
| role_permissions | Many-to-many: roles ↔ permissions |
| users | User accounts (belong to tenant, have role) |
| teams | Teams within a tenant |
| team_members | Many-to-many: teams ↔ users |
| outcomes | Business outcomes to achieve |
| hypotheses | Hypotheses linked to outcomes |
| decisions | Decisions (can link to outcomes/hypotheses) |
| decision_comments | Comments on decisions |
| stakeholders | People who make decisions |
| audit_logs | Audit trail |

---

## Table Schemas

### tenants
```sql
CREATE TABLE core.tenants (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    logo_url        VARCHAR(255),
    settings        TEXT,                           -- JSON
    status          VARCHAR(255),                   -- ENUM: see below
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE
);
-- status: ACTIVE | SUSPENDED | TRIAL | CANCELLED
```

### roles
```sql
CREATE TABLE core.roles (
    id              UUID PRIMARY KEY,
    tenant_id       UUID,                           -- NULL = system role
    code            VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    category        VARCHAR(255) NOT NULL,          -- ENUM: see below
    level           VARCHAR(255) NOT NULL,          -- ENUM: see below
    is_system_role  BOOLEAN,
    created_at      TIMESTAMP WITH TIME ZONE,
    UNIQUE (tenant_id, code)
);
-- category: SYSTEM | EXECUTIVE | PRODUCT | ENGINEERING | UX | QA | DATA | BUSINESS | STAKEHOLDER | CUSTOM
-- level: L1_INDIVIDUAL | L2_SENIOR | L3_LEAD | L4_MANAGER | L5_SENIOR_MANAGER | L6_VP | L7_SVP | L8_CXXX | L9_OWNER
```

### permissions
```sql
CREATE TABLE core.permissions (
    id              UUID PRIMARY KEY,
    code            VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    category        VARCHAR(255) NOT NULL
);
```

### role_permissions
```sql
CREATE TABLE core.role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(id),
    permission_id   UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);
```

### users
```sql
CREATE TABLE core.users (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(255) NOT NULL,          -- Split name fields
    last_name       VARCHAR(255) NOT NULL,
    title           VARCHAR(255),
    department      VARCHAR(255),
    avatar_url      VARCHAR(255),
    role_id         UUID NOT NULL REFERENCES roles(id),
    manager_id      UUID REFERENCES users(id),
    is_active       BOOLEAN,
    last_login_at   TIMESTAMP WITH TIME ZONE,
    refresh_token   VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE,
    UNIQUE (tenant_id, email)
);
```

### teams
```sql
CREATE TABLE core.teams (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    icon_url        VARCHAR(255),
    color           VARCHAR(255),
    lead_id         UUID REFERENCES users(id),
    is_active       BOOLEAN,
    settings        TEXT,                           -- JSON
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE,
    UNIQUE (tenant_id, slug)
);
```

### team_members
```sql
CREATE TABLE core.team_members (
    id              UUID PRIMARY KEY,
    team_id         UUID NOT NULL REFERENCES teams(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    team_role       VARCHAR(255) NOT NULL,          -- ENUM: see below
    joined_at       TIMESTAMP WITH TIME ZONE,
    added_by_id     UUID,
    UNIQUE (team_id, user_id)
);
-- team_role: LEAD | MEMBER | OBSERVER
```

### outcomes
```sql
CREATE TABLE core.outcomes (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    success_criteria    TEXT,
    target_metrics      TEXT,                       -- JSON
    current_metrics     TEXT,                       -- JSON
    status              VARCHAR(255) NOT NULL,      -- ENUM: see below
    priority            VARCHAR(255) NOT NULL,      -- ENUM: see below
    team_id             UUID REFERENCES teams(id),
    owner_id            UUID REFERENCES users(id),
    target_date         DATE,
    started_at          TIMESTAMP WITH TIME ZONE,
    validated_at        TIMESTAMP WITH TIME ZONE,
    invalidated_at      TIMESTAMP WITH TIME ZONE,
    validation_notes    TEXT,
    validated_by_id     UUID REFERENCES users(id),
    invalidated_by_id   UUID REFERENCES users(id),
    external_refs       TEXT,                       -- JSON
    tags                TEXT,                       -- JSON
    created_at          TIMESTAMP WITH TIME ZONE,
    updated_at          TIMESTAMP WITH TIME ZONE,
    created_by_id       UUID
);
-- status: DRAFT | NOT_STARTED | IN_PROGRESS | VALIDATING | VALIDATED | INVALIDATED | ABANDONED
-- priority: CRITICAL | HIGH | MEDIUM | LOW | BACKLOG
```

### hypotheses
```sql
CREATE TABLE core.hypotheses (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    outcome_id              UUID NOT NULL REFERENCES outcomes(id),
    title                   VARCHAR(500) NOT NULL,
    belief                  TEXT NOT NULL,
    expected_result         TEXT NOT NULL,
    measurement_criteria    TEXT,
    status                  VARCHAR(255) NOT NULL,  -- ENUM: see below
    priority                VARCHAR(255) NOT NULL,  -- ENUM: see below
    owner_id                UUID REFERENCES users(id),
    experiment_config       TEXT,                   -- JSON
    experiment_results      TEXT,                   -- JSON
    blocked_reason          TEXT,
    conclusion_notes        TEXT,
    external_refs           TEXT,                   -- JSON
    tags                    TEXT,                   -- JSON
    started_at              TIMESTAMP WITH TIME ZONE,
    deployed_at             TIMESTAMP WITH TIME ZONE,
    measuring_started_at    TIMESTAMP WITH TIME ZONE,
    concluded_at            TIMESTAMP WITH TIME ZONE,
    concluded_by_id         UUID REFERENCES users(id),
    created_at              TIMESTAMP WITH TIME ZONE,
    updated_at              TIMESTAMP WITH TIME ZONE,
    created_by_id           UUID
);
-- status: DRAFT | READY | BLOCKED | BUILDING | DEPLOYED | MEASURING | VALIDATED | INVALIDATED | ABANDONED
-- priority: CRITICAL | HIGH | MEDIUM | LOW
```

### decisions
```sql
CREATE TABLE core.decisions (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    context             TEXT,
    options             TEXT,                       -- JSON array of options
    status              VARCHAR(255) NOT NULL,      -- ENUM: see below
    priority            VARCHAR(255) NOT NULL,      -- ENUM: see below
    decision_type       VARCHAR(255),               -- ENUM: see below
    owner_id            UUID REFERENCES users(id),
    assigned_to_id      UUID REFERENCES users(id),
    outcome_id          UUID REFERENCES outcomes(id),
    hypothesis_id       UUID REFERENCES hypotheses(id),
    team_id             UUID REFERENCES teams(id),
    sla_hours           INTEGER,
    due_at              TIMESTAMP WITH TIME ZONE,
    escalation_level    INTEGER,
    escalated_at        TIMESTAMP WITH TIME ZONE,
    escalated_to_id     UUID REFERENCES users(id),
    decided_by_id       UUID REFERENCES users(id),
    decided_at          TIMESTAMP WITH TIME ZONE,
    decision_rationale  TEXT,
    selected_option     TEXT,                       -- JSON
    blocked_items       TEXT,                       -- JSON
    external_refs       TEXT,                       -- JSON
    tags                TEXT,                       -- JSON
    created_at          TIMESTAMP WITH TIME ZONE,
    updated_at          TIMESTAMP WITH TIME ZONE,
    created_by_id       UUID
);
-- status: NEEDS_INPUT | UNDER_DISCUSSION | DECIDED | IMPLEMENTED | DEFERRED | CANCELLED
-- priority: BLOCKING | HIGH | NORMAL | LOW
-- decision_type: PRODUCT | UX | TECHNICAL | ARCHITECTURAL | STRATEGIC | OPERATIONAL | RESOURCE | SCOPE | TIMELINE
```

### decision_comments
```sql
CREATE TABLE core.decision_comments (
    id              UUID PRIMARY KEY,
    decision_id     UUID NOT NULL REFERENCES decisions(id),
    author_id       UUID NOT NULL REFERENCES users(id),
    parent_id       UUID REFERENCES decision_comments(id),
    content         TEXT NOT NULL,
    option_id       VARCHAR(255),                   -- References an option in decision.options
    is_edited       BOOLEAN,
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE
);
```

### stakeholders
```sql
CREATE TABLE core.stakeholders (
    id                          UUID PRIMARY KEY,
    tenant_id                   UUID NOT NULL,
    name                        VARCHAR(255) NOT NULL,
    email                       VARCHAR(255) NOT NULL,
    title                       VARCHAR(255),
    organization                VARCHAR(255),
    phone                       VARCHAR(255),
    avatar_url                  VARCHAR(255),
    type                        VARCHAR(255) NOT NULL,  -- ENUM: see below
    user_id                     UUID REFERENCES users(id),  -- NULL if external
    expertise                   TEXT,                   -- JSON array
    preferred_contact_method    VARCHAR(255),
    availability_notes          TEXT,
    timezone                    VARCHAR(255),
    decisions_pending           INTEGER,
    decisions_completed         INTEGER,
    decisions_escalated         INTEGER,
    avg_response_time_hours     DOUBLE PRECISION,
    last_decision_at            TIMESTAMP WITH TIME ZONE,
    is_active                   BOOLEAN,
    notes                       TEXT,
    external_refs               TEXT,                   -- JSON
    created_at                  TIMESTAMP WITH TIME ZONE,
    updated_at                  TIMESTAMP WITH TIME ZONE,
    created_by_id               UUID
);
-- type: INTERNAL | EXTERNAL | EXECUTIVE | CUSTOMER | REGULATORY | TECHNICAL
```

### audit_logs
```sql
CREATE TABLE core.audit_logs (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    actor_id        UUID,
    actor_name      VARCHAR(255),
    actor_email     VARCHAR(255),
    action          VARCHAR(255) NOT NULL,          -- ENUM: see below
    entity_type     VARCHAR(255) NOT NULL,
    entity_id       UUID NOT NULL,
    entity_title    VARCHAR(255),
    description     TEXT,
    old_values      TEXT,                           -- JSON
    new_values      TEXT,                           -- JSON
    ip_address      VARCHAR(255),
    user_agent      VARCHAR(255),
    request_id      VARCHAR(255),
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL
);
-- action: CREATE | READ | UPDATE | DELETE | STATUS_CHANGE | DECISION_RESOLVED | DECISION_ESCALATED |
--         DECISION_ASSIGNED | DECISION_COMMENTED | OUTCOME_VALIDATED | OUTCOME_INVALIDATED |
--         HYPOTHESIS_TRANSITIONED | HYPOTHESIS_CONCLUDED | HYPOTHESIS_BLOCKED | HYPOTHESIS_UNBLOCKED |
--         LOGIN | LOGOUT | PASSWORD_CHANGED | USER_CREATED | USER_DEACTIVATED | ROLE_ASSIGNED |
--         TEAM_MEMBER_ADDED | TEAM_MEMBER_REMOVED
```

---

## ENUM Values Summary

| Table | Field | Valid Values |
|-------|-------|--------------|
| tenants | status | `ACTIVE`, `SUSPENDED`, `TRIAL`, `CANCELLED` |
| roles | category | `SYSTEM`, `EXECUTIVE`, `PRODUCT`, `ENGINEERING`, `UX`, `QA`, `DATA`, `BUSINESS`, `STAKEHOLDER`, `CUSTOM` |
| roles | level | `L1_INDIVIDUAL`, `L2_SENIOR`, `L3_LEAD`, `L4_MANAGER`, `L5_SENIOR_MANAGER`, `L6_VP`, `L7_SVP`, `L8_CXXX`, `L9_OWNER` |
| team_members | team_role | `LEAD`, `MEMBER`, `OBSERVER` |
| outcomes | status | `DRAFT`, `NOT_STARTED`, `IN_PROGRESS`, `VALIDATING`, `VALIDATED`, `INVALIDATED`, `ABANDONED` |
| outcomes | priority | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `BACKLOG` |
| hypotheses | status | `DRAFT`, `READY`, `BLOCKED`, `BUILDING`, `DEPLOYED`, `MEASURING`, `VALIDATED`, `INVALIDATED`, `ABANDONED` |
| hypotheses | priority | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
| decisions | status | `NEEDS_INPUT`, `UNDER_DISCUSSION`, `DECIDED`, `IMPLEMENTED`, `DEFERRED`, `CANCELLED` |
| decisions | priority | `BLOCKING`, `HIGH`, `NORMAL`, `LOW` |
| decisions | decision_type | `PRODUCT`, `UX`, `TECHNICAL`, `ARCHITECTURAL`, `STRATEGIC`, `OPERATIONAL`, `RESOURCE`, `SCOPE`, `TIMELINE` |
| stakeholders | type | `INTERNAL`, `EXTERNAL`, `EXECUTIVE`, `CUSTOMER`, `REGULATORY`, `TECHNICAL` |

---

## Foreign Key Relationships

```
tenants
    └── users.tenant_id
    └── teams.tenant_id
    └── outcomes.tenant_id
    └── hypotheses.tenant_id
    └── decisions.tenant_id
    └── stakeholders.tenant_id

roles
    └── users.role_id
    └── role_permissions.role_id

permissions
    └── role_permissions.permission_id

users
    └── users.manager_id (self-reference)
    └── teams.lead_id
    └── team_members.user_id
    └── outcomes.owner_id, validated_by_id, invalidated_by_id
    └── hypotheses.owner_id, concluded_by_id
    └── decisions.owner_id, assigned_to_id, decided_by_id, escalated_to_id
    └── decision_comments.author_id
    └── stakeholders.user_id

teams
    └── team_members.team_id
    └── outcomes.team_id
    └── decisions.team_id

outcomes
    └── hypotheses.outcome_id
    └── decisions.outcome_id

hypotheses
    └── decisions.hypothesis_id

decisions
    └── decision_comments.decision_id

decision_comments
    └── decision_comments.parent_id (self-reference for threading)
```

---

## Insert Order (for seed data)

Due to foreign key constraints, insert data in this order:

1. `tenants`
2. `roles` (system roles first with tenant_id = NULL, then tenant-specific)
3. `permissions`
4. `role_permissions`
5. `users`
6. `teams`
7. `team_members`
8. `stakeholders`
9. `outcomes`
10. `hypotheses`
11. `decisions`
12. `decision_comments`

---

## Notes for Seed Data

1. **Password Hash**: Use BCrypt. Example for "password123":
   ```
   $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBj0kKJYq8YBLqoFNDgqDBbOLq
   ```

2. **UUIDs**: Use predictable UUIDs for seed data to allow easy reference:
   - Tenant 1: `11111111-1111-1111-1111-111111111111`
   - Tenant 2: `22222222-2222-2222-2222-222222222222`
   - Users: `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001` through `...006`

3. **Timestamps**: Use `NOW()` with `INTERVAL` for realistic historical data:
   ```sql
   NOW() - INTERVAL '30 days'
   ```

4. **JSON Fields**: Store as TEXT, format as valid JSON:
   ```sql
   '{"key": "value"}'
   '["item1", "item2"]'
   ```

5. **Schema**: All tables are in the `core` schema. Prefix with `core.` or set search_path:
   ```sql
   SET search_path TO core, public;
   ```
