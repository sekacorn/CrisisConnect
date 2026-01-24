package org.crisisconnect.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.crisisconnect.dto.CreateNeedRequest;
import org.crisisconnect.dto.RedactedNeedResponse;
import org.crisisconnect.dto.UpdateNeedRequest;
import org.crisisconnect.model.entity.Need;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.repository.NeedRepository;
import org.crisisconnect.security.JwtUtil;
import org.crisisconnect.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Need controller with enhanced RBAC enforcement, privacy filtering, and rate limiting.
 *
 * Security layers:
 * 1. @PreAuthorize - Method-level role checks
 * 2. NeedSecurityService - Granular authorization (service area matching, org verification)
 * 3. NeedPrivacyFilterService - Privacy filtering (redacted vs full responses)
 * 4. RateLimitService - Abuse prevention
 *
 * Returns redacted data by default, full data only for authorized users.
 */
@RestController
@RequestMapping("/needs")
public class NeedController {

    @Autowired
    private NeedService needService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private NeedPrivacyFilterService privacyFilterService;

    @Autowired
    private NeedSecurityService needSecurityService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private NeedRepository needRepository;

    /**
     * Create a new need
     *
     * Authorization: FIELD_WORKER, NGO_STAFF, or ADMIN
     * Returns: Full need response (creator has full access to their own needs)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('FIELD_WORKER', 'NGO_STAFF', 'ADMIN')")
    public ResponseEntity<?> createNeed(
            @Valid @RequestBody CreateNeedRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        User user = authService.getCurrentUser(userDetails.getUsername());
        String ipAddress = httpRequest.getRemoteAddr();

        return ResponseEntity.ok(needService.createNeed(request, user.getId(), ipAddress));
    }

    /**
     * Get all needs (always returns redacted list)
     *
     * Authorization: Any authenticated user
     * Privacy: Always redacted, even for admins (list view for performance)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RedactedNeedResponse>> getAllNeeds(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = authService.getCurrentUser(userDetails.getUsername());

        // Get all needs (filtering by user visibility happens in service)
        List<Need> needs = needRepository.findAll();

        // Always return redacted list (privacy filter enforced)
        List<RedactedNeedResponse> redactedNeeds = privacyFilterService.filterNeedsList(needs);

        return ResponseEntity.ok(redactedNeeds);
    }

    /**
     * Get need by ID (redacted or full based on authorization)
     *
     * Authorization: Must have access to view need (checked by security service)
     * Privacy: Returns full response only if authorized, otherwise redacted
     * Rate Limiting: Max 20 views per hour for non-admins
     *
     * Returns 404 if need doesn't exist OR user doesn't have access (prevents enumeration)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getNeedById(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User user = authService.getCurrentUser(authentication);
        String ipAddress = httpRequest.getRemoteAddr();

        // Rate limiting (prevents data harvesting)
        try {
            rateLimitService.checkNeedViewRateLimit(user);
        } catch (RateLimitService.RateLimitExceededException e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        }

        // Fetch need
        Need need = needRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Need not found"));

        // Authorization check (return 404 instead of 403 to prevent enumeration)
        if (!needSecurityService.canAccessNeed(id, authentication)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Need not found");
        }

        // Privacy filtering (returns redacted or full based on authorization)
        Object response = privacyFilterService.filterNeed(need, user, ipAddress);

        return ResponseEntity.ok(response);
    }

    /**
     * Update need status
     *
     * Authorization: NGO_STAFF (from assigned verified org) or ADMIN
     * Checks: Must be able to update this specific need (not just any need)
     */
    @PatchMapping("/{id}")
    @PreAuthorize("@needSecurityService.canUpdateNeed(#id, authentication)")
    public ResponseEntity<Void> updateNeed(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNeedRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User user = authService.getCurrentUser(authentication);
        String ipAddress = httpRequest.getRemoteAddr();

        needService.updateNeed(id, request, user.getId(), ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Claim a need (assign to user's organization)
     *
     * Authorization: NGO_STAFF (from verified org) or ADMIN
     * Checks:
     * - Organization must be VERIFIED
     * - Need must be in user's service area
     * - Need must be unclaimed (PENDING/NEW status)
     */
    @PostMapping("/{id}/claim")
    @PreAuthorize("@needSecurityService.canClaimNeed(#id, authentication)")
    public ResponseEntity<Void> claimNeed(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User user = authService.getCurrentUser(authentication);
        String ipAddress = httpRequest.getRemoteAddr();

        // Delegate to service layer for business logic and audit logging
        needService.claimNeed(id, user.getId(), ipAddress);

        return ResponseEntity.ok().build();
    }
}

