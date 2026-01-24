# Privacy & Redaction Engine

**Owner:** Privacy & Redaction Engine Agent

## Core Principles

1. **Redacted by Default:** All need responses are redacted unless user has explicit need-to-know
2. **Server-Side Only:** Decryption happens only in service layer, never client-side
3. **Audit Everything:** Every access to full PII is logged
4. **No Client Caching:** Sensitive data never cached in browser/app
5. **Minimal Exposure:** Even authorized users see only what they need

---

## Redaction Rules

### RedactedNeedResponse (Default View)

**Who sees this:**
- BENEFICIARY (own case only)
- FIELD_WORKER (needs in service area not created by them)
- NGO_STAFF with unverified organization
- NGO_STAFF viewing unassigned needs in service area
- Public-facing reports/exports

**Fields included:**

```java
public class RedactedNeedResponse {
    private UUID id;
    private NeedCategory category;
    private NeedStatus status;
    private UrgencyLevel urgency;
    private String country;
    private String region; // Generalized (e.g., "Central Region", not "123 Main St")
    private String descriptionSummary; // First 100 chars or sanitized version
    private boolean hasVulnerabilities; // Aggregated flag
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Fields excluded (never sent):**
- Beneficiary name, phone, email
- Exact location/address
- Full description with identifying details
- Creator user details
- Assigned organization details
- Sensitive vulnerability details
- Any decrypted PII

---

### FullNeedResponse (Authorized View)

**Who sees this:**
- Need creator (FIELD_WORKER or NGO_STAFF)
- NGO_STAFF from assigned VERIFIED organization
- ADMIN

**Authorization check:**
```java
if (user.getRole() == ADMIN) return true;
if (need.getCreatedBy().equals(user)) return true;
if (user.getRole() == NGO_STAFF
    && user.getOrganization().getStatus() == VERIFIED
    && need.getAssignedToOrg().equals(user.getOrganization())) {
    return true;
}
return false;
```

**Fields included (all RedactedNeedResponse fields PLUS):**

```java
public class FullNeedResponse extends RedactedNeedResponse {
    private String fullDescription;
    private String exactLocation; // Decrypted from SensitiveInfo

    // Decrypted beneficiary information
    private String beneficiaryName; // Decrypted
    private String beneficiaryPhone; // Decrypted
    private String beneficiaryEmail; // Decrypted

    // Detailed vulnerability flags
    private boolean hasChildren;
    private boolean hasElderly;
    private boolean hasDisability;
    private boolean isFemaleHeaded;

    // Assignment details
    private String createdByName; // User name (not email)
    private UUID assignedToOrgId;
    private String assignedToOrgName;

    // Additional case notes
    private String sensitiveNotes; // Decrypted

    // Timestamps
    private LocalDateTime claimedAt;
    private LocalDateTime closedAt;
}
```

---

## Privacy Filter Implementation

### NeedPrivacyFilterService

**Responsibilities:**
1. Determine user authorization level for each need
2. Return appropriate response DTO (redacted vs full)
3. Decrypt PII only when returning full response
4. Log every full access event

**Core Methods:**

```java
@Service
public class NeedPrivacyFilterService {

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private SensitiveInfoRepository sensitiveInfoRepository;

    /**
     * Filter single need for current user
     */
    public Object filterNeed(Need need, User currentUser) {
        if (canViewFullDetails(need, currentUser)) {
            auditService.logNeedAccessed(need.getId(), currentUser.getId(), "FULL");
            return buildFullResponse(need, currentUser);
        } else {
            auditService.logNeedAccessed(need.getId(), currentUser.getId(), "REDACTED");
            return buildRedactedResponse(need);
        }
    }

    /**
     * Filter list of needs (always redacted for lists)
     */
    public List<RedactedNeedResponse> filterNeedsList(List<Need> needs, User currentUser) {
        return needs.stream()
            .map(this::buildRedactedResponse)
            .collect(Collectors.toList());
    }

    private boolean canViewFullDetails(Need need, User user) {
        // ADMIN always has full access
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // Creator has full access to their own needs
        if (need.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // NGO_STAFF from assigned VERIFIED org has full access
        if (user.getRole() == UserRole.NGO_STAFF
            && user.getOrganization() != null
            && user.getOrganization().getStatus() == OrganizationStatus.VERIFIED
            && need.getAssignedToOrg() != null
            && need.getAssignedToOrg().getId().equals(user.getOrganization().getId())) {
            return true;
        }

        return false;
    }

    private RedactedNeedResponse buildRedactedResponse(Need need) {
        return RedactedNeedResponse.builder()
            .id(need.getId())
            .category(need.getCategory())
            .status(need.getStatus())
            .urgency(need.getUrgency())
            .country(need.getCountry())
            .region(generalizeRegion(need.getRegion()))
            .descriptionSummary(sanitizeDescription(need.getDescription()))
            .hasVulnerabilities(need.isHasChildren() || need.isHasElderly()
                || need.isHasDisability() || need.isIsFemaleHeaded())
            .createdAt(need.getCreatedAt())
            .updatedAt(need.getUpdatedAt())
            .build();
    }

    private FullNeedResponse buildFullResponse(Need need, User currentUser) {
        SensitiveInfo sensitiveInfo = sensitiveInfoRepository.findByNeedId(need.getId())
            .orElse(null);

        FullNeedResponse response = FullNeedResponse.builder()
            // Include all redacted fields
            .id(need.getId())
            .category(need.getCategory())
            .status(need.getStatus())
            .urgency(need.getUrgency())
            .country(need.getCountry())
            .region(need.getRegion()) // Full region, not generalized
            .fullDescription(need.getDescription())

            // Detailed vulnerability flags
            .hasChildren(need.isHasChildren())
            .hasElderly(need.isHasElderly())
            .hasDisability(need.isHasDisability())
            .isFemaleHeaded(need.isIsFemaleHeaded())

            // Assignment details
            .createdByName(need.getCreatedBy().getName())
            .assignedToOrgId(need.getAssignedToOrg() != null ? need.getAssignedToOrg().getId() : null)
            .assignedToOrgName(need.getAssignedToOrg() != null ? need.getAssignedToOrg().getName() : null)

            // Timestamps
            .createdAt(need.getCreatedAt())
            .updatedAt(need.getUpdatedAt())
            .claimedAt(need.getClaimedAt())
            .closedAt(need.getClosedAt())
            .build();

        // Decrypt and add sensitive info if available
        if (sensitiveInfo != null) {
            response.setBeneficiaryName(
                encryptionService.decrypt(sensitiveInfo.getEncryptedFullName()));

            if (sensitiveInfo.getEncryptedPhone() != null) {
                response.setBeneficiaryPhone(
                    encryptionService.decrypt(sensitiveInfo.getEncryptedPhone()));
            }

            if (sensitiveInfo.getEncryptedEmail() != null) {
                response.setBeneficiaryEmail(
                    encryptionService.decrypt(sensitiveInfo.getEncryptedEmail()));
            }

            if (sensitiveInfo.getEncryptedExactLocation() != null) {
                response.setExactLocation(
                    encryptionService.decrypt(sensitiveInfo.getEncryptedExactLocation()));
            }

            if (sensitiveInfo.getEncryptedNotes() != null) {
                response.setSensitiveNotes(
                    encryptionService.decrypt(sensitiveInfo.getEncryptedNotes()));
            }
        }

        return response;
    }

    /**
     * Generalize region to prevent exact location disclosure
     */
    private String generalizeRegion(String region) {
        // Remove street addresses, keep only district/city level
        // Example: "Kampala Central, Plot 123" -> "Kampala Central"
        if (region == null) return null;
        return region.split(",")[0].trim();
    }

    /**
     * Sanitize description to prevent accidental PII leakage
     */
    private String sanitizeDescription(String description) {
        if (description == null) return null;

        // Truncate to 100 characters
        String truncated = description.length() > 100
            ? description.substring(0, 100) + "..."
            : description;

        // TODO: Add PII pattern detection and removal (phone numbers, emails, names)
        return truncated;
    }
}
```

---

## Controller Integration

### NeedController Example

```java
@RestController
@RequestMapping("/api/needs")
public class NeedController {

    @Autowired
    private NeedService needService;

    @Autowired
    private NeedPrivacyFilterService privacyFilterService;

    @Autowired
    private AuthService authService;

    /**
     * Get all needs (always redacted list view)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RedactedNeedResponse>> getAllNeeds(
        Authentication authentication
    ) {
        User currentUser = authService.getCurrentUser(authentication);
        List<Need> needs = needService.getNeedsForUser(currentUser);

        // Lists are always redacted
        List<RedactedNeedResponse> redactedNeeds =
            privacyFilterService.filterNeedsList(needs, currentUser);

        return ResponseEntity.ok(redactedNeeds);
    }

    /**
     * Get need by ID (redacted OR full based on authorization)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getNeedById(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        User currentUser = authService.getCurrentUser(authentication);
        Need need = needService.getNeedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Need not found"));

        // Check if user can see this need at all
        if (!needService.canUserViewNeed(need, currentUser)) {
            throw new ResourceNotFoundException("Need not found"); // 404 to prevent enumeration
        }

        // Privacy filter determines redacted vs full
        Object response = privacyFilterService.filterNeed(need, currentUser);

        return ResponseEntity.ok(response);
    }
}
```

---

## Data Export & Reporting Privacy

### Export Rules

**CSV/Excel exports are ALWAYS redacted** even for admins, unless explicitly requested with additional confirmation.

**Redacted Export Fields:**
- Need ID
- Category
- Status
- Urgency
- Country
- Region (generalized)
- Description (sanitized)
- Has vulnerabilities (boolean)
- Created date
- Status (PENDING, ASSIGNED, etc.)

**Never in exports:**
- Beneficiary names, phones, emails
- Exact locations
- Creator details
- Organization details
- Any decrypted PII

### Aggregation & Analytics

**Safe aggregations:**
- Count of needs by category/region/urgency/status
- Average time to claim/closure
- Needs by date range

**Minimum count thresholds:**
- Never show aggregations where count < 5 (prevents identification of small groups)
- Example: If only 1 need in "Region X" with "MEDICAL + CRITICAL", don't show breakdown

---

## Client-Side Privacy Controls

### Never Cache Sensitive Data

```typescript
// Good: No caching of full need responses
const fetchNeedDetails = async (id: string) => {
  const response = await api.get(`/api/needs/${id}`, {
    headers: { 'Cache-Control': 'no-store' }
  });
  return response.data; // Don't persist to localStorage or sessionStorage
};

// Bad: Caching sensitive data
localStorage.setItem('needDetails', JSON.stringify(fullNeedData)); // NEVER DO THIS
```

### UI Indicators for Restricted Data

```tsx
{need.beneficiaryName ? (
  <p><strong>Beneficiary:</strong> {need.beneficiaryName}</p>
) : (
  <div className="restricted-info">
    <LockIcon />
    <p>Beneficiary information restricted. Claim this need to view details.</p>
  </div>
)}
```

---

## Decryption Boundaries

### Where Decryption Happens

 **ALLOWED:**
- Service layer when building FullNeedResponse
- Background jobs with explicit authorization context
- Admin console with audit logging

✗ **FORBIDDEN:**
- Controller layer (too close to HTTP boundary)
- Repository layer (too early in flow)
- Client-side JavaScript (NEVER)
- Application logs
- Error messages
- Cache layers (Redis, etc.)

### Decryption Audit Trail

Every decryption must log:
```java
auditService.log(
    action: "NEED_PII_ACCESSED",
    userId: currentUser.getId(),
    entityType: "NEED",
    entityId: need.getId(),
    outcome: "SUCCESS",
    details: {
        "accessType": "FULL",
        "fieldsDecrypted": ["beneficiaryName", "beneficiaryPhone"]
    }
);
```

---

## Data Retention & Deletion

### Automatic PII Deletion

**Rule:** Sensitive info is deleted 90 days after need closure (configurable)

```java
@Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
public void deleteExpiredSensitiveInfo() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);

    List<Need> closedNeeds = needRepository.findByStatusAndClosedAtBefore(
        NeedStatus.CLOSED, cutoffDate
    );

    for (Need need : closedNeeds) {
        SensitiveInfo sensitiveInfo = sensitiveInfoRepository.findByNeedId(need.getId())
            .orElse(null);

        if (sensitiveInfo != null) {
            auditService.log(
                action: "SENSITIVE_INFO_DELETED",
                entityType: "NEED",
                entityId: need.getId(),
                outcome: "SUCCESS"
            );

            sensitiveInfoRepository.delete(sensitiveInfo);
        }
    }
}
```

### Anonymization on User Deletion

When a user is deleted, their needs remain but PII is anonymized:
```java
need.setCreatedBy(anonymousUser);
sensitiveInfo.setEncryptedFullName(encryptionService.encrypt("DELETED_USER"));
// Keep need metadata for reporting but remove identifying info
```

---

## Testing Privacy Controls

### Test Coverage Requirements

1. **Redaction tests:**
   - Verify RedactedNeedResponse never includes PII
   - Test generalized region formatting
   - Test description sanitization

2. **Authorization tests:**
   - BENEFICIARY sees only own redacted case
   - FIELD_WORKER sees full details for created needs
   - NGO_STAFF (unverified) sees only redacted
   - NGO_STAFF (verified, assigned) sees full details
   - NGO_STAFF (verified, unassigned) sees redacted
   - ADMIN sees full details for all

3. **Decryption boundary tests:**
   - Verify decryption only in service layer
   - Verify no PII in JSON responses for redacted views
   - Verify no PII in logs

4. **Audit logging tests:**
   - Verify full access is logged
   - Verify audit logs don't contain PII

5. **Export privacy tests:**
   - Verify CSV exports are redacted
   - Verify aggregation thresholds enforced

---

## Security Considerations

### Timing Attacks

Avoid revealing information through response times:
```java
// Bad: Different response times for authorized vs unauthorized
if (!authorized) return 403;
// Decryption happens only if authorized

// Good: Constant-time check
boolean authorized = checkAuthorization(); // Fast
Object response = authorized ? buildFull() : buildRedacted(); // Similar time
return response;
```

### Enumeration Prevention

Return 404 instead of 403 when user doesn't have access to a need:
```java
// This prevents attackers from enumerating valid need IDs
if (!canUserViewNeed(need, user)) {
    throw new ResourceNotFoundException("Need not found"); // Returns 404
}
```

---

## Privacy by Design Checklist

- [ ] All list endpoints return redacted responses
- [ ] Detail endpoints use privacy filter
- [ ] Decryption only in service layer
- [ ] Full access events are audit-logged
- [ ] No PII in error messages
- [ ] No PII in application logs
- [ ] No client-side caching of sensitive data
- [ ] Export endpoints are redacted
- [ ] Aggregations use minimum count thresholds
- [ ] Automatic PII deletion on schedule
- [ ] User deletion anonymizes data
- [ ] UI shows "restricted" placeholders for unauthorized fields
