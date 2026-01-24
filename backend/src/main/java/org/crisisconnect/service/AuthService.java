package org.crisisconnect.service;

import org.crisisconnect.dto.LoginRequest;
import org.crisisconnect.dto.LoginResponse;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.repository.UserRepository;
import org.crisisconnect.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication service handling login and JWT generation.
 * Enhanced with NIST SP 800-63B security features including:
 * - Account lockout after failed attempts
 * - Password expiration checking
 * - Login attempt tracking
 * - Last login tracking
 */
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AccountLockoutService accountLockoutService;

    @Autowired
    private PasswordValidationService passwordValidationService;

    @Autowired
    private MfaService mfaService;

    @Autowired
    private SessionManagementService sessionManagementService;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String userAgent = ""; // Can be passed from controller if needed

        // 1. Check if user exists
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // 2. Check if account is locked (before attempting authentication)
        if (user != null && accountLockoutService.isAccountLocked(user)) {
            long minutesRemaining = accountLockoutService.getMinutesUntilUnlock(user);
            accountLockoutService.recordLoginAttempt(
                request.getEmail(), false, ipAddress, userAgent,
                "Account locked - " + minutesRemaining + " minutes remaining"
            );
            auditService.logAction(null, "LOGIN_FAILED", "USER", null,
                "Account locked: " + request.getEmail(), ipAddress);
            throw new RuntimeException("Account is locked. Please try again in " + minutesRemaining + " minutes");
        }

        // 3. Check if password has expired
        if (user != null && passwordValidationService.isPasswordExpired(user)) {
            accountLockoutService.recordLoginAttempt(
                request.getEmail(), false, ipAddress, userAgent, "Password expired"
            );
            auditService.logAction(user.getId(), "LOGIN_FAILED", "USER", user.getId(),
                "Password expired", ipAddress);
            throw new RuntimeException("Password has expired. Please reset your password");
        }

        // 4. Check if account is active
        if (user != null && !user.getIsActive()) {
            accountLockoutService.recordLoginAttempt(
                request.getEmail(), false, ipAddress, userAgent, "Account inactive"
            );
            auditService.logAction(user.getId(), "LOGIN_FAILED", "USER", user.getId(),
                "Account inactive", ipAddress);
            throw new RuntimeException("Account is inactive. Please contact administrator");
        }

        // 5. Attempt authentication
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // 6. Authentication successful - update user and record attempt
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 7. Check if MFA is required
            if (user.getMfaEnabled() && user.getMfaSecret() != null) {
                // MFA is enabled - verify code if provided
                if (request.getMfaCode() == null || request.getMfaCode().isEmpty()) {
                    // No MFA code provided - return mfaRequired response
                    auditService.logAction(user.getId(), "LOGIN_MFA_REQUIRED", "USER", user.getId(),
                        "MFA verification required", ipAddress);
                    return LoginResponse.mfaRequired(user.getId(), user.getEmail());
                }

                // Verify MFA code
                boolean mfaValid = mfaService.verifyCode(user.getMfaSecret(), request.getMfaCode());
                if (!mfaValid) {
                    accountLockoutService.recordLoginAttempt(
                        request.getEmail(), false, ipAddress, userAgent, "Invalid MFA code"
                    );
                    auditService.logAction(user.getId(), "LOGIN_FAILED", "USER", user.getId(),
                        "Invalid MFA code", ipAddress);
                    throw new RuntimeException("Invalid MFA code");
                }

                // MFA code is valid - log successful MFA verification
                auditService.logAction(user.getId(), "LOGIN_MFA_VERIFIED", "USER", user.getId(),
                    "MFA verification successful", ipAddress);
            }

            // 8. Update last login info
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            userRepository.save(user);

            // Record successful login attempt
            accountLockoutService.recordLoginAttempt(
                request.getEmail(), true, ipAddress, userAgent, null
            );

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

            // Create session
            sessionManagementService.createSession(user.getId(), token, userAgent, ipAddress);

            // Audit log
            auditService.logAction(user.getId(), "LOGIN", "USER", user.getId(), null, ipAddress);

            return new LoginResponse(token, user.getId(), user.getEmail(), user.getName(),
                    user.getRole().name(), user.getOrganizationId());

        } catch (AuthenticationException e) {
            // 7. Authentication failed - record failed attempt
            String failureReason = "Invalid credentials";
            accountLockoutService.recordLoginAttempt(
                request.getEmail(), false, ipAddress, userAgent, failureReason
            );

            // Check remaining attempts and provide helpful message
            int remainingAttempts = accountLockoutService.getRemainingAttempts(request.getEmail());
            String errorMessage = "Invalid credentials";
            if (remainingAttempts > 0 && remainingAttempts <= 3) {
                errorMessage += ". " + remainingAttempts + " attempts remaining before account lockout";
            }

            auditService.logAction(null, "LOGIN_FAILED", "USER", null, request.getEmail(), ipAddress);
            throw new RuntimeException(errorMessage);
        }
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Get current user from Spring Security Authentication
     */
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        return getCurrentUser(email);
    }
}
