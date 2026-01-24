package org.crisisconnect.service;

import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Organization Security Service for RBAC
 *
 * Implements authorization checks for organization-related operations.
 * Used by @PreAuthorize annotations in controllers.
 *
 * Implements spec/20_roles_rbac.md requirements.
 */
@Service("orgSecurityService")
public class OrganizationSecurityService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AuthService authService;

    /**
     * Check if user's organization is VERIFIED
     *
     * This is the critical gate for NGO_STAFF to access full PII and claim needs.
     *
     * @param authentication Spring Security authentication
     * @return true if user belongs to VERIFIED organization
     */
    public boolean isVerified(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        if (user == null || user.getOrganizationId() == null) {
            return false;
        }

        Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
        return org != null && org.getStatus() == OrganizationStatus.VERIFIED;
    }

    /**
     * Check if user can manage/view organization details
     *
     * Rules:
     * - ADMIN can manage all organizations
     * - NGO_STAFF can view their own organization
     *
     * @param orgId Organization ID
     * @param authentication Spring Security authentication
     * @return true if authorized
     */
    public boolean canManageOrganization(UUID orgId, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        if (user == null) {
            return false;
        }

        // ADMIN can manage all organizations
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // Users can view their own organization
        if (user.getOrganizationId() != null && user.getOrganizationId().equals(orgId)) {
            return true;
        }

        return false;
    }

    /**
     * Check if user can verify organizations
     * Only ADMIN can verify/suspend organizations
     *
     * @param authentication Spring Security authentication
     * @return true if ADMIN
     */
    public boolean canVerifyOrganizations(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    /**
     * Check if user can create new organizations
     * Currently only ADMIN can create organizations (future: self-registration)
     *
     * @param authentication Spring Security authentication
     * @return true if authorized
     */
    public boolean canCreateOrganization(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    /**
     * Check if organization is not suspended
     *
     * @param orgId Organization ID
     * @return true if active (PENDING or VERIFIED)
     */
    public boolean isOrganizationActive(UUID orgId) {
        Organization org = organizationRepository.findById(orgId).orElse(null);
        return org != null && org.getStatus() != OrganizationStatus.SUSPENDED;
    }

    /**
     * Check if user belongs to a specific organization
     *
     * @param orgId Organization ID
     * @param authentication Spring Security authentication
     * @return true if user belongs to this org
     */
    public boolean belongsToOrganization(UUID orgId, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        return user != null
                && user.getOrganizationId() != null
                && user.getOrganizationId().equals(orgId);
    }

    /**
     * Get organization status for user
     *
     * @param authentication Spring Security authentication
     * @return OrganizationStatus or null if no organization
     */
    public OrganizationStatus getUserOrganizationStatus(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        if (user == null || user.getOrganizationId() == null) {
            return null;
        }

        Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
        return org != null ? org.getStatus() : null;
    }
}
