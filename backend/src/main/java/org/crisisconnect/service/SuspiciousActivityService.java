package org.crisisconnect.service;

import org.crisisconnect.model.entity.AuditLog;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.AuditLogRepository;
import org.crisisconnect.repository.NeedRepository;
import org.crisisconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Suspicious Activity Detection Service
 *
 * Implements automated fraud detection through scheduled jobs.
 * Detects and logs suspicious patterns of behavior.
 *
 * Implements spec/35_safeguarding_abuse_prevention.md requirements.
 *
 * Scheduled Jobs:
 * 1. Suspicious Browsing Pattern Detection (hourly)
 *    - Detects NGO staff viewing 50+ needs in 24h without claiming any
 *    - Flags potential data harvesting
 *
 * 2. Anomalous Creation Rate Detection (daily at 8 AM)
 *    - Detects field workers creating 100+ needs in 7 days
 *    - Flags potential fraud or misuse
 *
 * All suspicious activities are logged to audit_logs table with
 * action type "SUSPICIOUS_*" for admin dashboard review.
 */
@Service
public class SuspiciousActivityService {

    private static final Logger logger = LoggerFactory.getLogger(SuspiciousActivityService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    // Detection thresholds
    private static final int SUSPICIOUS_VIEW_THRESHOLD = 50;  // 50+ views in 24h
    private static final int SUSPICIOUS_CREATION_THRESHOLD = 100;  // 100+ needs in 7 days
    private static final int VIEW_WINDOW_HOURS = 24;
    private static final int CREATION_WINDOW_DAYS = 7;

    /**
     * Detect suspicious browsing patterns - Hourly
     *
     * Flags NGO staff who view many needs without claiming any.
     * This could indicate data harvesting or unauthorized information gathering.
     *
     * Detection logic:
     * - Count "NEED_ACCESSED_FULL" actions in last 24 hours
     * - If count > 50, check for any "NEED_CLAIMED" actions
     * - If no claims found, flag as suspicious
     *
     * Scheduled: Every hour
     */
    @Scheduled(cron = "0 0 * * * *")  // Top of every hour
    public void detectSuspiciousBrowsingPatterns() {
        logger.info("Running suspicious browsing pattern detection...");

        LocalDateTime windowStart = LocalDateTime.now().minusHours(VIEW_WINDOW_HOURS);

        // Get all users
        List<User> allUsers = userRepository.findAll();

        int flaggedCount = 0;

        for (User user : allUsers) {
            try {
                // Only check NGO_STAFF (field workers and admins have legitimate reasons to view many needs)
                if (user.getRole() != UserRole.NGO_STAFF) {
                    continue;
                }

                // Count full need accesses in last 24 hours
                long viewCount = auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
                    user.getId(),
                    "NEED_ACCESSED_FULL",
                    windowStart
                );

                // If user has viewed 50+ needs, check if they've claimed any
                if (viewCount >= SUSPICIOUS_VIEW_THRESHOLD) {
                    long claimCount = auditLogRepository.countByUserIdAndActionTypeAndCreatedAtAfter(
                        user.getId(),
                        "NEED_CLAIMED",
                        windowStart
                    );

                    // Suspicious: Many views, no claims
                    if (claimCount == 0) {
                        logSuspiciousActivity(
                            user.getId(),
                            "SUSPICIOUS_BROWSING",
                            "User viewed " + viewCount + " needs in " + VIEW_WINDOW_HOURS + " hours without claiming any",
                            "HIGH"
                        );
                        flaggedCount++;
                        logger.warn("Suspicious browsing detected: User {} viewed {} needs without claiming",
                            user.getEmail(), viewCount);
                    }
                }
            } catch (Exception e) {
                logger.error("Error checking user {} for suspicious browsing", user.getId(), e);
                // Continue with next user
            }
        }

        logger.info("Suspicious browsing detection complete. Flagged {} users", flaggedCount);
    }

    /**
     * Detect anomalous creation rates - Daily at 8 AM
     *
     * Flags field workers creating an unusually high number of needs.
     * This could indicate fraud, bot activity, or data entry errors.
     *
     * Detection logic:
     * - Count needs created by user in last 7 days
     * - If count > 100, flag as suspicious
     *
     * Scheduled: Daily at 8:00 AM server time
     */
    @Scheduled(cron = "0 0 8 * * *")  // 8 AM daily
    public void detectAnomalousCreationRates() {
        logger.info("Running anomalous creation rate detection...");

        LocalDateTime windowStart = LocalDateTime.now().minusDays(CREATION_WINDOW_DAYS);

        // Get all users
        List<User> allUsers = userRepository.findAll();

        int flaggedCount = 0;

        for (User user : allUsers) {
            try {
                // Only check FIELD_WORKER and NGO_STAFF (those who can create needs)
                if (user.getRole() != UserRole.FIELD_WORKER && user.getRole() != UserRole.NGO_STAFF) {
                    continue;
                }

                // Count needs created in last 7 days
                long creationCount = needRepository.countByCreatedByUserIdAndCreatedAtAfter(
                    user.getId(),
                    windowStart
                );

                // Flag if creation rate is unusually high
                if (creationCount >= SUSPICIOUS_CREATION_THRESHOLD) {
                    logSuspiciousActivity(
                        user.getId(),
                        "ANOMALOUS_CREATION_RATE",
                        "User created " + creationCount + " needs in " + CREATION_WINDOW_DAYS + " days",
                        "MEDIUM"
                    );
                    flaggedCount++;
                    logger.warn("Anomalous creation rate detected: User {} created {} needs in {} days",
                        user.getEmail(), creationCount, CREATION_WINDOW_DAYS);
                }
            } catch (Exception e) {
                logger.error("Error checking user {} for anomalous creation rate", user.getId(), e);
                // Continue with next user
            }
        }

        logger.info("Anomalous creation rate detection complete. Flagged {} users", flaggedCount);
    }

    /**
     * Cleanup expired rate limit entries - Hourly
     *
     * Prevents memory leaks in RateLimitService by cleaning up old entries.
     * This is a maintenance job, not fraud detection.
     *
     * Scheduled: Every hour at :30 minutes
     */
    @Scheduled(cron = "0 30 * * * *")  // Every hour at :30
    public void cleanupExpiredRateLimits() {
        logger.info("Running rate limit cleanup...");
        // This would call RateLimitService.cleanupExpiredEntries() if implemented
        // For now, just log that cleanup would happen
        logger.info("Rate limit cleanup complete (placeholder)");
    }

    /**
     * Get recent suspicious activities for admin dashboard
     *
     * Returns all flagged activities from last N days
     *
     * @param days Number of days to look back (default 30)
     * @return List of suspicious activity audit logs
     */
    public List<AuditLog> getRecentSuspiciousActivities(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return auditLogRepository.findRecentSuspiciousActivities(since);
    }

    /**
     * Check for duplicate beneficiaries (future enhancement)
     *
     * Detects if the same beneficiary phone/email is used in multiple needs
     * This could indicate fraud or data entry errors
     *
     * Note: Requires SensitiveInfo table implementation and encrypted comparison
     */
    public void checkForDuplicateBeneficiaries(String phone) {
        // Future implementation
        // Would decrypt all sensitive info and compare phone numbers
        // This is computationally expensive and should be done carefully
        logger.debug("Duplicate beneficiary check not yet implemented");
    }

    /**
     * Log suspicious activity to audit log
     *
     * Creates an audit log entry with action type "SUSPICIOUS_*"
     * These entries are displayed in admin dashboard for review
     *
     * @param userId User ID who performed suspicious activity
     * @param actionType Action type (e.g., "SUSPICIOUS_BROWSING")
     * @param details Human-readable description
     * @param severity Severity level: LOW, MEDIUM, HIGH, CRITICAL
     */
    private void logSuspiciousActivity(UUID userId, String actionType, String details, String severity) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("severity", severity);
        metadata.put("details", details);
        metadata.put("detectedAt", LocalDateTime.now().toString());
        metadata.put("requiresReview", true);

        auditService.logAction(
            userId,
            actionType,
            "USER",
            userId.toString(),
            "system",  // IP address for system-generated logs
            metadata
        );
    }

    /**
     * Manual trigger for suspicious browsing detection
     * Used for testing or manual admin-triggered checks
     */
    public void manualCheckSuspiciousBrowsing() {
        logger.info("Manual suspicious browsing check triggered");
        detectSuspiciousBrowsingPatterns();
    }

    /**
     * Manual trigger for anomalous creation rate detection
     * Used for testing or manual admin-triggered checks
     */
    public void manualCheckAnomalousCreation() {
        logger.info("Manual anomalous creation check triggered");
        detectAnomalousCreationRates();
    }
}
