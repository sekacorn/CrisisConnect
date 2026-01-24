# Domain Model & Data Architecture

**Owner:** Domain Model & Data Architect Agent

## Entity Relationship Overview

```
User (role: BENEFICIARY | FIELD_WORKER | NGO_STAFF | ADMIN)
  |
  +-- belongsTo --> Organization (if NGO_STAFF)
  |
  +-- creates --> Need

Organization (status: PENDING | VERIFIED | SUSPENDED)
  |
  +-- has --> ServiceArea[] (regions/categories served)
  |
  +-- assigned --> Need[] (claimed needs)

Need (metadata: public-ish)
  |
  +-- has one --> SensitiveInfo (encrypted PII)
  |
  +-- tracked by --> NeedUpdate[] (status history)
  |
  +-- logged by --> AuditLog[] (access/changes)
```

## Core Tables

### users
**Purpose:** System users with role-based permissions

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | Primary key |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Login identifier |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt hashed |
| name | VARCHAR(255) | NOT NULL | User's full name |
| role | ENUM | NOT NULL | BENEFICIARY, FIELD_WORKER, NGO_STAFF, ADMIN |
| organization_id | UUID | FK, NULL | Required for NGO_STAFF |
| is_active | BOOLEAN | DEFAULT TRUE | Account status |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE (email)
- INDEX (organization_id)
- INDEX (role, is_active)

### organizations
**Purpose:** NGOs and UN agencies providing assistance

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| name | VARCHAR(255) | NOT NULL | Organization name |
| type | ENUM | NOT NULL | NGO, UN_AGENCY, GOVERNMENT, OTHER |
| status | ENUM | NOT NULL | PENDING, VERIFIED, SUSPENDED |
| contact_email | VARCHAR(255) | NOT NULL | |
| contact_phone | VARCHAR(50) | NULL | |
| verified_at | TIMESTAMP | NULL | When verification completed |
| verified_by | UUID | FK (users.id), NULL | Admin who verified |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

**Indexes:**
- PRIMARY KEY (id)
- INDEX (status)
- INDEX (verified_at)

### service_areas
**Purpose:** Geographic and categorical service coverage

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| organization_id | UUID | FK, NOT NULL | |
| country | VARCHAR(100) | NOT NULL | ISO country code preferred |
| region | VARCHAR(255) | NOT NULL | State/province/district |
| category | ENUM | NOT NULL | FOOD, SHELTER, MEDICAL, etc. |
| is_active | BOOLEAN | DEFAULT TRUE | |
| created_at | TIMESTAMP | NOT NULL | |

**Indexes:**
- PRIMARY KEY (id)
- INDEX (organization_id)
- INDEX (country, region, category)

### needs
**Purpose:** Assistance requests (non-PII metadata)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| created_by | UUID | FK (users.id), NOT NULL | Field worker or NGO staff |
| assigned_to_org | UUID | FK (organizations.id), NULL | Claimed by org |
| category | ENUM | NOT NULL | FOOD, SHELTER, MEDICAL, etc. |
| status | ENUM | NOT NULL | PENDING, ASSIGNED, IN_PROGRESS, DELIVERED, CLOSED, CANCELLED |
| urgency | ENUM | NOT NULL | CRITICAL, HIGH, MEDIUM, LOW |
| country | VARCHAR(100) | NOT NULL | |
| region | VARCHAR(255) | NOT NULL | Generalized location |
| description | TEXT | NOT NULL | Public-facing summary |
| has_children | BOOLEAN | DEFAULT FALSE | Vulnerability flag (redacted) |
| has_elderly | BOOLEAN | DEFAULT FALSE | |
| has_disability | BOOLEAN | DEFAULT FALSE | |
| is_female_headed | BOOLEAN | DEFAULT FALSE | |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |
| claimed_at | TIMESTAMP | NULL | When assigned |
| closed_at | TIMESTAMP | NULL | When finalized |

**Indexes:**
- PRIMARY KEY (id)
- INDEX (region, category, urgency, status) -- for filtering
- INDEX (created_by)
- INDEX (assigned_to_org)
- INDEX (status, created_at DESC) -- for dashboard

### sensitive_info
**Purpose:** Encrypted PII stored separately

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| need_id | UUID | FK (needs.id), UNIQUE, NOT NULL | One-to-one |
| encrypted_full_name | TEXT | NOT NULL | AES-256 encrypted |
| encrypted_phone | TEXT | NULL | |
| encrypted_email | TEXT | NULL | |
| encrypted_exact_location | TEXT | NULL | Address if needed |
| encrypted_notes | TEXT | NULL | Sensitive case details |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE (need_id)

**Security:**
- Data encrypted at application layer before storage
- Decryption only in service layer for authorized users
- Never sent to client-side cache

### need_updates
**Purpose:** Audit trail for status changes

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| need_id | UUID | FK, NOT NULL | |
| updated_by | UUID | FK (users.id), NOT NULL | |
| old_status | ENUM | NULL | |
| new_status | ENUM | NOT NULL | |
| comment | TEXT | NULL | Optional update note |
| created_at | TIMESTAMP | NOT NULL | |

**Indexes:**
- PRIMARY KEY (id)
- INDEX (need_id, created_at DESC)

### audit_logs
**Purpose:** Comprehensive action logging for compliance

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| user_id | UUID | FK (users.id), NULL | NULL for system actions |
| action | VARCHAR(100) | NOT NULL | LOGIN, NEED_CREATED, NEED_ACCESSED, etc. |
| entity_type | VARCHAR(50) | NULL | NEED, ORGANIZATION, USER |
| entity_id | UUID | NULL | Reference to affected entity |
| outcome | VARCHAR(50) | NOT NULL | SUCCESS, FAILURE, DENIED |
| ip_address | VARCHAR(45) | NULL | IPv4/IPv6 |
| user_agent | VARCHAR(255) | NULL | Browser/client info |
| details | JSONB | NULL | Additional context (no PII) |
| created_at | TIMESTAMP | NOT NULL | |

**Indexes:**
- PRIMARY KEY (id)
- INDEX (user_id, created_at DESC)
- INDEX (action, created_at DESC)
- INDEX (entity_type, entity_id, created_at DESC)

## Data Minimization Strategy

### What We DO NOT Store
- Unnecessary demographic data (race, ethnicity, religion)
- Government ID numbers (unless absolutely required)
- Detailed household composition
- Financial information
- Passwords in plaintext (BCrypt only)

### What We Redact in Logs
- No PII in application logs
- No sensitive info in error messages
- Only IDs and action types in audit logs

## Encryption Strategy

### Encrypted Fields (SensitiveInfo table)
- Full name
- Phone number
- Email address
- Exact location/address
- Sensitive case notes

### Encryption Method
- Algorithm: AES-256-GCM
- Key: 32-byte secret from environment variable
- IV: Random per encryption, prepended to ciphertext
- Encoding: Base64 for storage
- Key rotation: Manual process (documented in ops guide)

### Decryption Boundaries
- ONLY in service layer
- ONLY for authorized users (creator, assigned NGO staff, admin)
- Audit log entry for every decryption
- Never cached client-side

## Migration Strategy (Flyway)

### Migration Naming Convention
```
V{version}__{description}.sql
V001__create_users_table.sql
V002__create_organizations_table.sql
V003__create_service_areas_table.sql
V004__create_needs_table.sql
V005__create_sensitive_info_table.sql
V006__create_need_updates_table.sql
V007__create_audit_logs_table.sql
V008__create_indexes.sql
```

### Migration Best Practices
- Each migration is idempotent where possible
- Rollback strategy documented
- Test migrations on staging before production
- Baseline existing production if mid-project

## Performance Considerations

### Query Optimization
- Index on common filter combinations (region + category + urgency + status)
- Limit list queries with pagination (default 50 records)
- Avoid N+1 queries with JPA fetch strategies

### Partitioning (Future)
- Consider partitioning audit_logs by date if volume is high
- Archive closed needs older than 1 year

## Data Retention Policy

### Active Needs
- Retained indefinitely until closed or cancelled

### Closed Needs
- Metadata: Retained for 2 years for reporting
- Sensitive PII: Deleted 90 days after closure (configurable)
- Audit logs: Retained for 7 years (compliance requirement)

### Deleted Users
- Anonymize user records (replace name/email with "DELETED_USER_{id}")
- Retain audit trail but unlink identifiable info

## Backup Strategy

### Database Backups
- Daily automated backups
- Encrypted at rest
- Retention: 30 days
- Test restoration quarterly

### Disaster Recovery
- RTO (Recovery Time Objective): 4 hours
- RPO (Recovery Point Objective): 24 hours
