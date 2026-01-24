# Product Vision - CrisisConnect

**Owner:** Humanitarian Product & Field Operations Agent

## Mission Statement

CrisisConnect connects field workers with verified NGOs and UN agencies to coordinate humanitarian assistance while protecting vulnerable populations through privacy-by-design architecture and strong security controls.

## Core User Journeys

### 1. Field Worker Intake Flow
1. Field worker receives request from beneficiary (in-person, phone, or referral)
2. Worker logs into CrisisConnect on mobile/web
3. Worker creates need record with minimal required fields:
   - Category (FOOD, SHELTER, MEDICAL, PROTECTION, etc.)
   - Urgency level (CRITICAL, HIGH, MEDIUM, LOW)
   - Region/location (generalized by default)
   - Brief description
   - Encrypted beneficiary contact info (stored separately)
4. System assigns unique need ID and saves with redacted metadata
5. Worker receives confirmation and can track status

### 2. NGO Need Discovery & Claim Flow
1. NGO staff logs in (must be from VERIFIED organization)
2. Views list of needs in their service areas (REDACTED view)
   - See: category, region, urgency, status, creation date
   - Cannot see: beneficiary details, exact location, full description
3. NGO staff filters by category/region/urgency
4. Clicks to claim a need
5. Upon claiming, system reveals FULL details (with audit log)
6. NGO coordinates delivery with beneficiary
7. NGO updates status: IN_PROGRESS → DELIVERED → CLOSED

### 3. Admin Organization Verification Flow
1. New organization registers (self-service or admin-created)
2. Organization status = PENDING
3. Admin reviews organization documents/credentials
4. Admin verifies or rejects organization
5. Only VERIFIED organizations can claim needs and see full details

## Key Definitions

### Urgency Levels
- **CRITICAL**: Immediate life-threatening (24-48 hours)
- **HIGH**: Urgent need (3-7 days)
- **MEDIUM**: Important but not urgent (1-2 weeks)
- **LOW**: Non-urgent assistance

### Need Categories
- FOOD (food assistance, nutrition)
- SHELTER (housing, temporary shelter)
- MEDICAL (healthcare, medicine)
- PROTECTION (safety, legal, case management)
- EDUCATION (school supplies, enrollment)
- WASH (water, sanitation, hygiene)
- NFI (non-food items, clothing)
- CASH (cash assistance, vouchers)
- LIVELIHOOD (job training, income generation)
- OTHER (miscellaneous assistance)

### Minimum Required Fields
To prevent incomplete or unusable records:
- Category (required)
- Urgency (required)
- Country + Region (required)
- Description (minimum 20 characters)
- Beneficiary name (encrypted)
- Contact method (encrypted phone OR email)

## Field Constraints & Requirements

### Offline/Low-Bandwidth Support
- Forms should work with intermittent connectivity
- Progressive enhancement for low-bandwidth areas
- Local form validation before submission
- Clear feedback when connection is lost
- Ability to save draft locally (future enhancement)

### Mobile-First Design
- Optimized for smartphones and tablets
- Touch-friendly interface
- Minimal data transfer
- Fast load times on 2G/3G networks

### Safe Defaults
- All views are REDACTED by default
- PII never sent to client unless explicitly authorized
- No caching of sensitive data in browser
- Clear visual indicators for restricted information
- Automatic session timeout for security

### Multi-Language Support (Future)
- English (v1)
- French, Arabic, Spanish (future)
- Right-to-left (RTL) support for Arabic

## Privacy & Safety Principles

### Data Minimization
- Collect only what is necessary for coordination
- No unnecessary demographic data
- Generalized location unless specific address needed

### Consent & Transparency
- Clear language about data usage
- Beneficiaries informed about information sharing
- Field workers trained on consent protocols

### Protection from Harm
- System prevents accidental exposure of vulnerable individuals
- Special handling for high-risk cases (domestic violence, trafficking)
- No public-facing search or browsing of beneficiaries
- Organization verification prevents malicious actors

## Success Metrics

### Efficiency
- Time from need creation to assignment: < 24 hours
- Need closure rate: > 80% within 30 days

### Coverage
- Active verified organizations: 20+ in first 6 months
- Geographic coverage: multiple countries/regions

### Security & Privacy
- Zero PII leakage incidents
- 100% audit coverage for sensitive actions
- Organization verification completion: < 72 hours

### User Satisfaction
- Field worker ease of use: positive feedback
- NGO staff find system valuable vs email/spreadsheets
- Admin can manage system without technical expertise

## Out of Scope (v1)

- Direct beneficiary registration (security risk)
- Public-facing need browsing (privacy risk)
- Payment processing (focus on coordination)
- Complex case management workflows
- Integration with external GIS/mapping systems (future)
- SMS/WhatsApp notifications (future)
