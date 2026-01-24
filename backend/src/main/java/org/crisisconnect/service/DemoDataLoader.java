package org.crisisconnect.service;

import jakarta.annotation.PostConstruct;
import org.crisisconnect.model.entity.*;
import org.crisisconnect.model.enums.*;
import org.crisisconnect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Demo Data Loader Service
 * Populates the database with realistic mock data for demonstration purposes.
 * Only runs in demo profile.
 */
@Service
@Profile("demo")
public class DemoDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataLoader.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ServiceAreaRepository serviceAreaRepository;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private SensitiveInfoRepository sensitiveInfoRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EncryptionService encryptionService;

    @Value("${demo.data.enabled:true}")
    private Boolean demoDataEnabled;

    private final Map<String, UUID> orgIds = new HashMap<>();
    private final Map<String, UUID> userIds = new HashMap<>();

    @PostConstruct
    @Transactional
    public void init() {
        if (!demoDataEnabled) {
            logger.info("Demo data loading disabled");
            return;
        }

        // Check if demo data already exists
        if (organizationRepository.count() > 0) {
            logger.info("Demo data already exists, skipping initialization");
            return;
        }

        try {
            logger.info("========================================");
            logger.info("Loading Demo Data...");
            logger.info("========================================");

            createOrganizations();
            createUsers();
            createServiceAreas();
            createNeeds();
            createAuditLogs();

            logger.info("========================================");
            logger.info("Demo Data Loaded Successfully!");
            logger.info("========================================");
            logDemoCredentials();
        } catch (Exception e) {
            logger.error("Failed to load demo data", e);
        }
    }

    private void createOrganizations() {
        logger.info("Creating organizations...");

        // Verified NGOs
        Organization redCross = createOrganization(
                "International Red Cross",
                OrganizationType.NGO,
                "Syria",
                OrganizationStatus.VERIFIED,
                "https://www.icrc.org",
                "+41-22-734-6001"
        );
        orgIds.put("redCross", redCross.getId());

        Organization msf = createOrganization(
                "Médecins Sans Frontières",
                OrganizationType.NGO,
                "Lebanon",
                OrganizationStatus.VERIFIED,
                "https://www.msf.org",
                "+41-22-849-8400"
        );
        orgIds.put("msf", msf.getId());

        Organization saveChildren = createOrganization(
                "Save the Children",
                OrganizationType.NGO,
                "Jordan",
                OrganizationStatus.VERIFIED,
                "https://www.savethechildren.org",
                "+1-800-728-3843"
        );
        orgIds.put("saveChildren", saveChildren.getId());

        // UN Agencies
        Organization unhcr = createOrganization(
                "UNHCR - UN Refugee Agency",
                OrganizationType.UN_AGENCY,
                "Turkey",
                OrganizationStatus.VERIFIED,
                "https://www.unhcr.org",
                "+41-22-739-8111"
        );
        orgIds.put("unhcr", unhcr.getId());

        Organization wfp = createOrganization(
                "World Food Programme",
                OrganizationType.UN_AGENCY,
                "Syria",
                OrganizationStatus.VERIFIED,
                "https://www.wfp.org",
                "+39-06-6513-2000"
        );
        orgIds.put("wfp", wfp.getId());

        // Local Groups
        Organization localAid = createOrganization(
                "Local Aid Network Lebanon",
                OrganizationType.LOCAL_GROUP,
                "Lebanon",
                OrganizationStatus.VERIFIED,
                "https://localaid-lb.org",
                "+961-1-123456"
        );
        orgIds.put("localAid", localAid.getId());

        // Pending Organizations
        Organization newNGO = createOrganization(
                "Hope Foundation",
                OrganizationType.NGO,
                "Jordan",
                OrganizationStatus.PENDING,
                "https://hopefoundation.org",
                "+962-6-123456"
        );
        orgIds.put("newNGO", newNGO.getId());

        // Suspended Organization
        Organization suspended = createOrganization(
                "Suspended Aid Group",
                OrganizationType.NGO,
                "Syria",
                OrganizationStatus.SUSPENDED,
                null,
                null
        );
        orgIds.put("suspended", suspended.getId());

        logger.info("Created {} organizations", organizationRepository.count());
    }

    private Organization createOrganization(String name, OrganizationType type, String country,
                                           OrganizationStatus status, String website, String phone) {
        Organization org = new Organization();
        org.setName(name);
        org.setType(type);
        org.setCountry(country);
        org.setStatus(status);
        org.setWebsiteUrl(website);
        org.setPhone(phone);
        return organizationRepository.save(org);
    }

    private void createUsers() {
        logger.info("Creating users...");

        // Field Workers (NIST-compliant passwords)
        User fieldWorker1 = createUser(
                "Sarah Johnson",
                "fieldworker1@crisisconnect.org",
                "Field2026!Worker",
                UserRole.FIELD_WORKER,
                null,
                true
        );
        userIds.put("fieldWorker1", fieldWorker1.getId());

        User fieldWorker2 = createUser(
                "Ahmed Hassan",
                "fieldworker2@crisisconnect.org",
                "Field2026!Helper",
                UserRole.FIELD_WORKER,
                null,
                true
        );
        userIds.put("fieldWorker2", fieldWorker2.getId());

        // NGO Staff Members (NIST-compliant passwords)
        User ngoStaff1 = createUser(
                "Maria Garcia",
                "maria.garcia@redcross.org",
                "RedCross2026!Staff",
                UserRole.NGO_STAFF,
                orgIds.get("redCross"),
                true
        );
        userIds.put("ngoStaff1", ngoStaff1.getId());

        User ngoStaff2 = createUser(
                "Dr. Jean-Pierre Dubois",
                "jp.dubois@msf.org",
                "MSF2026!Doctor",
                UserRole.NGO_STAFF,
                orgIds.get("msf"),
                true
        );
        userIds.put("ngoStaff2", ngoStaff2.getId());

        User ngoStaff3 = createUser(
                "Emily Watson",
                "emily.watson@savechildren.org",
                "SaveKids2026!NGO",
                UserRole.NGO_STAFF,
                orgIds.get("saveChildren"),
                true
        );
        userIds.put("ngoStaff3", ngoStaff3.getId());

        User unStaff1 = createUser(
                "Mohammad Al-Rashid",
                "m.alrashid@unhcr.org",
                "UNHCR2026!Refugee",
                UserRole.NGO_STAFF,
                orgIds.get("unhcr"),
                true
        );
        userIds.put("unStaff1", unStaff1.getId());

        User localStaff = createUser(
                "Layla Ibrahim",
                "layla@localaid-lb.org",
                "LocalAid2026!Help",
                UserRole.NGO_STAFF,
                orgIds.get("localAid"),
                true
        );
        userIds.put("localStaff", localStaff.getId());

        // Beneficiaries (NIST-compliant passwords)
        User beneficiary1 = createUser(
                "Anonymous Beneficiary 1",
                "beneficiary1@temp.org",
                "Beneficiary2026!One",
                UserRole.BENEFICIARY,
                null,
                true
        );
        userIds.put("beneficiary1", beneficiary1.getId());

        User beneficiary2 = createUser(
                "Anonymous Beneficiary 2",
                "beneficiary2@temp.org",
                "Beneficiary2026!Two",
                UserRole.BENEFICIARY,
                null,
                true
        );
        userIds.put("beneficiary2", beneficiary2.getId());

        // Inactive user (NIST-compliant password)
        User inactiveUser = createUser(
                "Inactive User",
                "inactive@crisisconnect.org",
                "Inactive2026!User",
                UserRole.FIELD_WORKER,
                null,
                false
        );
        userIds.put("inactive", inactiveUser.getId());

        logger.info("Created {} users (excluding admin)", userRepository.count() - 1);
    }

    private User createUser(String name, String email, String password, UserRole role,
                           UUID organizationId, boolean isActive) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setOrganizationId(organizationId);
        user.setIsActive(isActive);
        return userRepository.save(user);
    }

    private void createServiceAreas() {
        logger.info("Creating service areas...");

        // Red Cross - Multiple categories in Syria
        createServiceArea(orgIds.get("redCross"), "Syria", "Aleppo", null,
                Set.of(NeedCategory.MEDICAL, NeedCategory.FOOD, NeedCategory.SHELTER));
        createServiceArea(orgIds.get("redCross"), "Syria", "Damascus", null,
                Set.of(NeedCategory.MEDICAL, NeedCategory.FOOD));

        // MSF - Medical focus in Lebanon
        createServiceArea(orgIds.get("msf"), "Lebanon", "Beirut", null,
                Set.of(NeedCategory.MEDICAL));
        createServiceArea(orgIds.get("msf"), "Lebanon", "Tripoli", null,
                Set.of(NeedCategory.MEDICAL));

        // Save the Children - Child-focused services in Jordan
        createServiceArea(orgIds.get("saveChildren"), "Jordan", "Amman", null,
                Set.of(NeedCategory.FOOD, NeedCategory.SHELTER, NeedCategory.MEDICAL));

        // UNHCR - Broad refugee support in Turkey
        createServiceArea(orgIds.get("unhcr"), "Turkey", null, null,
                Set.of(NeedCategory.LEGAL, NeedCategory.DOCUMENTS, NeedCategory.SHELTER));

        // WFP - Food assistance in Syria
        createServiceArea(orgIds.get("wfp"), "Syria", null, null,
                Set.of(NeedCategory.FOOD));

        // Local Aid - Multiple services in Lebanon
        createServiceArea(orgIds.get("localAid"), "Lebanon", null, null,
                Set.of(NeedCategory.FOOD, NeedCategory.SHELTER, NeedCategory.OTHER));

        logger.info("Created {} service areas", serviceAreaRepository.count());
    }

    private void createServiceArea(UUID orgId, String country, String region, String city,
                                   Set<NeedCategory> categories) {
        ServiceArea area = new ServiceArea();
        area.setOrganizationId(orgId);
        area.setCountry(country);
        area.setRegionOrState(region);
        area.setCity(city);
        area.setServiceCategories(categories);
        serviceAreaRepository.save(area);
    }

    private void createNeeds() {
        logger.info("Creating needs...");

        // New needs (unassigned)
        createNeed(
                userIds.get("fieldWorker1"),
                NeedStatus.NEW,
                NeedCategory.MEDICAL,
                "Syria",
                "Aleppo",
                "Aleppo City",
                UrgencyLevel.HIGH,
                null,
                null,
                null,
                "Fatima Al-Sayed",
                "+963-123-456789",
                "fatima@temp.org",
                "Al-Shahba District, Building 15, Apt 3",
                "Patient requires urgent diabetes medication and insulin. Has been without treatment for 2 weeks."
        );

        createNeed(
                userIds.get("fieldWorker2"),
                NeedStatus.NEW,
                NeedCategory.FOOD,
                "Lebanon",
                "Beirut",
                "Beirut",
                UrgencyLevel.MEDIUM,
                null,
                null,
                null,
                "Hassan Khalil",
                "+961-456-789012",
                "hkhalil@temp.org",
                "Hamra Street, near AUB",
                "Family of 5 needs food assistance. Lost employment due to economic crisis."
        );

        createNeed(
                userIds.get("fieldWorker1"),
                NeedStatus.NEW,
                NeedCategory.SHELTER,
                "Jordan",
                "Amman",
                "Zarqa",
                UrgencyLevel.HIGH,
                null,
                null,
                null,
                "Aisha Mohammad",
                "+962-789-123456",
                null,
                "Zarqa Refugee Camp, Sector C, Tent 45",
                "Family of 7 living in damaged tent. Urgent need for new shelter before winter."
        );

        // In progress needs (assigned)
        createNeed(
                userIds.get("fieldWorker2"),
                NeedStatus.IN_PROGRESS,
                NeedCategory.MEDICAL,
                "Lebanon",
                "Tripoli",
                "Tripoli",
                UrgencyLevel.HIGH,
                orgIds.get("msf"),
                LocalDateTime.now().minusDays(2),
                null,
                "Youssef Ibrahim",
                "+961-789-456123",
                null,
                "Bab el-Tabbaneh, Street 7",
                "Child with severe malnutrition requiring immediate medical care. MSF team assigned."
        );

        createNeed(
                userIds.get("fieldWorker1"),
                NeedStatus.IN_PROGRESS,
                NeedCategory.FOOD,
                "Syria",
                "Damascus",
                "Damascus",
                UrgencyLevel.MEDIUM,
                orgIds.get("wfp"),
                LocalDateTime.now().minusDays(5),
                null,
                "Mariam Haddad",
                "+963-987-654321",
                "mariam.h@temp.org",
                "Mezzeh District, Building 8",
                "Elderly woman needs regular food supplies. WFP coordinating monthly deliveries."
        );

        // Resolved needs
        createNeed(
                userIds.get("fieldWorker2"),
                NeedStatus.RESOLVED,
                NeedCategory.DOCUMENTS,
                "Turkey",
                "Istanbul",
                "Istanbul",
                UrgencyLevel.MEDIUM,
                orgIds.get("unhcr"),
                LocalDateTime.now().minusDays(15),
                LocalDateTime.now().minusDays(2),
                "Omar Abdullah",
                "+90-555-123456",
                "omar.a@temp.org",
                "Fatih District",
                "Refugee documentation process completed. UNHCR provided necessary paperwork."
        );

        createNeed(
                userIds.get("fieldWorker1"),
                NeedStatus.RESOLVED,
                NeedCategory.SHELTER,
                "Jordan",
                "Amman",
                "Amman",
                UrgencyLevel.HIGH,
                orgIds.get("saveChildren"),
                LocalDateTime.now().minusDays(20),
                LocalDateTime.now().minusDays(5),
                "Zahra Hussein",
                "+962-777-987654",
                null,
                "Jabal Hussein",
                "Family relocated to safe housing. Save the Children provided temporary accommodation."
        );

        // Rejected need
        createNeed(
                userIds.get("fieldWorker2"),
                NeedStatus.REJECTED,
                NeedCategory.LEGAL,
                "Lebanon",
                "Beirut",
                "Beirut",
                UrgencyLevel.LOW,
                null,
                null,
                null,
                "Ali Najjar",
                "+961-333-444555",
                "ali.n@temp.org",
                "Achrafieh",
                "Legal assistance request rejected. Duplicate submission."
        );

        // Low urgency needs
        createNeed(
                userIds.get("fieldWorker1"),
                NeedStatus.NEW,
                NeedCategory.OTHER,
                "Syria",
                "Aleppo",
                "Aleppo City",
                UrgencyLevel.LOW,
                null,
                null,
                null,
                "Nour Saleh",
                "+963-222-333444",
                null,
                "Old City",
                "Request for educational materials for children. Non-urgent."
        );

        createNeed(
                userIds.get("fieldWorker2"),
                NeedStatus.NEW,
                NeedCategory.FOOD,
                "Turkey",
                "Gaziantep",
                "Gaziantep",
                UrgencyLevel.MEDIUM,
                null,
                null,
                null,
                "Leila Ahmed",
                "+90-555-987654",
                "leila@temp.org",
                "Sahinbey District",
                "Family of 4 needs food support. Currently managing with minimal resources."
        );

        logger.info("Created {} needs", needRepository.count());
    }

    private void createNeed(UUID createdBy, NeedStatus status, NeedCategory category,
                           String country, String region, String city, UrgencyLevel urgency,
                           UUID assignedOrgId, LocalDateTime assignedAt, LocalDateTime resolvedAt,
                           String fullName, String phone, String email, String location, String notes) {
        Need need = new Need();
        need.setCreatedByUserId(createdBy);
        need.setStatus(status);
        need.setCategory(category);
        need.setCountry(country);
        need.setRegionOrState(region);
        need.setCity(city);
        need.setUrgencyLevel(urgency);
        need.setAssignedOrganizationId(assignedOrgId);
        need.setAssignedAt(assignedAt);
        need.setResolvedAt(resolvedAt);

        if (status == NeedStatus.REJECTED || status == NeedStatus.RESOLVED) {
            need.setClosedAt(LocalDateTime.now().minusDays(1));
        }

        need = needRepository.save(need);

        // Create sensitive info
        SensitiveInfo sensitiveInfo = new SensitiveInfo();
        sensitiveInfo.setNeed(need);
        sensitiveInfo.setEncryptedFullName(encryptionService.encrypt(fullName));
        sensitiveInfo.setEncryptedPhone(encryptionService.encrypt(phone));
        sensitiveInfo.setEncryptedEmail(email != null ? encryptionService.encrypt(email) : null);
        sensitiveInfo.setEncryptedExactLocation(encryptionService.encrypt(location));
        sensitiveInfo.setEncryptedNotes(encryptionService.encrypt(notes));
        sensitiveInfoRepository.save(sensitiveInfo);
    }

    private void createAuditLogs() {
        logger.info("Creating audit logs...");

        // Get admin user
        User admin = userRepository.findByEmail("admin@crisisconnect.org").orElse(null);
        if (admin == null) return;

        // Organization verification logs
        createAuditLog(admin.getId(), "ORGANIZATION_VERIFIED",
                "organizations", orgIds.get("redCross"),
                "Verified organization: International Red Cross");

        createAuditLog(admin.getId(), "ORGANIZATION_VERIFIED",
                "organizations", orgIds.get("msf"),
                "Verified organization: Médecins Sans Frontières");

        createAuditLog(admin.getId(), "ORGANIZATION_SUSPENDED",
                "organizations", orgIds.get("suspended"),
                "Suspended organization: Suspended Aid Group - Failed verification");

        // User management logs
        createAuditLog(admin.getId(), "USER_CREATED",
                "users", userIds.get("ngoStaff1"),
                "Created NGO staff user: maria.garcia@redcross.org");

        createAuditLog(admin.getId(), "USER_DEACTIVATED",
                "users", userIds.get("inactive"),
                "Deactivated user: inactive@crisisconnect.org");

        // Access logs
        createAuditLog(userIds.get("ngoStaff1"), "SENSITIVE_DATA_ACCESSED",
                "needs", needRepository.findAll().get(0).getId(),
                "Accessed full need details for assigned case");

        logger.info("Created {} audit logs", auditLogRepository.count());
    }

    private void createAuditLog(UUID userId, String action, String entityType,
                                UUID entityId, String description) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setActionType(action);
        log.setTargetType(entityType);
        log.setTargetId(entityId);
        log.setMetadata(description);
        auditLogRepository.save(log);
    }

    private void logDemoCredentials() {
        logger.info("");
        logger.info("========================================");
        logger.info("DEMO USER CREDENTIALS (NIST-COMPLIANT)");
        logger.info("========================================");
        logger.info("");
        logger.info("ADMIN:");
        logger.info("  Email: admin@crisisconnect.org");
        logger.info("  Password: Admin2026!Secure");
        logger.info("  Note: 12+ chars, mixed case, numbers, special chars");
        logger.info("");
        logger.info("FIELD WORKERS:");
        logger.info("  Email: fieldworker1@crisisconnect.org");
        logger.info("  Password: Field2026!Worker");
        logger.info("");
        logger.info("  Email: fieldworker2@crisisconnect.org");
        logger.info("  Password: Field2026!Helper");
        logger.info("");
        logger.info("NGO STAFF:");
        logger.info("  Email: maria.garcia@redcross.org");
        logger.info("  Password: RedCross2026!Staff");
        logger.info("  Organization: International Red Cross");
        logger.info("");
        logger.info("  Email: jp.dubois@msf.org");
        logger.info("  Password: MSF2026!Doctor");
        logger.info("  Organization: Médecins Sans Frontières");
        logger.info("");
        logger.info("  Email: emily.watson@savechildren.org");
        logger.info("  Password: SaveKids2026!NGO");
        logger.info("  Organization: Save the Children");
        logger.info("");
        logger.info("  Email: m.alrashid@unhcr.org");
        logger.info("  Password: UNHCR2026!Refugee");
        logger.info("  Organization: UNHCR");
        logger.info("");
        logger.info("  Email: layla@localaid-lb.org");
        logger.info("  Password: LocalAid2026!Help");
        logger.info("  Organization: Local Aid Network");
        logger.info("");
        logger.info("BENEFICIARIES:");
        logger.info("  Email: beneficiary1@temp.org");
        logger.info("  Password: Beneficiary2026!One");
        logger.info("");
        logger.info("  Email: beneficiary2@temp.org");
        logger.info("  Password: Beneficiary2026!Two");
        logger.info("");
        logger.info("========================================");
        logger.info("All passwords meet NIST SP 800-63B requirements:");
        logger.info(" Minimum 12 characters");
        logger.info(" Uppercase and lowercase letters");
        logger.info(" Numbers");
        logger.info(" Special characters");
        logger.info("========================================");
        logger.info("");
    }
}
