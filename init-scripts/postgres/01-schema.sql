-- ============================================
-- CodeOps Database Schema
-- ============================================
-- Creates all tables, indexes, and constraints
-- Seed data is loaded separately via seed-data.sql

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create schemas (analytics and integrations are separate services)
CREATE SCHEMA IF NOT EXISTS analytics;
CREATE SCHEMA IF NOT EXISTS integrations;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA analytics TO codeops;
GRANT ALL PRIVILEGES ON SCHEMA integrations TO codeops;

-- Set default search path
ALTER USER codeops SET search_path TO public, analytics, integrations;

-- ============================================
-- PUBLIC SCHEMA TABLES
-- ============================================

-- TENANTS
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    logo_url VARCHAR(500),
    settings TEXT,  -- JSON
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- PERMISSIONS
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL
);

-- ROLES
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(20) NOT NULL,
    level VARCHAR(20) NOT NULL,
    is_system_role BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(tenant_id, code)
);

-- ROLE_PERMISSIONS (join table)
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- USERS
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    title VARCHAR(100),
    department VARCHAR(100),
    avatar_url VARCHAR(500),
    role_id UUID NOT NULL REFERENCES roles(id),
    manager_id UUID REFERENCES users(id),
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    refresh_token VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(tenant_id, email)
);

CREATE INDEX IF NOT EXISTS idx_user_tenant_id ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_tenant_active ON users(tenant_id, is_active);
CREATE INDEX IF NOT EXISTS idx_user_role_id ON users(role_id);

-- TEAMS
CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    color VARCHAR(20),
    lead_id UUID REFERENCES users(id),
    is_active BOOLEAN DEFAULT TRUE,
    settings TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(tenant_id, slug)
);

-- TEAM_MEMBERS
CREATE TABLE IF NOT EXISTS team_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    added_by_id UUID,
    UNIQUE(team_id, user_id)
);

-- STAKEHOLDERS
CREATE TABLE IF NOT EXISTS stakeholders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    title VARCHAR(100),
    organization VARCHAR(255),
    phone VARCHAR(50),
    avatar_url VARCHAR(500),
    type VARCHAR(20) NOT NULL DEFAULT 'INTERNAL',
    user_id UUID REFERENCES users(id),
    expertise TEXT,
    preferred_contact_method VARCHAR(50),
    availability_notes TEXT,
    timezone VARCHAR(50),
    decisions_pending INTEGER DEFAULT 0,
    decisions_completed INTEGER DEFAULT 0,
    decisions_escalated INTEGER DEFAULT 0,
    avg_response_time_hours DECIMAL(10,2),
    last_decision_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    external_refs TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by_id UUID
);

CREATE INDEX IF NOT EXISTS idx_stakeholder_tenant ON stakeholders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stakeholder_user ON stakeholders(user_id);

-- OUTCOMES
CREATE TABLE IF NOT EXISTS outcomes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    success_criteria TEXT,
    target_metrics TEXT,
    current_metrics TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    team_id UUID REFERENCES teams(id),
    owner_id UUID REFERENCES users(id),
    target_date DATE,
    started_at TIMESTAMP WITH TIME ZONE,
    validated_at TIMESTAMP WITH TIME ZONE,
    invalidated_at TIMESTAMP WITH TIME ZONE,
    validation_notes TEXT,
    validated_by_id UUID REFERENCES users(id),
    invalidated_by_id UUID REFERENCES users(id),
    external_refs TEXT,
    tags TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by_id UUID
);

CREATE INDEX IF NOT EXISTS idx_outcome_tenant_status ON outcomes(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_outcome_tenant_team ON outcomes(tenant_id, team_id);

-- HYPOTHESES
CREATE TABLE IF NOT EXISTS hypotheses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    outcome_id UUID NOT NULL REFERENCES outcomes(id),
    title VARCHAR(500) NOT NULL,
    belief TEXT NOT NULL,
    expected_result TEXT NOT NULL,
    measurement_criteria TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    effort VARCHAR(10),
    impact VARCHAR(10),
    confidence VARCHAR(10),
    owner_id UUID REFERENCES users(id),
    experiment_config TEXT,
    experiment_results TEXT,
    blocked_reason TEXT,
    conclusion_notes TEXT,
    external_refs TEXT,
    tags TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    deployed_at TIMESTAMP WITH TIME ZONE,
    measuring_started_at TIMESTAMP WITH TIME ZONE,
    concluded_at TIMESTAMP WITH TIME ZONE,
    concluded_by_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by_id UUID
);

CREATE INDEX IF NOT EXISTS idx_hypothesis_tenant_status ON hypotheses(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_hypothesis_tenant_outcome ON hypotheses(tenant_id, outcome_id);
CREATE INDEX IF NOT EXISTS idx_hypothesis_tenant_owner ON hypotheses(tenant_id, owner_id);

-- DECISION_COMMENTS
CREATE TABLE IF NOT EXISTS decision_comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    decision_id UUID NOT NULL,  -- FK added after decisions table created
    author_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    option_id VARCHAR(100),
    parent_id UUID REFERENCES decision_comments(id),
    is_edited BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- AUDIT_LOGS
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    actor_id UUID,
    actor_email VARCHAR(255),
    actor_name VARCHAR(255),
    action VARCHAR(30) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    entity_title VARCHAR(500),
    description TEXT,
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_id VARCHAR(100),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_tenant_timestamp ON audit_logs(tenant_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_logs(actor_id);

-- ============================================
-- DECISION TABLES
-- ============================================

-- DECISION_QUEUES
CREATE TABLE IF NOT EXISTS decision_queues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    sla_config TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(tenant_id, name)
);

CREATE INDEX IF NOT EXISTS idx_queue_tenant ON decision_queues(tenant_id);

-- DECISIONS
CREATE TABLE IF NOT EXISTS decisions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    context TEXT,
    options TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'NEEDS_INPUT',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    decision_type VARCHAR(20),
    owner_id UUID REFERENCES users(id),
    assigned_to_id UUID REFERENCES users(id),
    outcome_id UUID REFERENCES outcomes(id),
    hypothesis_id UUID REFERENCES hypotheses(id),
    team_id UUID REFERENCES teams(id),
    queue_id UUID REFERENCES decision_queues(id),
    stakeholder_id UUID REFERENCES stakeholders(id),
    sla_hours INTEGER,
    due_at TIMESTAMP WITH TIME ZONE,
    escalation_level INTEGER DEFAULT 0,
    escalated_at TIMESTAMP WITH TIME ZONE,
    escalated_to_id UUID REFERENCES users(id),
    decided_by_id UUID REFERENCES users(id),
    decided_at TIMESTAMP WITH TIME ZONE,
    decision_rationale TEXT,
    selected_option TEXT,
    resolution VARCHAR(2000),
    was_escalated BOOLEAN DEFAULT FALSE,
    blocked_items TEXT,
    external_refs TEXT,
    tags TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by_id UUID
);

CREATE INDEX IF NOT EXISTS idx_decision_tenant_status ON decisions(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_decision_tenant_priority ON decisions(tenant_id, priority);
CREATE INDEX IF NOT EXISTS idx_decision_owner ON decisions(owner_id);
CREATE INDEX IF NOT EXISTS idx_decision_assigned_to ON decisions(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_decision_queue ON decisions(queue_id);
CREATE INDEX IF NOT EXISTS idx_decision_due ON decisions(due_at);
CREATE INDEX IF NOT EXISTS idx_decision_stakeholder ON decisions(stakeholder_id);

-- Add FK from decision_comments to decisions
ALTER TABLE decision_comments
    ADD CONSTRAINT fk_decision_comments_decision
    FOREIGN KEY (decision_id) REFERENCES decisions(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_decision_comment_decision ON decision_comments(decision_id);

-- DECISION_VOTES
CREATE TABLE IF NOT EXISTS decision_votes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    decision_id UUID NOT NULL REFERENCES decisions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    vote VARCHAR(20) NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(decision_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_vote_decision ON decision_votes(decision_id);
CREATE INDEX IF NOT EXISTS idx_vote_user ON decision_votes(user_id);

-- KEY_RESULTS
CREATE TABLE IF NOT EXISTS key_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    outcome_id UUID NOT NULL REFERENCES outcomes(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    target_value DECIMAL(15,4) NOT NULL,
    current_value DECIMAL(15,4) DEFAULT 0,
    unit VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kr_outcome ON key_results(outcome_id);

-- STAKEHOLDER_RESPONSES
CREATE TABLE IF NOT EXISTS stakeholder_responses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    decision_id UUID NOT NULL REFERENCES decisions(id) ON DELETE CASCADE,
    stakeholder_id UUID NOT NULL REFERENCES stakeholders(id),
    response VARCHAR(2000),
    response_time_hours DECIMAL(10,2),
    within_sla BOOLEAN,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(decision_id, stakeholder_id)
);

CREATE INDEX IF NOT EXISTS idx_response_decision ON stakeholder_responses(decision_id);
CREATE INDEX IF NOT EXISTS idx_response_stakeholder ON stakeholder_responses(stakeholder_id);

-- ============================================
-- COMPLETION
-- ============================================
DO $$
BEGIN
    RAISE NOTICE '════════════════════════════════════════════════';
    RAISE NOTICE '  CODEOPS SCHEMA CREATED SUCCESSFULLY';
    RAISE NOTICE '════════════════════════════════════════════════';
    RAISE NOTICE '  Schemas: public, analytics, integrations';
    RAISE NOTICE '  Extensions: uuid-ossp, pgcrypto, pg_trgm';
    RAISE NOTICE '';
    RAISE NOTICE '  Next step: Load seed data with seed-data.sql';
    RAISE NOTICE '════════════════════════════════════════════════';
END $$;
