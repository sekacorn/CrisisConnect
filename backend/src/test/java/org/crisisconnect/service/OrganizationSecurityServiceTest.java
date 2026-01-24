package org.crisisconnect.service;

import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrganizationSecurityService
 *
 * Tests cover:
 * - Organization verification status checks
 * - Organization management authorization
 * - Organization verification authorization (admin only)
 * - Organization creation authorization
 * - Organization active status checks
 * - Organization membership checks
 * - User organization status retrieval
 */
@ExtendWith(MockitoExtension.class)
class OrganizationSecurityServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrganizationSecurityService orgSecurityService;

    private Organization verifiedOrg;
    private Organization pendingOrg;
    private Organization suspendedOrg;

    private User adminUser;
    private User ngoStaffVerified;
    private User ngoStaffPending;
    private User ngoStaffNoOrg;
    private User fieldWorker;

    @BeforeEach
    void setUp() {
        UUID verifiedOrgId = UUID.randomUUID();
        UUID pendingOrgId = UUID.randomUUID();
        UUID suspendedOrgId = UUID.randomUUID();

        // Create organizations with different statuses
        verifiedOrg = new Organization();
        verifiedOrg.setId(verifiedOrgId);
        verifiedOrg.setName("Verified NGO");
        verifiedOrg.setStatus(OrganizationStatus.VERIFIED);

        pendingOrg = new Organization();
        pendingOrg.setId(pendingOrgId);
        pendingOrg.setName("Pending NGO");
        pendingOrg.setStatus(OrganizationStatus.PENDING);

        suspendedOrg = new Organization();
        suspendedOrg.setId(suspendedOrgId);
        suspendedOrg.setName("Suspended NGO");
        suspendedOrg.setStatus(OrganizationStatus.SUSPENDED);

        // Create users
        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEmail("admin@test.com");

        ngoStaffVerified = new User();
        ngoStaffVerified.setId(UUID.randomUUID());
        ngoStaffVerified.setRole(UserRole.NGO_STAFF);
        ngoStaffVerified.setOrganizationId(verifiedOrgId);
        ngoStaffVerified.setEmail("verified@ngo.org");

        ngoStaffPending = new User();
        ngoStaffPending.setId(UUID.randomUUID());
        ngoStaffPending.setRole(UserRole.NGO_STAFF);
        ngoStaffPending.setOrganizationId(pendingOrgId);
        ngoStaffPending.setEmail("pending@ngo.org");

        ngoStaffNoOrg = new User();
        ngoStaffNoOrg.setId(UUID.randomUUID());
        ngoStaffNoOrg.setRole(UserRole.NGO_STAFF);
        ngoStaffNoOrg.setOrganizationId(null);
        ngoStaffNoOrg.setEmail("noorg@test.com");

        fieldWorker = new User();
        fieldWorker.setId(UUID.randomUUID());
        fieldWorker.setRole(UserRole.FIELD_WORKER);
        fieldWorker.setOrganizationId(null);
        fieldWorker.setEmail("worker@test.com");
    }

    // ==================== isVerified Tests ====================

    @Test
    void testIsVerified_VerifiedOrganization_ShouldReturnTrue() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);
        when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.of(verifiedOrg));

        // Act
        boolean result = orgSecurityService.isVerified(authentication);

        // Assert
        assertTrue(result, "User from verified organization should be verified");
        verify(organizationRepository).findById(verifiedOrg.getId());
    }

    @Test
    void testIsVerified_PendingOrganization_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffPending);
        when(organizationRepository.findById(pendingOrg.getId())).thenReturn(Optional.of(pendingOrg));

        // Act
        boolean result = orgSecurityService.isVerified(authentication);

        // Assert
        assertFalse(result, "User from pending organization should not be verified");
    }

    @Test
    void testIsVerified_SuspendedOrganization_ShouldReturnFalse() {
        // Arrange
        User suspendedUser = new User();
        suspendedUser.setId(UUID.randomUUID());
        suspendedUser.setRole(UserRole.NGO_STAFF);
        suspendedUser.setOrganizationId(suspendedOrg.getId());

        when(authService.getCurrentUser(authentication)).thenReturn(suspendedUser);
        when(organizationRepository.findById(suspendedOrg.getId())).thenReturn(Optional.of(suspendedOrg));

        // Act
        boolean result = orgSecurityService.isVerified(authentication);

        // Assert
        assertFalse(result, "User from suspended organization should not be verified");
    }

    @Test
    void testIsVerified_NoOrganization_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffNoOrg);

        // Act
        boolean result = orgSecurityService.isVerified(authentication);

        // Assert
        assertFalse(result, "User without organization should not be verified");
        verify(organizationRepository, never()).findById(any());
    }

    @Test
    void testIsVerified_NullUser_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(null);

        // Act
        boolean result = orgSecurityService.isVerified(authentication);

        // Assert
        assertFalse(result, "Null user should not be verified");
    }

    @Test
    void testIsVerified_OrganizationNotFound_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);
        when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.empty());

        // Act
        boolean result = orgSecurityService.isVerified(authentication);

        // Assert
        assertFalse(result, "User with non-existent organization should not be verified");
    }

    // ==================== canManageOrganization Tests ====================

    @Test
    void testCanManageOrganization_Admin_AllOrgs_ShouldReturnTrue() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(adminUser);

        // Act
        boolean result1 = orgSecurityService.canManageOrganization(verifiedOrg.getId(), authentication);
        boolean result2 = orgSecurityService.canManageOrganization(pendingOrg.getId(), authentication);
        boolean result3 = orgSecurityService.canManageOrganization(UUID.randomUUID(), authentication);

        // Assert
        assertTrue(result1, "Admin should manage any organization");
        assertTrue(result2, "Admin should manage any organization");
        assertTrue(result3, "Admin should manage any organization");
    }

    @Test
    void testCanManageOrganization_UserOwnOrg_ShouldReturnTrue() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);

        // Act
        boolean result = orgSecurityService.canManageOrganization(verifiedOrg.getId(), authentication);

        // Assert
        assertTrue(result, "User should manage their own organization");
    }

    @Test
    void testCanManageOrganization_UserDifferentOrg_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);

        // Act
        boolean result = orgSecurityService.canManageOrganization(pendingOrg.getId(), authentication);

        // Assert
        assertFalse(result, "User should not manage other organizations");
    }

    @Test
    void testCanManageOrganization_UserNoOrg_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffNoOrg);

        // Act
        boolean result = orgSecurityService.canManageOrganization(verifiedOrg.getId(), authentication);

        // Assert
        assertFalse(result, "User without organization should not manage any organization");
    }

    @Test
    void testCanManageOrganization_NullUser_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(null);

        // Act
        boolean result = orgSecurityService.canManageOrganization(verifiedOrg.getId(), authentication);

        // Assert
        assertFalse(result, "Null user should not manage organizations");
    }

    // ==================== canVerifyOrganizations Tests ====================

    @Test
    void testCanVerifyOrganizations_Admin_ShouldReturnTrue() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(adminUser);

        // Act
        boolean result = orgSecurityService.canVerifyOrganizations(authentication);

        // Assert
        assertTrue(result, "Admin should be able to verify organizations");
    }

    @Test
    void testCanVerifyOrganizations_NGOStaff_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);

        // Act
        boolean result = orgSecurityService.canVerifyOrganizations(authentication);

        // Assert
        assertFalse(result, "NGO staff should not be able to verify organizations");
    }

    @Test
    void testCanVerifyOrganizations_FieldWorker_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(fieldWorker);

        // Act
        boolean result = orgSecurityService.canVerifyOrganizations(authentication);

        // Assert
        assertFalse(result, "Field worker should not be able to verify organizations");
    }

    @Test
    void testCanVerifyOrganizations_NullUser_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(null);

        // Act
        boolean result = orgSecurityService.canVerifyOrganizations(authentication);

        // Assert
        assertFalse(result, "Null user should not be able to verify organizations");
    }

    // ==================== canCreateOrganization Tests ====================

    @Test
    void testCanCreateOrganization_Admin_ShouldReturnTrue() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(adminUser);

        // Act
        boolean result = orgSecurityService.canCreateOrganization(authentication);

        // Assert
        assertTrue(result, "Admin should be able to create organizations");
    }

    @Test
    void testCanCreateOrganization_NGOStaff_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);

        // Act
        boolean result = orgSecurityService.canCreateOrganization(authentication);

        // Assert
        assertFalse(result, "NGO staff should not be able to create organizations");
    }

    @Test
    void testCanCreateOrganization_NullUser_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(null);

        // Act
        boolean result = orgSecurityService.canCreateOrganization(authentication);

        // Assert
        assertFalse(result, "Null user should not be able to create organizations");
    }

    // ==================== isOrganizationActive Tests ====================

    @Test
    void testIsOrganizationActive_VerifiedOrg_ShouldReturnTrue() {
        // Arrange
        when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.of(verifiedOrg));

        // Act
        boolean result = orgSecurityService.isOrganizationActive(verifiedOrg.getId());

        // Assert
        assertTrue(result, "Verified organization should be active");
    }

    @Test
    void testIsOrganizationActive_PendingOrg_ShouldReturnTrue() {
        // Arrange
        when(organizationRepository.findById(pendingOrg.getId())).thenReturn(Optional.of(pendingOrg));

        // Act
        boolean result = orgSecurityService.isOrganizationActive(pendingOrg.getId());

        // Assert
        assertTrue(result, "Pending organization should be active");
    }

    @Test
    void testIsOrganizationActive_SuspendedOrg_ShouldReturnFalse() {
        // Arrange
        when(organizationRepository.findById(suspendedOrg.getId())).thenReturn(Optional.of(suspendedOrg));

        // Act
        boolean result = orgSecurityService.isOrganizationActive(suspendedOrg.getId());

        // Assert
        assertFalse(result, "Suspended organization should not be active");
    }

    @Test
    void testIsOrganizationActive_NotFound_ShouldReturnFalse() {
        // Arrange
        UUID unknownOrgId = UUID.randomUUID();
        when(organizationRepository.findById(unknownOrgId)).thenReturn(Optional.empty());

        // Act
        boolean result = orgSecurityService.isOrganizationActive(unknownOrgId);

        // Assert
        assertFalse(result, "Non-existent organization should not be active");
    }

    // ==================== belongsToOrganization Tests ====================

    @Test
    void testBelongsToOrganization_UserInOrg_ShouldReturnTrue() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);

        // Act
        boolean result = orgSecurityService.belongsToOrganization(verifiedOrg.getId(), authentication);

        // Assert
        assertTrue(result, "User should belong to their organization");
    }

    @Test
    void testBelongsToOrganization_UserDifferentOrg_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);

        // Act
        boolean result = orgSecurityService.belongsToOrganization(pendingOrg.getId(), authentication);

        // Assert
        assertFalse(result, "User should not belong to different organization");
    }

    @Test
    void testBelongsToOrganization_UserNoOrg_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffNoOrg);

        // Act
        boolean result = orgSecurityService.belongsToOrganization(verifiedOrg.getId(), authentication);

        // Assert
        assertFalse(result, "User without organization should not belong to any organization");
    }

    @Test
    void testBelongsToOrganization_NullUser_ShouldReturnFalse() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(null);

        // Act
        boolean result = orgSecurityService.belongsToOrganization(verifiedOrg.getId(), authentication);

        // Assert
        assertFalse(result, "Null user should not belong to any organization");
    }

    // ==================== getUserOrganizationStatus Tests ====================

    @Test
    void testGetUserOrganizationStatus_VerifiedOrg_ReturnsVerified() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);
        when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.of(verifiedOrg));

        // Act
        OrganizationStatus status = orgSecurityService.getUserOrganizationStatus(authentication);

        // Assert
        assertEquals(OrganizationStatus.VERIFIED, status, "Should return VERIFIED status");
    }

    @Test
    void testGetUserOrganizationStatus_PendingOrg_ReturnsPending() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffPending);
        when(organizationRepository.findById(pendingOrg.getId())).thenReturn(Optional.of(pendingOrg));

        // Act
        OrganizationStatus status = orgSecurityService.getUserOrganizationStatus(authentication);

        // Assert
        assertEquals(OrganizationStatus.PENDING, status, "Should return PENDING status");
    }

    @Test
    void testGetUserOrganizationStatus_UserNoOrg_ReturnsNull() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffNoOrg);

        // Act
        OrganizationStatus status = orgSecurityService.getUserOrganizationStatus(authentication);

        // Assert
        assertNull(status, "User without organization should return null status");
        verify(organizationRepository, never()).findById(any());
    }

    @Test
    void testGetUserOrganizationStatus_NullUser_ReturnsNull() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(null);

        // Act
        OrganizationStatus status = orgSecurityService.getUserOrganizationStatus(authentication);

        // Assert
        assertNull(status, "Null user should return null status");
    }

    @Test
    void testGetUserOrganizationStatus_OrgNotFound_ReturnsNull() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);
        when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.empty());

        // Act
        OrganizationStatus status = orgSecurityService.getUserOrganizationStatus(authentication);

        // Assert
        assertNull(status, "Non-existent organization should return null status");
    }

    // ==================== Integration Scenarios ====================

    @Test
    void testVerificationGate_VerifiedOrgCanClaimNeeds() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffVerified);
        when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.of(verifiedOrg));

        // Act
        boolean isVerified = orgSecurityService.isVerified(authentication);
        OrganizationStatus status = orgSecurityService.getUserOrganizationStatus(authentication);

        // Assert
        assertTrue(isVerified, "Verified org should pass verification gate");
        assertEquals(OrganizationStatus.VERIFIED, status, "Status should be VERIFIED");
    }

    @Test
    void testVerificationGate_PendingOrgCannotClaimNeeds() {
        // Arrange
        when(authService.getCurrentUser(authentication)).thenReturn(ngoStaffPending);
        when(organizationRepository.findById(pendingOrg.getId())).thenReturn(Optional.of(pendingOrg));

        // Act
        boolean isVerified = orgSecurityService.isVerified(authentication);
        OrganizationStatus status = orgSecurityService.getUserOrganizationStatus(authentication);

        // Assert
        assertFalse(isVerified, "Pending org should not pass verification gate");
        assertEquals(OrganizationStatus.PENDING, status, "Status should be PENDING");
    }

    @Test
    void testSuspensionEnforcement_SuspendedOrgBlocked() {
        // Arrange
        User suspendedUser = new User();
        suspendedUser.setId(UUID.randomUUID());
        suspendedUser.setRole(UserRole.NGO_STAFF);
        suspendedUser.setOrganizationId(suspendedOrg.getId());

        when(authService.getCurrentUser(authentication)).thenReturn(suspendedUser);
        when(organizationRepository.findById(suspendedOrg.getId())).thenReturn(Optional.of(suspendedOrg));

        // Act
        boolean isVerified = orgSecurityService.isVerified(authentication);
        boolean isActive = orgSecurityService.isOrganizationActive(suspendedOrg.getId());
        OrganizationStatus status = orgSecurityService.getUserOrganizationStatus(authentication);

        // Assert
        assertFalse(isVerified, "Suspended org should not be verified");
        assertFalse(isActive, "Suspended org should not be active");
        assertEquals(OrganizationStatus.SUSPENDED, status, "Status should be SUSPENDED");
    }
}
