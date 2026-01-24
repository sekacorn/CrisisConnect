package org.crisisconnect.service;

import org.crisisconnect.dto.CreateNeedRequest;
import org.crisisconnect.dto.FullNeedResponse;
import org.crisisconnect.dto.RedactedNeedResponse;
import org.crisisconnect.dto.UpdateNeedRequest;
import org.crisisconnect.model.entity.*;
import org.crisisconnect.model.enums.NeedStatus;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Need service with RBAC and privacy filtering.
 * Implements security-first approach with redacted responses by default.
 */
@Service
public class NeedService {

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private SensitiveInfoRepository sensitiveInfoRepository;

    @Autowired
    private NeedUpdateRepository needUpdateRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private AuditService auditService;

    @Transactional
    public FullNeedResponse createNeed(CreateNeedRequest request, UUID userId, String ipAddress) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Need need = new Need();
        need.setCreatedByUserId(userId);
        need.setStatus(NeedStatus.NEW);
        need.setCategory(request.getCategory());
        need.setCountry(request.getCountry());
        need.setRegionOrState(request.getRegionOrState());
        need.setCity(request.getCity());
        need.setUrgencyLevel(request.getUrgencyLevel());

        need = needRepository.save(need);

        // Store encrypted PII separately (including description which may contain sensitive info)
        SensitiveInfo sensitiveInfo = new SensitiveInfo();
        sensitiveInfo.setNeed(need);  // Set the Need entity, not just the ID

        // Encrypt all sensitive fields
        if (request.getEncryptedFullName() != null) {
            sensitiveInfo.setEncryptedFullName(encryptionService.encrypt(request.getEncryptedFullName()));
        }
        if (request.getEncryptedPhone() != null) {
            sensitiveInfo.setEncryptedPhone(encryptionService.encrypt(request.getEncryptedPhone()));
        }
        if (request.getEncryptedEmail() != null) {
            sensitiveInfo.setEncryptedEmail(encryptionService.encrypt(request.getEncryptedEmail()));
        }
        if (request.getEncryptedExactLocation() != null) {
            sensitiveInfo.setEncryptedExactLocation(encryptionService.encrypt(request.getEncryptedExactLocation()));
        }
        // Store description in encryptedNotes (description may contain sensitive info)
        if (request.getDescription() != null) {
            sensitiveInfo.setEncryptedNotes(encryptionService.encrypt(request.getDescription()));
        } else if (request.getEncryptedNotes() != null) {
            sensitiveInfo.setEncryptedNotes(encryptionService.encrypt(request.getEncryptedNotes()));
        }

        sensitiveInfoRepository.save(sensitiveInfo);

        auditService.logAction(userId, "CREATE_NEED", "NEED", need.getId(), null, ipAddress);

        return mapToFullResponse(need, sensitiveInfo);
    }

    public List<RedactedNeedResponse> getAllNeeds(UUID userId) {
        List<Need> needs = needRepository.findAll();
        return needs.stream()
                .map(this::mapToRedactedResponse)
                .collect(Collectors.toList());
    }

    public Object getNeedById(UUID needId, UUID userId, String ipAddress) {
        Need need = needRepository.findById(needId)
                .orElseThrow(() -> new RuntimeException("Need not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user has full access
        if (hasFullAccess(user, need)) {
            SensitiveInfo sensitiveInfo = sensitiveInfoRepository.findByNeed_Id(needId).orElse(null);
            auditService.logNeedAccess(userId, needId, true, ipAddress);
            if (sensitiveInfo != null) {
                auditService.logSensitiveInfoAccess(userId, needId, ipAddress);
            }
            return mapToFullResponse(need, sensitiveInfo);
        } else {
            auditService.logNeedAccess(userId, needId, false, ipAddress);
            return mapToRedactedResponse(need);
        }
    }

    @Transactional
    public void updateNeed(UUID needId, UpdateNeedRequest request, UUID userId, String ipAddress) {
        Need need = needRepository.findById(needId)
                .orElseThrow(() -> new RuntimeException("Need not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // RBAC: Only authorized users can update
        if (!canUpdateNeed(user, need)) {
            throw new RuntimeException("Unauthorized to update this need");
        }

        NeedStatus oldStatus = need.getStatus();
        need.setStatus(request.getStatus());

        if (request.getStatus() == NeedStatus.ASSIGNED && need.getAssignedOrganizationId() == null) {
            need.setAssignedOrganizationId(user.getOrganizationId());
            need.setAssignedAt(LocalDateTime.now());
        }

        if (request.getStatus() == NeedStatus.RESOLVED) {
            need.setResolvedAt(LocalDateTime.now());
        }

        needRepository.save(need);

        // Create update record
        NeedUpdate update = new NeedUpdate();
        update.setNeedId(needId);
        update.setUpdatedByUserId(userId);
        update.setStatusFrom(oldStatus);
        update.setStatusTo(request.getStatus());
        update.setComment(request.getComment());
        needUpdateRepository.save(update);

        auditService.logAction(userId, "UPDATE_NEED", "NEED", needId,
                "Status: " + oldStatus + " -> " + request.getStatus(), ipAddress);
    }

    /**
     * Claim a need (assign to user's organization)
     *
     * This method handles the business logic for claiming a need:
     * - Updates the need's assigned organization
     * - Sets the assigned timestamp
     * - Logs the claim action to audit log
     *
     * Authorization checks should be performed by the controller/security service before calling this method
     *
     * @param needId ID of the need to claim
     * @param userId ID of the user claiming the need
     * @param ipAddress IP address of the request (for audit logging)
     */
    @Transactional
    public void claimNeed(UUID needId, UUID userId, String ipAddress) {
        Need need = needRepository.findById(needId)
                .orElseThrow(() -> new RuntimeException("Need not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOrganizationId() == null) {
            throw new RuntimeException("User must belong to an organization to claim needs");
        }

        // Update need with assignment information
        need.setAssignedOrganizationId(user.getOrganizationId());
        need.setAssignedAt(LocalDateTime.now());
        need.setStatus(NeedStatus.ASSIGNED);
        needRepository.save(need);

        // Create need update record
        NeedUpdate update = new NeedUpdate();
        update.setNeedId(needId);
        update.setUpdatedByUserId(userId);
        update.setStatusFrom(need.getStatus());
        update.setStatusTo(NeedStatus.ASSIGNED);
        update.setComment("Need claimed by organization: " + user.getOrganizationId());
        needUpdateRepository.save(update);

        // Audit log the claim action
        auditService.logAction(
            userId,
            "NEED_CLAIMED",
            "NEED",
            needId,
            "Organization: " + user.getOrganizationId(),
            ipAddress
        );
    }

    private boolean hasFullAccess(User user, Need need) {
        // ADMIN has full access
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // Creator has full access
        if (need.getCreatedByUserId().equals(user.getId())) {
            return true;
        }

        // NGO_STAFF from assigned organization has full access
        if (user.getRole() == UserRole.NGO_STAFF &&
                need.getAssignedOrganizationId() != null &&
                need.getAssignedOrganizationId().equals(user.getOrganizationId())) {

            // Verify organization is verified
            Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
            return org != null && org.getStatus() == OrganizationStatus.VERIFIED;
        }

        return false;
    }

    private boolean canUpdateNeed(User user, Need need) {
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        if (user.getRole() == UserRole.NGO_STAFF) {
            Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
            return org != null && org.getStatus() == OrganizationStatus.VERIFIED;
        }

        return false;
    }

    private RedactedNeedResponse mapToRedactedResponse(Need need) {
        RedactedNeedResponse response = new RedactedNeedResponse();
        response.setId(need.getId());
        response.setStatus(need.getStatus());
        response.setCategory(need.getCategory());
        response.setCountry(need.getCountry());
        response.setRegionOrState(need.getRegionOrState());
        response.setUrgencyLevel(need.getUrgencyLevel());
        // No vulnerability flags in redacted response - that info is sensitive
        response.setGeneralizedVulnerabilityFlags("Contact for details");
        response.setCreatedAt(need.getCreatedAt());
        return response;
    }

    private FullNeedResponse mapToFullResponse(Need need, SensitiveInfo sensitiveInfo) {
        FullNeedResponse response = new FullNeedResponse();
        response.setId(need.getId());
        response.setCreatedByUserId(need.getCreatedByUserId());
        response.setStatus(need.getStatus());
        response.setCategory(need.getCategory());
        response.setCountry(need.getCountry());
        response.setRegionOrState(need.getRegionOrState());
        response.setCity(need.getCity());
        response.setUrgencyLevel(need.getUrgencyLevel());
        response.setAssignedOrganizationId(need.getAssignedOrganizationId());
        response.setAssignedAt(need.getAssignedAt());
        response.setResolvedAt(need.getResolvedAt());
        response.setClosedAt(need.getClosedAt());
        response.setCreatedAt(need.getCreatedAt());
        response.setUpdatedAt(need.getUpdatedAt());

        // Decrypt sensitive info if available
        if (sensitiveInfo != null) {
            // Decrypt and map to response field names
            if (sensitiveInfo.getEncryptedFullName() != null) {
                response.setFullName(encryptionService.decrypt(sensitiveInfo.getEncryptedFullName()));
            }
            if (sensitiveInfo.getEncryptedPhone() != null) {
                response.setPhone(encryptionService.decrypt(sensitiveInfo.getEncryptedPhone()));
            }
            if (sensitiveInfo.getEncryptedEmail() != null) {
                response.setEmail(encryptionService.decrypt(sensitiveInfo.getEncryptedEmail()));
            }
            if (sensitiveInfo.getEncryptedExactLocation() != null) {
                response.setExactLocation(encryptionService.decrypt(sensitiveInfo.getEncryptedExactLocation()));
            }
            if (sensitiveInfo.getEncryptedNotes() != null) {
                // Map encryptedNotes to description in response
                String decryptedNotes = encryptionService.decrypt(sensitiveInfo.getEncryptedNotes());
                response.setDescription(decryptedNotes);
                response.setNotes(decryptedNotes);
            }
        }

        return response;
    }
}
