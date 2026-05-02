# Production Readiness Checklist

CrisisConnect is a working starter system, not a certified production platform. Complete this checklist before using it with real users or sensitive data.

## Secrets And Configuration

- [ ] Replace all default secrets and passwords from `.env.example`.
- [ ] Generate strong `JWT_SECRET`, `ENCRYPTION_SECRET`, database password, and admin password.
- [ ] Store secrets in a secrets manager or protected deployment environment.
- [ ] Disable `ADMIN_BOOTSTRAP_ENABLED` after initial admin setup.
- [ ] Separate development, staging, and production credentials.
- [ ] Confirm secrets are not logged, committed, exported, or exposed to the frontend.

## CORS, TLS, And Network Controls

- [ ] Restrict `CORS_ALLOWED_ORIGINS` to trusted production domains.
- [ ] Serve all traffic over HTTPS/TLS.
- [ ] Redirect HTTP to HTTPS.
- [ ] Put the database on a private network.
- [ ] Restrict direct access to backend and database ports.
- [ ] Add firewall, reverse proxy, or load balancer rules appropriate to the hosting environment.
- [ ] Configure security headers, including HSTS, frame restrictions, content type protection, and a reviewed Content Security Policy.

## Authentication And Access Control

- [ ] Review all roles and permissions against real organizational workflows.
- [ ] Test beneficiary, field worker, NGO staff, and admin access paths.
- [ ] Verify organization verification gates before exposing sensitive details.
- [ ] Test account lockout, password policy, session revocation, and MFA behavior.
- [ ] Define an admin account review and offboarding process.
- [ ] Add SSO or stronger identity controls if required by the deploying organization.

## Data Protection And Privacy

- [ ] Confirm what personal data is required and remove unnecessary fields or workflows.
- [ ] Review encryption mode, key length, key storage, and key rotation.
- [ ] Validate redacted and full need responses with tests.
- [ ] Configure retention, deletion, anonymization, and backup deletion processes.
- [ ] Confirm audit logs do not contain sensitive personal data.
- [ ] Complete a GDPR/public-sector privacy review and DPIA where required.
- [ ] Create deployment-specific privacy notices and consent text.
- [ ] Define data breach notification and data subject request procedures.

## Audit Logs And Monitoring

- [ ] Centralize backend, frontend, reverse proxy, database, and host logs.
- [ ] Protect audit logs from unauthorized modification or deletion.
- [ ] Add alerts for failed logins, suspicious access, high-volume need views, admin changes, and errors.
- [ ] Define log retention and secure deletion rules.
- [ ] Document who reviews audit logs and how often.
- [ ] Test incident response using sample events.

## Backups And Recovery

- [ ] Configure encrypted database backups.
- [ ] Store backups outside the primary host.
- [ ] Test backup restoration.
- [ ] Define recovery time and recovery point objectives.
- [ ] Ensure deleted personal data is handled correctly in backups.
- [ ] Document disaster recovery steps and responsible owners.

## Dependency And Container Scanning

- [ ] Run Java dependency scanning.
- [ ] Run npm dependency scanning for frontend and e2e packages.
- [ ] Scan Docker images.
- [ ] Add secret scanning.
- [ ] Add SAST/DAST or equivalent security testing if required.
- [ ] Define a patch cadence for critical and high vulnerabilities.

## Accessibility Audit

- [ ] Run automated accessibility tests.
- [ ] Test keyboard-only navigation across all pages.
- [ ] Test with screen readers.
- [ ] Verify color contrast, focus indicators, form errors, landmarks, and announcements.
- [ ] Review against WCAG 2.1 AA, Section 508, and EN 301 549 as applicable.
- [ ] Publish an accessibility statement if required.

## AI Review

- [ ] Confirm whether the deployment adds any AI, model, scoring, triage, summarization, or translation feature.
- [ ] If AI is added, document the use case, inputs, outputs, risks, human oversight, appeal path, and logging.
- [ ] Review EU AI Act and local AI governance obligations.
- [ ] Clearly distinguish AI-assisted outputs from final professional decisions.
- [ ] Do not use AI outputs as final legal, medical, financial, humanitarian, safeguarding, or eligibility decisions without qualified human review.

## Legal, Safeguarding, And Organizational Review

- [ ] Complete legal review for the deployment jurisdiction.
- [ ] Complete humanitarian safeguarding and abuse-prevention review.
- [ ] Complete public-sector procurement or compliance review if applicable.
- [ ] Define staff training for privacy, security, and safe data entry.
- [ ] Define operational support, maintenance, and escalation owners.
- [ ] Document accepted risks before launch.

## Release Decision

- [ ] All critical and high security issues are resolved or formally accepted.
- [ ] All required legal/privacy/accessibility reviews are complete.
- [ ] Real user data is separated from demo/test environments.
- [ ] Monitoring, backups, restore, and incident response have been tested.
- [ ] A named owner is responsible for ongoing maintenance.
