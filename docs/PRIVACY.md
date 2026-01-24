# Privacy Policy & Data Handling

**CrisisConnect - Humanitarian Aid Coordination Platform**

## Our Commitment to Privacy

CrisisConnect is built on the principle of **privacy by design**. We protect vulnerable populations by minimizing data collection, encrypting sensitive information, and implementing strict access controls.

## What Information We Collect

### Non-Sensitive Information (stored in plain text)
- Need category (FOOD, SHELTER, MEDICAL, etc.)
- Urgency level
- Country and region (generalized)
- Status (PENDING, ASSIGNED, etc.)
- Creation and update timestamps

### Sensitive Information (encrypted at rest)
- Beneficiary full name
- Phone number
- Email address
- Exact location/address
- Sensitive case notes

### User Information
- Email (for login)
- Password (hashed with BCrypt)
- Name
- Role (BENEFICIARY, FIELD_WORKER, NGO_STAFF, ADMIN)
- Organization affiliation (if applicable)

### Automatically Collected
- IP address (for security audit logs only)
- Login timestamps
- User agent (browser/device information)

## How We Protect Your Information

### Encryption
- **At Rest:** Sensitive beneficiary information is encrypted using AES-256-GCM encryption before storage
- **In Transit:** All communications use HTTPS/TLS encryption
- **Passwords:** Hashed using BCrypt (never stored in plaintext)

### Access Controls
- **Role-Based Access Control (RBAC):** Four user roles with escalating privileges
- **Organization Verification:** Only verified organizations can access full beneficiary details
- **Need-to-Know Principle:** Users only see information necessary for their role

### Privacy Filtering
- **Redacted by Default:** All need listings show minimal information
- **Full Details Only for Authorized Users:**
  - Need creator (field worker)
  - Verified NGO staff assigned to the case
  - System administrators

### What We Never Store
- Government ID numbers
- Unnecessary demographic data (race, ethnicity, religion)
- Financial information
- Medical diagnoses (unless relevant to assistance category)

## Who Can Access Your Information

### BENEFICIARY
- Can view only their own case (highly redacted)
- Cannot see other cases

### FIELD_WORKER
- Can create needs on behalf of beneficiaries
- Can view full details of needs they created
- Can view redacted list of needs in their service area

### NGO_STAFF (Unverified Organization)
- Can view redacted needs only
- Cannot claim needs or access beneficiary details

### NGO_STAFF (Verified Organization)
- Can view redacted needs in their service areas
- Can claim needs in their service areas
- Can view full beneficiary details for assigned cases only
- Can update status of assigned needs

### ADMIN
- Full access to all needs (for coordination and oversight)
- Can verify/suspend organizations
- Can manage users
- All access is audit-logged

## How We Use Your Information

### Humanitarian Coordination
- Matching needs with appropriate organizations
- Tracking assistance delivery
- Reporting on humanitarian response (aggregated, no PII)

### Security & Compliance
- Audit logging for accountability
- Fraud detection and prevention
- Insider threat monitoring

### We Do NOT:
- Sell or share beneficiary information with third parties
- Use information for marketing purposes
- Share PII in reports or analytics
- Store information longer than necessary

## Data Retention

### Active Needs
- Retained until assistance is delivered and case is closed

### Closed Needs
- **Metadata:** Retained for 2 years for reporting purposes
- **Sensitive PII:** Automatically deleted 90 days after closure (configurable)
- **Audit Logs:** Retained for 7 years for compliance

### Deleted Users
- User accounts can be anonymized on request
- Anonymization replaces name/email with "DELETED_USER"
- Audit trail retained but identifiable info removed

## Your Rights

### Right to Access
- View your own case information
- Request a copy of your data

### Right to Rectification
- Request corrections to inaccurate information

### Right to Deletion
- Request deletion of your information (subject to legal obligations)
- Automatic deletion of sensitive info 90 days after case closure

### Right to Object
- Object to processing of your information
- Withdraw consent (where applicable)

### How to Exercise Your Rights
Contact your field worker or email: privacy@crisisconnect.org

## Safeguarding

### Protection from Harm
- We implement controls to prevent targeting or exploitation of beneficiaries
- Organization verification prevents malicious actors from accessing PII
- Suspicious activity is monitored and flagged for review
- High-risk cases receive extra protection

### If You Feel Unsafe
- Contact your field worker immediately
- Email sekacorn@gmail.com for urgent concerns
- Request restriction of your case information

## Data Sharing

### With Verified Organizations
- We share beneficiary details only with verified NGOs/UN agencies assigned to your case
- All access is audit-logged
- Organizations are vetted before verification

### Legal Requirements
- We may disclose information if required by law
- We will notify you unless prohibited by law

### We Do Not Share With:
- Marketing companies
- Data brokers
- Unverified organizations
- Public databases or websites

## Cookies & Tracking

### Authentication
- We use secure cookies to maintain your login session
- Cookies are HttpOnly and Secure (cannot be accessed by JavaScript)
- Session expires after 24 hours

### We Do Not Use:
- Tracking pixels
- Third-party analytics (Google Analytics, etc.)
- Advertising cookies
- Social media tracking

## Security Measures

### Technical Controls
- AES-256 encryption for sensitive data
- TLS/HTTPS for all communications
- BCrypt password hashing
- Rate limiting to prevent abuse
- Comprehensive audit logging

### Organizational Controls
- Organization verification process
- Access reviews
- Security training for staff
- Incident response procedures

### If There Is a Breach
- We will notify affected individuals within 72 hours
- We will notify relevant authorities as required
- We will provide guidance on protective actions

## Children's Privacy

- CrisisConnect does not knowingly collect information from children under 13
- Assistance needs involving children are created by authorized field workers on their behalf
- Children's information receives the same protections as adults

## International Data Transfers

- CrisisConnect is designed to be deployed locally in the country/region of operation
- If data is transferred internationally, it will be protected by appropriate safeguards
- Consult your local deployment's specific privacy notice

## Changes to This Policy

- We may update this privacy policy periodically
- Significant changes will be communicated to users
- Continued use constitutes acceptance of changes

## Contact Us

**Privacy Questions:**
Email: sekacorn@gmail.com

**Security Concerns:**
Email: sekacorn@gmail.com

**Data Protection Officer:**
[To be designated by deploying organization]

## Compliance

CrisisConnect is designed to comply with:
- **GDPR** (General Data Protection Regulation) - European Union
- **CCPA/CPRA** (California Consumer Privacy Act) - California, USA
- **HIPAA-inspired** data protection practices
- **NIST SP 800-53** cybersecurity controls

**Note:** This is a template privacy policy. Deploying organizations should review and customize based on their jurisdiction's legal requirements and consult legal counsel.

---

**Last Updated:** January 2026
**Version:** 1.0

---

**CrisisConnect protects those in crisis. Your privacy is our priority.**
