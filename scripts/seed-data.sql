-- ============================================
-- CodeOps Seed Data for Development/Testing
-- ============================================
-- Matches corrected Hibernate schema (ZC-014)
--
-- Run after infrastructure is up:
--   docker exec -i codeops-db psql -U codeops -d codeops < seed-data.sql
--
-- Test Credentials:
--   alice@acme.com / password123 (Tenant Owner)
--   grace@techstart.io / password123 (Tenant Owner)
-- ============================================

SET search_path TO public;

-- ============================================
-- 1. TENANTS
-- ============================================
INSERT INTO tenants (id, name, slug, logo_url, settings, status, created_at, updated_at) VALUES
  ('11111111-1111-1111-1111-111111111111',
   'Acme Corporation', 'acme', NULL,
   '{"features": ["analytics", "integrations"], "theme": "light"}',
   'ACTIVE', NOW() - INTERVAL '90 days', NOW()),
  ('22222222-2222-2222-2222-222222222222',
   'TechStart Inc', 'techstart', NULL,
   '{"features": ["analytics"], "theme": "dark"}',
   'ACTIVE', NOW() - INTERVAL '30 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 2. PERMISSIONS
-- ============================================
INSERT INTO permissions (id, code, name, description, category) VALUES
  ('eeeeeeee-0000-0000-0000-000000000001', 'TENANT_MANAGE', 'Manage Tenant', 'Full tenant administration', 'ADMIN'),
  ('eeeeeeee-0000-0000-0000-000000000002', 'USER_MANAGE', 'Manage Users', 'Create, update, delete users', 'ADMIN'),
  ('eeeeeeee-0000-0000-0000-000000000003', 'TEAM_MANAGE', 'Manage Teams', 'Create, update, delete teams', 'ADMIN'),
  ('eeeeeeee-0000-0000-0000-000000000004', 'OUTCOME_CREATE', 'Create Outcomes', 'Create new outcomes', 'OUTCOME'),
  ('eeeeeeee-0000-0000-0000-000000000005', 'OUTCOME_EDIT', 'Edit Outcomes', 'Edit existing outcomes', 'OUTCOME'),
  ('eeeeeeee-0000-0000-0000-000000000006', 'OUTCOME_DELETE', 'Delete Outcomes', 'Delete outcomes', 'OUTCOME'),
  ('eeeeeeee-0000-0000-0000-000000000007', 'OUTCOME_VALIDATE', 'Validate Outcomes', 'Mark outcomes as validated/invalidated', 'OUTCOME'),
  ('eeeeeeee-0000-0000-0000-000000000008', 'HYPOTHESIS_CREATE', 'Create Hypotheses', 'Create new hypotheses', 'HYPOTHESIS'),
  ('eeeeeeee-0000-0000-0000-000000000009', 'HYPOTHESIS_EDIT', 'Edit Hypotheses', 'Edit existing hypotheses', 'HYPOTHESIS'),
  ('eeeeeeee-0000-0000-0000-000000000010', 'HYPOTHESIS_CONCLUDE', 'Conclude Hypotheses', 'Mark hypotheses as validated/invalidated', 'HYPOTHESIS'),
  ('eeeeeeee-0000-0000-0000-000000000011', 'DECISION_CREATE', 'Create Decisions', 'Create new decisions', 'DECISION'),
  ('eeeeeeee-0000-0000-0000-000000000012', 'DECISION_EDIT', 'Edit Decisions', 'Edit existing decisions', 'DECISION'),
  ('eeeeeeee-0000-0000-0000-000000000013', 'DECISION_RESOLVE', 'Resolve Decisions', 'Mark decisions as decided', 'DECISION'),
  ('eeeeeeee-0000-0000-0000-000000000014', 'DECISION_VOTE', 'Vote on Decisions', 'Cast votes on decisions', 'DECISION'),
  ('eeeeeeee-0000-0000-0000-000000000015', 'DECISION_ESCALATE', 'Escalate Decisions', 'Escalate overdue decisions', 'DECISION'),
  ('eeeeeeee-0000-0000-0000-000000000016', 'STAKEHOLDER_MANAGE', 'Manage Stakeholders', 'Create, update stakeholders', 'STAKEHOLDER'),
  ('eeeeeeee-0000-0000-0000-000000000017', 'ANALYTICS_VIEW', 'View Analytics', 'Access analytics dashboard', 'ANALYTICS'),
  ('eeeeeeee-0000-0000-0000-000000000018', 'REPORTS_GENERATE', 'Generate Reports', 'Generate and export reports', 'ANALYTICS')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 3. ROLES (System roles - tenant_id is NULL)
-- ============================================
INSERT INTO roles (id, tenant_id, code, name, description, category, level, is_system_role, created_at) VALUES
  -- Platform Roles (no tenant)
  ('d0d0d0d0-0000-0000-0000-000000000001', NULL, 'PLATFORM_OWNER', 'Platform Owner', 'Full platform access', 'SYSTEM', 'L9_OWNER', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000002', NULL, 'PLATFORM_ADMIN', 'Platform Admin', 'Platform administration', 'SYSTEM', 'L8_CXXX', true, NOW()),

  -- Tenant Roles (system templates, tenant_id NULL)
  ('d0d0d0d0-0000-0000-0000-000000000003', NULL, 'TENANT_OWNER', 'Tenant Owner', 'Full tenant access', 'EXECUTIVE', 'L9_OWNER', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000004', NULL, 'TENANT_ADMIN', 'Tenant Admin', 'Tenant administration', 'EXECUTIVE', 'L8_CXXX', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000005', NULL, 'VP_PRODUCT', 'VP of Product', 'Product leadership', 'PRODUCT', 'L6_VP', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000006', NULL, 'VP_ENGINEERING', 'VP of Engineering', 'Engineering leadership', 'ENGINEERING', 'L6_VP', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000007', NULL, 'PRODUCT_MANAGER', 'Product Manager', 'Product management', 'PRODUCT', 'L4_MANAGER', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000008', NULL, 'ENGINEERING_MANAGER', 'Engineering Manager', 'Engineering management', 'ENGINEERING', 'L4_MANAGER', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000009', NULL, 'TECH_LEAD', 'Tech Lead', 'Technical leadership', 'ENGINEERING', 'L3_LEAD', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000010', NULL, 'SENIOR_ENGINEER', 'Senior Engineer', 'Senior engineering', 'ENGINEERING', 'L2_SENIOR', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000011', NULL, 'ENGINEER', 'Engineer', 'Engineering', 'ENGINEERING', 'L1_INDIVIDUAL', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000012', NULL, 'DESIGNER', 'Designer', 'UX/UI Design', 'UX', 'L2_SENIOR', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000013', NULL, 'DATA_ANALYST', 'Data Analyst', 'Data analysis', 'DATA', 'L2_SENIOR', true, NOW()),
  ('d0d0d0d0-0000-0000-0000-000000000014', NULL, 'STAKEHOLDER', 'Stakeholder', 'Decision stakeholder', 'STAKEHOLDER', 'L4_MANAGER', true, NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 4. ROLE PERMISSIONS
-- ============================================
-- Tenant Owner gets everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd0d0d0d0-0000-0000-0000-000000000003', id FROM permissions
ON CONFLICT DO NOTHING;

-- Tenant Admin gets everything except TENANT_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'd0d0d0d0-0000-0000-0000-000000000004', id FROM permissions WHERE code != 'TENANT_MANAGE'
ON CONFLICT DO NOTHING;

-- Product Manager
INSERT INTO role_permissions (role_id, permission_id) VALUES
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000004'),
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000005'),
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000007'),
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000008'),
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000009'),
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000011'),
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000012'),
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000013'),
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000014'),
  ('d0d0d0d0-0000-0000-0000-000000000007', 'eeeeeeee-0000-0000-0000-000000000017')
ON CONFLICT DO NOTHING;

-- Engineer
INSERT INTO role_permissions (role_id, permission_id) VALUES
  ('d0d0d0d0-0000-0000-0000-000000000011', 'eeeeeeee-0000-0000-0000-000000000008'),
  ('d0d0d0d0-0000-0000-0000-000000000011', 'eeeeeeee-0000-0000-0000-000000000009'),
  ('d0d0d0d0-0000-0000-0000-000000000011', 'eeeeeeee-0000-0000-0000-000000000011'),
  ('d0d0d0d0-0000-0000-0000-000000000011', 'eeeeeeee-0000-0000-0000-000000000014'),
  ('d0d0d0d0-0000-0000-0000-000000000011', 'eeeeeeee-0000-0000-0000-000000000017')
ON CONFLICT DO NOTHING;

-- ============================================
-- 5. USERS (password: password123)
-- ============================================
-- BCrypt hash for 'password123'
-- $2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW

INSERT INTO users (id, tenant_id, email, password_hash, first_name, last_name, title, department, avatar_url, role_id, manager_id, is_active, created_at, updated_at) VALUES
  -- Acme Corporation Users
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001', '11111111-1111-1111-1111-111111111111',
   'alice@acme.com', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'Alice', 'Anderson', 'CEO', 'Executive', NULL,
   'd0d0d0d0-0000-0000-0000-000000000003', NULL, true, NOW() - INTERVAL '90 days', NOW()),

  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002', '11111111-1111-1111-1111-111111111111',
   'bob@acme.com', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'Bob', 'Baker', 'CTO', 'Engineering', NULL,
   'd0d0d0d0-0000-0000-0000-000000000004', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001', true, NOW() - INTERVAL '85 days', NOW()),

  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003', '11111111-1111-1111-1111-111111111111',
   'carol@acme.com', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'Carol', 'Chen', 'VP Product', 'Product', NULL,
   'd0d0d0d0-0000-0000-0000-000000000005', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001', true, NOW() - INTERVAL '80 days', NOW()),

  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004', '11111111-1111-1111-1111-111111111111',
   'david@acme.com', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'David', 'Davis', 'Engineering Manager', 'Engineering', NULL,
   'd0d0d0d0-0000-0000-0000-000000000008', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002', true, NOW() - INTERVAL '75 days', NOW()),

  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005', '11111111-1111-1111-1111-111111111111',
   'emma@acme.com', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'Emma', 'Evans', 'Senior Engineer', 'Engineering', NULL,
   'd0d0d0d0-0000-0000-0000-000000000010', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004', true, NOW() - INTERVAL '70 days', NOW()),

  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa006', '11111111-1111-1111-1111-111111111111',
   'frank@acme.com', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'Frank', 'Foster', 'Product Manager', 'Product', NULL,
   'd0d0d0d0-0000-0000-0000-000000000007', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003', true, NOW() - INTERVAL '65 days', NOW()),

  -- TechStart Users
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001', '22222222-2222-2222-2222-222222222222',
   'grace@techstart.io', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'Grace', 'Garcia', 'Founder & CEO', 'Executive', NULL,
   'd0d0d0d0-0000-0000-0000-000000000003', NULL, true, NOW() - INTERVAL '30 days', NOW()),

  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002', '22222222-2222-2222-2222-222222222222',
   'henry@techstart.io', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'Henry', 'Hill', 'Product Lead', 'Product', NULL,
   'd0d0d0d0-0000-0000-0000-000000000007', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001', true, NOW() - INTERVAL '28 days', NOW()),

  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003', '22222222-2222-2222-2222-222222222222',
   'ivy@techstart.io', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'Ivy', 'Irwin', 'Tech Lead', 'Engineering', NULL,
   'd0d0d0d0-0000-0000-0000-000000000009', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001', true, NOW() - INTERVAL '25 days', NOW()),

  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004', '22222222-2222-2222-2222-222222222222',
   'jack@techstart.io', '$2b$10$aHx9ZvYUhUzvh7m/K9S6.ud9F.lWJ6gM49PeMc9Z.D4Z/7Zb62wvW',
   'Jack', 'Johnson', 'Engineer', 'Engineering', NULL,
   'd0d0d0d0-0000-0000-0000-000000000011', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003', true, NOW() - INTERVAL '20 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 6. TEAMS
-- ============================================
INSERT INTO teams (id, tenant_id, name, slug, description, icon_url, color, lead_id, is_active, settings, created_at, updated_at) VALUES
  -- Acme Teams
  ('cccccccc-cccc-cccc-cccc-ccccccccc001', '11111111-1111-1111-1111-111111111111',
   'Platform Team', 'platform', 'Core platform infrastructure and services', NULL, '#3B82F6',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004', true, NULL, NOW() - INTERVAL '85 days', NOW()),

  ('cccccccc-cccc-cccc-cccc-ccccccccc002', '11111111-1111-1111-1111-111111111111',
   'Mobile Team', 'mobile', 'iOS and Android application development', NULL, '#10B981',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005', true, NULL, NOW() - INTERVAL '80 days', NOW()),

  ('cccccccc-cccc-cccc-cccc-ccccccccc003', '11111111-1111-1111-1111-111111111111',
   'Product Team', 'product', 'Product management and strategy', NULL, '#8B5CF6',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa006', true, NULL, NOW() - INTERVAL '75 days', NOW()),

  -- TechStart Teams
  ('cccccccc-cccc-cccc-cccc-ccccccccc004', '22222222-2222-2222-2222-222222222222',
   'Core Team', 'core', 'Full-stack product development', NULL, '#F59E0B',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003', true, NULL, NOW() - INTERVAL '25 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 7. TEAM MEMBERS
-- ============================================
INSERT INTO team_members (id, team_id, user_id, team_role, joined_at, added_by_id) VALUES
  -- Platform Team
  ('dddddddd-dddd-dddd-dddd-ddddddddd001', 'cccccccc-cccc-cccc-cccc-ccccccccc001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004', 'LEAD', NOW() - INTERVAL '85 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002'),
  ('dddddddd-dddd-dddd-dddd-ddddddddd002', 'cccccccc-cccc-cccc-cccc-ccccccccc001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005', 'MEMBER', NOW() - INTERVAL '70 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004'),

  -- Mobile Team
  ('dddddddd-dddd-dddd-dddd-ddddddddd003', 'cccccccc-cccc-cccc-cccc-ccccccccc002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005', 'LEAD', NOW() - INTERVAL '80 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002'),

  -- Product Team
  ('dddddddd-dddd-dddd-dddd-ddddddddd004', 'cccccccc-cccc-cccc-cccc-ccccccccc003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa006', 'LEAD', NOW() - INTERVAL '75 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003'),
  ('dddddddd-dddd-dddd-dddd-ddddddddd005', 'cccccccc-cccc-cccc-cccc-ccccccccc003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003', 'MEMBER', NOW() - INTERVAL '75 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001'),

  -- TechStart Core Team
  ('dddddddd-dddd-dddd-dddd-ddddddddd006', 'cccccccc-cccc-cccc-cccc-ccccccccc004', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003', 'LEAD', NOW() - INTERVAL '25 days', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
  ('dddddddd-dddd-dddd-dddd-ddddddddd007', 'cccccccc-cccc-cccc-cccc-ccccccccc004', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002', 'MEMBER', NOW() - INTERVAL '25 days', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
  ('dddddddd-dddd-dddd-dddd-ddddddddd008', 'cccccccc-cccc-cccc-cccc-ccccccccc004', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004', 'MEMBER', NOW() - INTERVAL '20 days', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 8. DECISION QUEUES
-- ============================================
INSERT INTO decision_queues (id, tenant_id, name, description, is_default, sla_config, created_at, updated_at) VALUES
  -- Acme Queues
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeee001', '11111111-1111-1111-1111-111111111111',
   'Product Decisions', 'Product strategy and feature decisions', true,
   '{"BLOCKING": 4, "HIGH": 24, "NORMAL": 48, "LOW": 168}',
   NOW() - INTERVAL '85 days', NOW()),

  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeee002', '11111111-1111-1111-1111-111111111111',
   'Technical Decisions', 'Architecture and technical implementation choices', false,
   '{"BLOCKING": 4, "HIGH": 24, "NORMAL": 72, "LOW": 168}',
   NOW() - INTERVAL '80 days', NOW()),

  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeee003', '11111111-1111-1111-1111-111111111111',
   'UX Decisions', 'User experience and design decisions', false,
   '{"BLOCKING": 8, "HIGH": 24, "NORMAL": 48, "LOW": 120}',
   NOW() - INTERVAL '75 days', NOW()),

  -- TechStart Queue
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeee004', '22222222-2222-2222-2222-222222222222',
   'All Decisions', 'Central decision queue for startup', true,
   '{"BLOCKING": 2, "HIGH": 12, "NORMAL": 24, "LOW": 72}',
   NOW() - INTERVAL '25 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 9. STAKEHOLDERS
-- ============================================
INSERT INTO stakeholders (id, tenant_id, name, email, title, organization, phone, avatar_url, type, user_id, expertise, preferred_contact_method, availability_notes, timezone, decisions_pending, decisions_completed, decisions_escalated, avg_response_time_hours, last_decision_at, is_active, notes, external_refs, created_at, updated_at, created_by_id) VALUES
  -- Acme Stakeholders
  ('88888888-8888-8888-8888-888888888801', '11111111-1111-1111-1111-111111111111',
   'Alice Anderson', 'alice@acme.com', 'CEO', 'Acme Corporation', NULL, NULL,
   'EXECUTIVE', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001',
   '["strategy", "business", "vision"]', 'EMAIL', 'Available mornings', 'America/New_York',
   3, 25, 1, 4.5, NOW() - INTERVAL '2 days', true, NULL, NULL,
   NOW() - INTERVAL '85 days', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001'),

  ('88888888-8888-8888-8888-888888888802', '11111111-1111-1111-1111-111111111111',
   'Bob Baker', 'bob@acme.com', 'CTO', 'Acme Corporation', NULL, NULL,
   'TECHNICAL', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002',
   '["architecture", "security", "infrastructure"]', 'SLACK', 'Prefers async', 'America/New_York',
   2, 32, 0, 6.2, NOW() - INTERVAL '1 day', true, NULL, NULL,
   NOW() - INTERVAL '80 days', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001'),

  ('88888888-8888-8888-8888-888888888803', '11111111-1111-1111-1111-111111111111',
   'Carol Chen', 'carol@acme.com', 'VP Product', 'Acme Corporation', NULL, NULL,
   'INTERNAL', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003',
   '["product", "roadmap", "prioritization"]', 'EMAIL', NULL, 'America/Los_Angeles',
   4, 45, 2, 8.1, NOW() - INTERVAL '6 hours', true, NULL, NULL,
   NOW() - INTERVAL '75 days', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001'),

  ('88888888-8888-8888-8888-888888888804', '11111111-1111-1111-1111-111111111111',
   'External Legal Counsel', 'legal@lawfirm.com', 'Partner', 'Smith & Associates', '+1-555-0123', NULL,
   'EXTERNAL', NULL,
   '["contracts", "compliance", "ip"]', 'EMAIL', 'Response within 48h', 'America/Chicago',
   1, 8, 0, 36.0, NOW() - INTERVAL '10 days', true, 'Primary legal contact', NULL,
   NOW() - INTERVAL '60 days', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001'),

  -- TechStart Stakeholders
  ('88888888-8888-8888-8888-888888888805', '22222222-2222-2222-2222-222222222222',
   'Grace Garcia', 'grace@techstart.io', 'Founder & CEO', 'TechStart Inc', NULL, NULL,
   'EXECUTIVE', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001',
   '["strategy", "fundraising", "product"]', 'SLACK', 'Very responsive', 'America/Los_Angeles',
   2, 15, 0, 2.5, NOW() - INTERVAL '4 hours', true, NULL, NULL,
   NOW() - INTERVAL '25 days', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),

  ('88888888-8888-8888-8888-888888888806', '22222222-2222-2222-2222-222222222222',
   'Henry Hill', 'henry@techstart.io', 'Product Lead', 'TechStart Inc', NULL, NULL,
   'INTERNAL', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002',
   '["ux", "product", "customer"]', 'SLACK', NULL, 'America/Los_Angeles',
   3, 12, 1, 5.0, NOW() - INTERVAL '1 day', true, NULL, NULL,
   NOW() - INTERVAL '20 days', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 10. OUTCOMES
-- ============================================
INSERT INTO outcomes (id, tenant_id, title, description, success_criteria, target_metrics, current_metrics, status, priority, team_id, owner_id, target_date, started_at, validated_at, invalidated_at, validation_notes, validated_by_id, invalidated_by_id, external_refs, tags, created_at, updated_at, created_by_id) VALUES
  -- Acme Outcomes
  ('ffffffff-ffff-ffff-ffff-ffffffffff01', '11111111-1111-1111-1111-111111111111',
   'Launch API v2.0', 'Complete redesign of REST API with improved performance and developer experience',
   'API latency < 100ms p95, 100% backward compatibility, OpenAPI spec published',
   '{"latency_p95_ms": 100, "backward_compat_pct": 100, "endpoints": 50}',
   '{"latency_p95_ms": 145, "backward_compat_pct": 100, "endpoints": 38}',
   'IN_PROGRESS', 'HIGH', 'cccccccc-cccc-cccc-cccc-ccccccccc001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004',
   (CURRENT_DATE + INTERVAL '30 days')::DATE, NOW() - INTERVAL '55 days', NULL, NULL, NULL, NULL, NULL,
   '{"jira": "ACME-100"}', '["api", "platform", "v2"]',
   NOW() - INTERVAL '60 days', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003'),

  ('ffffffff-ffff-ffff-ffff-ffffffffff02', '11111111-1111-1111-1111-111111111111',
   'Mobile App 3.0 Release', 'Major update with offline mode and improved UX',
   'App store rating >= 4.5, crash rate < 0.1%, 50% reduction in support tickets',
   '{"rating": 4.5, "crash_rate_pct": 0.1, "ticket_reduction_pct": 50}',
   '{"rating": 4.2, "crash_rate_pct": 0.3, "ticket_reduction_pct": 25}',
   'IN_PROGRESS', 'CRITICAL', 'cccccccc-cccc-cccc-cccc-ccccccccc002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005',
   (CURRENT_DATE + INTERVAL '45 days')::DATE, NOW() - INTERVAL '40 days', NULL, NULL, NULL, NULL, NULL,
   '{"jira": "ACME-150"}', '["mobile", "ios", "android"]',
   NOW() - INTERVAL '45 days', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003'),

  ('ffffffff-ffff-ffff-ffff-ffffffffff03', '11111111-1111-1111-1111-111111111111',
   'SOC 2 Compliance', 'Achieve SOC 2 Type II certification',
   'Pass audit with zero critical findings, all controls documented',
   '{"critical_findings": 0, "controls_documented_pct": 100}',
   '{"critical_findings": 0, "controls_documented_pct": 100}',
   'VALIDATED', 'HIGH', 'cccccccc-cccc-cccc-cccc-ccccccccc001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002',
   (CURRENT_DATE - INTERVAL '10 days')::DATE, NOW() - INTERVAL '90 days', NOW() - INTERVAL '5 days', NULL,
   'Audit passed successfully. Certificate received.', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001', NULL,
   NULL, '["security", "compliance"]',
   NOW() - INTERVAL '120 days', NOW() - INTERVAL '5 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001'),

  -- TechStart Outcomes
  ('ffffffff-ffff-ffff-ffff-ffffffffff04', '22222222-2222-2222-2222-222222222222',
   'MVP Launch', 'Ship minimum viable product to first 10 customers',
   '10 paying customers, NPS > 30, < 5 critical bugs',
   '{"customers": 10, "nps": 30, "critical_bugs": 5}',
   '{"customers": 4, "nps": 42, "critical_bugs": 2}',
   'IN_PROGRESS', 'CRITICAL', 'cccccccc-cccc-cccc-cccc-ccccccccc004', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002',
   (CURRENT_DATE + INTERVAL '14 days')::DATE, NOW() - INTERVAL '20 days', NULL, NULL, NULL, NULL, NULL,
   NULL, '["mvp", "launch"]',
   NOW() - INTERVAL '25 days', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),

  ('ffffffff-ffff-ffff-ffff-ffffffffff05', '22222222-2222-2222-2222-222222222222',
   'Payment Integration', 'Integrate Stripe for subscription billing',
   'Successfully process test transactions, PCI compliance verified',
   '{"test_transactions": true, "pci_compliant": true}',
   '{"test_transactions": true, "pci_compliant": false}',
   'IN_PROGRESS', 'HIGH', 'cccccccc-cccc-cccc-cccc-ccccccccc004', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003',
   (CURRENT_DATE + INTERVAL '7 days')::DATE, NOW() - INTERVAL '10 days', NULL, NULL, NULL, NULL, NULL,
   NULL, '["payments", "stripe"]',
   NOW() - INTERVAL '15 days', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 11. KEY RESULTS
-- ============================================
INSERT INTO key_results (id, outcome_id, title, description, target_value, current_value, unit, created_at, updated_at) VALUES
  -- API v2.0 Key Results
  ('66666666-6666-6666-6666-666666666601', 'ffffffff-ffff-ffff-ffff-ffffffffff01',
   'API Latency p95', 'Response time at 95th percentile', 100, 145, 'ms', NOW() - INTERVAL '55 days', NOW()),
  ('66666666-6666-6666-6666-666666666602', 'ffffffff-ffff-ffff-ffff-ffffffffff01',
   'Backward Compatibility', 'Percentage of v1 endpoints supported', 100, 100, '%', NOW() - INTERVAL '55 days', NOW()),
  ('66666666-6666-6666-6666-666666666603', 'ffffffff-ffff-ffff-ffff-ffffffffff01',
   'Endpoint Coverage', 'Number of endpoints implemented', 50, 38, 'endpoints', NOW() - INTERVAL '55 days', NOW()),

  -- Mobile 3.0 Key Results
  ('66666666-6666-6666-6666-666666666604', 'ffffffff-ffff-ffff-ffff-ffffffffff02',
   'App Store Rating', 'Average rating across stores', 4.5, 4.2, 'stars', NOW() - INTERVAL '40 days', NOW()),
  ('66666666-6666-6666-6666-666666666605', 'ffffffff-ffff-ffff-ffff-ffffffffff02',
   'Crash Rate', 'Percentage of sessions with crashes', 0.1, 0.3, '%', NOW() - INTERVAL '40 days', NOW()),
  ('66666666-6666-6666-6666-666666666606', 'ffffffff-ffff-ffff-ffff-ffffffffff02',
   'Support Ticket Reduction', 'Reduction vs previous version', 50, 25, '%', NOW() - INTERVAL '40 days', NOW()),

  -- MVP Key Results
  ('66666666-6666-6666-6666-666666666607', 'ffffffff-ffff-ffff-ffff-ffffffffff04',
   'Paying Customers', 'Number of paying customers', 10, 4, 'customers', NOW() - INTERVAL '20 days', NOW()),
  ('66666666-6666-6666-6666-666666666608', 'ffffffff-ffff-ffff-ffff-ffffffffff04',
   'NPS Score', 'Net Promoter Score', 30, 42, 'points', NOW() - INTERVAL '20 days', NOW()),
  ('66666666-6666-6666-6666-666666666609', 'ffffffff-ffff-ffff-ffff-ffffffffff04',
   'Critical Bugs', 'Open critical bugs', 5, 2, 'bugs', NOW() - INTERVAL '20 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 12. HYPOTHESES
-- ============================================
INSERT INTO hypotheses (id, tenant_id, outcome_id, title, belief, expected_result, measurement_criteria, status, priority, owner_id, experiment_config, experiment_results, blocked_reason, conclusion_notes, external_refs, tags, effort, impact, confidence, started_at, deployed_at, measuring_started_at, concluded_at, concluded_by_id, created_at, updated_at, created_by_id) VALUES
  -- API v2.0 Hypotheses
  ('99999999-9999-9999-9999-999999999901', '11111111-1111-1111-1111-111111111111',
   'ffffffff-ffff-ffff-ffff-ffffffffff01',
   'GraphQL reduces API calls by 60%',
   'Mobile clients are over-fetching data with REST endpoints',
   'Switching to GraphQL will reduce API calls per session by 60%',
   'Measure API calls per session before/after migration',
   'DEPLOYED', 'HIGH', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005',
   '{"variant": "graphql", "rollout_pct": 25}', NULL, NULL, NULL,
   NULL, '["graphql", "performance"]', 'L', 'XL', 'MEDIUM',
   NOW() - INTERVAL '45 days', NOW() - INTERVAL '30 days', NOW() - INTERVAL '20 days', NULL, NULL,
   NOW() - INTERVAL '50 days', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004'),

  ('99999999-9999-9999-9999-999999999902', '11111111-1111-1111-1111-111111111111',
   'ffffffff-ffff-ffff-ffff-ffffffffff01',
   'Connection pooling improves latency 40%',
   'Database connections are being created per-request',
   'Connection pooling will reduce p95 latency by 40%',
   'Monitor p95 latency via APM before/after',
   'VALIDATED', 'HIGH', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005',
   '{"pool_size": 30}', '{"latency_reduction_pct": 45}', NULL, 'Exceeded expectations. 45% improvement measured.',
   NULL, '["database", "performance"]', 'M', 'XL', 'HIGH',
   NOW() - INTERVAL '50 days', NOW() - INTERVAL '45 days', NOW() - INTERVAL '40 days', NOW() - INTERVAL '35 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004',
   NOW() - INTERVAL '55 days', NOW() - INTERVAL '35 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004'),

  -- Mobile 3.0 Hypotheses
  ('99999999-9999-9999-9999-999999999903', '11111111-1111-1111-1111-111111111111',
   'ffffffff-ffff-ffff-ffff-ffffffffff02',
   'Offline mode increases DAU 25%',
   'Users in low-connectivity areas abandon the app',
   'Offline mode will increase DAU by 25% in target regions',
   'Compare DAU before/after, segment by network quality',
   'BUILDING', 'CRITICAL', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005',
   '{"target_regions": ["rural", "emerging"]}', NULL, NULL, NULL,
   NULL, '["offline", "mobile"]', 'XL', 'XL', 'MEDIUM',
   NOW() - INTERVAL '30 days', NULL, NULL, NULL, NULL,
   NOW() - INTERVAL '35 days', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003'),

  ('99999999-9999-9999-9999-999999999904', '11111111-1111-1111-1111-111111111111',
   'ffffffff-ffff-ffff-ffff-ffffffffff02',
   'Dark mode reduces uninstalls',
   'Users requesting dark mode are churning at higher rates',
   'Dark mode will reduce uninstall rate by 15%',
   'Track uninstall rate by dark mode preference',
   'INVALIDATED', 'LOW', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005',
   '{"ab_test": true}', '{"uninstall_change_pct": -2}', NULL, 'No statistically significant difference found.',
   NULL, '["ux", "mobile"]', 'S', 'M', 'LOW',
   NOW() - INTERVAL '40 days', NOW() - INTERVAL '35 days', NOW() - INTERVAL '30 days', NOW() - INTERVAL '20 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003',
   NOW() - INTERVAL '45 days', NOW() - INTERVAL '20 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003'),

  -- TechStart Hypotheses
  ('99999999-9999-9999-9999-999999999905', '22222222-2222-2222-2222-222222222222',
   'ffffffff-ffff-ffff-ffff-ffffffffff04',
   'Onboarding wizard reduces time-to-value',
   'Users are getting stuck during initial setup',
   '5-step wizard will reduce time to first successful action by 50%',
   'Track time from signup to first completed task',
   'MEASURING', 'HIGH', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002',
   '{"steps": 5, "variant": "wizard"}', NULL, NULL, NULL,
   NULL, '["onboarding", "ux"]', 'M', 'XL', 'HIGH',
   NOW() - INTERVAL '15 days', NOW() - INTERVAL '10 days', NOW() - INTERVAL '5 days', NULL, NULL,
   NOW() - INTERVAL '20 days', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),

  ('99999999-9999-9999-9999-999999999906', '22222222-2222-2222-2222-222222222222',
   'ffffffff-ffff-ffff-ffff-ffffffffff05',
   'Annual pricing increases LTV',
   'Monthly subscribers churn at higher rates',
   'Annual discount will increase average LTV by 40%',
   'Track LTV by billing cycle',
   'READY', 'HIGH', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002',
   '{"annual_discount_pct": 20}', NULL, NULL, NULL,
   NULL, '["pricing", "retention"]', 'S', 'XL', 'HIGH',
   NULL, NULL, NULL, NULL, NULL,
   NOW() - INTERVAL '10 days', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 13. DECISIONS
-- ============================================
INSERT INTO decisions (id, tenant_id, title, description, context, options, status, priority, decision_type, owner_id, assigned_to_id, outcome_id, hypothesis_id, team_id, queue_id, stakeholder_id, sla_hours, due_at, escalation_level, escalated_at, escalated_to_id, decided_by_id, decided_at, decision_rationale, selected_option, resolution, was_escalated, blocked_items, external_refs, tags, created_at, updated_at, created_by_id) VALUES
  -- Acme NEEDS_INPUT Decisions
  ('77777777-7777-7777-7777-777777777701', '11111111-1111-1111-1111-111111111111',
   'GraphQL client library: Apollo vs Relay?',
   'Need to choose GraphQL client library for frontend migration',
   'We are migrating mobile clients to GraphQL as part of API v2.0',
   '[{"id": "apollo", "title": "Apollo Client", "pros": ["Better DevTools", "Larger community"], "cons": ["Larger bundle"]}, {"id": "relay", "title": "Relay", "pros": ["Facebook backing", "Optimized"], "cons": ["Steeper learning curve"]}]',
   'NEEDS_INPUT', 'HIGH', 'TECHNICAL',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', '99999999-9999-9999-9999-999999999901',
   'cccccccc-cccc-cccc-cccc-ccccccccc001', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeee002', '88888888-8888-8888-8888-888888888802',
   24, NOW() + INTERVAL '20 hours', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false,
   NULL, NULL, '["graphql", "frontend"]',
   NOW() - INTERVAL '4 hours', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005'),

  ('77777777-7777-7777-7777-777777777702', '11111111-1111-1111-1111-111111111111',
   'Offline storage: SQLite vs Realm?',
   'Need to choose local database for mobile offline functionality',
   'Mobile 3.0 requires robust offline storage for sync',
   '[{"id": "sqlite", "title": "SQLite", "pros": ["Standard", "Lightweight"], "cons": ["Manual ORM"]}, {"id": "realm", "title": "Realm", "pros": ["Easy sync", "Reactive"], "cons": ["Vendor lock-in"]}]',
   'NEEDS_INPUT', 'BLOCKING', 'TECHNICAL',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005',
   'ffffffff-ffff-ffff-ffff-ffffffffff02', '99999999-9999-9999-9999-999999999903',
   'cccccccc-cccc-cccc-cccc-ccccccccc002', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeee002', '88888888-8888-8888-8888-888888888802',
   4, NOW() - INTERVAL '1 hour', 1, NOW() - INTERVAL '30 minutes', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002', NULL, NULL, NULL, NULL, NULL, true,
   '["99999999-9999-9999-9999-999999999903"]', NULL, '["mobile", "database"]',
   NOW() - INTERVAL '5 hours', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005'),

  -- Acme UNDER_DISCUSSION
  ('77777777-7777-7777-7777-777777777703', '11111111-1111-1111-1111-111111111111',
   'Microservices vs Modular Monolith for v2?',
   'Architecture decision for platform rewrite',
   'Current monolith is showing scaling issues. Need to decide on architecture for v2.',
   '[{"id": "micro", "title": "Microservices"}, {"id": "modular", "title": "Modular Monolith"}]',
   'UNDER_DISCUSSION', 'HIGH', 'ARCHITECTURAL',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004',
   NULL, NULL, 'cccccccc-cccc-cccc-cccc-ccccccccc001', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeee002', '88888888-8888-8888-8888-888888888802',
   72, NOW() + INTERVAL '48 hours', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false,
   NULL, '{"confluence": "ARCH-001"}', '["architecture", "platform"]',
   NOW() - INTERVAL '2 days', NOW(), 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002'),

  -- Acme DECIDED
  ('77777777-7777-7777-7777-777777777704', '11111111-1111-1111-1111-111111111111',
   'Connection pool size: 20, 30, or 50?',
   'Tuning connection pool for API v2.0 performance',
   'Load testing showed connection exhaustion under high load',
   '[{"id": "20", "title": "20 connections"}, {"id": "30", "title": "30 connections"}, {"id": "50", "title": "50 connections"}]',
   'DECIDED', 'HIGH', 'TECHNICAL',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', '99999999-9999-9999-9999-999999999902',
   'cccccccc-cccc-cccc-cccc-ccccccccc001', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeee002', '88888888-8888-8888-8888-888888888802',
   24, NOW() - INTERVAL '5 days', 0, NULL, NULL, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002', NOW() - INTERVAL '6 days',
   '30 connections provides optimal balance of resource usage and throughput',
   '{"id": "30", "title": "30 connections"}',
   'Selected 30 connections with auto-scaling. Testing showed optimal performance without resource waste.',
   false, NULL, NULL, '["database", "performance"]',
   NOW() - INTERVAL '8 days', NOW() - INTERVAL '6 days', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004'),

  -- TechStart Decisions
  ('77777777-7777-7777-7777-777777777705', '22222222-2222-2222-2222-222222222222',
   'Onboarding: 3, 5, or 7 steps?',
   'Determine optimal onboarding wizard length',
   'Need to balance thoroughness with completion rate',
   '[{"id": "3", "title": "3 steps"}, {"id": "5", "title": "5 steps"}, {"id": "7", "title": "7 steps"}]',
   'NEEDS_INPUT', 'HIGH', 'PRODUCT',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002',
   'ffffffff-ffff-ffff-ffff-ffffffffff04', '99999999-9999-9999-9999-999999999905',
   'cccccccc-cccc-cccc-cccc-ccccccccc004', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeee004', '88888888-8888-8888-8888-888888888805',
   12, NOW() + INTERVAL '8 hours', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false,
   NULL, NULL, '["onboarding", "ux"]',
   NOW() - INTERVAL '6 hours', NOW(), 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'),

  ('77777777-7777-7777-7777-777777777706', '22222222-2222-2222-2222-222222222222',
   'Payment processor: Stripe or Paddle?',
   'Select payment processing platform',
   'Need to integrate payments for MVP launch',
   '[{"id": "stripe", "title": "Stripe"}, {"id": "paddle", "title": "Paddle"}]',
   'DECIDED', 'BLOCKING', 'TECHNICAL',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003',
   'ffffffff-ffff-ffff-ffff-ffffffffff05', NULL,
   'cccccccc-cccc-cccc-cccc-ccccccccc004', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeee004', '88888888-8888-8888-8888-888888888805',
   4, NOW() - INTERVAL '3 days', 0, NULL, NULL, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001', NOW() - INTERVAL '4 days',
   'Stripe has better API, more payment methods, and lower fees for our volume',
   '{"id": "stripe", "title": "Stripe"}',
   'Selected Stripe. Better documentation, established SDKs, and global payment method support.',
   false, NULL, NULL, '["payments", "infrastructure"]',
   NOW() - INTERVAL '6 days', NOW() - INTERVAL '4 days', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 14. DECISION VOTES
-- ============================================
INSERT INTO decision_votes (id, decision_id, user_id, vote, comment, created_at) VALUES
  ('55555555-5555-5555-5555-555555555501', '77777777-7777-7777-7777-777777777701',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004', 'APPROVE', 'Apollo has better DevTools and community support', NOW() - INTERVAL '2 hours'),
  ('55555555-5555-5555-5555-555555555502', '77777777-7777-7777-7777-777777777701',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005', 'APPROVE', 'Agree with Apollo choice', NOW() - INTERVAL '1 hour'),
  ('55555555-5555-5555-5555-555555555503', '77777777-7777-7777-7777-777777777703',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004', 'APPROVE', 'Modular monolith gives us flexibility without complexity', NOW() - INTERVAL '1 day'),
  ('55555555-5555-5555-5555-555555555504', '77777777-7777-7777-7777-777777777703',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005', 'NEEDS_MORE_INFO', 'What about team scaling? Need more context on team structure plans', NOW() - INTERVAL '18 hours'),
  ('55555555-5555-5555-5555-555555555505', '77777777-7777-7777-7777-777777777705',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003', 'APPROVE', '5 steps seems like the right balance', NOW() - INTERVAL '3 hours')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 15. DECISION COMMENTS
-- ============================================
INSERT INTO decision_comments (id, decision_id, author_id, parent_id, content, option_id, is_edited, created_at, updated_at) VALUES
  ('44444444-4444-4444-4444-444444444401', '77777777-7777-7777-7777-777777777701',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa006', NULL,
   'We should also consider urql as a lightweight alternative', NULL, false,
   NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'),
  ('44444444-4444-4444-4444-444444444402', '77777777-7777-7777-7777-777777777701',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004', '44444444-4444-4444-4444-444444444401',
   'urql is interesting but lacks some features we need like normalized caching', NULL, false,
   NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
  ('44444444-4444-4444-4444-444444444403', '77777777-7777-7777-7777-777777777702',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002', NULL,
   'This is blocking offline work. We need a decision ASAP.', NULL, false,
   NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour'),
  ('44444444-4444-4444-4444-444444444404', '77777777-7777-7777-7777-777777777705',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004', NULL,
   'Industry benchmarks suggest 5 steps is optimal for B2B SaaS', NULL, false,
   NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- VERIFICATION
-- ============================================
DO $$
DECLARE
    tenant_count INT;
    user_count INT;
    team_count INT;
    outcome_count INT;
    hypothesis_count INT;
    decision_count INT;
    queue_count INT;
    vote_count INT;
    kr_count INT;
BEGIN
    SELECT COUNT(*) INTO tenant_count FROM tenants;
    SELECT COUNT(*) INTO user_count FROM users;
    SELECT COUNT(*) INTO team_count FROM teams;
    SELECT COUNT(*) INTO outcome_count FROM outcomes;
    SELECT COUNT(*) INTO hypothesis_count FROM hypotheses;
    SELECT COUNT(*) INTO decision_count FROM decisions;
    SELECT COUNT(*) INTO queue_count FROM decision_queues;
    SELECT COUNT(*) INTO vote_count FROM decision_votes;
    SELECT COUNT(*) INTO kr_count FROM key_results;

    RAISE NOTICE '════════════════════════════════════════════════';
    RAISE NOTICE '  CODEOPS SEED DATA LOADED SUCCESSFULLY';
    RAISE NOTICE '════════════════════════════════════════════════';
    RAISE NOTICE '  Tenants:       %', tenant_count;
    RAISE NOTICE '  Users:         %', user_count;
    RAISE NOTICE '  Teams:         %', team_count;
    RAISE NOTICE '  Queues:        %', queue_count;
    RAISE NOTICE '  Outcomes:      %', outcome_count;
    RAISE NOTICE '  Key Results:   %', kr_count;
    RAISE NOTICE '  Hypotheses:    %', hypothesis_count;
    RAISE NOTICE '  Decisions:     %', decision_count;
    RAISE NOTICE '  Votes:         %', vote_count;
    RAISE NOTICE '════════════════════════════════════════════════';
    RAISE NOTICE '  TEST CREDENTIALS';
    RAISE NOTICE '  alice@acme.com      / password123 (Tenant Owner)';
    RAISE NOTICE '  grace@techstart.io  / password123 (Tenant Owner)';
    RAISE NOTICE '════════════════════════════════════════════════';
END $$;
