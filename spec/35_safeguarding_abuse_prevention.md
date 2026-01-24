# Safeguarding & Abuse Prevention

**Owner:** Safeguarding & Abuse-Prevention Agent

## Mission Critical

**CrisisConnect handles information about vulnerable people. The system MUST prevent:**
1. Targeting or exploitation of beneficiaries
2. Retaliation against those who report abuses
3. Fraud (fake needs, impersonation)
4. Internal misuse by staff
5. Data harvesting or surveillance
6. Coercion or manipulation

---

## Threat Scenarios & Mitigations

### 1. Malicious Actor Posing as NGO

**Scenario:** Bad actor creates fake NGO to access beneficiary PII for exploitation, trafficking, or targeting.

**Mitigations:**
-  **Organization verification required** before any PII access
-  Manual admin review of organization credentials
-  Verification includes: registration documents, references, established web presence
-  Suspicious patterns flagged: new orgs claiming high-risk cases immediately
-  Audit trail of all organization verification actions

**Implementation:**
```java
// NGO cannot claim needs until VERIFIED
@PreAuthorize("@orgSecurityService.isVerified(authentication)")
public void claimNeed(UUID needId) { ... }

// Admin verification workflow
public void verifyOrganization(UUID orgId, VerificationRequest request) {
    // Require: registration docs, contact verification, reference checks
    auditService.log("ORG_VERIFICATION_APPROVED", orgId, admin);
}
```

---

### 2. Insider Threat (Rogue NGO Staff)

**Scenario:** Verified NGO staff member abuses access to browse cases for personal gain or to leak information.

**Mitigations:**
-  **Rate limiting** on need detail views (max 20 per hour for non-admins)
-  **Suspicious activity detection**: Flag users who view many needs without claiming
-  **Audit alerts**: Admin dashboard shows users with high view counts
-  **Need-to-know enforcement**: Staff only see full details for assigned needs
-  **Session timeout**: 30-minute idle timeout for NGO staff

**Implementation:**
```java
@Service
public class RateLimitService {

    private static final int MAX_NEED_VIEWS_PER_HOUR = 20;

    public void checkNeedViewRateLimit(User user) {
        if (user.getRole() == UserRole.ADMIN) return; // Admins exempt

        long recentViews = auditLogRepository.countByUserAndActionAndCreatedAtAfter(
            user.getId(),
            "NEED_ACCESSED",
            LocalDateTime.now().minusHours(1)
        );

        if (recentViews >= MAX_NEED_VIEWS_PER_HOUR) {
            auditService.log("RATE_LIMIT_EXCEEDED", user.getId(), "NEED_VIEW");
            throw new RateLimitException("Too many requests. Please try again later.");
        }
    }
}

@Service
public class SuspiciousActivityService {

    // Flag users who view many needs but claim none
    @Scheduled(cron = "0 0 * * * *") // Hourly
    public void detectSuspiciousPatterns() {
        List<User> ngoStaff = userRepository.findByRole(UserRole.NGO_STAFF);

        for (User user : ngoStaff) {
            long viewsLast24h = auditLogRepository.countViewsByUserLast24h(user.getId());
            long claimsLast24h = auditLogRepository.countClaimsByUserLast24h(user.getId());

            // More than 50 views but zero claims = suspicious
            if (viewsLast24h > 50 && claimsLast24h == 0) {
                auditService.log("SUSPICIOUS_BROWSING_PATTERN", user.getId());
                // Notify admins via internal alert
            }
        }
    }
}
```

---

### 3. High-Risk Case Targeting

**Scenario:** Cases flagged as domestic violence, trafficking, or persecution are especially vulnerable. Accessing their info could enable retaliation.

**Mitigations:**
-  **Extra redaction layer for high-risk flags** (config-driven)
-  High-risk cases require additional admin approval before assignment
-  Extra audit scrutiny: alerts for any access to high-risk cases
-  Option to mark needs as "restricted access" (admin-only temporarily)

**Implementation:**
```java
public enum RiskLevel {
    STANDARD,
    HIGH_RISK, // Domestic violence, persecution, trafficking indicators
    RESTRICTED // Admin approval required for any access
}

public class Need {
    // ...
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.STANDARD;
}

@Service
public class HighRiskCaseService {

    public Object filterNeedByRisk(Need need, User user) {
        if (need.getRiskLevel() == RiskLevel.RESTRICTED && user.getRole() != UserRole.ADMIN) {
            // Return ultra-redacted response (category + region only)
            return buildMinimalResponse(need);
        }

        if (need.getRiskLevel() == RiskLevel.HIGH_RISK) {
            // Log all access attempts
            auditService.log("HIGH_RISK_CASE_ACCESSED", user.getId(), need.getId());
        }

        // Proceed with normal privacy filtering
        return privacyFilterService.filterNeed(need, user);
    }
}
```

---

### 4. Fraudulent Needs (Fake Requests)

**Scenario:** Malicious field worker creates fake needs to divert assistance or inflate metrics.

**Mitigations:**
-  **Field worker must be associated with verified organization** (optional config)
-  Audit trail of all needs created by each field worker
-  Admin dashboard: flag field workers with unusually high creation rates
-  Duplicate detection: similar beneficiary names/phones/locations
-  Review workflow for suspicious patterns

**Implementation:**
```java
@Service
public class FraudDetectionService {

    @Scheduled(cron = "0 0 8 * * *") // Daily at 8 AM
    public void detectAnomalousCreationPatterns() {
        List<User> fieldWorkers = userRepository.findByRole(UserRole.FIELD_WORKER);

        for (User worker : fieldWorkers) {
            long needsCreatedLast7Days = needRepository.countByCreatedByAndCreatedAtAfter(
                worker.getId(), LocalDateTime.now().minusDays(7)
            );

            // More than 100 needs in 7 days = review needed
            if (needsCreatedLast7Days > 100) {
                auditService.log("ANOMALOUS_CREATION_RATE", worker.getId());
                // Flag for admin review
            }
        }
    }

    // Check for duplicate beneficiary info (fuzzy matching)
    public void checkForDuplicates(CreateNeedRequest request) {
        // Hash phone number and check for recent similar needs
        String phoneHash = hashPhone(request.getBeneficiaryPhone());

        long recentSimilar = needRepository.countBySimilarBeneficiaryLast30Days(phoneHash);

        if (recentSimilar > 3) {
            auditService.log("POSSIBLE_DUPLICATE_BENEFICIARY", phoneHash);
            // Warn creator or require admin approval
        }
    }
}
```

---

### 5. Phishing or Social Engineering

**Scenario:** Attacker impersonates CrisisConnect to trick beneficiaries or field workers into revealing information.

**Mitigations:**
-  **Clear branding and domain verification**
-  Email/SMS templates never ask for passwords or sensitive info
-  User training materials: "CrisisConnect will never ask for..."
-  No clickable links with auth tokens in emails (only generic login page)

**Safe Email Template Example:**
```
Subject: CrisisConnect - Need Status Update

Hello,

The status of assistance request #12345 has been updated to "IN_PROGRESS".

To view details, please log in to CrisisConnect at:
https://crisisconnect.org

Do not reply to this email. This is an automated notification.

CrisisConnect never asks for passwords or personal information via email.
```

---

### 6. Data Harvesting / Scraping

**Scenario:** Automated bots attempt to scrape need IDs and details.

**Mitigations:**
-  **Rate limiting on login attempts** (max 5 failures per 15 minutes)
-  **CAPTCHA on repeated login failures** (optional enhancement)
-  **API rate limiting** (max 100 requests per minute per user)
-  **No public API endpoints** (all require authentication)
-  Need IDs are UUIDs (not sequential integers) to prevent enumeration

**Implementation:**
```java
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        if (!request.getRequestURI().equals("/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = extractEmail(request);
        int failedAttempts = loginAttemptService.getFailedAttempts(email);

        if (failedAttempts >= MAX_LOGIN_ATTEMPTS) {
            auditService.log("LOGIN_RATE_LIMIT_EXCEEDED", email);
            response.setStatus(429); // Too Many Requests
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

---

### 7. Session Hijacking

**Scenario:** Attacker steals JWT token from compromised device or network.

**Mitigations:**
-  **Short token expiration** (24 hours max, 1 hour recommended for high-value accounts)
-  **HTTPS only** (no plaintext transmission)
-  **HttpOnly cookies** for token storage (if using cookies)
-  **IP address logging** in audit trail (detect unusual locations)
-  User notification of new logins from unfamiliar devices (future)

---

### 8. Privilege Escalation

**Scenario:** NGO_STAFF attempts to modify their own role to ADMIN.

**Mitigations:**
-  **Only ADMIN can change user roles**
-  Users cannot modify their own role
-  Audit log for all role changes

**Implementation:**
```java
@PreAuthorize("hasRole('ADMIN')")
public void updateUserRole(UUID userId, UserRole newRole, Authentication auth) {
    User currentUser = getCurrentUser(auth);
    User targetUser = userRepository.findById(userId).orElseThrow();

    // Prevent self-role-escalation
    if (currentUser.getId().equals(targetUser.getId())) {
        throw new ForbiddenException("Cannot modify your own role");
    }

    auditService.log("USER_ROLE_CHANGED", currentUser.getId(), userId,
        Map.of("oldRole", targetUser.getRole(), "newRole", newRole));

    targetUser.setRole(newRole);
    userRepository.save(targetUser);
}
```

---

### 9. SQL Injection / XSS

**Scenario:** Attacker injects malicious code through user input fields.

**Mitigations:**
-  **Parameterized queries** (JPA prevents SQL injection by default)
-  **Input validation** on all request DTOs
-  **Output encoding** on frontend (React escapes by default)
-  **CSP headers** to prevent script injection

**Validation Example:**
```java
public class CreateNeedRequest {
    @NotNull
    @Size(min = 3, max = 255)
    @Pattern(regexp = "^[a-zA-Z0-9\\s,.-]+$", message = "Invalid characters in name")
    private String beneficiaryName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone format")
    private String beneficiaryPhone;

    @Email
    private String beneficiaryEmail;

    @NotNull
    @Size(min = 20, max = 2000)
    private String description; // No HTML tags allowed
}
```

---

## Safeguarding UI/UX Controls

### Field Worker Training Prompts

```tsx
// On need creation form
<InfoBox>
  <strong>Privacy Notice:</strong>
  Only share information with beneficiary consent. Do not include:
  - Government ID numbers
  - Sensitive medical diagnoses
  - Information that could endanger the beneficiary
</InfoBox>
```

### High-Risk Case Warning

```tsx
{need.riskLevel === 'HIGH_RISK' && (
  <WarningBanner>
    <AlertIcon />
    This case has been flagged as high-risk. Handle with extra care.
    Access is being monitored.
  </WarningBanner>
)}
```

### Admin Alerts Dashboard

```tsx
// Admin view: recent suspicious activity
<SuspiciousActivityWidget>
  <h3>Alerts (Last 24 hours)</h3>
  <ul>
    <li>User #456 viewed 75 needs without claiming (investigate)</li>
    <li>Field worker #123 created 45 needs in one day (review)</li>
    <li>High-risk case #789 accessed 12 times (normal for assigned org)</li>
  </ul>
</SuspiciousActivityWidget>
```

---

## Abuse Reporting Mechanism

### Internal Reporting

NGO staff or field workers can flag concerning behavior:

```java
@PostMapping("/api/concerns")
@PreAuthorize("isAuthenticated()")
public void reportConcern(@RequestBody ConcernRequest request, Authentication auth) {
    User reporter = getCurrentUser(auth);

    Concern concern = Concern.builder()
        .reportedBy(reporter)
        .concernType(request.getType()) // SUSPICIOUS_USER, FAKE_NEED, DATA_BREACH, etc.
        .description(request.getDescription())
        .relatedEntityId(request.getEntityId())
        .status(ConcernStatus.PENDING)
        .build();

    concernRepository.save(concern);

    auditService.log("CONCERN_REPORTED", reporter.getId(), concern.getId());

    // Notify admins
    notificationService.notifyAdmins("New safeguarding concern reported");
}
```

### External Reporting (Security Disclosures)

**SECURITY.md file with contact:**
```
If you discover a security vulnerability or safeguarding concern:

Email: sekacorn@gmail.com
PGP Key: [link to public key]

Expected response time: 48 hours
We follow responsible disclosure practices.
```

---

## Testing Safeguarding Controls

### Test Coverage

1. **Rate limiting tests:**
   - Verify 429 after exceeding login attempts
   - Verify 429 after exceeding need view limits

2. **Suspicious activity detection:**
   - Test detection of high view/low claim ratio
   - Test anomalous creation rate detection

3. **High-risk case handling:**
   - Verify extra redaction for high-risk cases
   - Verify audit logs for high-risk access

4. **Fraud detection:**
   - Test duplicate beneficiary detection
   - Test creation rate anomaly alerts

5. **Authorization bypass attempts:**
   - Test self-role-escalation prevention
   - Test unverified org trying to claim needs

---

## Safeguarding Checklist

- [ ] Organization verification required for PII access
- [ ] Rate limiting on sensitive endpoints
- [ ] Suspicious activity monitoring and alerts
- [ ] High-risk case extra protections
- [ ] Fraud detection (duplicate beneficiaries, anomalous creation rates)
- [ ] No PII in notifications or emails
- [ ] Session timeout and secure token handling
- [ ] Audit trail for all sensitive actions
- [ ] Admin alert dashboard for security events
- [ ] Internal concern reporting mechanism
- [ ] External security disclosure process
- [ ] User training materials on safe data handling
- [ ] Regular security review of access patterns

---

## Incident Response Plan

### 1. Suspected Data Breach

**Actions:**
1. Admin disables affected user accounts immediately
2. Review audit logs for scope of breach
3. Notify affected organizations (not beneficiaries directly without coordination)
4. Patch vulnerability
5. Document incident and lessons learned

### 2. Insider Misuse

**Actions:**
1. Suspend user account
2. Review all needs accessed by user
3. Notify user's organization if NGO_STAFF
4. Consider law enforcement if evidence of exploitation
5. Strengthen monitoring and alerts

### 3. Fake NGO Detected

**Actions:**
1. Suspend organization immediately
2. Revoke access for all associated users
3. Review all needs claimed by org (reassign if needed)
4. Audit trail review for suspicious patterns
5. Report to authorities if applicable

---

**Safeguarding is not a feature—it is the foundation of trust.**
