package org.crisisconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Admin dashboard statistics response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    // User statistics
    private long totalUsers;
    private long activeUsers;
    private Map<String, Long> usersByRole;

    // Organization statistics
    private long totalOrganizations;
    private long verifiedOrganizations;
    private long pendingOrganizations;

    // Need statistics
    private long totalNeeds;
    private Map<String, Long> needsByStatus;
    private Map<String, Long> needsByCategory;
    private Map<String, Long> needsByUrgency;

    // Activity statistics
    private long recentLogins24h;
    private long recentNeedsCreated24h;
    private long recentNeedsClaimed24h;

    // Security statistics
    private long suspiciousActivities30d;
    private long rateLimitViolations24h;
    private long failedLoginAttempts24h;
}
