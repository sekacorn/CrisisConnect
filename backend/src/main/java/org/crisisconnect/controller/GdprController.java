package org.crisisconnect.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.crisisconnect.service.AuthService;
import org.crisisconnect.service.GdprService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GDPR Compliance Controller
 * Implements GDPR Article 15 (Right to Access) and Article 17 (Right to Erasure)
 */
@RestController
@RequestMapping("/gdpr")
public class GdprController {

    @Autowired
    private GdprService gdprService;

    @Autowired
    private AuthService authService;

    /**
     * Export user data in JSON format (GDPR Article 15).
     * GET /api/gdpr/export/json
     */
    @GetMapping("/export/json")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> exportDataAsJson(Authentication authentication) {
        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String jsonData = gdprService.exportUserDataAsJson(userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "my_data.json");

            return new ResponseEntity<>(jsonData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to export data: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Export user data in CSV format (GDPR Article 15).
     * GET /api/gdpr/export/csv
     */
    @GetMapping("/export/csv")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> exportDataAsCsv(Authentication authentication) {
        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String csvData = gdprService.exportUserDataAsCsv(userId);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");
            headers.setContentDispositionFormData("attachment", "my_data.csv");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error,Message\nerror,Failed to export data: " + e.getMessage());
        }
    }

    /**
     * Soft delete user account (GDPR Article 17).
     * POST /api/gdpr/delete/soft
     */
    @PostMapping("/delete/soft")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> softDeleteAccount(
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String ipAddress = getClientIp(request);

            // Check if user can be deleted
            if (!gdprService.canDeleteUser(userId)) {
                response.put("success", false);
                response.put("message", "Account cannot be deleted at this time due to active legal holds or pending processes");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            gdprService.softDeleteUser(userId, userId, ipAddress);

            response.put("success", true);
            response.put("message", "Account has been deactivated. Your data will be retained for " +
                    gdprService.getDataRetentionDays(userId) + " days as required by law, then permanently deleted.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete account: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Hard delete user account (GDPR Article 17).
     * ADMIN ONLY - Permanent deletion
     * POST /api/gdpr/delete/hard/{userId}
     */
    @PostMapping("/delete/hard/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> hardDeleteAccount(
            @PathVariable UUID userId,
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID adminId = authService.getCurrentUser(authentication).getId();
            String ipAddress = getClientIp(request);

            // Check if user can be deleted
            if (!gdprService.canDeleteUser(userId)) {
                response.put("success", false);
                response.put("message", "Account cannot be deleted at this time due to active legal holds or pending processes");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            gdprService.hardDeleteUser(userId, adminId, ipAddress);

            response.put("success", true);
            response.put("message", "Account has been permanently deleted from all systems");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to permanently delete account: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Anonymize user account (alternative to deletion).
     * POST /api/gdpr/anonymize
     */
    @PostMapping("/anonymize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> anonymizeAccount(
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID userId = authService.getCurrentUser(authentication).getId();
            String ipAddress = getClientIp(request);

            gdprService.anonymizeUser(userId, userId, ipAddress);

            response.put("success", true);
            response.put("message", "Account has been anonymized. All personal data has been removed while retaining statistical information.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to anonymize account: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
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
