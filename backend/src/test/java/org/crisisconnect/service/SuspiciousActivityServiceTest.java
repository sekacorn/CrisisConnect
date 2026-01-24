package org.crisisconnect.service;

import org.crisisconnect.model.entity.AuditLog;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.AuditLogRepository;
import org.crisisconnect.repository.NeedRepository;
import org.crisisconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for SuspiciousActivityService
 *
 * Tests fraud detection algorithms and scheduled job behavior:
 * - Suspicious browsing pattern detection (50+ views, no claims)
 * - Anomalous creation rate detection (100+ needs in 7 days)
 * - Recent suspicious activity retrieval
 * - Manual trigger methods
 * - Edge cases and error handling
 *
 * Coverage Target: 95%+
 */
@ExtendWith(MockitoExtension.class)
class SuspiciousActivityServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NeedRepository needRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SuspiciousActivityService suspiciousActivityService;

    private User adminUser;
    private User fieldWorkerUser;
    private User ngoStaffUser;
    private User beneficiaryUser;

    @BeforeEach
    void setUp() {
        // Create test users with different roles
        adminUser = createUser(UserRole.ADMIN, "admin@test.com");
        fieldWorkerUser = createUser(UserRole.FIELD_WORKER, "fieldworker@test.com");
        ngoStaffUser = createUser(UserRole.NGO_STAFF, "ngostaff@test.com");
        beneficiaryUser = createUser(UserRole.BENEFICIARY, "beneficiary@test.com");
    }

    // ==================== Helper Methods ====================

    private User createUser(UserRole role, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(role);
        // User entity doesn't have setActive() method - active is default
        return user;
    }

    private List<User> createUserList() {
        return Arrays.asList(adminUser, fieldWorkerUser, ngoStaffUser, beneficiaryUser);
    }

    // ==================== Suspicious Browsing Detection Tests ====================

    @Test
    void testDetectSuspiciousBrowsing_NgoStaffWith50ViewsAnd0Claims_FlagsUser() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // NGO staff viewed 50 needs (above threshold)
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(ngoStaffUser.getId()),
            eq("NEED_ACCESSED_FULL"),
            any(LocalDateTime.class)
        )).thenReturn(50L);

        // But claimed 0 needs (suspicious!)
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(ngoStaffUser.getId()),
            eq("NEED_CLAIMED"),
            any(LocalDateTime.class)
        )).thenReturn(0L);

        // Act
        suspiciousActivityService.detectSuspiciousBrowsingPatterns();

        // Assert
        ArgumentCaptor<String> actionTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ipAddressCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditService, times(1)).logAction(
            eq(ngoStaffUser.getId()),
            actionTypeCaptor.capture(),
            anyString(),
            anyString(),
            ipAddressCaptor.capture(),
            anyMap()
        );

        assertEquals("SUSPICIOUS_BROWSING", actionTypeCaptor.getValue());
        assertEquals("system", ipAddressCaptor.getValue());
    }

    @Test
    void testDetectSuspiciousBrowsing_NgoStaffWith60ViewsAnd10Claims_NotFlagged() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // NGO staff viewed 60 needs
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(ngoStaffUser.getId()),
            eq("NEED_ACCESSED_FULL"),
            any(LocalDateTime.class)
        )).thenReturn(60L);

        // And claimed 10 needs (legitimate activity)
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(ngoStaffUser.getId()),
            eq("NEED_CLAIMED"),
            any(LocalDateTime.class)
        )).thenReturn(10L);

        // Act
        suspiciousActivityService.detectSuspiciousBrowsingPatterns();

        // Assert - Should NOT flag this user
        verify(auditService, never()).logAction(
            eq(ngoStaffUser.getId()),
            eq("SUSPICIOUS_BROWSING"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectSuspiciousBrowsing_NgoStaffWith49Views_NotFlagged() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // Just below threshold (49 < 50)
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(ngoStaffUser.getId()),
            eq("NEED_ACCESSED_FULL"),
            any(LocalDateTime.class)
        )).thenReturn(49L);

        // Below threshold, so claim count won't be checked - use lenient
        lenient().when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(ngoStaffUser.getId()),
            eq("NEED_CLAIMED"),
            any(LocalDateTime.class)
        )).thenReturn(0L);

        // Act
        suspiciousActivityService.detectSuspiciousBrowsingPatterns();

        // Assert - Should NOT flag (below threshold)
        verify(auditService, never()).logAction(
            any(UUID.class),
            eq("SUSPICIOUS_BROWSING"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectSuspiciousBrowsing_FieldWorkerWithManyViews_NotFlagged() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // Field worker viewed 100 needs (but field workers aren't monitored for browsing)
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(fieldWorkerUser.getId()),
            eq("NEED_ACCESSED_FULL"),
            any(LocalDateTime.class)
        )).thenReturn(100L);

        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(fieldWorkerUser.getId()),
            eq("NEED_CLAIMED"),
            any(LocalDateTime.class)
        )).thenReturn(0L);

        // Act
        suspiciousActivityService.detectSuspiciousBrowsingPatterns();

        // Assert - Field workers should NOT be flagged for browsing
        verify(auditService, never()).logAction(
            eq(fieldWorkerUser.getId()),
            eq("SUSPICIOUS_BROWSING"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectSuspiciousBrowsing_AdminWithManyViews_NotFlagged() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // Admin viewed 200 needs (admins have legitimate reasons)
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(adminUser.getId()),
            eq("NEED_ACCESSED_FULL"),
            any(LocalDateTime.class)
        )).thenReturn(200L);

        // Act
        suspiciousActivityService.detectSuspiciousBrowsingPatterns();

        // Assert - Admins should NOT be flagged
        verify(auditService, never()).logAction(
            eq(adminUser.getId()),
            eq("SUSPICIOUS_BROWSING"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectSuspiciousBrowsing_ExceptionHandling_ContinuesProcessing() {
        // Arrange
        User user1 = createUser(UserRole.NGO_STAFF, "user1@test.com");
        User user2 = createUser(UserRole.NGO_STAFF, "user2@test.com");

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // First user causes exception
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(user1.getId()),
            anyString(),
            any(LocalDateTime.class)
        )).thenThrow(new RuntimeException("Database error"));

        // Second user has suspicious activity
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(user2.getId()),
            eq("NEED_ACCESSED_FULL"),
            any(LocalDateTime.class)
        )).thenReturn(60L);

        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(user2.getId()),
            eq("NEED_CLAIMED"),
            any(LocalDateTime.class)
        )).thenReturn(0L);

        // Act - Should not throw exception
        assertDoesNotThrow(() -> suspiciousActivityService.detectSuspiciousBrowsingPatterns());

        // Assert - Second user should still be flagged
        verify(auditService, times(1)).logAction(
            eq(user2.getId()),
            eq("SUSPICIOUS_BROWSING"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    // ==================== Anomalous Creation Rate Tests ====================

    @Test
    void testDetectAnomalousCreation_FieldWorkerWith100NeedsIn7Days_FlagsUser() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // Field worker created 100 needs in 7 days (exactly at threshold)
        when(needRepository.countByCreatedByUserIdAndCreatedAtAfter(
            eq(fieldWorkerUser.getId()),
            any(LocalDateTime.class)
        )).thenReturn(100L);

        // Act
        suspiciousActivityService.detectAnomalousCreationRates();

        // Assert
        ArgumentCaptor<String> actionTypeCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditService, times(1)).logAction(
            eq(fieldWorkerUser.getId()),
            actionTypeCaptor.capture(),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );

        assertEquals("ANOMALOUS_CREATION_RATE", actionTypeCaptor.getValue());
    }

    @Test
    void testDetectAnomalousCreation_FieldWorkerWith99Needs_NotFlagged() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // Just below threshold (99 < 100)
        when(needRepository.countByCreatedByUserIdAndCreatedAtAfter(
            eq(fieldWorkerUser.getId()),
            any(LocalDateTime.class)
        )).thenReturn(99L);

        // Act
        suspiciousActivityService.detectAnomalousCreationRates();

        // Assert - Should NOT flag
        verify(auditService, never()).logAction(
            any(UUID.class),
            eq("ANOMALOUS_CREATION_RATE"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectAnomalousCreation_NgoStaffWith150Needs_FlagsUser() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // NGO staff created 150 needs (above threshold)
        when(needRepository.countByCreatedByUserIdAndCreatedAtAfter(
            eq(ngoStaffUser.getId()),
            any(LocalDateTime.class)
        )).thenReturn(150L);

        // Act
        suspiciousActivityService.detectAnomalousCreationRates();

        // Assert
        verify(auditService, times(1)).logAction(
            eq(ngoStaffUser.getId()),
            eq("ANOMALOUS_CREATION_RATE"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectAnomalousCreation_AdminWithManyNeeds_NotFlagged() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // Admin created 500 needs (admins shouldn't be monitored for this)
        when(needRepository.countByCreatedByUserIdAndCreatedAtAfter(
            eq(adminUser.getId()),
            any(LocalDateTime.class)
        )).thenReturn(500L);

        // Act
        suspiciousActivityService.detectAnomalousCreationRates();

        // Assert - Admins should NOT be flagged
        verify(auditService, never()).logAction(
            eq(adminUser.getId()),
            eq("ANOMALOUS_CREATION_RATE"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectAnomalousCreation_BeneficiaryWithNeeds_NotFlagged() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        // Beneficiary shouldn't be able to create needs, but if they did...
        when(needRepository.countByCreatedByUserIdAndCreatedAtAfter(
            eq(beneficiaryUser.getId()),
            any(LocalDateTime.class)
        )).thenReturn(200L);

        // Act
        suspiciousActivityService.detectAnomalousCreationRates();

        // Assert - Beneficiaries are not monitored (they shouldn't create needs anyway)
        verify(auditService, never()).logAction(
            eq(beneficiaryUser.getId()),
            eq("ANOMALOUS_CREATION_RATE"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectAnomalousCreation_ExceptionHandling_ContinuesProcessing() {
        // Arrange
        User user1 = createUser(UserRole.FIELD_WORKER, "user1@test.com");
        User user2 = createUser(UserRole.FIELD_WORKER, "user2@test.com");

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // First user causes exception
        when(needRepository.countByCreatedByUserIdAndCreatedAtAfter(
            eq(user1.getId()),
            any(LocalDateTime.class)
        )).thenThrow(new RuntimeException("Database error"));

        // Second user has anomalous creation rate
        when(needRepository.countByCreatedByUserIdAndCreatedAtAfter(
            eq(user2.getId()),
            any(LocalDateTime.class)
        )).thenReturn(120L);

        // Act - Should not throw exception
        assertDoesNotThrow(() -> suspiciousActivityService.detectAnomalousCreationRates());

        // Assert - Second user should still be flagged
        verify(auditService, times(1)).logAction(
            eq(user2.getId()),
            eq("ANOMALOUS_CREATION_RATE"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    // ==================== Recent Suspicious Activities Tests ====================

    @Test
    void testGetRecentSuspiciousActivities_Returns30DaysOfData() {
        // Arrange
        List<AuditLog> mockLogs = new ArrayList<>();

        AuditLog log1 = createAuditLog(ngoStaffUser.getId(), "SUSPICIOUS_BROWSING");
        AuditLog log2 = createAuditLog(fieldWorkerUser.getId(), "ANOMALOUS_CREATION_RATE");

        mockLogs.add(log1);
        mockLogs.add(log2);

        when(auditLogRepository.findRecentSuspiciousActivities(any(LocalDateTime.class)))
            .thenReturn(mockLogs);

        // Act
        List<AuditLog> result = suspiciousActivityService.getRecentSuspiciousActivities(30);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("SUSPICIOUS_BROWSING", result.get(0).getActionType());
        assertEquals("ANOMALOUS_CREATION_RATE", result.get(1).getActionType());

        verify(auditLogRepository, times(1)).findRecentSuspiciousActivities(any(LocalDateTime.class));
    }

    @Test
    void testGetRecentSuspiciousActivities_EmptyResult_ReturnsEmptyList() {
        // Arrange
        when(auditLogRepository.findRecentSuspiciousActivities(any(LocalDateTime.class)))
            .thenReturn(new ArrayList<>());

        // Act
        List<AuditLog> result = suspiciousActivityService.getRecentSuspiciousActivities(7);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRecentSuspiciousActivities_CustomDaysParameter_UsesCorrectTimeWindow() {
        // Arrange
        when(auditLogRepository.findRecentSuspiciousActivities(any(LocalDateTime.class)))
            .thenReturn(new ArrayList<>());

        // Act
        suspiciousActivityService.getRecentSuspiciousActivities(7);

        // Assert - Verify the method was called (time window calculation happens inside)
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(auditLogRepository).findRecentSuspiciousActivities(dateCaptor.capture());

        LocalDateTime capturedDate = dateCaptor.getValue();
        LocalDateTime expectedDate = LocalDateTime.now().minusDays(7);

        // Allow 1 second tolerance for test execution time
        assertTrue(capturedDate.isBefore(expectedDate.plusSeconds(1)));
        assertTrue(capturedDate.isAfter(expectedDate.minusSeconds(1)));
    }

    // ==================== Manual Trigger Tests ====================

    @Test
    void testManualCheckSuspiciousBrowsing_CallsDetectionMethod() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            any(UUID.class),
            anyString(),
            any(LocalDateTime.class)
        )).thenReturn(0L);

        // Act
        assertDoesNotThrow(() -> suspiciousActivityService.manualCheckSuspiciousBrowsing());

        // Assert - Verify the detection method was called
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testManualCheckAnomalousCreation_CallsDetectionMethod() {
        // Arrange
        when(userRepository.findAll()).thenReturn(createUserList());

        when(needRepository.countByCreatedByUserIdAndCreatedAtAfter(
            any(UUID.class),
            any(LocalDateTime.class)
        )).thenReturn(0L);

        // Act
        assertDoesNotThrow(() -> suspiciousActivityService.manualCheckAnomalousCreation());

        // Assert - Verify the detection method was called
        verify(userRepository, times(1)).findAll();
    }

    // ==================== Rate Limit Cleanup Tests ====================

    @Test
    void testCleanupExpiredRateLimits_ExecutesWithoutError() {
        // Act - Should execute without throwing exception
        assertDoesNotThrow(() -> suspiciousActivityService.cleanupExpiredRateLimits());

        // Note: This is currently a placeholder method, so we just verify it doesn't crash
        // When implemented, add more specific assertions
    }

    // ==================== Edge Cases and Error Handling ====================

    @Test
    void testDetectSuspiciousBrowsing_EmptyUserList_HandlesGracefully() {
        // Arrange
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        assertDoesNotThrow(() -> suspiciousActivityService.detectSuspiciousBrowsingPatterns());

        // Assert - No audit logs should be created
        verify(auditService, never()).logAction(
            any(UUID.class),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectAnomalousCreation_EmptyUserList_HandlesGracefully() {
        // Arrange
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        assertDoesNotThrow(() -> suspiciousActivityService.detectAnomalousCreationRates());

        // Assert - No audit logs should be created
        verify(auditService, never()).logAction(
            any(UUID.class),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectSuspiciousBrowsing_NullEmailUser_HandlesGracefully() {
        // Arrange
        User nullEmailUser = createUser(UserRole.NGO_STAFF, null);
        when(userRepository.findAll()).thenReturn(Arrays.asList(nullEmailUser));

        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(nullEmailUser.getId()),
            eq("NEED_ACCESSED_FULL"),
            any(LocalDateTime.class)
        )).thenReturn(60L);

        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            eq(nullEmailUser.getId()),
            eq("NEED_CLAIMED"),
            any(LocalDateTime.class)
        )).thenReturn(0L);

        // Act - Should handle null email gracefully
        assertDoesNotThrow(() -> suspiciousActivityService.detectSuspiciousBrowsingPatterns());
    }

    @Test
    void testGetRecentSuspiciousActivities_RepositoryException_ThrowsException() {
        // Arrange
        when(auditLogRepository.findRecentSuspiciousActivities(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database connection lost"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            suspiciousActivityService.getRecentSuspiciousActivities(30);
        });
    }

    // ==================== Multiple Users Flagged Tests ====================

    @Test
    void testDetectSuspiciousBrowsing_MultipleUsers_FlagsAll() {
        // Arrange
        User ngoStaff1 = createUser(UserRole.NGO_STAFF, "staff1@test.com");
        User ngoStaff2 = createUser(UserRole.NGO_STAFF, "staff2@test.com");
        User ngoStaff3 = createUser(UserRole.NGO_STAFF, "staff3@test.com");

        when(userRepository.findAll()).thenReturn(Arrays.asList(ngoStaff1, ngoStaff2, ngoStaff3));

        // All three have suspicious browsing patterns
        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            any(UUID.class),
            eq("NEED_ACCESSED_FULL"),
            any(LocalDateTime.class)
        )).thenReturn(60L);

        when(auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
            any(UUID.class),
            eq("NEED_CLAIMED"),
            any(LocalDateTime.class)
        )).thenReturn(0L);

        // Act
        suspiciousActivityService.detectSuspiciousBrowsingPatterns();

        // Assert - All three should be flagged
        verify(auditService, times(3)).logAction(
            any(UUID.class),
            eq("SUSPICIOUS_BROWSING"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void testDetectAnomalousCreation_MultipleUsers_FlagsAll() {
        // Arrange
        User fw1 = createUser(UserRole.FIELD_WORKER, "fw1@test.com");
        User fw2 = createUser(UserRole.FIELD_WORKER, "fw2@test.com");

        when(userRepository.findAll()).thenReturn(Arrays.asList(fw1, fw2));

        // Both have anomalous creation rates
        when(needRepository.countByCreatedByUserIdAndCreatedAtAfter(
            any(UUID.class),
            any(LocalDateTime.class)
        )).thenReturn(150L);

        // Act
        suspiciousActivityService.detectAnomalousCreationRates();

        // Assert - Both should be flagged
        verify(auditService, times(2)).logAction(
            any(UUID.class),
            eq("ANOMALOUS_CREATION_RATE"),
            anyString(),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    // ==================== Helper Method for Test Data ====================

    private AuditLog createAuditLog(UUID userId, String actionType) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setUserId(userId);
        log.setActionType(actionType);
        log.setTargetType("USER");
        log.setTargetId(userId);
        log.setIpAddress("system");
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}
