package org.crisisconnect.controller;

import org.crisisconnect.model.entity.PasswordPolicy;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.PasswordPolicyRepository;
import org.crisisconnect.service.AuthService;
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
 * Password Policy Admin Controller
 * Admin-only endpoints for managing password policies
 */
@RestController
@RequestMapping("/admin/password-policies")
@PreAuthorize("hasRole('ADMIN')")
public class PasswordPolicyController {

    @Autowired
    private PasswordPolicyRepository passwordPolicyRepository;

    @Autowired
    private AuthService authService;

    /**
     * Get all password policies.
     * GET /api/admin/password-policies
     */
    @GetMapping
    public ResponseEntity<List<PasswordPolicy>> getAllPolicies() {
        List<PasswordPolicy> policies = passwordPolicyRepository.findAll();
        return ResponseEntity.ok(policies);
    }

    /**
     * Get password policy by ID.
     * GET /api/admin/password-policies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PasswordPolicy> getPolicyById(@PathVariable UUID id) {
        return passwordPolicyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get password policy by role.
     * GET /api/admin/password-policies/role/{role}
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<PasswordPolicy> getPolicyByRole(@PathVariable String role) {
        try {
            UserRole userRole = UserRole.valueOf(role);
            return passwordPolicyRepository.findByRoleAndIsActiveTrue(userRole)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create new password policy.
     * POST /api/admin/password-policies
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPolicy(
            @RequestBody PasswordPolicy policy,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            UUID adminId = authService.getCurrentUser(authentication).getId();
            policy.setCreatedByUserId(adminId);

            // Validate policy
            if (policy.getMinLength() < 8) {
                response.put("success", false);
                response.put("message", "Minimum password length must be at least 8 characters");
                return ResponseEntity.badRequest().body(response);
            }

            PasswordPolicy saved = passwordPolicyRepository.save(policy);

            response.put("success", true);
            response.put("message", "Password policy created successfully");
            response.put("policy", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create policy: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update existing password policy.
     * PUT /api/admin/password-policies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePolicy(
            @PathVariable UUID id,
            @RequestBody PasswordPolicy updatedPolicy) {
        Map<String, Object> response = new HashMap<>();

        try {
            PasswordPolicy existing = passwordPolicyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            // Update fields
            existing.setPolicyName(updatedPolicy.getPolicyName());
            existing.setRole(updatedPolicy.getRole());
            existing.setMinLength(updatedPolicy.getMinLength());
            existing.setRequireUppercase(updatedPolicy.getRequireUppercase());
            existing.setRequireLowercase(updatedPolicy.getRequireLowercase());
            existing.setRequireNumbers(updatedPolicy.getRequireNumbers());
            existing.setRequireSpecialChars(updatedPolicy.getRequireSpecialChars());
            existing.setPasswordExpiryDays(updatedPolicy.getPasswordExpiryDays());
            existing.setPasswordHistoryCount(updatedPolicy.getPasswordHistoryCount());
            existing.setMaxFailedAttempts(updatedPolicy.getMaxFailedAttempts());
            existing.setLockoutDurationMinutes(updatedPolicy.getLockoutDurationMinutes());
            existing.setSessionTimeoutMinutes(updatedPolicy.getSessionTimeoutMinutes());
            existing.setMaxSessionDurationHours(updatedPolicy.getMaxSessionDurationHours());
            existing.setEnforceMfa(updatedPolicy.getEnforceMfa());
            existing.setIsActive(updatedPolicy.getIsActive());
            existing.setDescription(updatedPolicy.getDescription());

            PasswordPolicy saved = passwordPolicyRepository.save(existing);

            response.put("success", true);
            response.put("message", "Password policy updated successfully");
            response.put("policy", saved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update policy: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete password policy.
     * DELETE /api/admin/password-policies/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePolicy(@PathVariable UUID id) {
        Map<String, Object> response = new HashMap<>();

        try {
            PasswordPolicy policy = passwordPolicyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            // Don't allow deleting the default policy
            if (policy.getIsDefault()) {
                response.put("success", false);
                response.put("message", "Cannot delete the default password policy");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            passwordPolicyRepository.delete(policy);

            response.put("success", true);
            response.put("message", "Password policy deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete policy: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Activate/deactivate password policy.
     * PATCH /api/admin/password-policies/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> togglePolicy(@PathVariable UUID id) {
        Map<String, Object> response = new HashMap<>();

        try {
            PasswordPolicy policy = passwordPolicyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            policy.setIsActive(!policy.getIsActive());
            PasswordPolicy saved = passwordPolicyRepository.save(policy);

            response.put("success", true);
            response.put("message", "Password policy " + (saved.getIsActive() ? "activated" : "deactivated"));
            response.put("policy", saved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to toggle policy: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get default password policy.
     * GET /api/admin/password-policies/default
     */
    @GetMapping("/default")
    public ResponseEntity<PasswordPolicy> getDefaultPolicy() {
        return passwordPolicyRepository.findTopByIsActiveTrueOrderByCreatedAtDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
