package org.crisisconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Suspicious activity entry for admin monitoring
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuspiciousActivityResponse {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userName;
    private String userRole;
    private String activityType;
    private String description;
    private Long activityCount;
    private LocalDateTime detectedAt;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
}
