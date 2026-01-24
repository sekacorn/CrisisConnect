package org.crisisconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log entry response for admin dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userName;
    private String actionType;
    private String entityType;
    private UUID entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
