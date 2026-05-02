# Privacy

CrisisConnect is a deployable software project, not a hosted service operated by this repository. Privacy obligations depend on how and where an organization deploys it.

## Data The App Can Store

The codebase includes models and workflows that can store:

- User account data: name, email, password hash, role, organization, account status, login/session metadata.
- Organization data: organization name, type, country, status, website, phone, service areas, and verification notes.
- Assistance need data: category, status, country, region/state, city, urgency, assignment, timestamps, and need updates.
- Sensitive need data: beneficiary name, phone, email, exact location, and sensitive notes stored separately in `sensitive_info`.
- Compliance/security data: audit logs, login attempts, password history, password policies, user sessions, user consent records, IP address, and action metadata.

The app is designed to show redacted need data by default and reveal full sensitive details only to authorized users, such as the creator, assigned verified organization staff, or admins.

## Demo / Mock vs Real Deployment

The repository includes demo mode and sample data for local evaluation. Demo users, organizations, needs, locations, and credentials are for testing only.

Do not mix real data with demo mode. Do not use demo credentials in production. Do not publish screenshots, logs, exported data, database dumps, or bug reports that contain real personal data.

## GDPR And Public-Sector Responsibilities

Deploying organizations are responsible for privacy and public-sector compliance, including:

- Identifying the lawful basis for processing personal data.
- Customizing privacy notices, consent text, retention schedules, and data subject request procedures.
- Determining controller, processor, data protection officer, and vendor responsibilities.
- Configuring retention, deletion, anonymization, backups, and audit log handling.
- Reviewing international transfers, hosting location, subprocessors, and cross-border access.
- Testing GDPR-style export, deletion, anonymization, and consent workflows before relying on them.
- Completing DPIAs, safeguarding reviews, accessibility reviews, security reviews, and legal review where needed.
- Ensuring staff are trained not to enter unnecessary, unsafe, or excessive personal data.

For public-sector, education, humanitarian, legal, medical, financial, infrastructure, or high-risk community deployments, obtain appropriate professional review before using the system with real people.
