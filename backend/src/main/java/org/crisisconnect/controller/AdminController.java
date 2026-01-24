package org.crisisconnect.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.crisisconnect.dto.*;
import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.service.AdminService;
import org.crisisconnect.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin dashboard controller
 *
 * Provides:
 * - Dashboard statistics and analytics
 * - Organization verification and management
 * - User management
 * - Audit log viewing
 * - Suspicious activity monitoring
 *
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AuthService authService;

    /**
     * Get dashboard statistics
     *
     * Returns comprehensive statistics including:
     * - User counts by role
     * - Organization verification status
     * - Need counts by status/category/urgency
     * - Recent activity (24h)
     * - Security metrics (30d)
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ==================== Organization Management ====================

    /**
     * Get all organizations with pagination
     *
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     */
    @GetMapping("/organizations")
    public ResponseEntity<Page<Organization>> getAllOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllOrganizations(page, size));
    }

    /**
     * Get organizations by status
     *
     * Use cases:
     * - View pending organizations for verification
     * - View verified organizations
     * - View rejected organizations
     */
    @GetMapping("/organizations/status/{status}")
    public ResponseEntity<List<Organization>> getOrganizationsByStatus(
            @PathVariable OrganizationStatus status) {
        return ResponseEntity.ok(adminService.getOrganizationsByStatus(status));
    }

    /**
     * Update organization status (verification)
     *
     * Typical workflow:
     * 1. Admin reviews pending organization
     * 2. Verifies legitimacy and service areas
     * 3. Approves (VERIFIED) or rejects (REJECTED)
     * 4. Provides verification notes
     *
     * @param id Organization ID
     * @param request Status and verification notes
     */
    @PatchMapping("/organizations/{id}")
    public ResponseEntity<Void> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User admin = authService.getCurrentUser(authentication);
        String ipAddress = httpRequest.getRemoteAddr();

        adminService.updateOrganizationStatus(id, request, admin.getId(), ipAddress);
        return ResponseEntity.ok().build();
    }

    // ==================== User Management ====================

    /**
     * Get all users with pagination
     *
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     */
    @GetMapping("/users")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    /**
     * Update user
     *
     * Admin can:
     * - Change user role
     * - Activate/deactivate user
     * - Update user name
     *
     * @param id User ID
     * @param request Update fields
     */
    @PatchMapping("/users/{id}")
    public ResponseEntity<Void> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User admin = authService.getCurrentUser(authentication);
        String ipAddress = httpRequest.getRemoteAddr();

        adminService.updateUser(id, request, admin.getId(), ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Get user's audit log history
     *
     * Shows all actions performed by a specific user
     * Useful for investigating suspicious behavior
     *
     * @param id User ID
     * @param days Number of days to look back (default 30)
     */
    @GetMapping("/users/{id}/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getUserAuditLogs(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getUserAuditLogs(id, days));
    }

    // ==================== Audit Logs ====================

    /**
     * Get audit logs with pagination and filtering
     *
     * @param page Page number (default 0)
     * @param size Page size (default 50)
     * @param actionType Filter by action type (optional)
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actionType) {
        return ResponseEntity.ok(adminService.getAuditLogs(page, size, actionType));
    }

    // ==================== Suspicious Activity Monitoring ====================

    /**
     * Get suspicious activities
     *
     * Returns activities flagged by scheduled jobs:
     * - SUSPICIOUS_BROWSING: User viewing many needs without claiming
     * - ANOMALOUS_CREATION_RATE: User creating many needs rapidly
     * - RATE_LIMIT_EXCEEDED: Multiple rate limit violations
     *
     * @param days Number of days to look back (default 30)
     */
    @GetMapping("/suspicious-activities")
    public ResponseEntity<List<SuspiciousActivityResponse>> getSuspiciousActivities(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getSuspiciousActivities(days));
    }
}
