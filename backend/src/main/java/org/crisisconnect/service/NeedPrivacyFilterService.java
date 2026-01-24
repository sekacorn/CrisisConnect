package org.crisisconnect.service;

import org.crisisconnect.dto.FullNeedResponse;
import org.crisisconnect.dto.RedactedNeedResponse;
import org.crisisconnect.model.entity.Need;
import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.SensitiveInfo;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.OrganizationRepository;
import org.crisisconnect.repository.SensitiveInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Privacy & Redaction Engine Service
 *
 * Centralizes privacy filtering logic for Need responses.
 * Implements spec/30_privacy_redaction.md requirements.
 *
 * Key Principles:
 * - Redacted by default for all users
 * - Full details only for authorized users (creator, assigned verified NGO, admin)
 * - All full access events are audit-logged
 * - Decryption happens server-side only, never client-side
 */
@Service
public class NeedPrivacyFilterService {

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private SensitiveInfoRepository sensitiveInfoRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Filter single need for current user
     * Returns RedactedNeedResponse or FullNeedResponse based on authorization
     */
    public Object filterNeed(Need need, User currentUser, String ipAddress) {
        if (canViewFullDetails(need, currentUser)) {
            auditService.logNeedAccess(currentUser.getId(), need.getId(), true, ipAddress);
            return buildFullResponse(need, currentUser);
        } else {
            auditService.logNeedAccess(currentUser.getId(), need.getId(), false, ipAddress);
            return buildRedactedResponse(need);
        }
    }

    /**
     * Filter list of needs (always redacted for lists)
     * Even admins get redacted list view for performance and privacy
     */
    public List<RedactedNeedResponse> filterNeedsList(List<Need> needs) {
        return needs.stream()
                .map(this::buildRedactedResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if user can view full details of a need
     *
     * Authorization rules:
     * 1. ADMIN always has full access
     * 2. Creator (FIELD_WORKER or NGO_STAFF) has full access to their own needs
     * 3. NGO_STAFF from assigned VERIFIED org has full access
     */
    private boolean canViewFullDetails(Need need, User user) {
        // ADMIN always has full access
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // Creator has full access to their own needs
        if (need.getCreatedByUserId() != null && need.getCreatedByUserId().equals(user.getId())) {
            return true;
        }

        // NGO_STAFF from assigned VERIFIED org has full access
        if (user.getRole() == UserRole.NGO_STAFF
                && user.getOrganizationId() != null
                && need.getAssignedOrganizationId() != null
                && need.getAssignedOrganizationId().equals(user.getOrganizationId())) {

            // Verify organization status is VERIFIED
            Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
            return org != null && org.getStatus() == OrganizationStatus.VERIFIED;
        }

        return false;
    }

    /**
     * Build redacted response (default view)
     *
     * Includes: ID, category, status, urgency, country, generalized region,
     * aggregated vulnerability flags, timestamps
     *
     * Excludes: PII, exact location, full description, creator details,
     * assignment details, decrypted sensitive info
     */
    private RedactedNeedResponse buildRedactedResponse(Need need) {
        RedactedNeedResponse response = new RedactedNeedResponse();
        response.setId(need.getId());
        response.setCategory(need.getCategory());
        response.setStatus(need.getStatus());
        response.setUrgencyLevel(need.getUrgencyLevel());
        response.setCountry(need.getCountry());
        response.setRegionOrState(generalizeRegion(need.getRegionOrState()));
        // Don't show vulnerability info in redacted view - contact for details
        response.setGeneralizedVulnerabilityFlags("Contact for details");
        response.setCreatedAt(need.getCreatedAt());
        return response;
    }

    /**
     * Build full response (authorized view)
     *
     * Includes all redacted fields PLUS:
     * - Full description
     * - Exact location details
     * - Decrypted beneficiary information
     * - Detailed vulnerability flags
     * - Assignment details
     * - Creator information
     */
    private FullNeedResponse buildFullResponse(Need need, User currentUser) {
        SensitiveInfo sensitiveInfo = sensitiveInfoRepository.findByNeed_Id(need.getId())
                .orElse(null);

        FullNeedResponse response = new FullNeedResponse();

        // Include all redacted fields
        response.setId(need.getId());
        response.setCategory(need.getCategory());
        response.setStatus(need.getStatus());
        response.setUrgencyLevel(need.getUrgencyLevel());
        response.setCountry(need.getCountry());

        // Full region, not generalized
        response.setRegionOrState(need.getRegionOrState());
        response.setCity(need.getCity());

        // Assignment and creator details
        response.setCreatedByUserId(need.getCreatedByUserId());
        response.setAssignedOrganizationId(need.getAssignedOrganizationId());
        response.setAssignedAt(need.getAssignedAt());
        response.setResolvedAt(need.getResolvedAt());
        response.setClosedAt(need.getClosedAt());

        // Timestamps
        response.setCreatedAt(need.getCreatedAt());
        response.setUpdatedAt(need.getUpdatedAt());

        // Decrypt and add sensitive info if available
        if (sensitiveInfo != null) {
            try {
                // Decrypt full name
                if (sensitiveInfo.getEncryptedFullName() != null) {
                    response.setFullName(
                        encryptionService.decrypt(sensitiveInfo.getEncryptedFullName()));
                }

                // Decrypt phone
                if (sensitiveInfo.getEncryptedPhone() != null) {
                    response.setPhone(
                        encryptionService.decrypt(sensitiveInfo.getEncryptedPhone()));
                }

                // Decrypt email
                if (sensitiveInfo.getEncryptedEmail() != null) {
                    response.setEmail(
                        encryptionService.decrypt(sensitiveInfo.getEncryptedEmail()));
                }

                // Decrypt exact location
                if (sensitiveInfo.getEncryptedExactLocation() != null) {
                    response.setExactLocation(
                        encryptionService.decrypt(sensitiveInfo.getEncryptedExactLocation()));
                }

                // Decrypt notes (includes description)
                if (sensitiveInfo.getEncryptedNotes() != null) {
                    String decryptedNotes = encryptionService.decrypt(sensitiveInfo.getEncryptedNotes());
                    response.setDescription(decryptedNotes);
                    response.setNotes(decryptedNotes);
                }

                // Log sensitive info access separately
                auditService.logSensitiveInfoAccess(currentUser.getId(), need.getId(),
                    "Decrypted PII fields");

            } catch (Exception e) {
                // Log decryption failure but don't expose error to client
                auditService.logAction(currentUser.getId(), "DECRYPTION_FAILED", "NEED",
                    need.getId(), "Failed to decrypt sensitive info", "system");
                // Return response without decrypted fields
            }
        }

        return response;
    }

    /**
     * Generalize region to prevent exact location disclosure
     *
     * Example: "Central Region, Kampala District, Plot 123" -> "Central Region"
     */
    private String generalizeRegion(String region) {
        if (region == null) {
            return null;
        }

        // Keep only first part before comma (district/state level)
        String[] parts = region.split(",");
        return parts[0].trim();
    }

}
