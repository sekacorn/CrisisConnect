package org.crisisconnect.service;

import org.crisisconnect.model.entity.UserSession;
import org.crisisconnect.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Session Management Service
 * Handles user session tracking, validation, and revocation
 * Enables features like "logout from all devices" and concurrent session limits
 */
@Service
public class SessionManagementService {

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private AuditService auditService;

    // Maximum concurrent sessions per user (0 = unlimited)
    @Value("${session.max-concurrent:5}")
    private int maxConcurrentSessions;

    // Session expiration time in hours
    @Value("${session.expiration-hours:24}")
    private int sessionExpirationHours;

    /**
     * Create a new session for a user.
     *
     * @param userId User ID
     * @param token JWT token
     * @param deviceInfo Device/browser information
     * @param ipAddress Client IP address
     * @return Created UserSession
     */
    @Transactional
    public UserSession createSession(UUID userId, String token, String deviceInfo, String ipAddress) {
        // Check concurrent session limit
        if (maxConcurrentSessions > 0) {
            long activeCount = sessionRepository.countActiveSessionsByUserId(userId);
            if (activeCount >= maxConcurrentSessions) {
                // Revoke oldest session to make room
                List<UserSession> activeSessions = sessionRepository.findActiveSessionsByUserId(userId);
                if (!activeSessions.isEmpty()) {
                    UserSession oldestSession = activeSessions.get(activeSessions.size() - 1);
                    oldestSession.revoke();
                    sessionRepository.save(oldestSession);

                    auditService.logAction(userId, "SESSION_REVOKED_LIMIT", "SESSION",
                        oldestSession.getId(), "Session revoked due to concurrent limit", ipAddress);
                }
            }
        }

        // Create new session
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setTokenHash(hashToken(token));
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        session.setIsActive(true);
        session.setExpiresAt(LocalDateTime.now().plusHours(sessionExpirationHours));
        session.setLastActivityAt(LocalDateTime.now());

        UserSession saved = sessionRepository.save(session);

        auditService.logAction(userId, "SESSION_CREATED", "SESSION",
            saved.getId(), "New session created from " + deviceInfo, ipAddress);

        return saved;
    }

    /**
     * Validate a session by token.
     *
     * @param token JWT token
     * @return true if session is valid
     */
    public boolean validateSession(String token) {
        String tokenHash = hashToken(token);
        UserSession session = sessionRepository.findByTokenHash(tokenHash).orElse(null);

        if (session == null || !session.isValid()) {
            return false;
        }

        // Update last activity
        session.updateActivity();
        sessionRepository.save(session);

        return true;
    }

    /**
     * Revoke a specific session.
     *
     * @param sessionId Session ID
     * @param requestingUserId User requesting revocation
     * @param ipAddress Client IP
     */
    @Transactional
    public void revokeSession(UUID sessionId, UUID requestingUserId, String ipAddress) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.revoke();
        sessionRepository.save(session);

        auditService.logAction(requestingUserId, "SESSION_REVOKED", "SESSION",
            sessionId, "Session manually revoked", ipAddress);
    }

    /**
     * Revoke all sessions for a user (logout from all devices).
     *
     * @param userId User ID
     * @param requestingUserId User requesting revocation
     * @param ipAddress Client IP
     * @return Number of sessions revoked
     */
    @Transactional
    public int revokeAllSessions(UUID userId, UUID requestingUserId, String ipAddress) {
        int count = sessionRepository.revokeAllSessionsByUserId(userId, LocalDateTime.now());

        auditService.logAction(requestingUserId, "ALL_SESSIONS_REVOKED", "USER",
            userId, "All sessions revoked for user", ipAddress);

        return count;
    }

    /**
     * Revoke all sessions except current one.
     *
     * @param userId User ID
     * @param currentToken Current JWT token (to exclude)
     * @param ipAddress Client IP
     * @return Number of sessions revoked
     */
    @Transactional
    public int revokeOtherSessions(UUID userId, String currentToken, String ipAddress) {
        String currentTokenHash = hashToken(currentToken);
        List<UserSession> sessions = sessionRepository.findActiveSessionsByUserId(userId);

        int count = 0;
        for (UserSession session : sessions) {
            if (!session.getTokenHash().equals(currentTokenHash)) {
                session.revoke();
                sessionRepository.save(session);
                count++;
            }
        }

        auditService.logAction(userId, "OTHER_SESSIONS_REVOKED", "USER",
            userId, "Other sessions revoked for user", ipAddress);

        return count;
    }

    /**
     * Get all active sessions for a user.
     *
     * @param userId User ID
     * @return List of active sessions
     */
    public List<UserSession> getActiveSessions(UUID userId) {
        return sessionRepository.findActiveSessionsByUserId(userId);
    }

    /**
     * Get all sessions (active and inactive) for a user.
     *
     * @param userId User ID
     * @return List of all sessions
     */
    public List<UserSession> getAllSessions(UUID userId) {
        return sessionRepository.findByUserId(userId);
    }

    /**
     * Hash a JWT token using SHA-256.
     *
     * @param token JWT token
     * @return SHA-256 hash as hex string
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Cleanup expired sessions (scheduled task - runs every hour).
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // Keep for 7 days after expiration
        int count = sessionRepository.deleteExpiredSessions(cutoff);

        if (count > 0) {
            auditService.logAction((UUID) null, "SESSIONS_CLEANUP", "SYSTEM", (UUID) null,
                "Cleaned up " + count + " expired sessions", "system");
        }
    }
}
