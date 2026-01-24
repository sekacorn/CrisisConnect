package org.crisisconnect.service;

import org.crisisconnect.dto.*;
import org.crisisconnect.model.entity.AuditLog;
import org.crisisconnect.model.entity.Need;
import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.*;
import org.crisisconnect.model.enums.OrganizationType;
import org.crisisconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminService
 *
 * Tests cover:
 * - Dashboard statistics aggregation
 * - Organization verification workflow
 * - User management operations
 * - Audit log retrieval and filtering
 * - Suspicious activity detection
 * - Pagination and sorting
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private NeedRepository needRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminService adminService;

    private User testAdmin;
    private User testUser;
    private Organization testOrg;
    private Need testNeed;
    private AuditLog testAuditLog;
    private String testIpAddress = "192.168.1.100";

    @BeforeEach
    void setUp() {
        // Create test admin
        testAdmin = new User();
        testAdmin.setId(UUID.randomUUID());
        testAdmin.setEmail("admin@crisisconnect.org");
        testAdmin.setName("Test Admin");
        testAdmin.setRole(UserRole.ADMIN);
        testAdmin.setIsActive(true);
        testAdmin.setCreatedAt(LocalDateTime.now());

        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("user@test.com");
        testUser.setName("Test User");
        testUser.setRole(UserRole.NGO_STAFF);
        testUser.setIsActive(true);
        testUser.setOrganizationId(UUID.randomUUID());
        testUser.setCreatedAt(LocalDateTime.now());

        // Create test organization
        testOrg = new Organization();
        testOrg.setId(UUID.randomUUID());
        testOrg.setName("Test NGO");
        testOrg.setStatus(OrganizationStatus.PENDING);
        testOrg.setCountry("USA");
        testOrg.setType(OrganizationType.NGO);
        testOrg.setCreatedAt(LocalDateTime.now());

        // Create test need
        testNeed = new Need();
        testNeed.setId(UUID.randomUUID());
        testNeed.setCategory(NeedCategory.FOOD);
        testNeed.setStatus(NeedStatus.NEW);
        testNeed.setUrgencyLevel(UrgencyLevel.HIGH);
        testNeed.setCountry("USA");
        testNeed.setRegionOrState("California");
        testNeed.setCreatedAt(LocalDateTime.now());

        // Create test audit log
        testAuditLog = new AuditLog();
        testAuditLog.setId(UUID.randomUUID());
        testAuditLog.setUserId(testUser.getId());
        testAuditLog.setActionType("LOGIN");
        testAuditLog.setTargetType("USER");
        testAuditLog.setTargetId(testUser.getId());
        testAuditLog.setMetadata("User logged in");
        testAuditLog.setIpAddress(testIpAddress);
        testAuditLog.setCreatedAt(LocalDateTime.now());
    }

    // ==================== Dashboard Statistics Tests ====================

    @Test
    void testGetDashboardStats_ReturnsComprehensiveStats() {
        // Arrange
        when(userRepository.count()).thenReturn(150L);
        when(userRepository.countByIsActive(true)).thenReturn(120L);
        when(userRepository.findAll()).thenReturn(createMockUsers());

        when(organizationRepository.count()).thenReturn(45L);
        when(organizationRepository.countByStatus(OrganizationStatus.VERIFIED)).thenReturn(30L);
        when(organizationRepository.countByStatus(OrganizationStatus.PENDING)).thenReturn(10L);

        when(needRepository.count()).thenReturn(500L);
        when(needRepository.findAll()).thenReturn(createMockNeeds());

        // Use any() for LocalDateTime to avoid timing issues
        when(auditLogRepository.countByActionTypeAndCreatedAtAfter(eq("LOGIN"), any(LocalDateTime.class))).thenReturn(85L);
        when(auditLogRepository.countByActionTypeAndCreatedAtAfter(eq("CREATE_NEED"), any(LocalDateTime.class))).thenReturn(42L);
        when(auditLogRepository.countByActionTypeAndCreatedAtAfter(eq("CLAIM_NEED"), any(LocalDateTime.class))).thenReturn(38L);
        when(auditLogRepository.countByActionTypeAndCreatedAtAfter(eq("RATE_LIMIT_EXCEEDED"), any(LocalDateTime.class))).thenReturn(5L);
        when(auditLogRepository.countByActionTypeAndCreatedAtAfter(eq("LOGIN_FAILED"), any(LocalDateTime.class))).thenReturn(12L);
        when(auditLogRepository.countByActionTypeAndCreatedAtAfter(eq("SUSPICIOUS_BROWSING"), any(LocalDateTime.class))).thenReturn(3L);
        when(auditLogRepository.countByActionTypeAndCreatedAtAfter(eq("ANOMALOUS_CREATION_RATE"), any(LocalDateTime.class))).thenReturn(2L);

        // Act
        AdminStatsResponse stats = adminService.getDashboardStats();

        // Assert
        assertNotNull(stats);

        // User statistics
        assertEquals(150L, stats.getTotalUsers());
        assertEquals(120L, stats.getActiveUsers());
        assertNotNull(stats.getUsersByRole());
        assertTrue(stats.getUsersByRole().containsKey("ADMIN"));
        assertTrue(stats.getUsersByRole().containsKey("NGO_STAFF"));

        // Organization statistics
        assertEquals(45L, stats.getTotalOrganizations());
        assertEquals(30L, stats.getVerifiedOrganizations());
        assertEquals(10L, stats.getPendingOrganizations());

        // Need statistics
        assertEquals(500L, stats.getTotalNeeds());
        assertNotNull(stats.getNeedsByStatus());
        assertNotNull(stats.getNeedsByCategory());
        assertNotNull(stats.getNeedsByUrgency());

        // Activity statistics
        assertEquals(85L, stats.getRecentLogins24h());
        assertEquals(42L, stats.getRecentNeedsCreated24h());
        assertEquals(38L, stats.getRecentNeedsClaimed24h());

        // Security statistics
        assertEquals(5L, stats.getRateLimitViolations24h());
        assertEquals(12L, stats.getFailedLoginAttempts24h());
        assertTrue(stats.getSuspiciousActivities30d() >= 0);
    }

    @Test
    void testGetDashboardStats_UsersByRole_GroupsCorrectly() {
        // Arrange
        List<User> users = Arrays.asList(
            createUser(UserRole.ADMIN),
            createUser(UserRole.ADMIN),
            createUser(UserRole.NGO_STAFF),
            createUser(UserRole.NGO_STAFF),
            createUser(UserRole.NGO_STAFF),
            createUser(UserRole.FIELD_WORKER),
            createUser(UserRole.BENEFICIARY)
        );

        when(userRepository.findAll()).thenReturn(users);
        when(userRepository.count()).thenReturn((long) users.size());
        when(userRepository.countByIsActive(true)).thenReturn(7L);
        when(organizationRepository.count()).thenReturn(0L);
        when(organizationRepository.countByStatus(any())).thenReturn(0L);
        when(needRepository.count()).thenReturn(0L);
        when(needRepository.findAll()).thenReturn(Collections.emptyList());
        when(auditLogRepository.countByActionTypeAndCreatedAtAfter(any(), any())).thenReturn(0L);

        // Act
        AdminStatsResponse stats = adminService.getDashboardStats();

        // Assert
        Map<String, Long> usersByRole = stats.getUsersByRole();
        assertEquals(2L, usersByRole.get("ADMIN"));
        assertEquals(3L, usersByRole.get("NGO_STAFF"));
        assertEquals(1L, usersByRole.get("FIELD_WORKER"));
        assertEquals(1L, usersByRole.get("BENEFICIARY"));
    }

    @Test
    void testGetDashboardStats_NeedsByStatus_GroupsCorrectly() {
        // Arrange
        List<Need> needs = Arrays.asList(
            createNeed(NeedStatus.NEW),
            createNeed(NeedStatus.NEW),
            createNeed(NeedStatus.NEW),
            createNeed(NeedStatus.ASSIGNED),
            createNeed(NeedStatus.ASSIGNED),
            createNeed(NeedStatus.RESOLVED)
        );

        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByIsActive(true)).thenReturn(0L);
        when(organizationRepository.count()).thenReturn(0L);
        when(organizationRepository.countByStatus(any())).thenReturn(0L);
        when(needRepository.count()).thenReturn((long) needs.size());
        when(needRepository.findAll()).thenReturn(needs);
        when(auditLogRepository.countByActionTypeAndCreatedAtAfter(any(), any())).thenReturn(0L);

        // Act
        AdminStatsResponse stats = adminService.getDashboardStats();

        // Assert
        Map<String, Long> needsByStatus = stats.getNeedsByStatus();
        assertEquals(3L, needsByStatus.get("NEW"));
        assertEquals(2L, needsByStatus.get("ASSIGNED"));
        assertEquals(1L, needsByStatus.get("RESOLVED"));
    }

    // ==================== Organization Management Tests ====================

    @Test
    void testGetAllOrganizations_ReturnsPaginatedResults() {
        // Arrange
        List<Organization> orgs = Arrays.asList(testOrg, createOrganization());
        Page<Organization> page = new PageImpl<>(orgs, PageRequest.of(0, 20), orgs.size());

        when(organizationRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        Page<Organization> result = adminService.getAllOrganizations(0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(organizationRepository).findAll(any(Pageable.class));
    }

    @Test
    void testGetOrganizationsByStatus_FiltersByStatus() {
        // Arrange
        Organization pendingOrg = createOrganization();
        pendingOrg.setStatus(OrganizationStatus.PENDING);

        when(organizationRepository.findByStatus(OrganizationStatus.PENDING))
            .thenReturn(Arrays.asList(pendingOrg));

        // Act
        List<Organization> result = adminService.getOrganizationsByStatus(OrganizationStatus.PENDING);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(OrganizationStatus.PENDING, result.get(0).getStatus());
        verify(organizationRepository).findByStatus(OrganizationStatus.PENDING);
    }

    @Test
    void testUpdateOrganizationStatus_VerificationWorkflow_Success() {
        // Arrange
        UUID orgId = testOrg.getId();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setStatus(OrganizationStatus.VERIFIED);
        request.setVerificationNotes("Organization verified after document review");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);

        // Act
        adminService.updateOrganizationStatus(orgId, request, testAdmin.getId(), testIpAddress);

        // Assert
        verify(organizationRepository).findById(orgId);
        verify(organizationRepository).save(argThat(org ->
            org.getStatus() == OrganizationStatus.VERIFIED
        ));

        verify(auditService).logAction(
            eq(testAdmin.getId()),
            eq("VERIFY_ORGANIZATION"),
            eq("ORGANIZATION"),
            eq(orgId),
            contains("PENDING"),
            eq(testIpAddress)
        );
    }

    @Test
    void testUpdateOrganizationStatus_Suspension_Success() {
        // Arrange
        UUID orgId = testOrg.getId();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setStatus(OrganizationStatus.SUSPENDED);
        request.setVerificationNotes("Organization suspended due to policy violation");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);

        // Act
        adminService.updateOrganizationStatus(orgId, request, testAdmin.getId(), testIpAddress);

        // Assert
        verify(organizationRepository).save(argThat(org ->
            org.getStatus() == OrganizationStatus.SUSPENDED
        ));

        verify(auditService).logAction(
            eq(testAdmin.getId()),
            eq("VERIFY_ORGANIZATION"),
            eq("ORGANIZATION"),
            eq(orgId),
            contains("Organization suspended"),
            eq(testIpAddress)
        );
    }

    @Test
    void testUpdateOrganizationStatus_NotFound_ThrowsException() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setStatus(OrganizationStatus.VERIFIED);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            adminService.updateOrganizationStatus(orgId, request, testAdmin.getId(), testIpAddress)
        );

        verify(organizationRepository, never()).save(any());
        verify(auditService, never()).logAction(any(UUID.class), anyString(), anyString(), any(UUID.class), anyString(), anyString());
    }

    // ==================== User Management Tests ====================

    @Test
    void testGetAllUsers_ReturnsPaginatedResults() {
        // Arrange
        List<User> users = Arrays.asList(testUser, testAdmin);
        Page<User> page = new PageImpl<>(users, PageRequest.of(0, 20), users.size());

        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        Page<User> result = adminService.getAllUsers(0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void testUpdateUser_AllFields_Success() {
        // Arrange
        UUID userId = testUser.getId();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated Name");
        request.setRole(UserRole.ADMIN);
        request.setIsActive(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        adminService.updateUser(userId, request, testAdmin.getId(), testIpAddress);

        // Assert
        verify(userRepository).save(argThat(user ->
            user.getName().equals("Updated Name") &&
            user.getRole() == UserRole.ADMIN &&
            !user.getIsActive()
        ));

        verify(auditService).logAction(
            eq(testAdmin.getId()),
            eq("UPDATE_USER"),
            eq("USER"),
            eq(userId),
            contains("Updated Name"),
            eq(testIpAddress)
        );
    }

    @Test
    void testUpdateUser_PartialUpdate_Success() {
        // Arrange
        UUID userId = testUser.getId();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setIsActive(false); // Only update active status

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        adminService.updateUser(userId, request, testAdmin.getId(), testIpAddress);

        // Assert
        verify(userRepository).save(argThat(user -> !user.getIsActive()));
        verify(auditService).logAction(any(UUID.class), eq("UPDATE_USER"), anyString(), any(UUID.class), anyString(), anyString());
    }

    @Test
    void testUpdateUser_NotFound_ThrowsException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            adminService.updateUser(userId, request, testAdmin.getId(), testIpAddress)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void testGetUserAuditLogs_ReturnsLogsForUser() {
        // Arrange
        UUID userId = testUser.getId();
        List<AuditLog> logs = Arrays.asList(testAuditLog, createAuditLog("LOGIN"));

        when(auditLogRepository.findByUserIdAndDateRange(eq(userId), any(), any()))
            .thenReturn(logs);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        List<AuditLogResponse> result = adminService.getUserAuditLogs(userId, 30);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(auditLogRepository).findByUserIdAndDateRange(eq(userId), any(), any());
    }

    // ==================== Audit Log Tests ====================

    @Test
    void testGetAuditLogs_NoFilter_ReturnsAll() {
        // Arrange
        List<AuditLog> logs = Arrays.asList(testAuditLog, createAuditLog("CREATE_NEED"));
        Page<AuditLog> page = new PageImpl<>(logs, PageRequest.of(0, 50), logs.size());

        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // Act
        Page<AuditLogResponse> result = adminService.getAuditLogs(0, 50, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(auditLogRepository).findAll(any(Pageable.class));
        verify(auditLogRepository, never()).findByActionType(any(), any());
    }

    @Test
    void testGetAuditLogs_WithFilter_ReturnsFiltered() {
        // Arrange
        List<AuditLog> logs = Arrays.asList(testAuditLog);
        Page<AuditLog> page = new PageImpl<>(logs, PageRequest.of(0, 50), logs.size());

        when(auditLogRepository.findByActionType(eq("LOGIN"), any(Pageable.class)))
            .thenReturn(page);
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // Act
        Page<AuditLogResponse> result = adminService.getAuditLogs(0, 50, "LOGIN");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("LOGIN", result.getContent().get(0).getActionType());
        verify(auditLogRepository).findByActionType(eq("LOGIN"), any(Pageable.class));
    }

    @Test
    void testGetAuditLogs_MapsUserDetails() {
        // Arrange
        List<AuditLog> logs = Arrays.asList(testAuditLog);
        Page<AuditLog> page = new PageImpl<>(logs, PageRequest.of(0, 50), logs.size());

        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // Act
        Page<AuditLogResponse> result = adminService.getAuditLogs(0, 50, null);

        // Assert
        AuditLogResponse response = result.getContent().get(0);
        assertEquals(testUser.getEmail(), response.getUserEmail());
        assertEquals(testUser.getName(), response.getUserName());
    }

    // ==================== Suspicious Activity Tests ====================

    @Test
    void testGetSuspiciousActivities_ReturnsActivities() {
        // Arrange
        AuditLog suspiciousLog = createAuditLog("SUSPICIOUS_BROWSING");
        suspiciousLog.setMetadata("User viewed 52 needs in 10 minutes");

        when(auditLogRepository.findRecentSuspiciousActivities(any()))
            .thenReturn(Arrays.asList(suspiciousLog));
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // Act
        List<SuspiciousActivityResponse> result = adminService.getSuspiciousActivities(30);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        SuspiciousActivityResponse activity = result.get(0);
        assertEquals("SUSPICIOUS_BROWSING", activity.getActivityType());
        assertEquals("MEDIUM", activity.getSeverity());
        assertNotNull(activity.getUserEmail());
    }

    @Test
    void testGetSuspiciousActivities_SeverityMapping() {
        // Arrange
        AuditLog highSeverity = createAuditLog("ANOMALOUS_CREATION_RATE");
        AuditLog mediumSeverity = createAuditLog("SUSPICIOUS_BROWSING");
        AuditLog lowSeverity = createAuditLog("RATE_LIMIT_EXCEEDED");

        when(auditLogRepository.findRecentSuspiciousActivities(any()))
            .thenReturn(Arrays.asList(highSeverity, mediumSeverity, lowSeverity));
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // Act
        List<SuspiciousActivityResponse> result = adminService.getSuspiciousActivities(30);

        // Assert
        assertEquals(3, result.size());
        assertEquals("HIGH", result.get(0).getSeverity());
        assertEquals("MEDIUM", result.get(1).getSeverity());
        assertEquals("LOW", result.get(2).getSeverity());
    }

    @Test
    void testGetSuspiciousActivities_ExtractsCount() {
        // Arrange
        AuditLog log = createAuditLog("SUSPICIOUS_BROWSING");
        log.setMetadata("User viewed 52 needs in 10 minutes");

        when(auditLogRepository.findRecentSuspiciousActivities(any()))
            .thenReturn(Arrays.asList(log));
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // Act
        List<SuspiciousActivityResponse> result = adminService.getSuspiciousActivities(30);

        // Assert
        assertEquals(52L, result.get(0).getActivityCount());
    }

    // ==================== Helper Methods ====================

    private User createUser(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(role.name().toLowerCase() + "@test.com");
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private Need createNeed(NeedStatus status) {
        Need need = new Need();
        need.setId(UUID.randomUUID());
        need.setStatus(status);
        need.setCategory(NeedCategory.FOOD);
        need.setUrgencyLevel(UrgencyLevel.MEDIUM);
        need.setCountry("USA");
        need.setCreatedAt(LocalDateTime.now());
        return need;
    }

    private Organization createOrganization() {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Test Organization");
        org.setStatus(OrganizationStatus.PENDING);
        org.setCountry("USA");
        org.setType(OrganizationType.NGO);
        org.setCreatedAt(LocalDateTime.now());
        return org;
    }

    private AuditLog createAuditLog(String actionType) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setUserId(testUser.getId());
        log.setActionType(actionType);
        log.setTargetType("NEED");
        log.setTargetId(UUID.randomUUID());
        log.setMetadata("Test audit log");
        log.setIpAddress(testIpAddress);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    private List<User> createMockUsers() {
        return Arrays.asList(
            createUser(UserRole.ADMIN),
            createUser(UserRole.NGO_STAFF),
            createUser(UserRole.FIELD_WORKER),
            createUser(UserRole.BENEFICIARY)
        );
    }

    private List<Need> createMockNeeds() {
        return Arrays.asList(
            createNeed(NeedStatus.NEW),
            createNeed(NeedStatus.ASSIGNED),
            createNeed(NeedStatus.RESOLVED)
        );
    }
}
