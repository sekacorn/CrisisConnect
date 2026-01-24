package org.crisisconnect.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.crisisconnect.dto.LoginRequest;
import org.crisisconnect.dto.LoginResponse;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.service.AuthService;
import org.crisisconnect.service.RateLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authentication controller with rate limiting for login attempts.
 *
 * Security measures:
 * - Rate limiting: Max 5 failed login attempts per 15 minutes
 * - Audit logging: All login attempts logged (success and failure)
 * - Generic error messages: No user enumeration
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RateLimitService rateLimitService;

    /**
     * Login endpoint with rate limiting
     *
     * Rate Limit: 5 failed attempts per 15 minutes per email
     * Returns: JWT token on success
     * Security: Returns generic error message (no user enumeration)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();
        String email = request.getEmail();

        // Check if email is rate limited
        if (rateLimitService.isLoginRateLimited(email)) {
            int remainingTime = 15; // Could calculate actual remaining time
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many login attempts. Please try again in " + remainingTime + " minutes."
            );
        }

        try {
            // Attempt login
            LoginResponse response = authService.login(request, ipAddress);

            // Success: Clear failed login attempts
            rateLimitService.clearFailedLogins(email);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Failed login: Record attempt
            boolean rateLimitExceeded = rateLimitService.recordFailedLogin(email);

            if (rateLimitExceeded) {
                throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Account temporarily locked for 15 minutes."
                );
            }

            // Generic error message (prevents user enumeration)
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }
}
