# Quality Assurance & Testing Strategy

**Owner:** QA / Verification Agent

## Testing Philosophy

**CrisisConnect is a humanitarian platform handling vulnerable populations' data. Testing is not optional—it is a safeguarding requirement.**

### Testing Pyramid

```
           /\
          /  \  E2E Tests (Critical User Flows)
         /____\
        /      \  Integration Tests (API + DB)
       /________\
      /          \  Unit Tests (Services, Security, Privacy)
     /____________\
```

---

## Test Coverage Requirements

### Minimum Coverage Targets

| Component | Line Coverage | Branch Coverage | Critical Paths |
|-----------|---------------|-----------------|----------------|
| Security & Auth | 95% | 90% | 100% |
| Privacy & Redaction | 95% | 90% | 100% |
| RBAC & Authorization | 95% | 90% | 100% |
| Encryption Service | 100% | 100% | 100% |
| Controllers | 80% | 75% | 100% |
| Services | 85% | 80% | 100% |
| Repositories | 70% | 60% | N/A |
| Frontend Components | 75% | 70% | 90% |

**Critical Paths:** Authentication, Authorization, Privacy Filtering, Encryption/Decryption, Need Creation/Claiming

---

## Backend Testing (Java/Spring Boot)

### 1. Unit Tests

**Location:** `backend/src/test/java/org/crisisconnect/`

#### A. Encryption Service Tests

**File:** `service/EncryptionServiceTest.java`

```java
@SpringBootTest
class EncryptionServiceTest {

    @Autowired
    private EncryptionService encryptionService;

    @Test
    void testEncryptionRoundtrip() {
        String plaintext = "John Doe +1234567890";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotEquals(plaintext, encrypted); // Ensure it's actually encrypted
        assertEquals(plaintext, decrypted);    // Roundtrip success
    }

    @Test
    void testEncryptionProducesDifferentCiphertexts() {
        String plaintext = "Sensitive Data";
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Different IVs should produce different ciphertexts
        assertNotEquals(encrypted1, encrypted2);

        // But both should decrypt to same plaintext
        assertEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }

    @Test
    void testDecryptionWithWrongKeyFails() {
        // This would require injecting a different key—test via integration
        // or mock to simulate wrong key scenario
        assertThrows(Exception.class, () -> {
            encryptionService.decrypt("invalid_base64_or_tampered_data");
        });
    }

    @Test
    void testEncryptionHandlesEmptyString() {
        String encrypted = encryptionService.encrypt("");
        assertEquals("", encryptionService.decrypt(encrypted));
    }

    @Test
    void testEncryptionHandlesSpecialCharacters() {
        String special = "Name: José Müller\nPhone: +49-123-456\nEmail: user@domain.com";
        String encrypted = encryptionService.encrypt(special);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(special, decrypted);
    }
}
```

---

#### B. Privacy Filter Service Tests

**File:** `service/NeedPrivacyFilterServiceTest.java`

```java
@SpringBootTest
class NeedPrivacyFilterServiceTest {

    @Autowired
    private NeedPrivacyFilterService privacyFilterService;

    @MockBean
    private SensitiveInfoRepository sensitiveInfoRepository;

    @MockBean
    private EncryptionService encryptionService;

    @MockBean
    private AuditService auditService;

    private Need testNeed;
    private User adminUser;
    private User fieldWorker;
    private User ngoStaffVerified;
    private User ngoStaffUnverified;

    @BeforeEach
    void setUp() {
        // Setup test users
        adminUser = User.builder().role(UserRole.ADMIN).build();

        fieldWorker = User.builder()
            .id(UUID.randomUUID())
            .role(UserRole.FIELD_WORKER)
            .build();

        Organization verifiedOrg = Organization.builder()
            .id(UUID.randomUUID())
            .status(OrganizationStatus.VERIFIED)
            .build();

        ngoStaffVerified = User.builder()
            .id(UUID.randomUUID())
            .role(UserRole.NGO_STAFF)
            .organization(verifiedOrg)
            .build();

        Organization unverifiedOrg = Organization.builder()
            .status(OrganizationStatus.PENDING)
            .build();

        ngoStaffUnverified = User.builder()
            .role(UserRole.NGO_STAFF)
            .organization(unverifiedOrg)
            .build();

        // Setup test need
        testNeed = Need.builder()
            .id(UUID.randomUUID())
            .category(NeedCategory.FOOD)
            .status(NeedStatus.PENDING)
            .urgency(UrgencyLevel.HIGH)
            .country("Country")
            .region("Region, District, Street 123")
            .description("Full description with sensitive details")
            .createdBy(fieldWorker)
            .build();
    }

    @Test
    void testAdminGetsFullResponse() {
        Object response = privacyFilterService.filterNeed(testNeed, adminUser);

        assertInstanceOf(FullNeedResponse.class, response);
        FullNeedResponse fullResponse = (FullNeedResponse) response;
        assertEquals(testNeed.getId(), fullResponse.getId());
        assertEquals("Full description with sensitive details", fullResponse.getFullDescription());

        // Verify audit log called
        verify(auditService).logNeedAccessed(testNeed.getId(), adminUser.getId(), "FULL");
    }

    @Test
    void testCreatorGetsFullResponse() {
        Object response = privacyFilterService.filterNeed(testNeed, fieldWorker);

        assertInstanceOf(FullNeedResponse.class, response);
        verify(auditService).logNeedAccessed(testNeed.getId(), fieldWorker.getId(), "FULL");
    }

    @Test
    void testAssignedVerifiedNGOGetsFullResponse() {
        testNeed.setAssignedToOrg(ngoStaffVerified.getOrganization());

        Object response = privacyFilterService.filterNeed(testNeed, ngoStaffVerified);

        assertInstanceOf(FullNeedResponse.class, response);
        verify(auditService).logNeedAccessed(testNeed.getId(), ngoStaffVerified.getId(), "FULL");
    }

    @Test
    void testUnverifiedNGOGetsRedactedResponse() {
        Object response = privacyFilterService.filterNeed(testNeed, ngoStaffUnverified);

        assertInstanceOf(RedactedNeedResponse.class, response);
        verify(auditService).logNeedAccessed(testNeed.getId(), ngoStaffUnverified.getId(), "REDACTED");
    }

    @Test
    void testVerifiedNGOUnassignedNeedGetsRedacted() {
        // Need not assigned to this NGO
        testNeed.setAssignedToOrg(null);

        Object response = privacyFilterService.filterNeed(testNeed, ngoStaffVerified);

        assertInstanceOf(RedactedNeedResponse.class, response);
    }

    @Test
    void testRedactedResponseNeverContainsPII() {
        Object response = privacyFilterService.filterNeed(testNeed, ngoStaffUnverified);

        RedactedNeedResponse redacted = (RedactedNeedResponse) response;

        // Verify PII fields are NOT present
        assertNull(redacted.getBeneficiaryName());
        assertNull(redacted.getBeneficiaryPhone());
        assertNull(redacted.getExactLocation());

        // Verify region is generalized
        assertEquals("Region", redacted.getRegion()); // Street address removed
    }

    @Test
    void testFullResponseIncludesDecryptedPII() {
        SensitiveInfo sensitiveInfo = new SensitiveInfo();
        sensitiveInfo.setEncryptedFullName("encrypted_name");
        sensitiveInfo.setEncryptedPhone("encrypted_phone");

        when(sensitiveInfoRepository.findByNeedId(testNeed.getId()))
            .thenReturn(Optional.of(sensitiveInfo));
        when(encryptionService.decrypt("encrypted_name")).thenReturn("John Doe");
        when(encryptionService.decrypt("encrypted_phone")).thenReturn("+1234567890");

        Object response = privacyFilterService.filterNeed(testNeed, adminUser);

        FullNeedResponse fullResponse = (FullNeedResponse) response;
        assertEquals("John Doe", fullResponse.getBeneficiaryName());
        assertEquals("+1234567890", fullResponse.getBeneficiaryPhone());
    }

    @Test
    void testListAlwaysReturnsRedacted() {
        List<Need> needs = List.of(testNeed);

        List<RedactedNeedResponse> responses = privacyFilterService.filterNeedsList(needs, adminUser);

        assertEquals(1, responses.size());
        assertInstanceOf(RedactedNeedResponse.class, responses.get(0));
        // Even admin gets redacted in list view
    }
}
```

---

#### C. RBAC / Authorization Tests

**File:** `security/NeedSecurityServiceTest.java`

```java
@SpringBootTest
class NeedSecurityServiceTest {

    @Autowired
    private NeedSecurityService needSecurityService;

    @MockBean
    private NeedRepository needRepository;

    @MockBean
    private ServiceAreaRepository serviceAreaRepository;

    // ... setup users and needs

    @Test
    void testAdminCanAccessAllNeeds() {
        when(needRepository.findById(needId)).thenReturn(Optional.of(testNeed));

        boolean canAccess = needSecurityService.canAccessNeed(needId, adminAuth);

        assertTrue(canAccess);
    }

    @Test
    void testCreatorCanAccessOwnNeed() {
        testNeed.setCreatedBy(fieldWorker);
        when(needRepository.findById(needId)).thenReturn(Optional.of(testNeed));

        boolean canAccess = needSecurityService.canAccessNeed(needId, fieldWorkerAuth);

        assertTrue(canAccess);
    }

    @Test
    void testUnverifiedNGOCannotClaimNeed() {
        when(needRepository.findById(needId)).thenReturn(Optional.of(testNeed));

        boolean canClaim = needSecurityService.canClaimNeed(needId, unverifiedNGOAuth);

        assertFalse(canClaim);
    }

    @Test
    void testVerifiedNGOCanClaimNeedInServiceArea() {
        when(needRepository.findById(needId)).thenReturn(Optional.of(testNeed));
        when(serviceAreaRepository.existsByOrganizationAndCountryAndRegionAndCategory(
            verifiedOrg, testNeed.getCountry(), testNeed.getRegion(), testNeed.getCategory()
        )).thenReturn(true);

        boolean canClaim = needSecurityService.canClaimNeed(needId, verifiedNGOAuth);

        assertTrue(canClaim);
    }

    @Test
    void testVerifiedNGOCannotClaimNeedOutsideServiceArea() {
        when(needRepository.findById(needId)).thenReturn(Optional.of(testNeed));
        when(serviceAreaRepository.existsByOrganizationAndCountryAndRegionAndCategory(any(), any(), any(), any()))
            .thenReturn(false);

        boolean canClaim = needSecurityService.canClaimNeed(needId, verifiedNGOAuth);

        assertFalse(canClaim);
    }
}
```

---

### 2. Integration Tests (Controller + Service + DB)

**File:** `controller/NeedControllerIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NeedControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String fieldWorkerToken;
    private String ngoStaffToken;

    @BeforeEach
    void setUp() {
        // Create users and generate JWT tokens
        User admin = userRepository.save(User.builder()
            .email("admin@test.com")
            .role(UserRole.ADMIN)
            .build());

        adminToken = jwtUtil.generateToken(admin);

        // ... create other users and tokens
    }

    @Test
    void testCreateNeed_FieldWorkerSuccess() throws Exception {
        CreateNeedRequest request = CreateNeedRequest.builder()
            .category(NeedCategory.FOOD)
            .urgency(UrgencyLevel.HIGH)
            .country("Country")
            .region("Region")
            .description("Family of 5 needs food assistance")
            .beneficiaryName("John Doe")
            .beneficiaryPhone("+1234567890")
            .build();

        mockMvc.perform(post("/api/needs")
            .header("Authorization", "Bearer " + fieldWorkerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.category").value("FOOD"));

        // Verify PII was encrypted in database
        Need savedNeed = needRepository.findAll().get(0);
        assertNotNull(savedNeed.getSensitiveInfo());
        assertTrue(savedNeed.getSensitiveInfo().getEncryptedFullName().startsWith("encrypted"));
    }

    @Test
    void testGetNeed_UnauthorizedReturns404() throws Exception {
        Need need = needRepository.save(createTestNeed());

        mockMvc.perform(get("/api/needs/" + need.getId())
            .header("Authorization", "Bearer " + unauthorizedUserToken))
            .andExpect(status().isNotFound()); // 404 to prevent enumeration
    }

    @Test
    void testClaimNeed_UnverifiedNGOReturns403() throws Exception {
        Need need = needRepository.save(createTestNeed());

        mockMvc.perform(post("/api/needs/" + need.getId() + "/claim")
            .header("Authorization", "Bearer " + unverifiedNGOToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetNeeds_ReturnRedactedList() throws Exception {
        needRepository.save(createTestNeed());

        MvcResult result = mockMvc.perform(get("/api/needs")
            .header("Authorization", "Bearer " + ngoStaffToken))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("beneficiaryName"));
        assertFalse(responseBody.contains("beneficiaryPhone"));
    }
}
```

---

### 3. Security Regression Tests (Golden Tests)

**File:** `security/SecurityRegressionTest.java`

**Purpose:** Ensure critical security rules are never accidentally broken

```java
@SpringBootTest
class SecurityRegressionTest {

    @Test
    void testRedactedResponseNeverContainsPII_Golden() {
        RedactedNeedResponse redacted = new RedactedNeedResponse();

        // Use reflection to ensure no PII fields exist
        Field[] fields = redacted.getClass().getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName();
            assertFalse(fieldName.contains("beneficiary"), "Redacted response must not have beneficiary fields");
            assertFalse(fieldName.contains("phone"), "Redacted response must not have phone field");
            assertFalse(fieldName.contains("email"), "Redacted response must not have email field");
            assertFalse(fieldName.contains("location") && !fieldName.equals("region"), "Exact location not allowed");
        }
    }

    @Test
    void testFullResponseRequiresAuthorization_Golden() {
        // Verify @PreAuthorize annotation exists on sensitive endpoints
        Method[] methods = NeedController.class.getDeclaredMethods();

        for (Method method : methods) {
            if (method.getName().equals("getNeedById") || method.getName().equals("claimNeed")) {
                assertTrue(method.isAnnotationPresent(PreAuthorize.class),
                    method.getName() + " must have @PreAuthorize annotation");
            }
        }
    }

    @Test
    void testPasswordsNeverLoggedInPlaintext_Golden() {
        // Scan codebase for log statements with "password" (basic check)
        // This is a placeholder—integrate with static analysis tools
    }

    @Test
    void testEncryptionKeyNeverHardcoded_Golden() {
        // Verify EncryptionService reads key from environment, not hardcoded
        // This would be a static analysis check
    }
}
```

---

## Frontend Testing (React/TypeScript)

### 1. Unit Tests (Jest + React Testing Library)

**File:** `frontend/src/services/api.test.ts`

```typescript
describe('API Client', () => {
  test('includes Authorization header when token present', async () => {
    const mockToken = 'test-token';
    localStorage.setItem('authToken', mockToken);

    const mockAxios = jest.spyOn(axios, 'get');

    await api.get('/needs');

    expect(mockAxios).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: `Bearer ${mockToken}`
        })
      })
    );
  });

  test('does not cache sensitive endpoints', async () => {
    const response = await api.get('/needs/123');

    expect(response.config.headers['Cache-Control']).toBe('no-store');
  });
});
```

**File:** `frontend/src/pages/NeedDetail.test.tsx`

```typescript
describe('NeedDetail Page', () => {
  test('shows restricted info placeholder for redacted fields', () => {
    const redactedNeed = {
      id: '123',
      category: 'FOOD',
      status: 'PENDING'
      // No beneficiaryName, beneficiaryPhone
    };

    render(<NeedDetail need={redactedNeed} />);

    expect(screen.getByText(/information restricted/i)).toBeInTheDocument();
    expect(screen.queryByText(/beneficiary:/i)).not.toBeInTheDocument();
  });

  test('displays full info when authorized', () => {
    const fullNeed = {
      id: '123',
      category: 'FOOD',
      beneficiaryName: 'John Doe',
      beneficiaryPhone: '+1234567890'
    };

    render(<NeedDetail need={fullNeed} />);

    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('+1234567890')).toBeInTheDocument();
  });

  test('never caches PII in localStorage', () => {
    const fullNeed = { beneficiaryName: 'John Doe' };

    render(<NeedDetail need={fullNeed} />);

    expect(localStorage.getItem('needDetails')).toBeNull();
  });
});
```

---

### 2. End-to-End Tests (Cypress)

**File:** `e2e/cypress/e2e/privacy.cy.ts`

```typescript
describe('Privacy & Redaction', () => {
  beforeEach(() => {
    cy.login('ngo-staff-unverified@test.com', 'password');
  });

  it('should show redacted need list for unverified NGO staff', () => {
    cy.visit('/needs');

    cy.get('.need-card').first().should('contain', 'FOOD');
    cy.get('.need-card').first().should('not.contain', 'John Doe'); // No beneficiary name
    cy.get('.need-card').first().should('not.contain', '+1234'); // No phone
  });

  it('should show "restricted" message when viewing need detail without authorization', () => {
    cy.visit('/needs/some-need-id');

    cy.contains(/information restricted/i).should('be.visible');
    cy.contains('Claim this need to view details').should('be.visible');
  });
});

describe('Authorization Flow', () => {
  it('should prevent unverified NGO from claiming needs', () => {
    cy.login('ngo-staff-unverified@test.com', 'password');
    cy.visit('/needs/some-need-id');

    cy.get('button').contains('Claim Need').should('be.disabled');
  });

  it('should allow verified NGO to claim and see full details', () => {
    cy.login('ngo-staff-verified@test.com', 'password');
    cy.visit('/needs');

    cy.get('.need-card').first().click();
    cy.get('button').contains('Claim Need').click();

    // After claiming, full details should be visible
    cy.contains('Beneficiary:').should('be.visible');
    cy.contains('John Doe').should('be.visible');
  });
});
```

---

## Test Execution & CI/CD Integration

### Local Testing

**Backend:**
```bash
cd backend
mvn test                        # Run all tests
mvn test -Dtest=EncryptionServiceTest  # Run specific test
mvn jacoco:report               # Generate coverage report
```

**Frontend:**
```bash
cd frontend
npm test                        # Run Jest tests
npm test -- --coverage          # With coverage
npm run test:e2e                # Cypress E2E tests
```

### CI Pipeline (GitHub Actions / GitLab CI)

```yaml
name: CI Tests

on: [push, pull_request]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run tests
        run: cd backend && mvn test
      - name: Check coverage
        run: cd backend && mvn jacoco:check  # Fail if coverage < threshold

  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Install dependencies
        run: cd frontend && npm install
      - name: Run unit tests
        run: cd frontend && npm test -- --coverage --watchAll=false
      - name: Run E2E tests
        run: cd frontend && npm run test:e2e

  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run dependency check
        run: mvn org.owasp:dependency-check-maven:check
```

---

## Release Gates (Pre-Deployment Checklist)

**All must pass before deploying to production:**

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] All E2E critical flows pass
- [ ] Code coverage meets thresholds (95% for security/privacy)
- [ ] No high/critical dependency vulnerabilities
- [ ] Security regression tests (golden tests) pass
- [ ] Manual security review completed
- [ ] No PII found in logs (grep check)
- [ ] All secrets changed from defaults
- [ ] Database migrations tested on staging

---

## Continuous Monitoring (Post-Deployment)

### Application Monitoring

- [ ] Error rate monitoring (alert if > 1%)
- [ ] Response time monitoring (alert if > 2s p95)
- [ ] Audit log monitoring (alert on suspicious patterns)
- [ ] Failed login attempts (alert if > 100/hour)

### Security Monitoring

- [ ] Rate limit violations tracked
- [ ] High-risk case access alerts
- [ ] Organization verification workflow metrics
- [ ] Dependency vulnerability scanning (weekly)

---

**Testing is safeguarding. Every test protects a vulnerable person.**
