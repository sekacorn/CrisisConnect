# Data Retention Policy

**CrisisConnect - Humanitarian Aid Coordination Platform**

## Purpose

This document defines how long different types of data are retained in the CrisisConnect system and the procedures for data deletion and archival.

## Guiding Principles

1. **Data Minimization:** Retain only what is necessary
2. **Purpose Limitation:** Data retained only for its original purpose
3. **Storage Limitation:** Delete data when no longer needed
4. **Transparency:** Clear policies communicated to users

---

## Retention Periods by Data Type

### 1. Need Records

#### Active Needs (PENDING, ASSIGNED, IN_PROGRESS)
**Retention:** Indefinitely until resolved or cancelled

**Rationale:** Active cases require ongoing coordination

**Stored Data:**
- Need metadata (category, urgency, region, status)
- Encrypted beneficiary information
- Assignment details
- Status updates

---

#### Resolved/Closed Needs
**Retention:**

| Data Type | Retention Period | Auto-Deletion |
|-----------|------------------|---------------|
| Metadata (category, region, status) | 2 years | No (for reporting) |
| Encrypted PII (name, phone, email) | 90 days | Yes |
| Exact location details | 90 days | Yes |
| Sensitive notes | 90 days | Yes |
| Assignment history | 2 years | No |
| Status update log | 2 years | No |

**Rationale:**
- Metadata retained for humanitarian reporting and coordination analysis
- PII deleted quickly to minimize exposure risk
- Assignment history retained for accountability and lessons learned

**Automatic Deletion Process:**
- Scheduled job runs daily at 2 AM
- Deletes SensitiveInfo records for needs closed > 90 days ago
- Audit log entry created for each deletion

---

#### Cancelled Needs
**Retention:**

| Data Type | Retention Period | Auto-Deletion |
|-----------|------------------|---------------|
| Metadata | 1 year | No |
| Encrypted PII | Immediate | Yes |
| All other details | Immediate | Yes |

**Rationale:** Cancelled needs have minimal value for reporting

---

### 2. User Accounts

#### Active Users
**Retention:** Indefinitely while account is active

**Stored Data:**
- Email, name, role
- Password hash (BCrypt)
- Organization affiliation
- Account creation date
- Last login timestamp

---

#### Inactive Users (No Login > 1 Year)
**Action:**
- Email notification sent at 11 months of inactivity
- Warning sent at 12 months
- Account disabled at 13 months (can be reactivated)
- Account deleted at 18 months of inactivity

**Exception:** Admin accounts are not auto-disabled

---

#### Deleted/Deactivated Users
**Retention:**

| Data Type | Retention Period | Action |
|-----------|------------------|--------|
| User ID (for audit trail integrity) | Permanent | Retained |
| Email and name | Immediate | Anonymized to "DELETED_USER_{ID}" |
| Password hash | Immediate | Deleted |
| Associated needs (as creator) | Per need retention policy | Anonymized creator reference |
| Audit logs | 7 years | User ID retained, name removed |

**Rationale:** Maintain audit trail integrity while removing identifiable info

---

### 3. Organizations

#### Active Organizations
**Retention:** Indefinitely while active

---

#### Suspended Organizations
**Retention:**
- Organization record: 5 years (for compliance)
- Associated service areas: Deleted immediately
- User accounts: Disabled, deleted after 1 year

---

#### Deleted Organizations
**Retention:**
- Organization name and ID: 5 years (anonymized after 1 year)
- Associated needs: Retained per need retention policy
- Associated users: Deleted per user retention policy

---

### 4. Audit Logs

#### Security Audit Logs
**Retention:** 7 years (compliance requirement)

**Includes:**
- Login attempts (success/failure)
- Need access (full vs redacted)
- Organization verification changes
- User role changes
- Sensitive info access
- Suspicious activity flags
- Rate limit violations

**Rationale:** Legal and compliance requirements often mandate 7-year retention for security logs

---

#### Operational Logs (Application Logs)
**Retention:** 90 days

**Includes:**
- Application errors
- Performance metrics
- System events

**Rationale:** Sufficient for troubleshooting and monitoring

**Important:** Application logs must NEVER contain PII

---

### 5. Need Updates (Status Change Log)
**Retention:** Same as parent need

| Need Status | Retention Period |
|-------------|------------------|
| Active | Indefinitely |
| Closed | 2 years |
| Cancelled | 1 year |

**Rationale:** Status history is important for accountability and workflow analysis

---

### 6. Sensitive Information (Encrypted PII)

**Retention:**
- Active needs: Until need is closed
- Closed needs: 90 days after closure
- Cancelled needs: Deleted immediately

**Deletion Process:**
1. Scheduled job runs daily
2. Identifies SensitiveInfo records for eligible deletion
3. Creates audit log entry: "SENSITIVE_INFO_DELETED"
4. Permanently deletes record from database

**No Backup Retention:** Deleted sensitive info is also removed from backups within 30 days

---

### 7. Database Backups

**Retention:**
- Daily backups: 30 days
- Weekly backups: 3 months
- Monthly backups: 1 year

**Encryption:** All backups encrypted at rest

**Deletion:** Backups older than retention period are securely deleted

**PII in Backups:** PII deleted from production database is removed from backups within 30 days

---

## Data Deletion Procedures

### Automated Deletion

**Daily Scheduled Jobs (2 AM server time):**
1. Delete expired SensitiveInfo records (needs closed > 90 days)
2. Clean up rate limit entries (expired windows)
3. Purge old application logs (> 90 days)

**Weekly Scheduled Jobs:**
1. Warn inactive users (> 11 months no login)
2. Disable inactive users (> 13 months)

**Monthly Scheduled Jobs:**
1. Delete old closed need metadata (> 2 years)
2. Delete old cancelled needs (> 1 year)
3. Archive old audit logs (move to cold storage if > 2 years)

---

### Manual Deletion (User Request)

#### Beneficiary Data Deletion Request
**Process:**
1. User submits request via field worker or email
2. Admin verifies identity
3. If need is active: case must be closed first
4. Admin initiates deletion:
   - Sensitive info deleted immediately
   - Need metadata anonymized or deleted
   - Audit log entry created
5. User notified within 30 days

**Exceptions:**
- Cannot delete if legal hold applies
- Audit logs retained per compliance requirements (IDs only, no PII)

---

#### User Account Deletion Request
**Process:**
1. User submits request
2. Admin verifies identity
3. Account marked for deletion
4. 30-day grace period (can cancel deletion)
5. After 30 days:
   - Personal info anonymized
   - Password hash deleted
   - Associated needs anonymized
   - Audit logs de-identified
6. User ID retained for referential integrity

---

### Secure Deletion Methods

**Database Records:**
- Permanent DELETE from database (no soft delete for PII)
- Foreign key references updated to NULL or "DELETED"
- Database vacuum/optimize run to reclaim space

**Backups:**
- Old backups securely overwritten before disposal
- Backup media destroyed per NIST guidelines if retiring hardware

**Logs:**
- Log files securely deleted (not just rm, use shred or equivalent)
- Centralized logging systems purge old entries

---

## Data Archival

### When to Archive (Not Delete)

**Aggregate Statistics (No PII):**
- Retain indefinitely for humanitarian coordination analysis
- Examples:
  - Monthly need counts by category/region
  - Average time to resolution
  - Organization performance metrics

**Audit Logs (IDs Only, No PII):**
- Archive to cold storage after 2 years
- Retain for full 7 years
- Access requires admin approval

**Closed Need Metadata (No PII):**
- Retain for 2 years for reporting
- Archived to cold storage if needed

---

## Compliance & Legal Holds

### Legal Holds
If data is subject to legal proceedings:
- Data deletion is suspended for affected records
- Legal hold flag applied to prevent auto-deletion
- Data retained until legal hold is lifted
- Users notified if their data deletion request is delayed

### Regulatory Compliance
- GDPR: Right to erasure honored within 30 days (unless legal exception)
- CCPA: Data deletion requests processed within 45 days
- Humanitarian sector standards: Balance privacy with accountability

---

## Exceptions & Special Cases

### High-Risk Cases
- Cases flagged as high-risk (domestic violence, trafficking, persecution) may have extended PII retention if required for beneficiary safety
- Requires explicit justification and admin approval
- Reviewed quarterly

### Ongoing Assistance
- If beneficiary has multiple needs over time, PII may be retained across cases to avoid duplication
- Requires beneficiary consent

---

## Monitoring & Auditing

### Quarterly Reviews
Admin team reviews:
- Data deletion job logs
- Storage growth trends
- Compliance with retention policies
- User deletion requests (resolution time)

### Annual Audit
- External audit of data retention compliance
- Review of retention policies vs. actual practice
- Update policies based on legal changes

---

## User Notifications

### Proactive Notifications
- Users notified when their PII will be deleted (e.g., need closed, 90 days approaching)
- Inactive users warned before account deletion
- Organization contacts notified before org deletion

### Notification Channels
- Email (primary)
- In-app notification (if applicable)

---

## Responsibilities

| Role | Responsibility |
|------|---------------|
| **Admin** | Review and approve manual deletion requests; monitor automated jobs |
| **System Administrators** | Configure and maintain scheduled deletion jobs; ensure backups follow policy |
| **Data Protection Officer (if designated)** | Oversee compliance; handle escalations |
| **Legal Counsel** | Review retention periods; advise on legal holds |

---

## Policy Review & Updates

- **Review Frequency:** Annually or when legal requirements change
- **Approval Authority:** Admin team + legal counsel (if applicable)
- **Change Log:**

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | January 2026 | Initial policy |

---

## Contact

**Data Retention Questions:**
Email: sekacorn@gmail.com

**Deletion Requests:**
Email: sekacorn@gmail.com

**Legal Holds:**
Email: sekacorn@gmail.com (or designated contact)

---

**Data retention is a safeguarding measure. We keep what we need, delete what we don't.**
