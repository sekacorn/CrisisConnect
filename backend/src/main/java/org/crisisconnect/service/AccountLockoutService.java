package org.crisisconnect.service;

import org.crisisconnect.model.entity.LoginAttempt;
import org.crisisconnect.model.entity.PasswordPolicy;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.repository.LoginAttemptRepository;
import org.crisisconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing account lockouts based on failed login attempts.
 * Implements NIST SP 800-63B requirement for account lockout after failed attempts.
 */
@Service
public class AccountLockoutService {

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordValidationService passwordValidationService;

    @Autowired
    private AuditService auditService;

    /**
     * Record a login attempt (successful or failed).
     *
     * @param email User email
     * @param successful Whether login was successful
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @param failureReason Reason for failure (if applicable)
     */
    @Transactional
    public void recordLoginAttempt(String email, boolean successful, String ipAddress,
                                   String userAgent, String failureReason) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setEmail(email);
        attempt.setSuccessful(successful);
        attempt.setIpAddress(ipAddress);
        attempt.setUserAgent(userAgent);
        attempt.setFailureReason(failureReason);
        attempt.setAttemptedAt(LocalDateTime.now());

        loginAttemptRepository.save(attempt);

        // If failed, check if we need to lock the account
        if (!successful) {
            checkAndLockAccount(email, ipAddress);
        } else {
            // Successful login - reset failed attempts counter
            resetFailedAttempts(email);
        }
    }

    /**
     * Check if account should be locked based on recent failed attempts.
     */
    @Transactional
    private void checkAndLockAccount(String email, String ipAddress) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return; // User doesn't exist, nothing to lock
        }

        PasswordPolicy policy = passwordValidationService.getPasswordPolicy(user.getRole());

        // Count recent failed attempts (within last 30 minutes)
        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        long failedCount = loginAttemptRepository.countFailedAttemptsByEmailSince(email, since);

        // Update user's failed login attempts counter
        user.setFailedLoginAttempts((int) failedCount);

        // Lock account if threshold exceeded
        if (failedCount >= policy.getMaxFailedAttempts()) {
            LocalDateTime lockUntil = LocalDateTime.now()
                    .plusMinutes(policy.getLockoutDurationMinutes());
            user.setAccountLockedUntil(lockUntil);

            auditService.logAction(
                user.getId(),
                "ACCOUNT_LOCKED",
                "USER",
                user.getId(),
                String.format("Account locked after %d failed attempts", failedCount),
                ipAddress
            );
        }

        userRepository.save(user);
    }

    /**
     * Reset failed login attempts counter after successful login.
     */
    @Transactional
    private void resetFailedAttempts(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        }
    }

    /**
     * Check if account is currently locked.
     *
     * @param user User to check
     * @return true if account is locked, false otherwise
     */
    public boolean isAccountLocked(User user) {
        if (user.getAccountLockedUntil() == null) {
            return false;
        }

        // Check if lock has expired
        if (user.getAccountLockedUntil().isBefore(LocalDateTime.now())) {
            // Lock expired, unlock account
            unlockAccount(user);
            return false;
        }

        return true;
    }

    /**
     * Check if account is locked by email.
     *
     * @param email User email
     * @return true if account is locked, false otherwise
     */
    public boolean isAccountLocked(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }
        return isAccountLocked(user);
    }

    /**
     * Manually unlock an account (admin action).
     *
     * @param userId User ID to unlock
     * @param adminId Admin user performing the unlock
     * @param ipAddress IP address of admin
     */
    @Transactional
    public void unlockAccount(UUID userId, UUID adminId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        unlockAccount(user);

        auditService.logAction(
            adminId,
            "ACCOUNT_UNLOCKED",
            "USER",
            userId,
            "Account manually unlocked by admin",
            ipAddress
        );
    }

    /**
     * Unlock account (internal method).
     */
    @Transactional
    private void unlockAccount(User user) {
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    /**
     * Get recent login attempts for a user.
     *
     * @param email User email
     * @param limit Maximum number of attempts to return
     * @return List of recent login attempts
     */
    public List<LoginAttempt> getRecentAttempts(String email, int limit) {
        List<LoginAttempt> attempts = loginAttemptRepository.findByEmailOrderByAttemptedAtDesc(email);
        return attempts.subList(0, Math.min(limit, attempts.size()));
    }

    /**
     * Get recent login attempts from an IP address.
     *
     * @param ipAddress IP address
     * @param since Time threshold
     * @return List of recent login attempts from this IP
     */
    public List<LoginAttempt> getRecentAttemptsByIp(String ipAddress, LocalDateTime since) {
        return loginAttemptRepository.findRecentAttemptsByIp(ipAddress, since);
    }

    /**
     * Get time remaining until account unlock.
     *
     * @param user User to check
     * @return Minutes remaining until unlock, or 0 if not locked
     */
    public long getMinutesUntilUnlock(User user) {
        if (user.getAccountLockedUntil() == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getAccountLockedUntil().isBefore(now)) {
            return 0;
        }

        return java.time.Duration.between(now, user.getAccountLockedUntil()).toMinutes();
    }

    /**
     * Check for suspicious login patterns (e.g., many failed attempts from same IP).
     *
     * @param ipAddress IP address to check
     * @param thresholdMinutes Time window to check
     * @param maxAttempts Maximum allowed attempts
     * @return true if suspicious activity detected
     */
    public boolean isSuspiciousActivity(String ipAddress, int thresholdMinutes, int maxAttempts) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(thresholdMinutes);
        List<LoginAttempt> attempts = loginAttemptRepository.findRecentAttemptsByIp(ipAddress, since);

        long failedCount = attempts.stream()
                .filter(a -> !a.getSuccessful())
                .count();

        return failedCount >= maxAttempts;
    }

    /**
     * Get total failed attempts count for an email.
     *
     * @param email User email
     * @param since Time threshold
     * @return Count of failed attempts
     */
    public long getFailedAttemptsCount(String email, LocalDateTime since) {
        return loginAttemptRepository.countFailedAttemptsByEmailSince(email, since);
    }

    /**
     * Check if user needs to be warned about remaining attempts.
     *
     * @param email User email
     * @return Remaining attempts before lockout, or -1 if not at risk
     */
    public int getRemainingAttempts(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return -1;
        }

        PasswordPolicy policy = passwordValidationService.getPasswordPolicy(user.getRole());
        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        long failedCount = loginAttemptRepository.countFailedAttemptsByEmailSince(email, since);

        int remaining = policy.getMaxFailedAttempts() - (int) failedCount;
        return Math.max(0, remaining);
    }
}
