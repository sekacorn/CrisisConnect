# Security

CrisisConnect handles workflows that may involve sensitive personal information. Treat every deployment as security-sensitive, even when using demo data.

## Reporting Vulnerabilities

Do not open a public issue for security vulnerabilities.

Report vulnerabilities to: `sekacorn@gmail.com`

Include a clear description, steps to reproduce, likely impact, and any suggested fix. Please do not include real beneficiary, participant, patient, student, client, or survivor data in reports.

## Data Not To Use In Demos

Do not use real or identifiable data in demos, screenshots, tests, bug reports, seed data, or shared development environments.

Avoid:

- Real names, phone numbers, emails, addresses, government IDs, case IDs, or exact locations
- Medical, legal, financial, immigration, education, safety, or safeguarding details about real people
- Data about minors, survivors of violence, refugees, displaced people, or other vulnerable groups
- Production JWTs, passwords, API keys, database dumps, logs, or encryption secrets

Use the repository's mock/demo data or clearly fictional data instead.

## Production Hardening Checklist

Before using CrisisConnect with real users or sensitive data:

- Replace all default passwords, JWT secrets, encryption secrets, and database credentials.
- Disable admin bootstrap after creating the required admin account.
- Restrict CORS to approved production domains.
- Put the app behind HTTPS/TLS and redirect HTTP to HTTPS.
- Keep the database on a private network and enable encrypted database connections where available.
- Configure encrypted, tested backups and a restore process.
- Replace or supplement in-memory rate limits for multi-instance deployments.
- Add centralized logs, monitoring, alerting, and incident response procedures.
- Review audit logs for PII leakage and protect audit log integrity.
- Run dependency, container, and secret scans.
- Perform role-based access, privacy, accessibility, and abuse-prevention testing.
- Review encryption mode, key length, key storage, and key rotation before production.
- Complete legal, privacy, accessibility, safeguarding, and security review for the deployment context.

See [docs/PRODUCTION_READINESS_CHECKLIST.md](docs/PRODUCTION_READINESS_CHECKLIST.md) for a fuller checklist.
