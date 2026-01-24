package org.crisisconnect.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.service.AuthService;
import org.crisisconnect.service.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Multi-Factor Authentication Controller
 * Provides endpoints for MFA setup and management
 */
@RestController
@RequestMapping("/mfa")
public class MfaController {

    @Autowired
    private MfaService mfaService;

    @Autowired
    private AuthService authService;

    /**
     * Generate MFA secret and QR code for enrollment.
     * GET /api/mfa/setup
     */
    @GetMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> setupMfa(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = authService.getCurrentUser(authentication);
            UUID userId = user.getId();

            // Check if MFA is already enabled
            if (mfaService.isMfaEnabled(userId)) {
                response.put("success", false);
                response.put("message", "MFA is already enabled. Please disable it first to re-enroll.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Generate new secret
            String secret = mfaService.generateSecret(userId);
            String qrCodeUrl = mfaService.getQrCodeUrl(userId, secret);

            response.put("success", true);
            response.put("secret", secret);
            response.put("qrCodeUrl", qrCodeUrl);
            response.put("message", "Scan the QR code with your authenticator app (Google Authenticator, Authy, etc.) and verify with a code.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to setup MFA: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Enable MFA after verification.
     * POST /api/mfa/enable
     * Body: { "code": "123456" }
     */
    @PostMapping("/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> enableMfa(
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String code = request.get("code");
            String ipAddress = getClientIp(httpRequest);

            if (code == null || code.isEmpty()) {
                response.put("success", false);
                response.put("message", "TOTP code is required");
                return ResponseEntity.badRequest().body(response);
            }

            boolean enabled = mfaService.enableMfa(userId, code, ipAddress);

            if (enabled) {
                response.put("success", true);
                response.put("message", "MFA has been successfully enabled for your account");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid verification code. Please try again.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to enable MFA: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Disable MFA.
     * POST /api/mfa/disable
     * Body: { "code": "123456" }
     */
    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> disableMfa(
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String code = request.get("code");
            String ipAddress = getClientIp(httpRequest);

            if (code == null || code.isEmpty()) {
                response.put("success", false);
                response.put("message", "TOTP code is required for security verification");
                return ResponseEntity.badRequest().body(response);
            }

            boolean disabled = mfaService.disableMfa(userId, code, ipAddress);

            if (disabled) {
                response.put("success", true);
                response.put("message", "MFA has been disabled for your account");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid verification code. Please try again.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to disable MFA: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Verify TOTP code (for testing or general verification).
     * POST /api/mfa/verify
     * Body: { "code": "123456" }
     */
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> verifyCode(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = authService.getCurrentUser(authentication);
            String code = request.get("code");

            if (code == null || code.isEmpty()) {
                response.put("success", false);
                response.put("message", "TOTP code is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (user.getMfaSecret() == null) {
                response.put("success", false);
                response.put("message", "MFA is not set up for this account");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            boolean valid = mfaService.verifyCode(user.getMfaSecret(), code);

            response.put("success", true);
            response.put("valid", valid);
            response.put("message", valid ? "Code is valid" : "Code is invalid");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to verify code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get MFA status for current user.
     * GET /api/mfa/status
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMfaStatus(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            boolean enabled = mfaService.isMfaEnabled(userId);

            response.put("success", true);
            response.put("enabled", enabled);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get MFA status: " + e.getMessage());
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
