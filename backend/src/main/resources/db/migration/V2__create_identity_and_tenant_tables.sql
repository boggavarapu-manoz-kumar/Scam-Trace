-- ============================================================================
-- V2__create_identity_and_tenant_tables.sql
-- Digital Risk Protection Platform - Identity & Multi-Tenant Model
-- ============================================================================

-- 1. Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_email ON users (email);

-- 2. Organizations table
CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_organizations_slug ON organizations (slug);

-- 3. Organization Memberships table
CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_org FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_organization UNIQUE (user_id, organization_id)
);

CREATE INDEX idx_membership_user_id ON organization_memberships (user_id);
CREATE INDEX idx_membership_org_id ON organization_memberships (organization_id);

-- 4. Security Audit Logs table
CREATE TABLE security_audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    organization_id UUID,
    event_type VARCHAR(64) NOT NULL,
    ip_address VARCHAR(45),
    details VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_org FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_user_id ON security_audit_logs (user_id);
CREATE INDEX idx_audit_org_id ON security_audit_logs (organization_id);
CREATE INDEX idx_audit_created_at ON security_audit_logs (created_at);
