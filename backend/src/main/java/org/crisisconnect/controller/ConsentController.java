package org.crisisconnect.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.crisisconnect.model.entity.UserConsent;
import org.crisisconnect.model.enums.ConsentType;
import org.crisisconnect.service.AuthService;
import org.crisisconnect.service.ConsentManagementService;
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

/**
 * Consent Management Controller
 * Implements GDPR Article 7 (Conditions for consent)
 */
@RestController
@RequestMapping("/consent")
public class ConsentController {

    @Autowired
    private ConsentManagementService consentService;

    @Autowired
    private AuthService authService;

    /**
     * Get all consents for current user.
     * GET /api/consent
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserConsent>> getMyConsents(Authentication authentication) {
        UUID userId = authService.getCurrentUser(authentication).getId();
        List<UserConsent> consents = consentService.getUserConsents(userId);
        return ResponseEntity.ok(consents);
    }

    /**
     * Get active consents for current user.
     * GET /api/consent/active
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserConsent>> getActiveConsents(Authentication authentication) {
        UUID userId = authService.getCurrentUser(authentication).getId();
        List<UserConsent> consents = consentService.getActiveConsents(userId);
        return ResponseEntity.ok(consents);
    }

    /**
     * Grant consent.
     * POST /api/consent/grant
     */
    @PostMapping("/grant")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> grantConsent(
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            ConsentType consentType = ConsentType.valueOf(request.get("consentType"));
            String consentText = request.get("consentText");
            String consentVersion = request.get("consentVersion");
            String ipAddress = getClientIp(httpRequest);

            UserConsent consent = consentService.grantConsent(
                    userId, consentType, consentText, consentVersion, ipAddress);

            response.put("success", true);
            response.put("message", "Consent granted successfully");
            response.put("consent", consent);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to grant consent: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Revoke consent.
     * POST /api/consent/revoke
     */
    @PostMapping("/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> revokeConsent(
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            ConsentType consentType = ConsentType.valueOf(request.get("consentType"));
            String ipAddress = getClientIp(httpRequest);

            UserConsent consent = consentService.revokeConsent(userId, consentType, ipAddress);

            response.put("success", true);
            response.put("message", "Consent revoked successfully");
            response.put("consent", consent);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to revoke consent: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Check if user has specific consent.
     * GET /api/consent/check/{consentType}
     */
    @GetMapping("/check/{consentType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> checkConsent(
            @PathVariable String consentType,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            ConsentType type = ConsentType.valueOf(consentType);
            boolean hasConsent = consentService.hasConsent(userId, type);

            response.put("consentType", consentType);
            response.put("granted", hasConsent);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", "Invalid consent type: " + consentType);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Withdraw all consents.
     * POST /api/consent/withdraw-all
     */
    @PostMapping("/withdraw-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> withdrawAllConsents(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String ipAddress = getClientIp(httpRequest);

            consentService.withdrawAllConsents(userId, ipAddress);

            response.put("success", true);
            response.put("message", "All consents have been withdrawn");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to withdraw consents: " + e.getMessage());
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
}
