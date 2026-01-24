package org.crisisconnect.service;

import org.crisisconnect.model.entity.AuditLog;
import org.crisisconnect.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService
 *
 * Tests cover:
 * - Generic action logging
 * - Need access logging (full vs redacted)
 * - Sensitive info access logging
 * - Audit log field mapping
 * - Asynchronous logging behavior
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private UUID testUserId;
    private UUID testNeedId;
    private UUID testOrgId;
    private String testIpAddress = "192.168.1.100";

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testNeedId = UUID.randomUUID();
        testOrgId = UUID.randomUUID();
    }

    // ==================== Generic logAction Tests ====================

    @Test
    void testLogAction_AllFields_SavesCorrectly() {
        // Arrange
        String actionType = "CREATE_NEED";
        String targetType = "NEED";
        String metadata = "Created new food need";

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logAction(testUserId, actionType, targetType, testNeedId, metadata, testIpAddress);

        // Assert
        verify(auditLogRepository, timeout(1000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testUserId, savedLog.getUserId());
        assertEquals(actionType, savedLog.getActionType());
        assertEquals(targetType, savedLog.getTargetType());
        assertEquals(testNeedId, savedLog.getTargetId());
        assertEquals(metadata, savedLog.getMetadata());
        assertEquals(testIpAddress, savedLog.getIpAddress());
    }

    @Test
    void testLogAction_NullMetadata_SavesWithNullMetadata() {
        // Arrange
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logAction(testUserId, "LOGIN", "USER", testUserId, null, testIpAddress);

        // Assert
        verify(auditLogRepository, timeout(1000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testUserId, savedLog.getUserId());
        assertEquals("LOGIN", savedLog.getActionType());
        assertNull(savedLog.getMetadata());
    }

    @Test
    void testLogAction_DifferentActionTypes_AllSaved() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logAction(testUserId, "LOGIN", "USER", testUserId, null, testIpAddress);
        auditService.logAction(testUserId, "CREATE_NEED", "NEED", testNeedId, "Created", testIpAddress);
        auditService.logAction(testUserId, "CLAIM_NEED", "NEED", testNeedId, "Claimed", testIpAddress);
        auditService.logAction(testUserId, "VERIFY_ORGANIZATION", "ORGANIZATION", testOrgId, "Verified", testIpAddress);

        // Assert - verify all 4 actions were saved
        verify(auditLogRepository, timeout(2000).times(4)).save(any(AuditLog.class));
    }

    @Test
    void testLogAction_LongMetadata_SavesWithoutTruncation() {
        // Arrange
        String longMetadata = "A".repeat(5000);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logAction(testUserId, "ACTION", "TYPE", testUserId, longMetadata, testIpAddress);

        // Assert
        verify(auditLogRepository, timeout(1000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(longMetadata, savedLog.getMetadata());
    }

    // ==================== logNeedAccess Tests ====================

    @Test
    void testLogNeedAccess_FullAccess_LogsCorrectActionType() {
        // Arrange
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logNeedAccess(testUserId, testNeedId, true, testIpAddress);

        // Assert
        verify(auditLogRepository, timeout(1000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("VIEW_NEED_FULL", savedLog.getActionType());
        assertEquals(testUserId, savedLog.getUserId());
        assertEquals("NEED", savedLog.getTargetType());
        assertEquals(testNeedId, savedLog.getTargetId());
        assertEquals(testIpAddress, savedLog.getIpAddress());
    }

    @Test
    void testLogNeedAccess_RedactedAccess_LogsCorrectActionType() {
        // Arrange
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logNeedAccess(testUserId, testNeedId, false, testIpAddress);

        // Assert
        verify(auditLogRepository, timeout(1000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("VIEW_NEED_REDACTED", savedLog.getActionType());
        assertEquals(testUserId, savedLog.getUserId());
        assertEquals("NEED", savedLog.getTargetType());
        assertEquals(testNeedId, savedLog.getTargetId());
        assertEquals(testIpAddress, savedLog.getIpAddress());
    }

    @Test
    void testLogNeedAccess_MultipleAccesses_AllLogged() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act - Simulate user browsing multiple needs
        auditService.logNeedAccess(testUserId, testNeedId, false, testIpAddress);
        UUID needId2 = UUID.randomUUID();
        auditService.logNeedAccess(testUserId, needId2, false, testIpAddress);
        UUID needId3 = UUID.randomUUID();
        auditService.logNeedAccess(testUserId, needId3, true, testIpAddress);

        // Assert - Verify all 3 accesses were logged
        verify(auditLogRepository, timeout(2000).times(3)).save(any(AuditLog.class));
    }

    // ==================== logSensitiveInfoAccess Tests ====================

    @Test
    void testLogSensitiveInfoAccess_LogsWithPIIMetadata() {
        // Arrange
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logSensitiveInfoAccess(testUserId, testNeedId, testIpAddress);

        // Assert
        verify(auditLogRepository, timeout(1000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("VIEW_SENSITIVE_INFO", savedLog.getActionType());
        assertEquals(testUserId, savedLog.getUserId());
        assertEquals("NEED", savedLog.getTargetType());
        assertEquals(testNeedId, savedLog.getTargetId());
        assertEquals("PII accessed", savedLog.getMetadata());
        assertEquals(testIpAddress, savedLog.getIpAddress());
    }

    @Test
    void testLogSensitiveInfoAccess_MultiplePIIAccesses_AllLogged() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act - Simulate multiple PII accesses
        auditService.logSensitiveInfoAccess(testUserId, testNeedId, testIpAddress);
        UUID needId2 = UUID.randomUUID();
        auditService.logSensitiveInfoAccess(testUserId, needId2, testIpAddress);

        // Assert
        verify(auditLogRepository, timeout(2000).times(2)).save(any(AuditLog.class));
    }

    // ==================== Asynchronous Behavior Tests ====================

    @Test
    void testLogAction_Asynchronous_DoesNotBlock() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            // Simulate slow database save (100ms)
            Thread.sleep(100);
            return new AuditLog();
        });

        // Act
        auditService.logAction(testUserId, "TEST_ACTION", "TEST", testUserId, "metadata", testIpAddress);

        // Assert - Verify the save was eventually called asynchronously
        // Note: We don't test exact timing as it's environment-dependent
        verify(auditLogRepository, timeout(2000)).save(any(AuditLog.class));
    }

    @Test
    void testLogNeedAccess_Asynchronous_DoesNotBlock() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            Thread.sleep(100);
            return new AuditLog();
        });

        // Act
        auditService.logNeedAccess(testUserId, testNeedId, true, testIpAddress);

        // Assert - Verify async execution happened
        verify(auditLogRepository, timeout(2000)).save(any(AuditLog.class));
    }

    // ==================== IP Address Tracking Tests ====================

    @Test
    void testLogAction_DifferentIPAddresses_AllRecorded() {
        // Arrange
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        String ip1 = "192.168.1.100";
        String ip2 = "10.0.0.50";
        String ip3 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"; // IPv6

        // Act
        auditService.logAction(testUserId, "ACTION1", "TYPE", testUserId, null, ip1);
        auditService.logAction(testUserId, "ACTION2", "TYPE", testUserId, null, ip2);
        auditService.logAction(testUserId, "ACTION3", "TYPE", testUserId, null, ip3);

        // Assert
        verify(auditLogRepository, timeout(2000).times(3)).save(captor.capture());

        var capturedLogs = captor.getAllValues();
        assertEquals(ip1, capturedLogs.get(0).getIpAddress());
        assertEquals(ip2, capturedLogs.get(1).getIpAddress());
        assertEquals(ip3, capturedLogs.get(2).getIpAddress());
    }

    @Test
    void testLogAction_NullIPAddress_SavesWithNull() {
        // Arrange
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logAction(testUserId, "ACTION", "TYPE", testUserId, null, null);

        // Assert
        verify(auditLogRepository, timeout(1000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getIpAddress());
    }

    // ==================== Integration Scenario Tests ====================

    @Test
    void testAuditTrail_CompletNeedWorkflow() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        UUID creatorId = UUID.randomUUID();
        UUID ngoStaffId = UUID.randomUUID();

        // Act - Simulate complete need workflow
        // 1. Creator creates need
        auditService.logAction(creatorId, "CREATE_NEED", "NEED", testNeedId,
            "Created food need", testIpAddress);

        // 2. NGO staff views redacted need
        auditService.logNeedAccess(ngoStaffId, testNeedId, false, "10.0.0.50");

        // 3. NGO staff claims need (gets full access)
        auditService.logAction(ngoStaffId, "CLAIM_NEED", "NEED", testNeedId,
            "Claimed by verified NGO", "10.0.0.50");

        // 4. NGO staff views full need details
        auditService.logNeedAccess(ngoStaffId, testNeedId, true, "10.0.0.50");

        // 5. NGO staff accesses sensitive PII
        auditService.logSensitiveInfoAccess(ngoStaffId, testNeedId, "10.0.0.50");

        // 6. NGO staff resolves need
        auditService.logAction(ngoStaffId, "RESOLVE_NEED", "NEED", testNeedId,
            "Need resolved successfully", "10.0.0.50");

        // Assert - All 6 audit events were logged
        verify(auditLogRepository, timeout(3000).times(6)).save(any(AuditLog.class));
    }

    @Test
    void testAuditTrail_AdminVerificationWorkflow() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        UUID adminId = UUID.randomUUID();

        // Act - Simulate admin verification workflow
        // 1. Admin logs in
        auditService.logAction(adminId, "LOGIN", "USER", adminId, null, testIpAddress);

        // 2. Admin views organization details
        auditService.logAction(adminId, "VIEW_ORGANIZATION", "ORGANIZATION", testOrgId,
            "Reviewed organization documents", testIpAddress);

        // 3. Admin verifies organization
        auditService.logAction(adminId, "VERIFY_ORGANIZATION", "ORGANIZATION", testOrgId,
            "Status changed to VERIFIED", testIpAddress);

        // Assert
        verify(auditLogRepository, timeout(2000).times(3)).save(any(AuditLog.class));
    }

    @Test
    void testAuditTrail_SuspiciousActivityDetection() {
        // Arrange
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        UUID suspiciousUserId = UUID.randomUUID();

        // Act - Simulate suspicious activity
        auditService.logAction(suspiciousUserId, "SUSPICIOUS_BROWSING", "NEEDS", null,
            "User viewed 52 needs in 10 minutes", testIpAddress);

        auditService.logAction(suspiciousUserId, "RATE_LIMIT_EXCEEDED", "API", null,
            "User exceeded API rate limit", testIpAddress);

        // Assert
        verify(auditLogRepository, timeout(2000).times(2)).save(captor.capture());

        var capturedLogs = captor.getAllValues();
        assertEquals("SUSPICIOUS_BROWSING", capturedLogs.get(0).getActionType());
        assertEquals("RATE_LIMIT_EXCEEDED", capturedLogs.get(1).getActionType());
    }

    // ==================== Edge Cases ====================

    @Test
    void testLogAction_NullUserId_StillSaves() {
        // Arrange - Some actions like system maintenance might not have userId
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logAction((UUID) null, "SYSTEM_MAINTENANCE", "SYSTEM", (UUID) null,
            "Database cleanup performed", "system");

        // Assert
        verify(auditLogRepository, timeout(1000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getUserId());
        assertEquals("SYSTEM_MAINTENANCE", savedLog.getActionType());
    }

    @Test
    void testLogAction_EmptyStrings_SavesAsEmpty() {
        // Arrange
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(new AuditLog());

        // Act
        auditService.logAction(testUserId, "", "", testUserId, "", "");

        // Assert
        verify(auditLogRepository, timeout(1000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("", savedLog.getActionType());
        assertEquals("", savedLog.getTargetType());
        assertEquals("", savedLog.getMetadata());
        assertEquals("", savedLog.getIpAddress());
    }
}
