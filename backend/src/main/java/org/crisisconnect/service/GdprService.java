package org.crisisconnect.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.crisisconnect.model.entity.*;
import org.crisisconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;

/**
 * GDPR Compliance Service
 * Implements GDPR Article 15 (Right to Access) and Article 17 (Right to Erasure)
 */
@Service
public class GdprService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserConsentRepository userConsentRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Export all user data in JSON format (GDPR Article 15).
     *
     * @param userId User ID
     * @return JSON string containing all user data
     */
    public String exportUserDataAsJson(UUID userId) throws Exception {
        Map<String, Object> userData = collectUserData(userId);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userData);
    }

    /**
     * Export all user data in CSV format (GDPR Article 15).
     *
     * @param userId User ID
     * @return CSV string containing all user data
     */
    public String exportUserDataAsCsv(UUID userId) throws Exception {
        Map<String, Object> userData = collectUserData(userId);
        StringBuilder csv = new StringBuilder();

        // CSV Header
        csv.append("Category,Field,Value\n");

        // Flatten the nested map structure for CSV
        flattenMapToCsv(userData, "", csv);

        return csv.toString();
    }

    /**
     * Collect all user data from all tables.
     */
    private Map<String, Object> collectUserData(UUID userId) {
        Map<String, Object> data = new LinkedHashMap<>();

        // User profile data
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId().toString());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole().name());
        profile.put("organizationId", user.getOrganizationId() != null ? user.getOrganizationId().toString() : null);
        profile.put("isActive", user.getIsActive());
        profile.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        profile.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
        profile.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
        profile.put("lastLoginIp", user.getLastLoginIp());
        profile.put("mfaEnabled", user.getMfaEnabled());
        profile.put("passwordChangedAt", user.getPasswordChangedAt() != null ? user.getPasswordChangedAt().toString() : null);
        profile.put("passwordExpiresAt", user.getPasswordExpiresAt() != null ? user.getPasswordExpiresAt().toString() : null);
        data.put("profile", profile);

        // Consent records
        List<UserConsent> consents = userConsentRepository.findByUserId(userId);
        List<Map<String, Object>> consentList = new ArrayList<>();
        for (UserConsent consent : consents) {
            Map<String, Object> consentData = new LinkedHashMap<>();
            consentData.put("consentType", consent.getConsentType().name());
            consentData.put("granted", consent.getGranted());
            consentData.put("consentText", consent.getConsentText());
            consentData.put("consentVersion", consent.getConsentVersion());
            consentData.put("grantedAt", consent.getGrantedAt() != null ? consent.getGrantedAt().toString() : null);
            consentData.put("revokedAt", consent.getRevokedAt() != null ? consent.getRevokedAt().toString() : null);
            consentList.add(consentData);
        }
        data.put("consents", consentList);

        // Login history (last 100 attempts)
        List<LoginAttempt> loginAttempts = loginAttemptRepository.findByEmailOrderByAttemptedAtDesc(user.getEmail());
        List<Map<String, Object>> loginHistory = new ArrayList<>();
        int count = 0;
        for (LoginAttempt attempt : loginAttempts) {
            if (count++ >= 100) break;
            Map<String, Object> attemptData = new LinkedHashMap<>();
            attemptData.put("successful", attempt.getSuccessful());
            attemptData.put("ipAddress", attempt.getIpAddress());
            attemptData.put("userAgent", attempt.getUserAgent());
            attemptData.put("attemptedAt", attempt.getAttemptedAt() != null ? attempt.getAttemptedAt().toString() : null);
            attemptData.put("failureReason", attempt.getFailureReason());
            loginHistory.add(attemptData);
        }
        data.put("loginHistory", loginHistory);

        // Password history count (not actual hashes for security)
        List<PasswordHistory> passwordHistory = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        data.put("passwordChangeCount", passwordHistory.size());

        // Metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("exportedAt", LocalDateTime.now().toString());
        metadata.put("dataRetentionPolicy", "Data retained according to legal requirements");
        metadata.put("exportFormat", "GDPR Article 15 - Right to Access");
        data.put("metadata", metadata);

        return data;
    }

    /**
     * Flatten nested map to CSV format.
     */
    private void flattenMapToCsv(Map<String, Object> map, String prefix, StringBuilder csv) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenMapToCsv((Map<String, Object>) value, key, csv);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        flattenMapToCsv((Map<String, Object>) item, key + "[" + i + "]", csv);
                    } else {
                        csv.append(escapeCsv(key)).append("[").append(i).append("],")
                           .append("value,")
                           .append(escapeCsv(String.valueOf(item))).append("\n");
                    }
                }
            } else {
                csv.append(escapeCsv(key)).append(",")
                   .append("value,")
                   .append(escapeCsv(value != null ? String.valueOf(value) : "")).append("\n");
            }
        }
    }

    /**
     * Escape CSV special characters.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Delete user data (GDPR Article 17 - Right to Erasure).
     * Soft delete: Mark user as deleted but retain data for legal requirements.
     *
     * @param userId User ID
     * @param requestingUserId User requesting the deletion
     * @param ipAddress IP address of requester
     */
    @Transactional
    public void softDeleteUser(UUID userId, UUID requestingUserId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete: deactivate account
        user.setIsActive(false);
        user.setEmail("deleted_" + userId + "@deleted.local");
        user.setName("Deleted User");
        userRepository.save(user);

        // Log the deletion
        auditService.logAction(
            requestingUserId,
            "USER_SOFT_DELETE",
            "USER",
            userId,
            "User data soft deleted (GDPR Article 17)",
            ipAddress
        );
    }

    /**
     * Permanently delete user data (GDPR Article 17 - Right to Erasure).
     * Hard delete: Remove all user data from system.
     * WARNING: This cannot be undone!
     *
     * @param userId User ID
     * @param requestingUserId User requesting the deletion
     * @param ipAddress IP address of requester
     */
    @Transactional
    public void hardDeleteUser(UUID userId, UUID requestingUserId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Log the deletion BEFORE deleting (so we have audit trail)
        auditService.logAction(
            requestingUserId,
            "USER_HARD_DELETE",
            "USER",
            userId,
            "User data permanently deleted (GDPR Article 17)",
            ipAddress
        );

        // Delete all related data
        userConsentRepository.deleteAll(userConsentRepository.findByUserId(userId));
        passwordHistoryRepository.deleteByUserId(userId);

        // Note: LoginAttempt records may be retained for security audit purposes
        // Delete only if explicitly required by GDPR request

        // Finally, delete the user
        userRepository.delete(user);
    }

    /**
     * Anonymize user data (alternative to deletion for legal compliance).
     * Replaces PII with anonymized values while retaining statistical data.
     *
     * @param userId User ID
     * @param requestingUserId User requesting anonymization
     * @param ipAddress IP address of requester
     */
    @Transactional
    public void anonymizeUser(UUID userId, UUID requestingUserId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Anonymize personal data
        user.setName("Anonymous User " + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail("anonymous_" + userId + "@anonymized.local");
        user.setIsActive(false);
        user.setLastLoginIp(null);
        user.setMfaSecret(null);
        userRepository.save(user);

        // Anonymize login attempts
        List<LoginAttempt> attempts = loginAttemptRepository.findByEmailOrderByAttemptedAtDesc(user.getEmail());
        for (LoginAttempt attempt : attempts) {
            attempt.setEmail("anonymized@example.com");
            attempt.setIpAddress("0.0.0.0");
            attempt.setUserAgent("Anonymized");
            loginAttemptRepository.save(attempt);
        }

        // Log the anonymization
        auditService.logAction(
            requestingUserId,
            "USER_ANONYMIZED",
            "USER",
            userId,
            "User data anonymized (GDPR Article 17 alternative)",
            ipAddress
        );
    }

    /**
     * Check if user can request data deletion.
     * Some users (e.g., with active legal holds) cannot be deleted.
     */
    public boolean canDeleteUser(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        // Add business logic checks here
        // For example: check for active legal holds, ongoing investigations, etc.

        return true;
    }

    /**
     * Get data retention period for user data.
     */
    public int getDataRetentionDays(UUID userId) {
        // Return data retention period based on legal requirements
        // This can vary by user role, jurisdiction, etc.
        return 90; // Default: 90 days
    }
}
