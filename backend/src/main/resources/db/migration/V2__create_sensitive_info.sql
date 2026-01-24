-- V2: Create sensitive_info table for encrypted PII
CREATE TABLE sensitive_info (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    need_id UUID NOT NULL UNIQUE REFERENCES needs(id) ON DELETE CASCADE,
    encrypted_full_name TEXT,
    encrypted_phone TEXT,
    encrypted_email TEXT,
    encrypted_exact_location TEXT,
    encrypted_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_sensitive_need ON sensitive_info(need_id);

COMMENT ON TABLE sensitive_info IS 'Encrypted PII - access restricted and logged';
