package org.crisisconnect.service;

import org.crisisconnect.dto.*;
import org.crisisconnect.model.entity.AuditLog;
import org.crisisconnect.model.entity.Need;
import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.NeedStatus;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin service for dashboard operations
 *
 * Provides:
 * - Organization verification and management
 * - User management
 * - Audit log viewing
 * - Suspicious activity monitoring
 * - System statistics and analytics
 */
@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Get comprehensive admin dashboard statistics
     */
    public AdminStatsResponse getDashboardStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        // User statistics
        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countByIsActive(true));
        stats.setUsersByRole(getUsersByRole());

        // Organization statistics
        stats.setTotalOrganizations(organizationRepository.count());
        stats.setVerifiedOrganizations(organizationRepository.countByStatus(OrganizationStatus.VERIFIED));
        stats.setPendingOrganizations(organizationRepository.countByStatus(OrganizationStatus.PENDING));

        // Need statistics
        stats.setTotalNeeds(needRepository.count());
        stats.setNeedsByStatus(getNeedsByStatus());
        stats.setNeedsByCategory(getNeedsByCategory());
        stats.setNeedsByUrgency(getNeedsByUrgency());

        // Activity statistics (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        stats.setRecentLogins24h(auditLogRepository.countByActionTypeAndCreatedAtAfter("LOGIN", yesterday));
        stats.setRecentNeedsCreated24h(auditLogRepository.countByActionTypeAndCreatedAtAfter("CREATE_NEED", yesterday));
        stats.setRecentNeedsClaimed24h(auditLogRepository.countByActionTypeAndCreatedAtAfter("CLAIM_NEED", yesterday));

        // Security statistics
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        stats.setSuspiciousActivities30d(countSuspiciousActivities(last30Days));
        stats.setRateLimitViolations24h(auditLogRepository.countByActionTypeAndCreatedAtAfter("RATE_LIMIT_EXCEEDED", yesterday));
        stats.setFailedLoginAttempts24h(auditLogRepository.countByActionTypeAndCreatedAtAfter("LOGIN_FAILED", yesterday));

        return stats;
    }

    /**
     * Get all users with pagination
     */
    public Page<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(pageable);
    }

    /**
     * Get all organizations with pagination
     */
    public Page<Organization> getAllOrganizations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return organizationRepository.findAll(pageable);
    }

    /**
     * Get organizations by status
     */
    public List<Organization> getOrganizationsByStatus(OrganizationStatus status) {
        return organizationRepository.findByStatus(status);
    }

    /**
     * Update organization status (verification)
     */
    @Transactional
    public void updateOrganizationStatus(UUID organizationId, UpdateOrganizationRequest request, UUID adminUserId, String ipAddress) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        OrganizationStatus oldStatus = org.getStatus();
        org.setStatus(request.getStatus());
        organizationRepository.save(org);

        // Audit log
        String details = String.format("Status changed from %s to %s. Notes: %s",
            oldStatus, request.getStatus(), request.getVerificationNotes());
        auditService.logAction(adminUserId, "VERIFY_ORGANIZATION", "ORGANIZATION", organizationId, details, ipAddress);
    }

    /**
     * Update user
     */
    @Transactional
    public void updateUser(UUID userId, UpdateUserRequest request, UUID adminUserId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        userRepository.save(user);

        // Audit log
        String details = String.format("User updated: name=%s, role=%s, active=%s",
            request.getName(), request.getRole(), request.getIsActive());
        auditService.logAction(adminUserId, "UPDATE_USER", "USER", userId, details, ipAddress);
    }

    /**
     * Get audit logs with pagination
     */
    public Page<AuditLogResponse> getAuditLogs(int page, int size, String actionType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> auditLogs;

        if (actionType != null && !actionType.isEmpty()) {
            auditLogs = auditLogRepository.findByActionType(actionType, pageable);
        } else {
            auditLogs = auditLogRepository.findAll(pageable);
        }

        return auditLogs.map(this::mapToAuditLogResponse);
    }

    /**
     * Get suspicious activities
     */
    public List<SuspiciousActivityResponse> getSuspiciousActivities(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AuditLog> logs = auditLogRepository.findRecentSuspiciousActivities(since);

        return logs.stream()
                .map(this::mapToSuspiciousActivityResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get audit logs for specific user
     */
    public List<AuditLogResponse> getUserAuditLogs(UUID userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();
        List<AuditLog> logs = auditLogRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        return logs.stream()
                .map(this::mapToAuditLogResponse)
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    private Map<String, Long> getUsersByRole() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .collect(Collectors.groupingBy(
                    user -> user.getRole().name(),
                    Collectors.counting()
                ));
    }

    private Map<String, Long> getNeedsByStatus() {
        List<Need> needs = needRepository.findAll();
        return needs.stream()
                .collect(Collectors.groupingBy(
                    need -> need.getStatus().name(),
                    Collectors.counting()
                ));
    }

    private Map<String, Long> getNeedsByCategory() {
        List<Need> needs = needRepository.findAll();
        return needs.stream()
                .collect(Collectors.groupingBy(
                    need -> need.getCategory().name(),
                    Collectors.counting()
                ));
    }

    private Map<String, Long> getNeedsByUrgency() {
        List<Need> needs = needRepository.findAll();
        return needs.stream()
                .collect(Collectors.groupingBy(
                    need -> need.getUrgencyLevel().name(),
                    Collectors.counting()
                ));
    }

    private long countSuspiciousActivities(LocalDateTime since) {
        List<String> suspiciousActions = Arrays.asList(
            "SUSPICIOUS_BROWSING",
            "ANOMALOUS_CREATION_RATE",
            "RATE_LIMIT_EXCEEDED"
        );

        return suspiciousActions.stream()
                .mapToLong(action -> auditLogRepository.countByActionTypeAndCreatedAtAfter(action, since))
                .sum();
    }

    private AuditLogResponse mapToAuditLogResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setUserId(log.getUserId());
        response.setActionType(log.getActionType());
        response.setEntityType(log.getTargetType());
        response.setEntityId(log.getTargetId());
        response.setDetails(log.getMetadata());
        response.setIpAddress(log.getIpAddress());
        response.setCreatedAt(log.getCreatedAt());

        // Fetch user details if userId is present
        if (log.getUserId() != null) {
            userRepository.findById(log.getUserId()).ifPresent(user -> {
                response.setUserEmail(user.getEmail());
                response.setUserName(user.getName());
            });
        }

        return response;
    }

    private SuspiciousActivityResponse mapToSuspiciousActivityResponse(AuditLog log) {
        SuspiciousActivityResponse response = new SuspiciousActivityResponse();
        response.setId(log.getId());
        response.setUserId(log.getUserId());
        response.setActivityType(log.getActionType());
        response.setDescription(log.getMetadata());
        response.setDetectedAt(log.getCreatedAt());

        // Determine severity based on action type
        response.setSeverity(determineSeverity(log.getActionType()));

        // Fetch user details
        if (log.getUserId() != null) {
            userRepository.findById(log.getUserId()).ifPresent(user -> {
                response.setUserEmail(user.getEmail());
                response.setUserName(user.getName());
                response.setUserRole(user.getRole().name());
            });
        }

        // Extract activity count from metadata if available
        response.setActivityCount(extractCountFromDetails(log.getMetadata()));

        return response;
    }

    private String determineSeverity(String actionType) {
        switch (actionType) {
            case "ANOMALOUS_CREATION_RATE":
                return "HIGH";
            case "SUSPICIOUS_BROWSING":
                return "MEDIUM";
            case "RATE_LIMIT_EXCEEDED":
                return "LOW";
            default:
                return "LOW";
        }
    }

    private Long extractCountFromDetails(String metadata) {
        if (metadata == null) return 0L;

        // Try to extract number from metadata like "viewed 52 needs" or "created 105 needs"
        try {
            String[] parts = metadata.split(" ");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i + 1].equals("needs") || parts[i + 1].equals("views") || parts[i + 1].equals("attempts")) {
                    return Long.parseLong(parts[i]);
                }
            }
        } catch (NumberFormatException e) {
            // Ignore parsing errors
        }

        return 0L;
    }
}
