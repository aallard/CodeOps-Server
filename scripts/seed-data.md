# CodeOps v0.2.0 — Comprehensive Seed Data

You are inserting seed data into the CodeOps PostgreSQL database so the app can be thoroughly tested. The database uses Hibernate DDL-auto, so tables already exist. Connect to the database and run the SQL below.

**Before you start:** read the connection details from `CodeOps-Server/src/main/resources/application.yml` to get the JDBC URL, username, and password. Use `psql` to connect and execute.

## Important Notes

- All UUIDs below are deterministic so foreign keys align. Do NOT generate random ones.
- Timestamps use `NOW()` and relative offsets so data always looks fresh.
- Password hash is bcrypt for the string `pass` — use: `$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci`
- Avatar URLs use `https://i.pravatar.cc/150?u=EMAIL` for realistic profile photos.
- The tenant ID `11111111-1111-1111-1111-111111111111` is used throughout.
- Insert in dependency order: tenant → permissions → roles → users → projects → teams → team_members → outcomes → key_results → hypotheses → experiments → decision_queues → stakeholders → decisions → decision_votes → decision_comments

## SQL

```sql
-- ============================================================
-- 0. CLEAN SLATE (optional — remove existing seed data)
-- ============================================================
-- Run these only if you want to reset. They cascade.
-- DELETE FROM audit_logs WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM decision_comments WHERE decision_id IN (SELECT id FROM decisions WHERE tenant_id = '11111111-1111-1111-1111-111111111111');
-- DELETE FROM decision_votes WHERE decision_id IN (SELECT id FROM decisions WHERE tenant_id = '11111111-1111-1111-1111-111111111111');
-- DELETE FROM decisions WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM experiments WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM hypotheses WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM key_results WHERE outcome_id IN (SELECT id FROM outcomes WHERE tenant_id = '11111111-1111-1111-1111-111111111111');
-- DELETE FROM outcomes WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM stakeholders WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM team_members WHERE team_id IN (SELECT id FROM teams WHERE tenant_id = '11111111-1111-1111-1111-111111111111');
-- DELETE FROM teams WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM projects WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM users WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM role_permissions WHERE role_id IN (SELECT id FROM roles WHERE tenant_id = '11111111-1111-1111-1111-111111111111');
-- DELETE FROM roles WHERE tenant_id = '11111111-1111-1111-1111-111111111111';
-- DELETE FROM tenants WHERE id = '11111111-1111-1111-1111-111111111111';

-- ============================================================
-- 1. TENANT
-- ============================================================
INSERT INTO tenants (id, name, slug, status, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'CodeOps Labs', 'codeops-labs', 'ACTIVE', NOW() - INTERVAL '90 days')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2. PERMISSIONS (idempotent)
-- ============================================================
INSERT INTO permissions (id, code, name, description, category) VALUES
('aaaa0001-0000-0000-0000-000000000001', 'project.read',     'View Projects',     'View project details and dashboards',  'PROJECT'),
('aaaa0001-0000-0000-0000-000000000002', 'project.write',    'Edit Projects',     'Create, update, and archive projects', 'PROJECT'),
('aaaa0001-0000-0000-0000-000000000003', 'outcome.read',     'View Outcomes',     'View outcome details',                'OUTCOME'),
('aaaa0001-0000-0000-0000-000000000004', 'outcome.write',    'Edit Outcomes',     'Create and modify outcomes',           'OUTCOME'),
('aaaa0001-0000-0000-0000-000000000005', 'hypothesis.read',  'View Hypotheses',   'View hypothesis details',             'HYPOTHESIS'),
('aaaa0001-0000-0000-0000-000000000006', 'hypothesis.write', 'Edit Hypotheses',   'Create and modify hypotheses',        'HYPOTHESIS'),
('aaaa0001-0000-0000-0000-000000000007', 'decision.read',    'View Decisions',    'View decisions',                      'DECISION'),
('aaaa0001-0000-0000-0000-000000000008', 'decision.write',   'Edit Decisions',    'Create and resolve decisions',         'DECISION'),
('aaaa0001-0000-0000-0000-000000000009', 'experiment.read',  'View Experiments',  'View experiments',                    'EXPERIMENT'),
('aaaa0001-0000-0000-0000-000000000010', 'experiment.write', 'Edit Experiments',  'Create and manage experiments',        'EXPERIMENT'),
('aaaa0001-0000-0000-0000-000000000011', 'team.read',        'View Teams',        'View team members',                   'TEAM'),
('aaaa0001-0000-0000-0000-000000000012', 'team.write',       'Edit Teams',        'Manage team membership',              'TEAM'),
('aaaa0001-0000-0000-0000-000000000013', 'admin.full',       'Full Admin',        'Full admin privileges',               'ADMIN')
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- 3. ROLES
-- ============================================================
INSERT INTO roles (id, tenant_id, code, name, description, category, level, is_system_role, created_at) VALUES
('bbbb0001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'ADMIN',           'Admin',            'Full system access',                'SYSTEM',      'L8_CXXX',       true,  NOW() - INTERVAL '90 days'),
('bbbb0001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'PRODUCT_LEAD',    'Product Lead',     'Product leadership and ownership',   'PRODUCT',     'L4_MANAGER',    false, NOW() - INTERVAL '90 days'),
('bbbb0001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'ENGINEER',        'Engineer',         'Engineering contributor',            'ENGINEERING', 'L2_SENIOR',     false, NOW() - INTERVAL '90 days'),
('bbbb0001-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'DESIGNER',        'UX Designer',      'Design and user experience',         'UX',          'L2_SENIOR',     false, NOW() - INTERVAL '90 days'),
('bbbb0001-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'DATA_ANALYST',    'Data Analyst',     'Data analysis and metrics',          'DATA',        'L2_SENIOR',     false, NOW() - INTERVAL '90 days'),
('bbbb0001-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'STAKEHOLDER',     'Stakeholder',      'External decision stakeholder',      'STAKEHOLDER', 'L6_VP',         false, NOW() - INTERVAL '90 days')
ON CONFLICT DO NOTHING;

-- Role-permission mappings (admin gets everything)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'bbbb0001-0000-0000-0000-000000000001', id FROM permissions
ON CONFLICT DO NOTHING;

-- Product lead gets project/outcome/hypothesis/decision/experiment/team
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'bbbb0001-0000-0000-0000-000000000002', id FROM permissions WHERE category IN ('PROJECT','OUTCOME','HYPOTHESIS','DECISION','EXPERIMENT','TEAM')
ON CONFLICT DO NOTHING;

-- Engineer gets read on everything, write on hypothesis/experiment
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'bbbb0001-0000-0000-0000-000000000003', id FROM permissions WHERE code LIKE '%.read' OR category IN ('HYPOTHESIS','EXPERIMENT')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. USERS (10 people with realistic names and avatars)
-- ============================================================
INSERT INTO users (id, tenant_id, email, password_hash, first_name, last_name, title, department, avatar_url, role_id, is_active, last_login_at, created_at) VALUES
-- Leadership
('cccc0001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'adam@allard.com',     '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'Adam',     'Allard',     'CEO & Founder',           'Executive',   'https://i.pravatar.cc/150?u=adam@allard.com',     (SELECT id FROM roles WHERE code = 'TENANT_OWNER' LIMIT 1), true, NOW() - INTERVAL '1 hour',  NOW() - INTERVAL '90 days'),
('cccc0001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'sarah.chen@codeopslabs.com', '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'Sarah',    'Chen',       'VP of Product',           'Product',     'https://i.pravatar.cc/150?u=sarah.chen@codeopslabs.com', 'bbbb0001-0000-0000-0000-000000000002', true, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '85 days'),
-- Product
('cccc0001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'marcus.johnson@codeopslabs.com', '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'Marcus',  'Johnson',    'Senior Product Manager',  'Product',     'https://i.pravatar.cc/150?u=marcus.johnson@codeopslabs.com', 'bbbb0001-0000-0000-0000-000000000002', true, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '80 days'),
('cccc0001-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'priya.patel@codeopslabs.com', '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'Priya',    'Patel',      'Product Manager',         'Product',     'https://i.pravatar.cc/150?u=priya.patel@codeopslabs.com', 'bbbb0001-0000-0000-0000-000000000002', true, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '75 days'),
-- Engineering
('cccc0001-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'james.nakamura@codeopslabs.com', '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'James',   'Nakamura',   'Staff Engineer',          'Engineering', 'https://i.pravatar.cc/150?u=james.nakamura@codeopslabs.com', 'bbbb0001-0000-0000-0000-000000000003', true, NOW() - INTERVAL '4 hours', NOW() - INTERVAL '78 days'),
('cccc0001-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'elena.rodriguez@codeopslabs.com', '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'Elena',   'Rodriguez',  'Senior Backend Engineer', 'Engineering', 'https://i.pravatar.cc/150?u=elena.rodriguez@codeopslabs.com', 'bbbb0001-0000-0000-0000-000000000003', true, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '70 days'),
('cccc0001-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'alex.kim@codeopslabs.com', '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'Alex',     'Kim',        'Frontend Engineer',       'Engineering', 'https://i.pravatar.cc/150?u=alex.kim@codeopslabs.com', 'bbbb0001-0000-0000-0000-000000000003', true, NOW() - INTERVAL '8 hours', NOW() - INTERVAL '65 days'),
-- Design
('cccc0001-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'maya.okonkwo@codeopslabs.com', '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'Maya',    'Okonkwo',    'Lead UX Designer',        'Design',      'https://i.pravatar.cc/150?u=maya.okonkwo@codeopslabs.com', 'bbbb0001-0000-0000-0000-000000000004', true, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '72 days'),
-- Data
('cccc0001-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'david.liu@codeopslabs.com', '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'David',    'Liu',        'Data Analyst',            'Data',        'https://i.pravatar.cc/150?u=david.liu@codeopslabs.com', 'bbbb0001-0000-0000-0000-000000000005', true, NOW() - INTERVAL '7 hours', NOW() - INTERVAL '60 days'),
-- Stakeholder
('cccc0001-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'rachel.foster@codeopslabs.com', '$2b$10$NryqiSai.Y87xZ28fLKNB.QXP/TUxzchK2mw5agRRbqqgK9R6NCci', 'Rachel',  'Foster',     'VP of Engineering',       'Engineering', 'https://i.pravatar.cc/150?u=rachel.foster@codeopslabs.com', 'bbbb0001-0000-0000-0000-000000000006', true, NOW() - INTERVAL '12 hours', NOW() - INTERVAL '88 days')
ON CONFLICT (id) DO NOTHING;

-- Set manager relationships
UPDATE users SET manager_id = 'cccc0001-0000-0000-0000-000000000001' WHERE id IN ('cccc0001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000010');
UPDATE users SET manager_id = 'cccc0001-0000-0000-0000-000000000002' WHERE id IN ('cccc0001-0000-0000-0000-000000000003', 'cccc0001-0000-0000-0000-000000000004');
UPDATE users SET manager_id = 'cccc0001-0000-0000-0000-000000000010' WHERE id IN ('cccc0001-0000-0000-0000-000000000005', 'cccc0001-0000-0000-0000-000000000006', 'cccc0001-0000-0000-0000-000000000007');

-- ============================================================
-- 5. PROJECTS (3 projects in different states)
-- ============================================================
INSERT INTO projects (id, tenant_id, name, slug, description, status, color, owner_id, created_by_id, created_at, updated_at) VALUES
('dddd0001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111',
 'AI-Powered Search Revamp', 'ai-search-revamp',
 'Replace legacy keyword search with vector embeddings and LLM-powered query understanding. Target: 40% improvement in search relevance scores and 25% increase in conversion from search results.',
 'ACTIVE', '#4F46E5', 'cccc0001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000001',
 NOW() - INTERVAL '45 days', NOW() - INTERVAL '2 hours'),

('dddd0001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111',
 'Mobile-First Checkout Redesign', 'mobile-checkout-v2',
 'Complete redesign of the mobile checkout experience to reduce cart abandonment. Focus on one-tap payments, address autocomplete, and progressive disclosure. Current mobile conversion is 1.8% vs desktop 3.4%.',
 'ACTIVE', '#059669', 'cccc0001-0000-0000-0000-000000000003', 'cccc0001-0000-0000-0000-000000000002',
 NOW() - INTERVAL '30 days', NOW() - INTERVAL '6 hours'),

('dddd0001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111',
 'Platform Observability & SRE', 'platform-observability',
 'Build comprehensive observability stack: distributed tracing, custom metrics dashboards, SLO tracking, and automated incident response. Reduce MTTR from 45 minutes to under 10 minutes.',
 'PLANNING', '#D97706', 'cccc0001-0000-0000-0000-000000000010', 'cccc0001-0000-0000-0000-000000000001',
 NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 6. TEAMS (3 teams mapped to projects)
-- ============================================================
INSERT INTO teams (id, tenant_id, project_id, name, slug, description, color, lead_id, is_active, created_at) VALUES
('eeee0001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Search & Discovery', 'search-discovery', 'AI search, recommendations, and content discovery', '#4F46E5',
 'cccc0001-0000-0000-0000-000000000005', true, NOW() - INTERVAL '44 days'),

('eeee0001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002',
 'Commerce Experience', 'commerce-experience', 'Checkout, payments, and purchase flow', '#059669',
 'cccc0001-0000-0000-0000-000000000007', true, NOW() - INTERVAL '29 days'),

('eeee0001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000003',
 'Platform Engineering', 'platform-engineering', 'Infrastructure, observability, and developer experience', '#D97706',
 'cccc0001-0000-0000-0000-000000000006', true, NOW() - INTERVAL '9 days')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 7. TEAM MEMBERS
-- ============================================================
INSERT INTO team_members (id, team_id, user_id, team_role, joined_at, added_by_id) VALUES
-- Search & Discovery team
('ff000001-0000-0000-0000-000000000001', 'eeee0001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000005', 'LEAD',   NOW() - INTERVAL '44 days', 'cccc0001-0000-0000-0000-000000000002'),
('ff000001-0000-0000-0000-000000000002', 'eeee0001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000006', 'MEMBER', NOW() - INTERVAL '43 days', 'cccc0001-0000-0000-0000-000000000005'),
('ff000001-0000-0000-0000-000000000003', 'eeee0001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000009', 'MEMBER', NOW() - INTERVAL '40 days', 'cccc0001-0000-0000-0000-000000000005'),
('ff000001-0000-0000-0000-000000000004', 'eeee0001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000004', 'MEMBER', NOW() - INTERVAL '42 days', 'cccc0001-0000-0000-0000-000000000002'),
-- Commerce Experience team
('ff000001-0000-0000-0000-000000000005', 'eeee0001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000007', 'LEAD',   NOW() - INTERVAL '29 days', 'cccc0001-0000-0000-0000-000000000003'),
('ff000001-0000-0000-0000-000000000006', 'eeee0001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000008', 'MEMBER', NOW() - INTERVAL '28 days', 'cccc0001-0000-0000-0000-000000000007'),
('ff000001-0000-0000-0000-000000000007', 'eeee0001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000003', 'MEMBER', NOW() - INTERVAL '28 days', 'cccc0001-0000-0000-0000-000000000002'),
-- Platform Engineering team
('ff000001-0000-0000-0000-000000000008', 'eeee0001-0000-0000-0000-000000000003', 'cccc0001-0000-0000-0000-000000000006', 'LEAD',   NOW() - INTERVAL '9 days', 'cccc0001-0000-0000-0000-000000000010'),
('ff000001-0000-0000-0000-000000000009', 'eeee0001-0000-0000-0000-000000000003', 'cccc0001-0000-0000-0000-000000000005', 'MEMBER', NOW() - INTERVAL '8 days', 'cccc0001-0000-0000-0000-000000000006')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 8. DECISION QUEUES
-- ============================================================
INSERT INTO decision_queues (id, tenant_id, name, description, is_default, created_at) VALUES
('aabb0001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Product Decisions',   'Product direction, scope, and feature decisions', true,  NOW() - INTERVAL '60 days'),
('aabb0001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Technical Decisions', 'Architecture, stack, and infrastructure decisions', false, NOW() - INTERVAL '60 days'),
('aabb0001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'UX Decisions',       'Design, interaction, and usability decisions',     false, NOW() - INTERVAL '60 days')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 9. STAKEHOLDERS (4 stakeholders)
-- ============================================================
INSERT INTO stakeholders (id, tenant_id, project_id, name, email, title, organization, type, user_id, expertise, timezone, decisions_pending, decisions_completed, decisions_escalated, avg_response_time_hours, is_active, created_at) VALUES
('aabb0002-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Sarah Chen', 'sarah.chen@codeopslabs.com', 'VP of Product', 'CodeOps Labs', 'INTERNAL',
 'cccc0001-0000-0000-0000-000000000002', 'Product strategy, market analysis, pricing', 'America/Los_Angeles',
 3, 18, 1, 3.2, true, NOW() - INTERVAL '45 days'),

('aabb0002-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Rachel Foster', 'rachel.foster@codeopslabs.com', 'VP of Engineering', 'CodeOps Labs', 'INTERNAL',
 'cccc0001-0000-0000-0000-000000000010', 'System architecture, scalability, team capacity', 'America/New_York',
 2, 14, 0, 5.8, true, NOW() - INTERVAL '45 days'),

('aabb0002-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002',
 'Adam Allard', 'adam@allard.com', 'CEO & Founder', 'CodeOps Labs', 'EXECUTIVE',
 'cccc0001-0000-0000-0000-000000000001', 'Business strategy, fundraising, partnerships', 'America/Chicago',
 1, 22, 2, 8.5, true, NOW() - INTERVAL '30 days'),

('aabb0002-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', NULL,
 'Dr. Kenji Tanaka', 'kenji.tanaka@advisors.com', 'AI Research Advisor', 'Independent', 'EXTERNAL',
 NULL, 'Machine learning, NLP, recommendation systems', 'Asia/Tokyo',
 1, 5, 0, 18.0, true, NOW() - INTERVAL '40 days')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 10. OUTCOMES (7 outcomes across projects)
-- ============================================================
INSERT INTO outcomes (id, tenant_id, project_id, title, description, success_criteria, status, priority, team_id, owner_id, target_date, started_at, created_by_id, created_at, updated_at) VALUES
-- AI Search project outcomes
('11110001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Increase search relevance score to 85%+',
 'Our current keyword-based search returns relevant results only 62% of the time. By implementing vector embeddings and semantic understanding, we target 85%+ relevance as measured by human evaluation panels.',
 'Search relevance score >= 85% measured by weekly human evaluation panel of 200 queries',
 'IN_PROGRESS', 'CRITICAL', 'eeee0001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000004',
 (CURRENT_DATE + INTERVAL '45 days')::date, NOW() - INTERVAL '38 days', 'cccc0001-0000-0000-0000-000000000002',
 NOW() - INTERVAL '40 days', NOW() - INTERVAL '1 day'),

('11110001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Reduce average search latency to <200ms (p95)',
 'Current p95 latency is 480ms. Users abandon searches after 300ms. We need to hit <200ms p95 while adding the new AI-powered ranking layer.',
 'p95 search latency < 200ms measured over 7-day rolling window',
 'IN_PROGRESS', 'HIGH', 'eeee0001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000005',
 (CURRENT_DATE + INTERVAL '30 days')::date, NOW() - INTERVAL '35 days', 'cccc0001-0000-0000-0000-000000000005',
 NOW() - INTERVAL '38 days', NOW() - INTERVAL '3 days'),

('11110001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Drive 25% increase in search-to-purchase conversion',
 'Better search results should directly translate to higher conversion. Current search-to-purchase rate is 4.2%.',
 'Search-to-purchase conversion rate >= 5.25% measured over 30-day period post-launch',
 'NOT_STARTED', 'HIGH', 'eeee0001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000003',
 (CURRENT_DATE + INTERVAL '75 days')::date, NULL, 'cccc0001-0000-0000-0000-000000000002',
 NOW() - INTERVAL '35 days', NOW() - INTERVAL '10 days'),

-- Mobile Checkout project outcomes
('11110001-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002',
 'Increase mobile checkout conversion from 1.8% to 3.0%',
 'Mobile conversion is nearly half of desktop. The new checkout flow with one-tap payments and progressive disclosure should close this gap.',
 'Mobile checkout conversion rate >= 3.0% measured over 14-day post-launch window',
 'IN_PROGRESS', 'CRITICAL', 'eeee0001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000003',
 (CURRENT_DATE + INTERVAL '35 days')::date, NOW() - INTERVAL '22 days', 'cccc0001-0000-0000-0000-000000000003',
 NOW() - INTERVAL '25 days', NOW() - INTERVAL '4 hours'),

('11110001-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002',
 'Reduce cart abandonment rate below 55%',
 'Current mobile cart abandonment is 72%. Industry average is 60%. Target is 55% through UX improvements.',
 'Cart abandonment rate < 55% on mobile devices over 14-day measurement window',
 'IN_PROGRESS', 'HIGH', 'eeee0001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000008',
 (CURRENT_DATE + INTERVAL '40 days')::date, NOW() - INTERVAL '18 days', 'cccc0001-0000-0000-0000-000000000003',
 NOW() - INTERVAL '22 days', NOW() - INTERVAL '1 day'),

-- Platform Observability outcomes
('11110001-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000003',
 'Reduce MTTR from 45 minutes to under 10 minutes',
 'Mean Time To Recovery is too high. Distributed tracing, better alerting, and automated runbooks should dramatically reduce this.',
 'Rolling 30-day MTTR < 10 minutes across all P1 and P2 incidents',
 'DRAFT', 'CRITICAL', 'eeee0001-0000-0000-0000-000000000003', 'cccc0001-0000-0000-0000-000000000006',
 (CURRENT_DATE + INTERVAL '90 days')::date, NULL, 'cccc0001-0000-0000-0000-000000000010',
 NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days'),

('11110001-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000003',
 'Achieve 99.95% uptime SLO across all customer-facing services',
 'Current uptime is 99.8% which translates to ~17.5 hours of downtime per year. Need to reduce to ~4.4 hours.',
 'Rolling 90-day uptime >= 99.95% across API gateway, web app, and mobile backend',
 'DRAFT', 'HIGH', 'eeee0001-0000-0000-0000-000000000003', 'cccc0001-0000-0000-0000-000000000010',
 (CURRENT_DATE + INTERVAL '120 days')::date, NULL, 'cccc0001-0000-0000-0000-000000000010',
 NOW() - INTERVAL '7 days', NOW() - INTERVAL '3 days')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 11. KEY RESULTS
-- ============================================================
INSERT INTO key_results (id, outcome_id, title, description, target_value, current_value, unit, created_at, updated_at) VALUES
-- Search relevance outcome KRs
('22220001-0000-0000-0000-000000000001', '11110001-0000-0000-0000-000000000001', 'Human evaluation panel relevance score', 'Weekly panel of 5 evaluators scoring 200 queries', 85.0, 71.0, '%', NOW() - INTERVAL '38 days', NOW() - INTERVAL '1 day'),
('22220001-0000-0000-0000-000000000002', '11110001-0000-0000-0000-000000000001', 'Zero-result search rate', 'Percentage of searches returning no results', 2.0, 8.5, '%', NOW() - INTERVAL '38 days', NOW() - INTERVAL '2 days'),
('22220001-0000-0000-0000-000000000003', '11110001-0000-0000-0000-000000000001', 'Click-through rate on first 3 results', NULL, 65.0, 48.0, '%', NOW() - INTERVAL '38 days', NOW() - INTERVAL '1 day'),
-- Search latency KRs
('22220001-0000-0000-0000-000000000004', '11110001-0000-0000-0000-000000000002', 'p95 search latency', 'Measured at API gateway level', 200.0, 320.0, 'ms', NOW() - INTERVAL '35 days', NOW() - INTERVAL '3 days'),
('22220001-0000-0000-0000-000000000005', '11110001-0000-0000-0000-000000000002', 'p50 search latency', NULL, 80.0, 95.0, 'ms', NOW() - INTERVAL '35 days', NOW() - INTERVAL '3 days'),
-- Mobile conversion KRs
('22220001-0000-0000-0000-000000000006', '11110001-0000-0000-0000-000000000004', 'Mobile checkout conversion rate', NULL, 3.0, 2.1, '%', NOW() - INTERVAL '22 days', NOW() - INTERVAL '4 hours'),
('22220001-0000-0000-0000-000000000007', '11110001-0000-0000-0000-000000000004', 'Time to complete checkout (mobile)', 'Median time from cart to order confirmed', 45.0, 78.0, 'seconds', NOW() - INTERVAL '22 days', NOW() - INTERVAL '1 day'),
-- Cart abandonment KRs
('22220001-0000-0000-0000-000000000008', '11110001-0000-0000-0000-000000000005', 'Mobile cart abandonment rate', NULL, 55.0, 65.0, '%', NOW() - INTERVAL '18 days', NOW() - INTERVAL '1 day'),
('22220001-0000-0000-0000-000000000009', '11110001-0000-0000-0000-000000000005', 'Payment method selection drop-off', 'Users who leave at payment step', 15.0, 32.0, '%', NOW() - INTERVAL '18 days', NOW() - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 12. HYPOTHESES (10 across projects, various lifecycle stages)
-- ============================================================
INSERT INTO hypotheses (id, tenant_id, project_id, outcome_id, title, belief, expected_result, measurement_criteria, status, priority, effort, impact, confidence, owner_id, started_at, deployed_at, measuring_started_at, concluded_at, conclusion_notes, created_by_id, created_at, updated_at) VALUES
-- AI Search hypotheses
('33330001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001', '11110001-0000-0000-0000-000000000001',
 'Vector embeddings will outperform keyword matching for long-tail queries',
 'We believe that using sentence-transformer embeddings for queries longer than 3 words will dramatically improve relevance for long-tail searches, which make up 60% of our query volume.',
 'Relevance score for long-tail queries (4+ words) improves from 48% to 80%+',
 'A/B test comparing keyword vs vector results on queries with 4+ words, measured by human evaluation panel',
 'MEASURING', 'CRITICAL', 'L', 'XL', 'HIGH',
 'cccc0001-0000-0000-0000-000000000005', NOW() - INTERVAL '30 days', NOW() - INTERVAL '15 days', NOW() - INTERVAL '7 days', NULL, NULL,
 'cccc0001-0000-0000-0000-000000000004', NOW() - INTERVAL '38 days', NOW() - INTERVAL '1 day'),

('33330001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001', '11110001-0000-0000-0000-000000000001',
 'LLM query rewriting improves zero-result searches',
 'We believe that using an LLM to rewrite ambiguous or misspelled queries before executing the search will reduce zero-result rates by at least 60%.',
 'Zero-result search rate drops from 8.5% to below 3.5%',
 'Compare zero-result rates between control (raw query) and treatment (LLM-rewritten query)',
 'BUILDING', 'HIGH', 'M', 'L', 'MEDIUM',
 'cccc0001-0000-0000-0000-000000000006', NOW() - INTERVAL '20 days', NULL, NULL, NULL, NULL,
 'cccc0001-0000-0000-0000-000000000004', NOW() - INTERVAL '25 days', NOW() - INTERVAL '3 days'),

('33330001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001', '11110001-0000-0000-0000-000000000002',
 'Pre-computed embedding cache will keep latency under 200ms',
 'We believe that pre-computing and caching embeddings for the top 10,000 queries (covering ~70% of traffic) will keep p95 latency under 200ms even with the new vector search layer.',
 'p95 latency stays under 200ms with vector search enabled',
 'Latency percentiles measured via API gateway for 7-day window',
 'VALIDATED', 'HIGH', 'M', 'XL', 'HIGH',
 'cccc0001-0000-0000-0000-000000000005', NOW() - INTERVAL '32 days', NOW() - INTERVAL '20 days', NOW() - INTERVAL '14 days', NOW() - INTERVAL '5 days',
 'Validated. Pre-computed cache covers 72% of queries. p95 dropped to 185ms under load testing with 2x current traffic. Shipping to production.',
 'cccc0001-0000-0000-0000-000000000005', NOW() - INTERVAL '35 days', NOW() - INTERVAL '5 days'),

('33330001-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001', '11110001-0000-0000-0000-000000000003',
 'Personalized search ranking increases conversion',
 'We believe that incorporating user browsing history and purchase patterns into search ranking will increase search-to-purchase conversion by at least 15%.',
 'Search-to-purchase conversion increases by >= 15% for logged-in users',
 'A/B test: default ranking vs personalized ranking for logged-in users, measured over 14 days',
 'READY', 'MEDIUM', 'L', 'XL', 'MEDIUM',
 'cccc0001-0000-0000-0000-000000000009', NULL, NULL, NULL, NULL, NULL,
 'cccc0001-0000-0000-0000-000000000003', NOW() - INTERVAL '20 days', NOW() - INTERVAL '8 days'),

-- Mobile Checkout hypotheses
('33330001-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002', '11110001-0000-0000-0000-000000000004',
 'One-tap Apple/Google Pay doubles mobile conversion',
 'We believe that adding Apple Pay and Google Pay as primary payment options (shown before credit card form) will at least double mobile checkout conversion from 1.8% to 3.6%.',
 'Mobile checkout conversion rate >= 3.6% for users who see one-tap payment options',
 'A/B test: current checkout vs one-tap-first checkout on iOS and Android',
 'MEASURING', 'CRITICAL', 'M', 'XL', 'HIGH',
 'cccc0001-0000-0000-0000-000000000007', NOW() - INTERVAL '18 days', NOW() - INTERVAL '8 days', NOW() - INTERVAL '5 days', NULL, NULL,
 'cccc0001-0000-0000-0000-000000000003', NOW() - INTERVAL '22 days', NOW() - INTERVAL '2 days'),

('33330001-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002', '11110001-0000-0000-0000-000000000004',
 'Single-page checkout reduces drop-off vs multi-step',
 'We believe that collapsing the 4-step checkout into a single scrollable page with progressive disclosure will reduce checkout abandonment by 30%.',
 'Checkout step-to-step drop-off decreases by >= 30%',
 'A/B test: multi-step vs single-page checkout, measuring drop-off between each section',
 'BUILDING', 'HIGH', 'L', 'L', 'MEDIUM',
 'cccc0001-0000-0000-0000-000000000008', NOW() - INTERVAL '14 days', NULL, NULL, NULL, NULL,
 'cccc0001-0000-0000-0000-000000000003', NOW() - INTERVAL '18 days', NOW() - INTERVAL '4 days'),

('33330001-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002', '11110001-0000-0000-0000-000000000005',
 'Address autocomplete eliminates the #1 abandonment trigger',
 'We believe that Google Places autocomplete for shipping address will reduce the payment-step drop-off rate from 32% to under 15%, since address entry is the #1 cited reason for mobile abandonment.',
 'Payment method selection drop-off decreases to < 15%',
 'Compare drop-off rate at address step for autocomplete vs manual entry',
 'INVALIDATED', 'HIGH', 'S', 'L', 'HIGH',
 'cccc0001-0000-0000-0000-000000000007', NOW() - INTERVAL '25 days', NOW() - INTERVAL '18 days', NOW() - INTERVAL '14 days', NOW() - INTERVAL '3 days',
 'Invalidated. Address autocomplete only reduced drop-off to 28% (from 32%). Root cause analysis showed the real issue is shipping cost surprise, not address entry friction. Pivoting to show estimated shipping costs earlier in the flow.',
 'cccc0001-0000-0000-0000-000000000003', NOW() - INTERVAL '26 days', NOW() - INTERVAL '3 days'),

-- Platform observability hypotheses
('33330001-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000003', '11110001-0000-0000-0000-000000000006',
 'Distributed tracing cuts investigation time by 70%',
 'We believe that adding OpenTelemetry distributed tracing across all microservices will reduce mean investigation time from 35 minutes to under 10 minutes.',
 'Mean investigation time (from alert to root cause identified) < 10 minutes',
 'Track investigation time for all P1/P2 incidents over 30-day window',
 'DRAFT', 'CRITICAL', 'XL', 'XL', 'HIGH',
 'cccc0001-0000-0000-0000-000000000006', NULL, NULL, NULL, NULL, NULL,
 'cccc0001-0000-0000-0000-000000000010', NOW() - INTERVAL '7 days', NOW() - INTERVAL '2 days'),

('33330001-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000003', '11110001-0000-0000-0000-000000000007',
 'Automated canary deployments prevent 80% of production incidents',
 'We believe that automated canary analysis (comparing error rates, latency, CPU) during deployment will catch and auto-rollback 80% of bad deployments before they impact customers.',
 'Canary catches >= 80% of deployments that would have caused incidents',
 'Track canary catches vs post-deployment incidents over 60-day window',
 'DRAFT', 'HIGH', 'XL', 'XL', 'MEDIUM',
 'cccc0001-0000-0000-0000-000000000005', NULL, NULL, NULL, NULL, NULL,
 'cccc0001-0000-0000-0000-000000000006', NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day'),

('33330001-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002', '11110001-0000-0000-0000-000000000005',
 'Showing shipping cost estimate on product page reduces cart abandonment',
 'We believe that displaying estimated shipping cost on the product detail page (before add-to-cart) will reduce cart abandonment by 20% because shipping cost surprise is the #1 reason for mobile abandonment.',
 'Cart abandonment rate decreases by >= 20% (from 65% to <= 52%)',
 'A/B test: product pages with vs without shipping estimate, measuring cart-to-purchase rate',
 'READY', 'HIGH', 'S', 'XL', 'HIGH',
 'cccc0001-0000-0000-0000-000000000007', NULL, NULL, NULL, NULL, NULL,
 'cccc0001-0000-0000-0000-000000000003', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 13. EXPERIMENTS (6 experiments)
-- ============================================================
INSERT INTO experiments (id, tenant_id, project_id, hypothesis_id, name, description, type, status, traffic_split, primary_metric, sample_size_target, current_sample_size, control_value, variant_value, confidence_level, duration_days, start_date, end_date, owner_id, created_by_id, created_at, updated_at) VALUES
-- Running: vector embeddings A/B test
('44440001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001', '33330001-0000-0000-0000-000000000001',
 'Vector Search vs Keyword Search (Long-tail)', 'A/B test comparing vector embedding-based search against keyword matching for queries with 4+ words.',
 'A_B_TEST', 'RUNNING', '50/50', 'search_relevance_score', 50000, 38500,
 48.2, 76.8, 96.5, 14,
 NOW() - INTERVAL '7 days', NOW() + INTERVAL '7 days',
 'cccc0001-0000-0000-0000-000000000005', 'cccc0001-0000-0000-0000-000000000004',
 NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 hour'),

-- Running: Apple/Google Pay A/B test
('44440001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002', '33330001-0000-0000-0000-000000000005',
 'One-Tap Payment First vs Credit Card First', 'Show Apple Pay / Google Pay as primary payment method above the fold, with credit card form collapsed below.',
 'A_B_TEST', 'RUNNING', '50/50', 'mobile_checkout_conversion', 30000, 21000,
 1.8, 3.1, 94.2, 14,
 NOW() - INTERVAL '5 days', NOW() + INTERVAL '9 days',
 'cccc0001-0000-0000-0000-000000000007', 'cccc0001-0000-0000-0000-000000000003',
 NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 hours'),

-- Concluded: embedding cache test
('44440001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001', '33330001-0000-0000-0000-000000000003',
 'Pre-computed Embedding Cache Load Test', 'Canary deployment testing pre-computed embedding cache for top 10K queries under 2x load.',
 'CANARY', 'CONCLUDED', '10/90', 'p95_latency_ms', 100000, 100000,
 310.0, 185.0, 99.1, 7,
 NOW() - INTERVAL '14 days', NOW() - INTERVAL '7 days',
 'cccc0001-0000-0000-0000-000000000005', 'cccc0001-0000-0000-0000-000000000005',
 NOW() - INTERVAL '16 days', NOW() - INTERVAL '5 days'),

-- Concluded (invalidated): address autocomplete
('44440001-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002', '33330001-0000-0000-0000-000000000007',
 'Google Places Address Autocomplete', 'Test Google Places autocomplete for shipping address entry on mobile checkout.',
 'A_B_TEST', 'CONCLUDED', '50/50', 'address_step_dropoff_rate', 20000, 20000,
 32.0, 28.0, 72.3, 10,
 NOW() - INTERVAL '14 days', NOW() - INTERVAL '4 days',
 'cccc0001-0000-0000-0000-000000000007', 'cccc0001-0000-0000-0000-000000000003',
 NOW() - INTERVAL '18 days', NOW() - INTERVAL '3 days'),

-- Draft: personalized ranking
('44440001-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001', '33330001-0000-0000-0000-000000000004',
 'Personalized Search Ranking Experiment', 'Feature flag to enable user-history-based re-ranking for logged-in users.',
 'FEATURE_FLAG', 'DRAFT', '20/80', 'search_to_purchase_conversion', 40000, 0,
 NULL, NULL, NULL, 21,
 NULL, NULL,
 'cccc0001-0000-0000-0000-000000000009', 'cccc0001-0000-0000-0000-000000000003',
 NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days'),

-- Running: shipping cost estimate feature flag
('44440001-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002', '33330001-0000-0000-0000-000000000010',
 'Shipping Estimate on Product Page', 'Feature flag showing estimated shipping cost based on user location on the product detail page.',
 'FEATURE_FLAG', 'DRAFT', '10/90', 'cart_abandonment_rate', 25000, 0,
 NULL, NULL, NULL, 14,
 NULL, NULL,
 'cccc0001-0000-0000-0000-000000000007', 'cccc0001-0000-0000-0000-000000000003',
 NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 14. DECISIONS (12 decisions in various states)
-- ============================================================
INSERT INTO decisions (id, tenant_id, project_id, title, description, context, status, priority, decision_type, owner_id, assigned_to_id, outcome_id, hypothesis_id, team_id, queue_id, stakeholder_id, sla_hours, due_at, decided_by_id, decided_at, decision_rationale, selected_option, was_escalated, created_by_id, created_at, updated_at) VALUES

-- BLOCKING: needs immediate attention
('55550001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Which embedding model to use for production: OpenAI ada-002 vs Cohere embed-v3?',
 'We need to decide on the embedding model before we can ship vector search to production. Both have been prototyped.',
 'ada-002: 1536 dims, $0.0001/1K tokens, well-documented. Cohere embed-v3: 1024 dims, $0.0001/1K tokens, better multilingual support. Our benchmark shows Cohere is 8% more accurate on our dataset but ada-002 has 2x the ecosystem support.',
 'NEEDS_INPUT', 'BLOCKING', 'TECHNICAL',
 'cccc0001-0000-0000-0000-000000000005', 'cccc0001-0000-0000-0000-000000000002',
 '11110001-0000-0000-0000-000000000001', '33330001-0000-0000-0000-000000000001',
 'eeee0001-0000-0000-0000-000000000001', 'aabb0001-0000-0000-0000-000000000002',
 'aabb0002-0000-0000-0000-000000000002',
 4, NOW() - INTERVAL '2 hours',
 NULL, NULL, NULL, NULL, false,
 'cccc0001-0000-0000-0000-000000000005', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '1 hour'),

-- HIGH: SLA approaching
('55550001-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002',
 'Should we support guest checkout or require account creation?',
 'The new mobile checkout flow needs to handle unauthenticated users. Guest checkout is faster but we lose user data for personalization.',
 'Competitor analysis: Amazon requires account, Shopify default is guest checkout. Our current desktop flow requires account. Mobile users are 3x less likely to create accounts.',
 'UNDER_DISCUSSION', 'HIGH', 'PRODUCT',
 'cccc0001-0000-0000-0000-000000000003', 'cccc0001-0000-0000-0000-000000000002',
 '11110001-0000-0000-0000-000000000004', NULL,
 'eeee0001-0000-0000-0000-000000000002', 'aabb0001-0000-0000-0000-000000000001',
 'aabb0002-0000-0000-0000-000000000001',
 8, NOW() + INTERVAL '3 hours',
 NULL, NULL, NULL, NULL, false,
 'cccc0001-0000-0000-0000-000000000003', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '30 minutes'),

-- NORMAL: under discussion
('55550001-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Search results page: infinite scroll or paginated?',
 'The redesigned search results need a loading strategy. Infinite scroll is more mobile-friendly but harder to deep-link and may impact SEO.',
 NULL,
 'UNDER_DISCUSSION', 'NORMAL', 'UX',
 'cccc0001-0000-0000-0000-000000000008', 'cccc0001-0000-0000-0000-000000000008',
 '11110001-0000-0000-0000-000000000001', NULL,
 'eeee0001-0000-0000-0000-000000000001', 'aabb0001-0000-0000-0000-000000000003',
 NULL,
 24, NOW() + INTERVAL '18 hours',
 NULL, NULL, NULL, NULL, false,
 'cccc0001-0000-0000-0000-000000000008', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '2 hours'),

('55550001-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002',
 'Payment processor: Stripe vs Adyen for international expansion',
 'We need to select a payment processor that supports our international expansion plans (EU + APAC in Q3). Current Braintree setup is US-only.',
 NULL,
 'NEEDS_INPUT', 'HIGH', 'STRATEGIC',
 'cccc0001-0000-0000-0000-000000000003', 'cccc0001-0000-0000-0000-000000000001',
 '11110001-0000-0000-0000-000000000004', NULL,
 'eeee0001-0000-0000-0000-000000000002', 'aabb0001-0000-0000-0000-000000000001',
 'aabb0002-0000-0000-0000-000000000003',
 8, NOW() + INTERVAL '2 hours',
 NULL, NULL, NULL, NULL, false,
 'cccc0001-0000-0000-0000-000000000003', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '1 hour'),

-- DECIDED: recently resolved
('55550001-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Vector database: Pinecone vs pgvector extension',
 'Need a vector store for search embeddings. Pinecone is managed and fast. pgvector keeps us on Postgres with no new infrastructure.',
 NULL,
 'DECIDED', 'HIGH', 'ARCHITECTURAL',
 'cccc0001-0000-0000-0000-000000000005', 'cccc0001-0000-0000-0000-000000000010',
 '11110001-0000-0000-0000-000000000002', NULL,
 'eeee0001-0000-0000-0000-000000000001', 'aabb0001-0000-0000-0000-000000000002',
 'aabb0002-0000-0000-0000-000000000002',
 24, NOW() - INTERVAL '2 days',
 'cccc0001-0000-0000-0000-000000000010', NOW() - INTERVAL '3 days',
 'Going with pgvector. Our dataset is <5M vectors which is well within pgvector capabilities. Avoids new vendor dependency, simpler ops, and team already knows Postgres. Will reassess if we hit scale limits.',
 'pgvector', false,
 'cccc0001-0000-0000-0000-000000000005', NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days'),

('55550001-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002',
 'Mobile checkout: bottom sheet or full screen for payment form?',
 'Design decision for how the payment form appears on mobile. Bottom sheet saves context, full screen reduces distraction.',
 NULL,
 'DECIDED', 'NORMAL', 'UX',
 'cccc0001-0000-0000-0000-000000000008', 'cccc0001-0000-0000-0000-000000000003',
 '11110001-0000-0000-0000-000000000004', NULL,
 'eeee0001-0000-0000-0000-000000000002', 'aabb0001-0000-0000-0000-000000000003',
 NULL,
 24, NOW() - INTERVAL '4 days',
 'cccc0001-0000-0000-0000-000000000003', NOW() - INTERVAL '5 days',
 'Full screen. User testing showed bottom sheet felt cramped on smaller phones (iPhone SE, Pixel 6a). Full screen with sticky CTA at bottom tested better across all screen sizes.',
 'Full screen with sticky CTA', false,
 'cccc0001-0000-0000-0000-000000000008', NOW() - INTERVAL '7 days', NOW() - INTERVAL '5 days'),

-- IMPLEMENTED: shipped
('55550001-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Search API rate limiting strategy',
 'We need rate limiting before adding the new AI search layer to prevent cost overruns on the embedding API.',
 NULL,
 'IMPLEMENTED', 'HIGH', 'TECHNICAL',
 'cccc0001-0000-0000-0000-000000000006', 'cccc0001-0000-0000-0000-000000000010',
 '11110001-0000-0000-0000-000000000002', NULL,
 'eeee0001-0000-0000-0000-000000000001', 'aabb0001-0000-0000-0000-000000000002',
 NULL,
 24, NOW() - INTERVAL '15 days',
 'cccc0001-0000-0000-0000-000000000010', NOW() - INTERVAL '18 days',
 'Token bucket algorithm at 100 req/s per user, 1000 req/s globally. Implemented via Redis with sliding window. Includes bypass for internal services.',
 'Token bucket (100/s user, 1000/s global)', false,
 'cccc0001-0000-0000-0000-000000000006', NOW() - INTERVAL '22 days', NOW() - INTERVAL '14 days'),

('55550001-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Search index refresh strategy: real-time vs batch',
 'How quickly should product changes appear in search results?',
 NULL,
 'IMPLEMENTED', 'NORMAL', 'TECHNICAL',
 'cccc0001-0000-0000-0000-000000000005', 'cccc0001-0000-0000-0000-000000000005',
 '11110001-0000-0000-0000-000000000001', NULL,
 'eeee0001-0000-0000-0000-000000000001', 'aabb0001-0000-0000-0000-000000000002',
 NULL,
 72, NOW() - INTERVAL '25 days',
 'cccc0001-0000-0000-0000-000000000005', NOW() - INTERVAL '28 days',
 'Hybrid approach. CDC (Change Data Capture) via Debezium for price/stock/title changes (< 30s delay). Full re-index nightly for embedding regeneration. Best of both worlds.',
 'Hybrid: CDC for hot fields, nightly batch for embeddings', false,
 'cccc0001-0000-0000-0000-000000000005', NOW() - INTERVAL '32 days', NOW() - INTERVAL '22 days'),

-- More NEEDS_INPUT
('55550001-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000003',
 'Observability stack: Datadog vs Grafana Cloud vs self-hosted',
 'Need to decide on the core observability platform before we can start instrumenting services.',
 'Budget: $5K/mo. Datadog: full-featured but expensive at scale. Grafana Cloud: cheaper, open-source compatible. Self-hosted: cheapest but operational burden.',
 'NEEDS_INPUT', 'BLOCKING', 'ARCHITECTURAL',
 'cccc0001-0000-0000-0000-000000000006', 'cccc0001-0000-0000-0000-000000000010',
 '11110001-0000-0000-0000-000000000006', '33330001-0000-0000-0000-000000000008',
 'eeee0001-0000-0000-0000-000000000003', 'aabb0001-0000-0000-0000-000000000002',
 'aabb0002-0000-0000-0000-000000000002',
 4, NOW() + INTERVAL '1 hour',
 NULL, NULL, NULL, NULL, false,
 'cccc0001-0000-0000-0000-000000000006', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '45 minutes'),

('55550001-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002',
 'Shipping cost estimation API: ShipEngine vs EasyPost',
 'We need real-time shipping estimates for the new product page feature. Both APIs provide multi-carrier rates.',
 NULL,
 'NEEDS_INPUT', 'NORMAL', 'TECHNICAL',
 'cccc0001-0000-0000-0000-000000000007', 'cccc0001-0000-0000-0000-000000000003',
 '11110001-0000-0000-0000-000000000005', '33330001-0000-0000-0000-000000000010',
 'eeee0001-0000-0000-0000-000000000002', 'aabb0001-0000-0000-0000-000000000002',
 NULL,
 24, NOW() + INTERVAL '20 hours',
 NULL, NULL, NULL, NULL, false,
 'cccc0001-0000-0000-0000-000000000007', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '2 hours'),

-- DEFERRED
('55550001-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000001',
 'Multi-language search support scope',
 'Which languages should we support in v1 of the AI search? Full multilingual vs English-first with Spanish/French.',
 NULL,
 'DEFERRED', 'LOW', 'SCOPE',
 'cccc0001-0000-0000-0000-000000000004', 'cccc0001-0000-0000-0000-000000000002',
 '11110001-0000-0000-0000-000000000003', NULL,
 'eeee0001-0000-0000-0000-000000000001', 'aabb0001-0000-0000-0000-000000000001',
 NULL,
 72, NOW() - INTERVAL '5 days',
 NULL, NULL, NULL, NULL, false,
 'cccc0001-0000-0000-0000-000000000004', NOW() - INTERVAL '12 days', NOW() - INTERVAL '5 days'),

-- Escalated decision
('55550001-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', 'dddd0001-0000-0000-0000-000000000002',
 'Deprecate legacy checkout or maintain both flows?',
 'Once the new mobile checkout launches, do we immediately deprecate the old flow or run both in parallel for 90 days?',
 'Engineering wants immediate deprecation to avoid maintaining two codepaths. Product wants 90-day parallel run for safety.',
 'UNDER_DISCUSSION', 'HIGH', 'STRATEGIC',
 'cccc0001-0000-0000-0000-000000000003', 'cccc0001-0000-0000-0000-000000000001',
 '11110001-0000-0000-0000-000000000004', NULL,
 'eeee0001-0000-0000-0000-000000000002', 'aabb0001-0000-0000-0000-000000000001',
 'aabb0002-0000-0000-0000-000000000003',
 8, NOW() + INTERVAL '4 hours',
 NULL, NULL, NULL, NULL, true,
 'cccc0001-0000-0000-0000-000000000003', NOW() - INTERVAL '28 hours', NOW() - INTERVAL '2 hours')
ON CONFLICT (id) DO NOTHING;

-- Update escalation fields on escalated decision
UPDATE decisions SET
  escalation_level = 1,
  escalated_at = NOW() - INTERVAL '4 hours',
  escalated_to_id = 'cccc0001-0000-0000-0000-000000000001'
WHERE id = '55550001-0000-0000-0000-000000000012';

-- ============================================================
-- 15. DECISION VOTES
-- ============================================================
INSERT INTO decision_votes (id, decision_id, user_id, vote, comment, created_at) VALUES
-- Votes on embedding model decision
('66660001-0000-0000-0000-000000000001', '55550001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000005', 'APPROVE', 'Cohere''s 8% accuracy advantage is significant for our use case. The multilingual support also future-proofs us.', NOW() - INTERVAL '4 hours'),
('66660001-0000-0000-0000-000000000002', '55550001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000006', 'APPROVE', 'Agree with James. Cohere also has better batch embedding APIs which will help with the nightly re-index job.', NOW() - INTERVAL '3 hours'),
('66660001-0000-0000-0000-000000000003', '55550001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000009', 'NEEDS_MORE_INFO', 'Can we see cost projections at 10M embeddings/month? The per-token pricing looks similar but batch vs real-time pricing differs.', NOW() - INTERVAL '2 hours'),

-- Votes on guest checkout decision
('66660001-0000-0000-0000-000000000004', '55550001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000008', 'APPROVE', 'Guest checkout is essential for mobile. We can prompt account creation after purchase confirmation.', NOW() - INTERVAL '3 hours'),
('66660001-0000-0000-0000-000000000005', '55550001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000007', 'APPROVE', 'Second this. Post-purchase account creation converts at 40% vs 15% pre-purchase on competitor platforms.', NOW() - INTERVAL '2 hours'),
('66660001-0000-0000-0000-000000000006', '55550001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000009', 'REJECT', 'We lose attribution for analytics without account creation. Can we at least require email?', NOW() - INTERVAL '1 hour'),

-- Votes on vector database decision (already decided)
('66660001-0000-0000-0000-000000000007', '55550001-0000-0000-0000-000000000005', 'cccc0001-0000-0000-0000-000000000005', 'APPROVE', 'pgvector is the right call for our scale. We can migrate to Pinecone later if needed.', NOW() - INTERVAL '4 days'),
('66660001-0000-0000-0000-000000000008', '55550001-0000-0000-0000-000000000005', 'cccc0001-0000-0000-0000-000000000006', 'APPROVE', 'Agree. Less operational overhead and we can use existing Postgres expertise.', NOW() - INTERVAL '4 days'),

-- Votes on observability stack
('66660001-0000-0000-0000-000000000009', '55550001-0000-0000-0000-000000000009', 'cccc0001-0000-0000-0000-000000000006', 'APPROVE', 'Grafana Cloud. Open-source compatible, within budget, and we can migrate to self-hosted later if costs grow.', NOW() - INTERVAL '2 hours'),
('66660001-0000-0000-0000-000000000010', '55550001-0000-0000-0000-000000000009', 'cccc0001-0000-0000-0000-000000000005', 'NEEDS_MORE_INFO', 'Need to validate that Grafana Cloud supports our distributed tracing volume. Can we get a trial?', NOW() - INTERVAL '1 hour')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 16. DECISION COMMENTS
-- ============================================================
INSERT INTO decision_comments (id, decision_id, author_id, content, parent_id, is_edited, created_at) VALUES
-- Thread on embedding model decision
('77770001-0000-0000-0000-000000000001', '55550001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000004',
 'I ran the benchmark suite last week. Here are the results across 5,000 test queries: ada-002 scored 71.2% relevance, Cohere scored 79.8%. The gap widens on non-English queries — ada-002 drops to 58% while Cohere maintains 74%.', NULL, false, NOW() - INTERVAL '5 hours'),

('77770001-0000-0000-0000-000000000002', '55550001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000005',
 'Those numbers make a strong case for Cohere. @David can you model the cost impact at our current query volume?', '77770001-0000-0000-0000-000000000001', false, NOW() - INTERVAL '4 hours'),

('77770001-0000-0000-0000-000000000003', '55550001-0000-0000-0000-000000000001', 'cccc0001-0000-0000-0000-000000000009',
 'At 2M queries/day with average 12 tokens per query, monthly cost would be approximately $720 for either provider. The real cost difference is in batch re-indexing — Cohere is about 30% cheaper there.', '77770001-0000-0000-0000-000000000002', false, NOW() - INTERVAL '2 hours'),

-- Thread on guest checkout
('77770001-0000-0000-0000-000000000004', '55550001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000003',
 'I spoke with three e-commerce CTOs at the conference last week. All three said guest checkout increased their mobile conversion by 40-60%. One showed me their data — the drop-off at the "create account" screen was 45%.', NULL, false, NOW() - INTERVAL '4 hours'),

('77770001-0000-0000-0000-000000000005', '55550001-0000-0000-0000-000000000002', 'cccc0001-0000-0000-0000-000000000002',
 'We should support guest checkout but require email for order updates. That gives us enough for analytics while removing the account creation barrier. We can use the email to create a soft account behind the scenes.', '77770001-0000-0000-0000-000000000004', false, NOW() - INTERVAL '2 hours'),

-- Comment on observability decision
('77770001-0000-0000-0000-000000000006', '55550001-0000-0000-0000-000000000009', 'cccc0001-0000-0000-0000-000000000010',
 'I can get us a 30-day Grafana Cloud trial with enterprise features. Will set it up this week so we can validate tracing volume before committing.', NULL, false, NOW() - INTERVAL '1 hour')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 17. VERIFY COUNTS
-- ============================================================
-- Run these to verify the seed data was inserted correctly:
SELECT 'tenants' AS entity, COUNT(*) AS count FROM tenants WHERE id = '11111111-1111-1111-1111-111111111111'
UNION ALL SELECT 'users', COUNT(*) FROM users WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
UNION ALL SELECT 'projects', COUNT(*) FROM projects WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
UNION ALL SELECT 'teams', COUNT(*) FROM teams WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
UNION ALL SELECT 'team_members', COUNT(*) FROM team_members WHERE team_id IN (SELECT id FROM teams WHERE tenant_id = '11111111-1111-1111-1111-111111111111')
UNION ALL SELECT 'outcomes', COUNT(*) FROM outcomes WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
UNION ALL SELECT 'key_results', COUNT(*) FROM key_results WHERE outcome_id IN (SELECT id FROM outcomes WHERE tenant_id = '11111111-1111-1111-1111-111111111111')
UNION ALL SELECT 'hypotheses', COUNT(*) FROM hypotheses WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
UNION ALL SELECT 'experiments', COUNT(*) FROM experiments WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
UNION ALL SELECT 'decisions', COUNT(*) FROM decisions WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
UNION ALL SELECT 'decision_votes', COUNT(*) FROM decision_votes WHERE decision_id IN (SELECT id FROM decisions WHERE tenant_id = '11111111-1111-1111-1111-111111111111')
UNION ALL SELECT 'decision_comments', COUNT(*) FROM decision_comments WHERE decision_id IN (SELECT id FROM decisions WHERE tenant_id = '11111111-1111-1111-1111-111111111111')
UNION ALL SELECT 'stakeholders', COUNT(*) FROM stakeholders WHERE tenant_id = '11111111-1111-1111-1111-111111111111'
UNION ALL SELECT 'decision_queues', COUNT(*) FROM decision_queues WHERE tenant_id = '11111111-1111-1111-1111-111111111111';

-- Expected results:
-- tenants:           1
-- users:            10
-- projects:          3
-- teams:             3
-- team_members:      9
-- outcomes:          7
-- key_results:       9
-- hypotheses:       10
-- experiments:       6
-- decisions:        12
-- decision_votes:   10
-- decision_comments: 6
-- stakeholders:      4
-- decision_queues:   3
```

## What This Data Covers

**3 Projects** in different states (Active, Active, Planning) with realistic descriptions, color accents, and ownership.

**10 Users** spanning CEO, VP Product, VP Engineering, 2 Product Managers, Staff Engineer, 2 Senior Engineers, Frontend Engineer, UX Designer, and Data Analyst. All have avatar URLs via pravatar.cc and realistic titles/departments. Manager chains are set.

**3 Teams** mapped to projects with leads and 3-4 members each, including cross-team membership (Elena is on both Search and Platform teams).

**7 Outcomes** with measurable success criteria, key results with current vs target values showing realistic progress, proper priority/status distribution.

**10 Hypotheses** across all lifecycle stages: 2 Draft, 2 Ready, 2 Building, 2 Measuring, 1 Validated, 1 Invalidated. Each has a structured "We believe / Will result in / Measured by" format with real product logic. The invalidated one includes a pivot rationale.

**6 Experiments** across types (A/B Test, Feature Flag, Canary): 2 Running with realistic interim data (traffic split, sample sizes, control/variant values, confidence levels), 2 Concluded, 2 Draft.

**12 Decisions** distributed across all statuses: 3 NEEDS_INPUT (including 2 BLOCKING with SLA breaching), 3 UNDER_DISCUSSION (1 escalated), 2 DECIDED, 2 IMPLEMENTED, 1 DEFERRED. Includes votes (10), threaded comments (6), stakeholder assignments, SLA deadlines, and resolution rationale on decided items.

**4 Stakeholders** including 3 internal (mapped to users) and 1 external advisor, with response time metrics and decision counts.

All data tells a coherent product story: an AI search revamp in active development, a mobile checkout redesign running experiments, and an observability initiative in early planning.
