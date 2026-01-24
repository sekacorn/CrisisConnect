package org.crisisconnect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.crisisconnect.model.entity.AuditLog;
import org.crisisconnect.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Audit logging service for compliance and security monitoring.
 * All sensitive actions are logged asynchronously.
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void logAction(UUID userId, String actionType, String targetType, UUID targetId, String metadata, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setActionType(actionType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setMetadata(metadata);
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    /**
     * Overloaded logAction that accepts Map for metadata
     * Converts the map to JSON string for storage
     */
    @Async
    public void logAction(UUID userId, String actionType, String targetType, String targetIdString, String ipAddress, Map<String, Object> metadataMap) {
        UUID targetId = null;
        try {
            if (targetIdString != null && !targetIdString.isEmpty()) {
                targetId = UUID.fromString(targetIdString);
            }
        } catch (IllegalArgumentException e) {
            // If it's not a valid UUID, just leave it as null
            logger.warn("Invalid UUID string: {}", targetIdString);
        }

        String metadataJson = null;
        if (metadataMap != null && !metadataMap.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadataMap);
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize metadata map to JSON", e);
                metadataJson = metadataMap.toString(); // Fallback to toString
            }
        }

        logAction(userId, actionType, targetType, targetId, metadataJson, ipAddress);
    }

    @Async
    public void logNeedAccess(UUID userId, UUID needId, boolean fullAccess, String ipAddress) {
        String actionType = fullAccess ? "VIEW_NEED_FULL" : "VIEW_NEED_REDACTED";
        logAction(userId, actionType, "NEED", needId, null, ipAddress);
    }

    @Async
    public void logSensitiveInfoAccess(UUID userId, UUID needId, String ipAddress) {
        logAction(userId, "VIEW_SENSITIVE_INFO", "NEED", needId, "PII accessed", ipAddress);
    }
}
