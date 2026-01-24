# Security Threat Model

**Owner:** Security Architect / Threat Modeling Agent

## Threat Modeling Framework

We use **STRIDE** (Microsoft's threat classification) to identify and mitigate threats:

- **S**poofing Identity
- **T**ampering with Data
- **R**epudiation
- **I**nformation Disclosure
- **D**enial of Service
- **E**levation of Privilege

---

## System Components & Trust Boundaries

```
┌─────────────────────────────────────────────────────────────┐
│                       Internet (Untrusted)                   │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
            ┌───────────────────────┐
            │   Reverse Proxy       │
            │   (nginx + TLS)       │
            └───────────┬───────────┘
                        │
        ┌───────────────┴────────────────┐
        │                                │
        ▼                                ▼
┌───────────────┐              ┌──────────────────┐
│  React SPA    │              │  Spring Boot API │
│  (Public)     │◄─────────────┤  (JWT Auth +     │
│               │              │   RBAC)          │
└───────────────┘              └────────┬─────────┘
                                        │
                                        ▼
                              ┌──────────────────────┐
                              │   PostgreSQL         │
                              │   (Encrypted PII)    │
                              └──────────────────────┘
```

**Trust Boundaries:**
1. **Internet ↔ Reverse Proxy:** HTTPS/TLS required
2. **React SPA ↔ Spring Boot API:** JWT bearer token required
3. **Spring Boot ↔ PostgreSQL:** Internal network (Docker network or private VPC)

---

## Threat Analysis by Component

### 1. Authentication System

#### Threats

| Threat Type | Scenario | Impact | Mitigation |
|-------------|----------|--------|------------|
| **Spoofing** | Attacker guesses weak password | High | BCrypt hashing (cost 12+), password complexity requirements |
| **Spoofing** | Stolen JWT token | High | Short expiration (24h), HTTPS only, HttpOnly cookies |
| **Spoofing** | Brute force login | Medium | Rate limiting (5 attempts per 15 min), account lockout, CAPTCHA |
| **Repudiation** | User denies login action | Low | Audit log with IP + user-agent for all login attempts |
| **DoS** | Flooding login endpoint | Medium | Rate limiting, IP-based throttling |

#### Mitigations Implemented

 BCrypt password hashing (cost factor 12)
 JWT with 24-hour expiration
 Rate limiting on login endpoint
 Audit logging for all auth attempts
 HTTPS enforcement via reverse proxy

#### Future Enhancements

- Multi-factor authentication (MFA) for admin accounts
- OAuth2/SAML for NGO single sign-on
- Token refresh mechanism with rotation
- Device fingerprinting for anomaly detection

---

### 2. Authorization & RBAC

#### Threats

| Threat Type | Scenario | Impact | Mitigation |
|-------------|----------|--------|------------|
| **Elevation of Privilege** | NGO_STAFF modifies own role to ADMIN | Critical | Users cannot change own roles, only ADMIN can modify roles |
| **Elevation of Privilege** | Unverified org accesses PII | Critical | Org verification gate enforced in code (@PreAuthorize) |
| **Information Disclosure** | User enumerates need IDs to find valid cases | Medium | Return 404 instead of 403 for unauthorized access |
| **Tampering** | JWT token modified to change role | High | JWT signature verification, secret key protection |

#### Mitigations Implemented

 Method-level security annotations (@PreAuthorize)
 Organization verification required for PII access
 JWT signature verification
 Self-role-modification prevention
 404 responses to prevent enumeration

---

### 3. Data Storage (PostgreSQL)

#### Threats

| Threat Type | Scenario | Impact | Mitigation |
|-------------|----------|--------|------------|
| **Information Disclosure** | Database backup stolen | Critical | AES-256 encryption for PII fields, encrypted backups |
| **Information Disclosure** | SQL injection | Critical | Parameterized queries (JPA), input validation |
| **Tampering** | Unauthorized DB access | Critical | Strong DB passwords, firewall rules, private network |
| **Information Disclosure** | Logs contain PII | High | No PII in application logs, sanitized error messages |

#### Mitigations Implemented

 AES-256-GCM encryption for SensitiveInfo table
 JPA parameterized queries (SQL injection protection)
 Database credentials in environment variables
 No PII in logs policy
 Audit logging for sensitive data access

#### Database Security Hardening

**Production Checklist:**

- [ ] Database accessible only from application server (firewall rules)
- [ ] TLS/SSL for database connections
- [ ] Unique, strong DB password (32+ characters)
- [ ] Database user has minimum required privileges (no DROP/CREATE)
- [ ] Automated encrypted backups to secure storage
- [ ] Backup restoration tested quarterly
- [ ] Database audit logging enabled (pg_audit)

---

### 4. Encryption System

#### Threats

| Threat Type | Scenario | Impact | Mitigation |
|-------------|----------|--------|------------|
| **Information Disclosure** | Encryption key leaked | Critical | Key stored in secure env var, not in code/repo |
| **Information Disclosure** | Weak encryption algorithm | High | AES-256-GCM (industry standard) |
| **Tampering** | Encrypted data modified | Medium | GCM mode provides authentication (detects tampering) |
| **Information Disclosure** | Key visible in logs/errors | High | Never log encryption keys or decrypted data |

#### Encryption Implementation

**Algorithm:** AES-256-GCM
**Key Size:** 256 bits (32 bytes)
**Mode:** Galois/Counter Mode (provides confidentiality + authenticity)
**IV:** Random 12-byte IV generated per encryption, prepended to ciphertext
**Encoding:** Base64 for database storage

**Key Management:**

```java
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKey secretKey;

    public EncryptionService(@Value("${encryption.secret}") String secret) {
        if (secret == null || secret.length() != 64) { // 32 bytes hex = 64 chars
            throw new IllegalStateException("Encryption key must be 32 bytes (64 hex chars)");
        }
        byte[] keyBytes = hexToBytes(secret);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        byte[] iv = generateRandomIV();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Prepend IV to ciphertext
        byte[] combined = new byte[IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
        System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decrypt(String encryptedBase64) {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        // Extract IV and ciphertext
        byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
```

**Key Storage Best Practices:**

 Store in environment variable (`ENCRYPTION_SECRET`)
 Use secrets management (AWS Secrets Manager, HashiCorp Vault, etc.)
 Never commit to Git
 Rotate keys periodically (document rotation procedure)
 Use different keys for dev/staging/production

---

### 5. API Endpoints

#### Threats

| Threat Type | Scenario | Impact | Mitigation |
|-------------|----------|--------|------------|
| **DoS** | API flooding | Medium | Rate limiting (100 req/min per user) |
| **Information Disclosure** | Error messages leak sensitive info | Medium | Generic error messages, detailed errors only in logs |
| **Tampering** | Malicious input (XSS, injection) | High | Input validation, output encoding, CSP headers |
| **Repudiation** | User denies creating fraudulent need | Medium | Audit logging for all create/update actions |

#### API Security Controls

**Input Validation:**
```java
@PostMapping("/api/needs")
@PreAuthorize("hasAnyRole('FIELD_WORKER', 'NGO_STAFF', 'ADMIN')")
public ResponseEntity<?> createNeed(@Valid @RequestBody CreateNeedRequest request) {
    // @Valid triggers Bean Validation
    // Sanitize inputs before processing
}

public class CreateNeedRequest {
    @NotNull
    @Size(min = 3, max = 255)
    private String beneficiaryName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$")
    private String beneficiaryPhone;

    @NotNull
    @Size(min = 20, max = 2000)
    private String description;
}
```

**Rate Limiting:**
```java
@Component
public class RateLimitInterceptor extends HandlerInterceptorAdapter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        User user = getCurrentUser(request);
        if (user == null) return true; // Auth filter will handle

        int requestCount = rateLimitService.getRequestCount(user.getId());
        if (requestCount > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429); // Too Many Requests
            return false;
        }

        rateLimitService.incrementRequestCount(user.getId());
        return true;
    }
}
```

**CORS Configuration:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins.split(",")) // Only specified domains
            .allowedMethods("GET", "POST", "PATCH", "DELETE")
            .allowedHeaders("Authorization", "Content-Type")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

### 6. Frontend (React SPA)

#### Threats

| Threat Type | Scenario | Impact | Mitigation |
|-------------|----------|--------|------------|
| **Information Disclosure** | PII cached in browser | High | No localStorage/sessionStorage for sensitive data |
| **Tampering** | XSS injection | Medium | React auto-escapes, CSP headers, sanitize user input |
| **Information Disclosure** | Token stolen via XSS | High | HttpOnly cookies (or careful token storage), CSP |
| **Spoofing** | Clickjacking | Low | X-Frame-Options: DENY header |

#### Frontend Security Controls

**CSP Headers (Reverse Proxy):**
```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self' https://api.crisisconnect.org; frame-ancestors 'none';" always;
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

**Safe Token Storage:**
```typescript
// Good: Session storage for short-lived tokens
sessionStorage.setItem('authToken', token);

// Better: HttpOnly cookie set by backend (not accessible to JS)
// Backend sets: Set-Cookie: token=...; HttpOnly; Secure; SameSite=Strict

// Bad: Never do this
localStorage.setItem('beneficiaryData', JSON.stringify(pii)); // ✗ NEVER
```

**No PII Caching:**
```typescript
// Ensure sensitive API calls don't cache
const fetchNeedDetails = async (id: string) => {
  return axios.get(`/api/needs/${id}`, {
    headers: {
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      'Pragma': 'no-cache'
    }
  });
};
```

---

### 7. Audit Logging System

#### Threats

| Threat Type | Scenario | Impact | Mitigation |
|-------------|----------|--------|------------|
| **Repudiation** | User denies malicious action | High | Comprehensive audit logs for all sensitive actions |
| **Tampering** | Attacker deletes audit logs | High | Write-only audit log access, separate backup |
| **Information Disclosure** | Audit logs contain PII | Medium | Only log IDs and action types, no PII |

#### Audit Log Schema (Revisited)

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL, -- LOGIN_SUCCESS, NEED_CREATED, NEED_ACCESSED_FULL, etc.
    entity_type VARCHAR(50),      -- NEED, ORGANIZATION, USER
    entity_id UUID,                -- Reference ID (not PII)
    outcome VARCHAR(50) NOT NULL,  -- SUCCESS, FAILURE, DENIED
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    details JSONB,                 -- Additional context (NO PII)
    created_at TIMESTAMP NOT NULL
);

-- Index for efficient queries
CREATE INDEX idx_audit_user_time ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_action_time ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id, created_at DESC);
```

**What to Log:**
 Login attempts (success/failure)
 Need created/claimed/updated
 Full need details accessed
 Organization verification changes
 User role changes
 Suspicious activity flags
 Rate limit violations

**What NOT to Log:**
✗ Passwords (hashed or plaintext)
✗ Encryption keys
✗ Decrypted PII
✗ JWT tokens (full value)
✗ Sensitive beneficiary information

---

## NIST SP 800-53 Controls Mapping (High-Level)

| Control Family | CrisisConnect Implementation |
|----------------|------------------------------|
| **AC (Access Control)** | RBAC with 4 roles, method-level security, org verification gates |
| **AU (Audit & Accountability)** | Comprehensive audit logging for all sensitive actions |
| **AT (Awareness & Training)** | User training materials on safe data handling (docs) |
| **CM (Configuration Management)** | Environment-based config, no secrets in code |
| **IA (Identification & Authentication)** | JWT-based auth, BCrypt password hashing, MFA (future) |
| **IR (Incident Response)** | Security disclosure process, admin alert dashboard |
| **PE (Physical & Environmental)** | Out of scope (assumes cloud/hosted infrastructure) |
| **PL (Planning)** | Threat model (this doc), security specs |
| **RA (Risk Assessment)** | Threat modeling (STRIDE), safeguarding scenarios |
| **SA (System & Services Acquisition)** | Dependency scanning (future), secure dev practices |
| **SC (System & Communications Protection)** | TLS/HTTPS, encrypted DB connections, AES-256 for PII |
| **SI (System & Information Integrity)** | Input validation, rate limiting, error handling |

---

## Secrets Management

### Environment Variables Required

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=crisisconnect
DB_USERNAME=crisisconnect
DB_PASSWORD=CHANGE_ME_32_CHARS_MINIMUM

# JWT
JWT_SECRET=CHANGE_ME_MINIMUM_32_CHARACTERS_RANDOM

# Encryption (32 bytes = 64 hex characters)
ENCRYPTION_SECRET=CHANGE_ME_64_HEX_CHARS_FOR_AES256

# CORS
CORS_ALLOWED_ORIGINS=https://crisisconnect.org,https://app.crisisconnect.org

# Admin Bootstrap (disable after first setup)
ADMIN_BOOTSTRAP_ENABLED=false
ADMIN_EMAIL=admin@crisisconnect.org
ADMIN_PASSWORD=CHANGE_ME_STRONG_PASSWORD
```

### Secret Generation Commands

```bash
# Generate JWT secret (32 chars)
openssl rand -base64 32

# Generate encryption key (32 bytes = 64 hex chars)
openssl rand -hex 32

# Generate strong password
openssl rand -base64 24
```

---

## Security Testing Requirements

### Automated Security Tests

1. **RBAC Tests:**
   - Each role's access to each endpoint
   - Organization verification enforcement
   - Self-role-modification prevention

2. **Injection Tests:**
   - SQL injection attempts (parameterized query validation)
   - XSS payload in input fields
   - Path traversal attempts

3. **Privacy Tests:**
   - Redacted responses never contain PII
   - Full responses only for authorized users
   - Audit logs for all full access

4. **Rate Limiting Tests:**
   - Login rate limit enforcement
   - API rate limit enforcement

5. **Encryption Tests:**
   - Encryption/decryption roundtrip
   - Tampered ciphertext detection (GCM auth tag)
   - Wrong key failure

### Manual Security Review

- [ ] Dependency vulnerability scan (OWASP Dependency-Check, Snyk)
- [ ] Code review for hardcoded secrets
- [ ] Review all @PreAuthorize annotations
- [ ] Verify no PII in logs (grep logs for common PII patterns)
- [ ] Penetration testing (external if possible)

---

## Deployment Security Checklist

- [ ] All secrets changed from defaults
- [ ] HTTPS/TLS enabled via reverse proxy
- [ ] Database on private network (not publicly accessible)
- [ ] CORS restricted to production domain only
- [ ] Admin bootstrap disabled (`ADMIN_BOOTSTRAP_ENABLED=false`)
- [ ] Firewall rules configured (only ports 80/443 public)
- [ ] Automated encrypted backups configured
- [ ] Monitoring and alerting set up
- [ ] Security disclosure contact published
- [ ] Dependency vulnerabilities patched

---

**Security is a continuous process, not a one-time implementation.**
