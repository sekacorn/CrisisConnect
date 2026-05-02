# CrisisConnect

## Project Name and Short Mission Statement

CrisisConnect is free and low-cost humanitarian coordination software for public-interest teams.

CrisisConnect helps field workers, nonprofits, universities, researchers, students, civic technologists, and community organizations coordinate assistance requests without buying expensive proprietary case-intake or aid-coordination software. The project is meant to be run, inspected, modified, and adapted by teams doing mission-driven work.

> Quick links: [Quick Start](#quick-start) | [Testing](#testing) | [Compliance and Trust Posture](#compliance-and-trust-posture) | [Security](SECURITY.md) | [Privacy](PRIVACY.md) | [Production Readiness Checklist](docs/PRODUCTION_READINESS_CHECKLIST.md)

## The Problem

Humanitarian and public-interest teams often need case intake, referral tracking, role-based access, audit logs, and privacy controls, but many commercial platforms are priced for large enterprises. Smaller nonprofits, university labs, student groups, and local organizations may end up using spreadsheets, email threads, chat apps, or shared documents for sensitive coordination work.

That creates practical risks:

- Software cost can block small teams from using safer workflows.
- Sensitive beneficiary or participant information can be copied into tools that were not designed for controlled access.
- Researchers and civic technologists may need a system they can inspect, adapt, and evaluate.
- Public-sector or grant-funded teams may need evidence of privacy, security, accessibility, and compliance planning before deployment.
- Field teams need low-friction tools for creating needs, matching them to organizations, and tracking status without exposing unnecessary personal data.

## What This Solves

CrisisConnect provides a working full-stack starter system for coordinating assistance needs.

It includes a React frontend, a Spring Boot API, role-based access control, organization verification, redacted-by-default need listings, encrypted sensitive information storage, audit logging, demo data, Docker deployment files, and automated tests. The core workflow is simple: field workers create assistance needs, verified organizations view and claim needs in their service areas, and admins manage users, organizations, audit logs, and security signals.

This repository does not replace legal, security, humanitarian safeguarding, or accessibility review. It gives teams a practical base they can run and adapt instead of starting from a blank project or paying for a closed system they cannot inspect.

## Who It Is For

- Nonprofits and NGOs coordinating aid, referrals, or public-interest services
- Universities and research groups studying humanitarian technology, privacy, access control, or public-sector workflows
- Researchers who need inspectable software for pilots, prototypes, or controlled studies
- Students learning full-stack, security-aware, public-interest software design
- Public-interest teams and civic technologists building community tools
- Community organizations that need low-cost case intake and coordination software
- Humanitarian field teams evaluating privacy-preserving coordination workflows

## Free / Low-Cost Use

This project is licensed under the [MIT License](LICENSE). The license permits use, copying, modification, merging, publishing, distribution, sublicensing, and selling copies of the software, provided the copyright notice and license text are included.

That means a nonprofit, university, researcher, student, or public-interest team can clone, fork, run, modify, and deploy the project without paying a license fee to this repository. The software is provided as-is, without warranty.

Security vulnerability contact exists in the repository: `sekacorn@gmail.com`. No separate general support, sales, funding, or service-level contact is clearly defined in the repo.

## What Is Included

Current code and documentation include:

- Frontend: React 18, TypeScript, React Router, Axios API client, a public landing page, login, dashboard, needs list, need detail, need creation, and admin views.
- Backend: Java/Spring Boot REST API with controllers for authentication, needs, organizations, admin functions, sessions, MFA, consent, GDPR-style export/deletion, and password policies.
- Data model: users, organizations, service areas, needs, need updates, audit logs, sensitive information, password policy/history, login attempts, user sessions, and user consent.
- Security controls: JWT authentication, BCrypt password hashing, Spring Security, role checks, organization verification, redacted responses, encrypted sensitive fields, audit logging, account lockout/password policy services, session services, and in-memory rate limiting.
- Privacy workflows: separation of sensitive information from operational need records, GDPR-style export/delete/anonymize endpoints, consent tracking services, and data retention documentation.
- Accessibility work: skip link, accessible loading states, ARIA utilities, focus styles, reduced-motion/high-contrast styles, and Cypress accessibility tests.
- Demo and development support: H2 demo mode, sample organizations/users/needs, start/stop scripts for Windows and Unix-like shells.
- Deployment files: Dockerfiles for frontend/backend, `docker-compose.yml`, nginx config, `.env.example`, and deployment documentation. The Compose file includes PostgreSQL, but the current backend Dockerfile defaults to demo/H2 settings unless production Spring datasource/profile settings are supplied.
- Tests: backend JUnit/Mockito/Spring tests, frontend Jest/React Testing Library tests, and Cypress end-to-end/accessibility tests.
- Documentation: deployment, testing, accessibility, compliance implementation notes, NIST analysis, privacy, data retention, threat model, QA, RBAC, safeguarding, and product vision docs.
- Assets: screenshots embedded below.

Planned or partially documented ideas such as SMS/WhatsApp integration, GIS mapping, mobile apps, multilingual support, SSO/SAML, external SIEM integration, and formal certification are not implemented as production features in this repo.

## Recommended Features For Future Versions

Teams evaluating this kind of software often need more than the current starter workflow before using it broadly. The following features would be useful next additions, but they should be treated as future work unless implemented in the code:

- Mobile-first field intake with offline drafts, sync conflict handling, and low-bandwidth behavior.
- Multilingual interface support, including right-to-left language support where needed.
- SMS, WhatsApp, email, or hotline integrations for intake follow-up and status notifications.
- Map and service-area views for matching needs to nearby organizations without exposing exact locations unnecessarily.
- Configurable intake forms so different programs can collect only the fields they need.
- Referral workflows between organizations, including rejection reasons, handoff notes, and escalation paths.
- Safer beneficiary contact controls, including consent prompts, restricted notes, and high-risk case flags.
- Reporting exports for aggregate, non-identifying statistics by category, urgency, geography, and response time.
- Data import/export tools for common nonprofit and research workflows, with redaction controls.
- Stronger production identity options such as SSO/SAML/OIDC and enforced MFA for administrators.
- Centralized monitoring, tamper-resistant audit logs, and security alert integrations.
- Backup, restore, retention, and deletion jobs that are configurable per deployment.
- Full accessibility audit coverage across all workflows and documented remediation status.
- Deployment templates for common low-cost hosts, universities, and small organization infrastructure.
- Optional AI-assisted triage or summarization only with human review, clear audit logs, and legal/privacy assessment.

## Screenshots

The repository includes screenshots from the current demo application. These are demo views and should not contain real beneficiary or operational data.

### Needs List

![Assistance needs list with redacted need cards](Screenshot%20(1).png)

### Main Dashboard

![Field worker dashboard with need actions and security summary](Screenshot%20(9).png)

### Beneficiary Dashboard

![Beneficiary dashboard with limited available actions](Screenshot%20(14).png)

### Admin Statistics

![Admin statistics dashboard with user, organization, and need metrics](Screenshot%20(2).png)

### Admin Statistics, Lower Sections

![Admin statistics dashboard with category, urgency, activity, and security metrics](Screenshot%20(8).png)

### Organization Management

![Admin organization management table with status filter](Screenshot%20(3).png)

### Organization Management, Alternate Capture

![Admin organization management table showing organization status badges](Screenshot%20(4).png)

### User Management

![Admin user management table with roles, status, and edit actions](Screenshot%20(5).png)

### User Management, Alternate Capture

![Admin user management table with active and inactive users](Screenshot%20(6).png)

### Suspicious Activity Monitoring

![Admin suspicious activities table showing no recent suspicious activity](Screenshot%20(7).png)

### Create Assistance Need

![Create assistance need form showing need information and location fields](Screenshot%20(11).png)

### Create Assistance Need, Sensitive Information

![Create assistance need form showing encrypted sensitive information fields](Screenshot%20(12).png)

### Needs List, Alternate Captures

![Assistance needs list with privacy notice and status badges](Screenshot%20(10).png)

![Assistance needs list showing multiple redacted assistance requests](Screenshot%20(15).png)

### Dashboard, Cropped Capture

![Cropped dashboard capture showing view needs and security summary](Screenshot%20(13).png)

## Compliance and Trust Posture

CrisisConnect has compliance-oriented design work, but this repository does not contain proof of certification, authorization, third-party audit, legal approval, or production accreditation. Deployers remain responsible for reviewing the code, configuration, policies, hosting environment, staff procedures, and local legal obligations before using it with real people or sensitive data.

### GDPR

Why it may matter: CrisisConnect can store personal data about users and beneficiaries, including contact details, exact locations, and case notes.

Support in the repo: sensitive beneficiary data is stored separately from operational need records; sensitive fields are encrypted by the backend service; default need listings are redacted; consent, export, soft deletion, hard deletion, and anonymization endpoints exist; data retention and privacy documentation are present.

Deployment responsibility: identify lawful basis, customize privacy notices, appoint responsible data roles where required, configure retention jobs and backups, document processor/controller relationships, handle data subject requests, review international transfers, and validate that encryption and deletion behavior meets the deployer's jurisdiction and risk model.

### European Accessibility Act / EN 301 549

Why it may matter: public-interest tools may be used by staff, volunteers, students, beneficiaries, or public-sector partners with accessibility requirements.

Support in the repo: accessibility utilities, skip navigation, semantic markup work, focus styling, reduced-motion/high-contrast CSS, accessible loading states, and Cypress accessibility tests are included.

Deployment responsibility: complete manual testing with assistive technology, run a full WCAG/EN 301 549 audit, verify all pages and admin workflows, publish an accessibility statement if required, and fix remaining component-level gaps before public use.

### EU AI Act

Why it may matter: a future deployment could add AI triage, risk scoring, prioritization, translation, summarization, or decision support for vulnerable people.

Support in the repo: no AI model, AI service, automated decision engine, or model training workflow was found in the current code. CrisisConnect is currently a rules-and-workflow application.

Deployment responsibility: if AI is added, classify the AI use case, document human oversight, data governance, model risks, bias testing, explainability, logging, and appeals. AI-assisted output should not be treated as a final humanitarian, legal, medical, financial, or safety decision without qualified human review.

### NIS2 Directive

Why it may matter: organizations operating essential or important services in the EU may have cybersecurity governance, incident reporting, and supplier-risk obligations.

Support in the repo: threat modeling, audit logs, role-based access, security documentation, rate limiting, Docker deployment guidance, and incident reporting contact are present.

Deployment responsibility: establish organizational incident response, vulnerability management, supplier review, monitoring, backups, business continuity, access reviews, and statutory reporting processes.

### Cyber Resilience Act

Why it may matter: deployers or distributors of software products in the EU may need secure development, vulnerability handling, update, and documentation practices.

Support in the repo: MIT license, security policy, Docker files, dependency lockfiles, tests, and security/compliance documentation provide a starting point.

Deployment responsibility: maintain dependency scanning, vulnerability disclosure and patch processes, SBOM/release practices where required, secure update procedures, and product risk documentation.

### Section 508 / WCAG 2.1 AA

Why it may matter: US public-sector, education, and federally funded deployments may require accessible electronic services.

Support in the repo: accessibility implementation docs, accessible frontend utilities, keyboard/focus improvements, and Cypress accessibility tests exist.

Deployment responsibility: verify WCAG 2.1 AA conformance page by page, test with screen readers and keyboard-only navigation, document exceptions, and remediate before production or public-sector use.

### NIST SP 800-53

Why it may matter: universities, research institutions, government partners, and public-sector deployers may map controls to NIST security families.

Support in the repo: the code supports access control, audit logging, authentication, encryption, configuration through environment variables, security documentation, and partial control mapping in `NIST_COMPLIANCE_ANALYSIS.md`.

Deployment responsibility: perform a formal control assessment, define system boundaries, write a system security plan, configure monitoring, manage incident response, test backups, harden infrastructure, and close gaps such as centralized logging, key rotation, CI security scanning, and independent security review.

### NIST Cybersecurity Framework

Why it may matter: nonprofit and university teams may use the CSF to organize security work without seeking federal authorization.

Support in the repo: Identify and Protect activities are partially supported by data models, threat model docs, environment configuration, RBAC, encryption, and access controls. Detect has audit logs and suspicious activity views. Respond and Recover are mainly documented responsibilities.

Deployment responsibility: maintain an asset inventory, risk register, monitoring process, incident response plan, recovery plan, backup process, and post-incident review procedure.

### FedRAMP Readiness

Why it may matter: a cloud deployment for US federal use would require substantial security documentation and authorization work.

Support in the repo: there is no FedRAMP authorization package, agency authorization, 3PAO assessment, SSP, SAR, POA&M, or continuous monitoring evidence. Existing NIST-oriented docs may help early planning only.

Deployment responsibility: treat FedRAMP as a separate authorization project involving cloud boundary definition, FedRAMP-authorized services, formal control implementation, evidence collection, scanning, continuous monitoring, and agency/3PAO review.

## Current Status

CrisisConnect is best treated as a working prototype, research tool, and starter kit for humanitarian/public-interest coordination. It has meaningful backend, frontend, security, privacy, accessibility, demo, and test work, but it should not be considered production-ready for sensitive real-world deployment without review.

Important limitations and review points:

- The default application configuration uses H2, disables Flyway, and uses `create-drop`; production deployments need explicit PostgreSQL Spring datasource/profile configuration, enabled migrations, backups, and hardening.
- Demo mode loads mock organizations, users, needs, and beneficiary-like data. Do not use demo data or demo credentials with real cases.
- Encryption is implemented, but the current service derives a 16-byte AES key and uses the configured AES transformation rather than a fully documented production key-management scheme with rotation.
- Rate limiting is in-memory and should be replaced or backed by shared infrastructure such as Redis or a reverse proxy for multi-instance deployments.
- The repo contains compliance analyses and implementation notes, but no certification or authorization evidence.
- Accessibility work is substantial but not proven complete across every screen.
- A public landing page exists at `/`, while authenticated workflows remain behind the login route.
- MFA, consent, session, GDPR, password policy, and suspicious activity services/controllers exist, but deployers should test the full end-to-end behavior before relying on them.
- No AI functionality is currently implemented.

For legal, medical, educational, infrastructure, public-sector, or humanitarian safeguarding use, deployment requires qualified professional review.

## Quick Start

### Prerequisites

- Git
- Docker and Docker Compose for the Docker path
- For manual development: Java 17 or higher, Maven, Node.js 18 or higher, and npm

### Option 1: Demo/Development Scripts

Windows:

```cmd
start-all.bat --h2
```

Unix-like shells:

```bash
./start-all.sh --h2
```

Then open:

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080/api`
- H2 console: `http://localhost:8080/h2-console`

Demo admin credentials from the repo:

- Email: `admin@crisisconnect.org`
- Password: `Admin2026!Secure`

### Option 2: Docker Compose

```bash
cp .env.example .env
docker-compose up -d
```

Then open:

- Frontend: `http://localhost`
- Backend API: `http://localhost:8080/api`

Before using Docker for anything beyond local evaluation, edit `.env`, replace all default passwords and secrets, and review backend datasource/profile settings. The current backend Dockerfile sets demo/H2 defaults, so PostgreSQL production wiring needs explicit configuration.

### Option 3: Manual Development

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm start
```

## Project Structure

```text
backend/                         Spring Boot API, security, services, entities, repositories, migrations, tests
frontend/                        React/TypeScript app, pages, components, API client, accessibility utilities, tests
e2e/                             Cypress end-to-end, performance, workflow, and accessibility tests
spec/                            Product, domain, RBAC, privacy, safeguarding, security, and QA specifications
docs/                            Privacy, data retention, project structure, and production readiness docs
ACCESSIBILITY_*.md               Accessibility implementation notes and summary
COMPLIANCE_*.md                  Compliance implementation notes and status
NIST_COMPLIANCE_ANALYSIS.md      NIST SP 800-53-oriented gap analysis
DEPLOYMENT.md                    Deployment and hardening guidance
TESTING.md                       Testing guide
docker-compose.yml               Local Docker stack with frontend, backend, and PostgreSQL service definitions
.env.example                     Environment variable template
SECURITY.md                      Vulnerability reporting and hardening checklist
PRIVACY.md                       Privacy responsibilities and data handling summary
LICENSE                          MIT License
```

## Testing

Run all available test groups with the repo scripts:

Windows:

```cmd
test.bat
```

Unix-like shells:

```bash
./test.sh
```

Component-level commands:

```bash
cd backend
mvn test
```

```bash
cd frontend
npm test
```

```bash
cd e2e
npm install
npm test
```

The frontend `npm test` command uses Create React App/Jest watch mode by default. For a single non-watch run, use:

```bash
cd frontend
npm test -- --watchAll=false
```

## Documentation

- [Deployment Guide](DEPLOYMENT.md)
- [Testing Guide](TESTING.md)
- [Security Policy](SECURITY.md)
- [Privacy Summary](PRIVACY.md)
- [Production Readiness Checklist](docs/PRODUCTION_READINESS_CHECKLIST.md)
- [Data Retention Policy](docs/DATA_RETENTION.md)
- [Detailed Privacy Doc](docs/PRIVACY.md)
- [Project Structure](docs/PROJECT_STRUCTURE.md)
- [Accessibility Summary](ACCESSIBILITY_SUMMARY.md)
- [Accessibility Implementation](ACCESSIBILITY_IMPLEMENTATION.md)
- [Compliance Implementation Guide](COMPLIANCE_IMPLEMENTATION_GUIDE.md)
- [NIST Compliance Analysis](NIST_COMPLIANCE_ANALYSIS.md)
- [Technical Specs](spec/)

## License

CrisisConnect is released under the [MIT License](LICENSE). You may use, copy, modify, merge, publish, distribute, sublicense, and sell copies of the software, provided the license notice is included. The software is provided as-is, without warranty.
