# RBAC & Authorization Model

**Owner:** RBAC & Authorization Agent

## Role Hierarchy

```
BENEFICIARY (lowest privileges)
  |
  v
FIELD_WORKER (can create needs on behalf of beneficiaries)
  |
  v
NGO_STAFF (can view and claim needs in service areas, must be VERIFIED org)
  |
  v
ADMIN (full system access, org verification, user management)
```

## Role Definitions

### BENEFICIARY
**Purpose:** Person receiving assistance (minimal system access)

**Permissions:**
- View ONLY their own case (highly redacted)
- Cannot create needs (must work through field worker)
- Cannot browse other cases

**Use Cases:**
- Check status of their assistance request
- View updates on their case

**Data Access:**
- Own need: See ID, category, status, creation date only
- Other needs: No access

---

### FIELD_WORKER
**Purpose:** Front-line workers creating needs on behalf of beneficiaries

**Permissions:**
- CREATE needs with encrypted beneficiary information
- VIEW needs they created (full details)
- VIEW redacted list of needs in their assigned service areas
- CANNOT claim or reassign needs
- CANNOT view needs created by other field workers (unless same org)

**Data Access:**
- Own created needs: Full details + decrypted PII
- Needs in service area: Redacted only
- Other needs: No access

**Organization Requirement:** Optional (can be independent)

---

### NGO_STAFF
**Purpose:** Organization representatives who claim and deliver assistance

**Permissions (VERIFIED org only):**
- VIEW redacted needs in their organization's service areas
- CLAIM unassigned needs in service areas
- VIEW full details of needs assigned to their organization
- UPDATE status of assigned needs
- CREATE needs (optional workflow)

**Permissions (UNVERIFIED org):**
- VIEW redacted needs only
- CANNOT claim or update any needs
- CANNOT access full details

**Data Access:**
- Needs assigned to their org: Full details + decrypted PII
- Needs in service areas (unassigned): Redacted only
- Other needs: No access

**Organization Requirement:** REQUIRED + VERIFIED status

---

### ADMIN
**Purpose:** System administrators and humanitarian coordinators

**Permissions:**
- FULL access to all needs (redacted and full views)
- VERIFY or SUSPEND organizations
- CREATE/UPDATE/DELETE users
- ASSIGN/REASSIGN needs to any organization
- VIEW all audit logs
- ACCESS system configuration and reports

**Data Access:**
- All needs: Full details + decrypted PII
- All organizations and users
- Complete audit trail
- System-wide analytics

**Organization Requirement:** Optional

---

## Permission Matrix by Endpoint

### Authentication Endpoints

| Endpoint | BENEFICIARY | FIELD_WORKER | NGO_STAFF | ADMIN |
|----------|-------------|--------------|-----------|-------|
| POST /api/auth/login |  |  |  |  |
| GET /api/auth/me |  |  |  |  |
| POST /api/auth/logout |  |  |  |  |

### Need Endpoints

| Endpoint | BENEFICIARY | FIELD_WORKER | NGO_STAFF (Unverified) | NGO_STAFF (Verified) | ADMIN |
|----------|-------------|--------------|------------------------|----------------------|-------|
| POST /api/needs | ✗ |  | ✗ |  |  |
| GET /api/needs | Own only (redacted) | Created by self (full) + service area (redacted) | Service area (redacted) | Service area (redacted) + assigned (full) | All (full) |
| GET /api/needs/{id} | Own (redacted) | If creator (full), else ✗ | Redacted | If assigned (full), else redacted | Full |
| PATCH /api/needs/{id} | ✗ | ✗ | ✗ | If assigned  |  |
| POST /api/needs/{id}/claim | ✗ | ✗ | ✗ |  (service area only) |  |
| DELETE /api/needs/{id} | ✗ | ✗ | ✗ | ✗ |  (soft delete) |

### Organization Endpoints

| Endpoint | BENEFICIARY | FIELD_WORKER | NGO_STAFF | ADMIN |
|----------|-------------|--------------|-----------|-------|
| GET /api/organizations | ✗ | ✗ | Own org only | All |
| GET /api/organizations/{id} | ✗ | ✗ | If own org  |  |
| POST /api/organizations | ✗ | ✗ | ✗ |  |
| PATCH /api/organizations/{id}/verify | ✗ | ✗ | ✗ |  |
| PATCH /api/organizations/{id}/suspend | ✗ | ✗ | ✗ |  |

### User Endpoints

| Endpoint | BENEFICIARY | FIELD_WORKER | NGO_STAFF | ADMIN |
|----------|-------------|--------------|-----------|-------|
| GET /api/users | ✗ | ✗ | Own org users | All |
| POST /api/users | ✗ | ✗ | ✗ |  |
| PATCH /api/users/{id} | Own only | Own only | Own only |  |
| DELETE /api/users/{id} | ✗ | ✗ | ✗ |  |

### Reporting Endpoints

| Endpoint | BENEFICIARY | FIELD_WORKER | NGO_STAFF | ADMIN |
|----------|-------------|--------------|-----------|-------|
| GET /api/reports/summary | ✗ | Limited (own region) | Limited (service areas) | Full |
| GET /api/reports/export | ✗ | ✗ | Own org data | All (redacted) |

### Audit Endpoints

| Endpoint | BENEFICIARY | FIELD_WORKER | NGO_STAFF | ADMIN |
|----------|-------------|--------------|-----------|-------|
| GET /api/audit | ✗ | ✗ | ✗ |  |

---

## Authorization Rules

### Organization Verification Gate

**Rule:** NGO_STAFF users can only claim needs and view full details if their organization status is VERIFIED.

**Implementation:**
```java
@PreAuthorize("hasRole('NGO_STAFF') and @orgSecurityService.isVerified(authentication)")
```

**Checks:**
1. User has NGO_STAFF role
2. User belongs to an organization
3. Organization status = VERIFIED
4. Organization is not suspended

### Need Assignment Authorization

**Rule:** User can only update or view full details of a need if:
1. User is the creator (FIELD_WORKER or above), OR
2. User is NGO_STAFF from the assigned organization (VERIFIED), OR
3. User is ADMIN

**Implementation:**
```java
@PreAuthorize("@needSecurityService.canAccessNeed(#needId, authentication)")
```

### Service Area Matching

**Rule:** NGO_STAFF can only claim needs in their organization's registered service areas.

**Checks:**
1. Need.country + region + category matches at least one ServiceArea
2. ServiceArea.is_active = true
3. ServiceArea.organization_id = user's organization

---

## JWT Token Structure

### Claims
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "name": "User Name",
  "role": "NGO_STAFF",
  "organizationId": "org-uuid",
  "organizationStatus": "VERIFIED",
  "iat": 1234567890,
  "exp": 1234654290
}
```

### Token Lifecycle
- **Expiration:** 24 hours default
- **Refresh:** Client must re-login (no refresh tokens in v1)
- **Revocation:** Logout clears client-side token (no server-side blacklist in v1)

---

## Method-Level Security Annotations

### Spring Security @PreAuthorize Examples

```java
// Simple role check
@PreAuthorize("hasRole('ADMIN')")

// Multiple roles (OR)
@PreAuthorize("hasAnyRole('ADMIN', 'NGO_STAFF')")

// Custom security service check
@PreAuthorize("@needSecurityService.canAccessNeed(#id, authentication)")

// Verified organization check
@PreAuthorize("hasRole('NGO_STAFF') and @orgSecurityService.isVerified(authentication)")

// Complex condition
@PreAuthorize("hasRole('ADMIN') or (hasRole('NGO_STAFF') and @needSecurityService.isAssignedToUserOrg(#id, authentication))")
```

---

## Attribute-Level Access Control

### Need Entity Field Access

| Field | Redacted View | Full View (Authorized) |
|-------|---------------|------------------------|
| id |  |  |
| category |  |  |
| status |  |  |
| urgency |  |  |
| country |  |  |
| region |  (generalized) |  (exact) |
| description |  (truncated/sanitized) |  (full) |
| vulnerability flags |  (boolean aggregated) |  (detailed) |
| created_at |  |  |
| updated_at |  |  |
| created_by | ✗ |  (user name only) |
| assigned_to_org | ✗ |  (org name) |
| beneficiary_name | ✗ |  (decrypted) |
| beneficiary_phone | ✗ |  (decrypted) |
| beneficiary_email | ✗ |  (decrypted) |
| exact_location | ✗ |  (decrypted) |
| sensitive_notes | ✗ |  (decrypted) |

---

## Security Service Implementations

### NeedSecurityService

```java
@Service
public class NeedSecurityService {

    public boolean canAccessNeed(UUID needId, Authentication auth) {
        User user = getCurrentUser(auth);
        Need need = needRepository.findById(needId).orElse(null);

        if (need == null) return false;
        if (user.getRole() == UserRole.ADMIN) return true;
        if (need.getCreatedBy().getId().equals(user.getId())) return true;

        if (user.getRole() == UserRole.NGO_STAFF) {
            return need.getAssignedToOrg() != null
                && need.getAssignedToOrg().getId().equals(user.getOrganization().getId())
                && user.getOrganization().getStatus() == OrganizationStatus.VERIFIED;
        }

        return false;
    }

    public boolean canClaimNeed(UUID needId, Authentication auth) {
        User user = getCurrentUser(auth);
        Need need = needRepository.findById(needId).orElse(null);

        if (need == null || user.getRole() != UserRole.NGO_STAFF) return false;
        if (user.getOrganization() == null) return false;
        if (user.getOrganization().getStatus() != OrganizationStatus.VERIFIED) return false;
        if (need.getStatus() != NeedStatus.PENDING) return false;

        // Check service area matching
        return serviceAreaRepository.existsByOrganizationAndCountryAndRegionAndCategory(
            user.getOrganization(), need.getCountry(), need.getRegion(), need.getCategory()
        );
    }
}
```

### OrganizationSecurityService

```java
@Service
public class OrganizationSecurityService {

    public boolean isVerified(Authentication auth) {
        User user = getCurrentUser(auth);
        return user.getOrganization() != null
            && user.getOrganization().getStatus() == OrganizationStatus.VERIFIED;
    }

    public boolean canManageOrganization(UUID orgId, Authentication auth) {
        User user = getCurrentUser(auth);
        if (user.getRole() == UserRole.ADMIN) return true;
        return user.getOrganization() != null
            && user.getOrganization().getId().equals(orgId);
    }
}
```

---

## Error Handling for Authorization

### HTTP Status Codes

- **401 Unauthorized:** Missing or invalid JWT token
- **403 Forbidden:** Valid token but insufficient permissions
- **404 Not Found:** Resource doesn't exist OR user doesn't have access (to prevent enumeration)

### Error Response Format

```json
{
  "error": "FORBIDDEN",
  "message": "You do not have permission to access this resource",
  "timestamp": "2025-01-10T12:34:56Z"
}
```

**Important:** Never reveal specific reasons for denial (e.g., "organization not verified") to prevent information leakage.

---

## Testing Strategy

### RBAC Test Coverage

1. **Role-based endpoint access**
   - Test each role against each endpoint
   - Verify 403 for unauthorized roles

2. **Organization verification gates**
   - Test NGO_STAFF with PENDING org (should be denied)
   - Test NGO_STAFF with VERIFIED org (should succeed)
   - Test NGO_STAFF with SUSPENDED org (should be denied)

3. **Need assignment authorization**
   - Test access to own created needs
   - Test access to assigned needs
   - Test denial of unassigned needs

4. **Service area matching**
   - Test claim within service area (success)
   - Test claim outside service area (denial)

5. **Attribute-level redaction**
   - Verify redacted responses don't include PII
   - Verify full responses include all fields for authorized users

---

## Audit Requirements

All authorization checks must be logged:

- **Successful access:** Log user, action, resource ID
- **Denied access:** Log user, attempted action, reason (internal only)
- **Organization verification checks:** Log verification outcomes
- **Need claims:** Log who claimed what, when
