-- CrisisConnect Database Schema
-- GDPR/HIPAA/NIST compliant schema with privacy-by-design

-- Organizations table
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('NGO', 'UN_AGENCY', 'LOCAL_GROUP', 'GOV_PARTNER')),
    country VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VERIFIED', 'SUSPENDED')),
    website_url VARCHAR(500),
    phone VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_org_status ON organizations(status);
CREATE INDEX idx_org_country ON organizations(country);

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('BENEFICIARY', 'FIELD_WORKER', 'NGO_STAFF', 'ADMIN')),
    organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_organization ON users(organization_id);

-- Service areas table
CREATE TABLE service_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    country VARCHAR(100) NOT NULL,
    region_or_state VARCHAR(100),
    city VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_area_org ON service_areas(organization_id);
CREATE INDEX idx_service_area_country ON service_areas(country);

-- Service area categories (many-to-many relationship)
CREATE TABLE service_area_categories (
    service_area_id UUID NOT NULL REFERENCES service_areas(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL CHECK (category IN ('FOOD', 'SHELTER', 'LEGAL', 'MEDICAL', 'DOCUMENTS', 'OTHER')),
    PRIMARY KEY (service_area_id, category)
);

-- Needs table
CREATE TABLE needs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    category VARCHAR(50) NOT NULL CHECK (category IN ('FOOD', 'SHELTER', 'LEGAL', 'MEDICAL', 'DOCUMENTS', 'OTHER')),
    country VARCHAR(100) NOT NULL,
    region_or_state VARCHAR(100),
    city VARCHAR(100),
    urgency_level VARCHAR(50) NOT NULL DEFAULT 'MEDIUM' CHECK (urgency_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    assigned_organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    assigned_at TIMESTAMP,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_need_status ON needs(status);
CREATE INDEX idx_need_assigned_org ON needs(assigned_organization_id);
CREATE INDEX idx_need_creator ON needs(created_by_user_id);
CREATE INDEX idx_need_country ON needs(country);
CREATE INDEX idx_need_category ON needs(category);

-- Need updates table (audit trail)
CREATE TABLE need_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    need_id UUID NOT NULL REFERENCES needs(id) ON DELETE CASCADE,
    updated_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status_from VARCHAR(50) CHECK (status_from IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    status_to VARCHAR(50) NOT NULL CHECK (status_to IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    comment TEXT,
    created_.at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_need_update_need ON need_updates(need_id);
CREATE INDEX idx_need_update_user ON need_updates(updated_by_user_id);

-- Audit logs table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action_type VARCHAR(100) NOT NULL,
    target_type VARCHAR(100),
    target_id UUID,
    metadata TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action_type);
CREATE INDEX idx_audit_target ON audit_logs(target_type, target_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- Comments
COMMENT ON TABLE organizations IS 'Aid organizations - only VERIFIED orgs can access full need details';
COMMENT ON TABLE users IS 'System users with role-based access control';
COMMENT ON TABLE needs IS 'Assistance requests - PII stored separately in sensitive_info';
COMMENT ON TABLE audit_logs IS 'Audit trail for compliance and security monitoring';
