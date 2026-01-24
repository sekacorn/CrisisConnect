package org.crisisconnect.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.crisisconnect.model.entity.UserSession;
import org.crisisconnect.service.AuthService;
import org.crisisconnect.service.SessionManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Session Management Controller
 * Provides REST endpoints for managing user sessions
 */
@RestController
@RequestMapping("/sessions")
@PreAuthorize("isAuthenticated()")
public class SessionController {

    @Autowired
    private SessionManagementService sessionService;

    @Autowired
    private AuthService authService;

    /**
     * Get all active sessions for current user.
     * GET /api/sessions
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getActiveSessions(Authentication authentication) {
        UUID userId = authService.getCurrentUser(authentication).getId();
        List<UserSession> sessions = sessionService.getActiveSessions(userId);

        // Convert to response format (hide sensitive data)
        List<Map<String, Object>> response = sessions.stream().map(session -> {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("id", session.getId());
            sessionData.put("deviceInfo", session.getDeviceInfo());
            sessionData.put("ipAddress", session.getIpAddress());
            sessionData.put("createdAt", session.getCreatedAt());
            sessionData.put("lastActivityAt", session.getLastActivityAt());
            sessionData.put("expiresAt", session.getExpiresAt());
            return sessionData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get specific session by ID.
     * GET /api/sessions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSession(
            @PathVariable UUID id,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            List<UserSession> sessions = sessionService.getAllSessions(userId);

            UserSession session = sessions.stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (session == null) {
                response.put("success", false);
                response.put("message", "Session not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.put("success", true);
            response.put("session", Map.of(
                    "id", session.getId(),
                    "deviceInfo", session.getDeviceInfo(),
                    "ipAddress", session.getIpAddress(),
                    "isActive", session.getIsActive(),
                    "createdAt", session.getCreatedAt(),
                    "lastActivityAt", session.getLastActivityAt(),
                    "expiresAt", session.getExpiresAt(),
                    "revokedAt", session.getRevokedAt()
            ));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Revoke a specific session.
     * DELETE /api/sessions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> revokeSession(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String ipAddress = getClientIp(request);

            sessionService.revokeSession(id, userId, ipAddress);

            response.put("success", true);
            response.put("message", "Session revoked successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to revoke session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Revoke all sessions for current user (logout from all devices).
     * POST /api/sessions/revoke-all
     */
    @PostMapping("/revoke-all")
    public ResponseEntity<Map<String, Object>> revokeAllSessions(
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String ipAddress = getClientIp(request);

            int count = sessionService.revokeAllSessions(userId, userId, ipAddress);

            response.put("success", true);
            response.put("message", "All sessions have been revoked");
            response.put("count", count);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to revoke all sessions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Revoke all other sessions except current one.
     * POST /api/sessions/revoke-others
     */
    @PostMapping("/revoke-others")
    public ResponseEntity<Map<String, Object>> revokeOtherSessions(
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String ipAddress = getClientIp(request);

            // Extract JWT token from Authorization header
            String token = extractTokenFromRequest(request);
            if (token == null) {
                response.put("success", false);
                response.put("message", "No authorization token found");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            int count = sessionService.revokeOtherSessions(userId, token, ipAddress);

            response.put("success", true);
            response.put("message", "Other sessions have been revoked");
            response.put("count", count);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to revoke other sessions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR", "HTTP_CLIENT_IP", "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract JWT token from Authorization header.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
