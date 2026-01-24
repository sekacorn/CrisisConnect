package org.crisisconnect.service;

import org.crisisconnect.model.entity.Need;
import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.ServiceArea;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.NeedStatus;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.NeedRepository;
import org.crisisconnect.repository.OrganizationRepository;
import org.crisisconnect.repository.ServiceAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Need Security Service for RBAC
 *
 * Implements authorization checks for need-related operations.
 * Used by @PreAuthorize annotations in controllers.
 *
 * Implements spec/20_roles_rbac.md requirements.
 */
@Service("needSecurityService")
public class NeedSecurityService {

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ServiceAreaRepository serviceAreaRepository;

    @Autowired
    private AuthService authService;

    /**
     * Check if user can access a need (view details)
     *
     * Authorization rules:
     * - ADMIN: Can access all needs
     * - Creator: Can access own created needs
     * - NGO_STAFF (verified, assigned): Can access needs assigned to their org
     *
     * @param needId Need ID
     * @param authentication Spring Security authentication
     * @return true if authorized
     */
    public boolean canAccessNeed(UUID needId, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        if (user == null) {
            return false;
        }

        Need need = needRepository.findById(needId).orElse(null);
        if (need == null) {
            return false;
        }

        // ADMIN has full access
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // Creator has access to their own needs
        if (need.getCreatedByUserId() != null && need.getCreatedByUserId().equals(user.getId())) {
            return true;
        }

        // NGO_STAFF from assigned VERIFIED org has access
        if (user.getRole() == UserRole.NGO_STAFF
                && user.getOrganizationId() != null
                && need.getAssignedOrganizationId() != null
                && need.getAssignedOrganizationId().equals(user.getOrganizationId())) {

            Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
            return org != null && org.getStatus() == OrganizationStatus.VERIFIED;
        }

        return false;
    }

    /**
     * Check if user can claim/assign a need to their organization
     *
     * Requirements:
     * - User must be NGO_STAFF or ADMIN
     * - Organization must be VERIFIED
     * - Need must be in PENDING or NEW status
     * - Need must match organization's service areas
     *
     * @param needId Need ID
     * @param authentication Spring Security authentication
     * @return true if can claim
     */
    public boolean canClaimNeed(UUID needId, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        if (user == null) {
            return false;
        }

        Need need = needRepository.findById(needId).orElse(null);
        if (need == null) {
            return false;
        }

        // Only NGO_STAFF and ADMIN can claim
        if (user.getRole() != UserRole.NGO_STAFF && user.getRole() != UserRole.ADMIN) {
            return false;
        }

        // Need must be unclaimed (NEW status)
        if (need.getStatus() != NeedStatus.NEW) {
            return false;
        }

        // ADMIN can claim any need
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // NGO_STAFF specific checks
        if (user.getRole() == UserRole.NGO_STAFF) {
            // Must have organization
            if (user.getOrganizationId() == null) {
                return false;
            }

            // Organization must be VERIFIED
            Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
            if (org == null || org.getStatus() != OrganizationStatus.VERIFIED) {
                return false;
            }

            // Check if need matches organization's service areas
            return matchesServiceArea(need, org);
        }

        return false;
    }

    /**
     * Check if user can update a need (change status, add notes)
     *
     * Requirements:
     * - ADMIN can update any need
     * - NGO_STAFF can update needs assigned to their VERIFIED org
     *
     * @param needId Need ID
     * @param authentication Spring Security authentication
     * @return true if can update
     */
    public boolean canUpdateNeed(UUID needId, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        if (user == null) {
            return false;
        }

        Need need = needRepository.findById(needId).orElse(null);
        if (need == null) {
            return false;
        }

        // ADMIN can update any need
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // NGO_STAFF can update needs assigned to their organization
        if (user.getRole() == UserRole.NGO_STAFF
                && user.getOrganizationId() != null
                && need.getAssignedOrganizationId() != null
                && need.getAssignedOrganizationId().equals(user.getOrganizationId())) {

            // Verify organization is VERIFIED
            Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
            return org != null && org.getStatus() == OrganizationStatus.VERIFIED;
        }

        return false;
    }

    /**
     * Check if need is assigned to user's organization
     *
     * @param needId Need ID
     * @param authentication Spring Security authentication
     * @return true if assigned to user's org
     */
    public boolean isAssignedToUserOrg(UUID needId, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        if (user == null || user.getOrganizationId() == null) {
            return false;
        }

        Need need = needRepository.findById(needId).orElse(null);
        if (need == null) {
            return false;
        }

        return need.getAssignedOrganizationId() != null
                && need.getAssignedOrganizationId().equals(user.getOrganizationId());
    }

    /**
     * Check if need matches organization's service areas
     *
     * A match requires:
     * - Country matches
     * - Region/state matches
     * - Category matches
     * - Service area is active
     *
     * @param need The need to check
     * @param org The organization
     * @return true if matches at least one service area
     */
    private boolean matchesServiceArea(Need need, Organization org) {
        // TODO: Re-enable proper service area matching once ServiceArea entity is fixed
        // Temporary implementation: check if organization has any service areas in the same country
        List<ServiceArea> serviceAreas = serviceAreaRepository.findByOrganizationId(org.getId());
        return serviceAreas.stream()
                .anyMatch(sa -> sa.getCountry().equalsIgnoreCase(need.getCountry()) &&
                        sa.getServiceCategories().contains(need.getCategory()));
    }

    /**
     * Check if user can view need in list (basic visibility check)
     * This is less restrictive than full access
     *
     * @param need The need
     * @param user The user
     * @return true if should be visible in lists
     */
    public boolean canViewNeedInList(Need need, User user) {
        // ADMIN sees all
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // FIELD_WORKER sees needs they created + needs in their region (future)
        if (user.getRole() == UserRole.FIELD_WORKER) {
            if (need.getCreatedByUserId() != null && need.getCreatedByUserId().equals(user.getId())) {
                return true;
            }
            // TODO: Add region-based filtering for field workers
        }

        // NGO_STAFF sees needs in their service areas
        if (user.getRole() == UserRole.NGO_STAFF && user.getOrganizationId() != null) {
            Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
            if (org != null && matchesServiceArea(need, org)) {
                return true;
            }
        }

        // BENEFICIARY sees only their own case (future enhancement)
        // Currently not implemented

        return false;
    }
}
