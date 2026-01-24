package org.crisisconnect.service;

import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service for Abuse Prevention
 *
 * Implements spec/35_safeguarding_abuse_prevention.md requirements.
 *
 * Rate limits:
 * - Login attempts: 5 failures per 15 minutes
 * - Need detail views: 20 per hour (non-admin)
 * - API requests: 100 per minute (general)
 */
@Service
public class RateLimitService {

    // In-memory rate limiting (for production, use Redis or similar)
    private final Map<String, RateLimitEntry> loginAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, RateLimitEntry> needViewCounts = new ConcurrentHashMap<>();
    private final Map<UUID, RateLimitEntry> apiRequestCounts = new ConcurrentHashMap<>();

    @Autowired
    private AuditService auditService;

    // Rate limit thresholds
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_WINDOW_MINUTES = 15;

    private static final int MAX_NEED_VIEWS_PER_HOUR = 20;
    private static final int NEED_VIEW_WINDOW_MINUTES = 60;

    private static final int MAX_API_REQUESTS_PER_MINUTE = 100;
    private static final int API_REQUEST_WINDOW_MINUTES = 1;

    /**
     * Record a failed login attempt
     *
     * @param email User email
     * @return true if rate limit exceeded
     */
    public boolean recordFailedLogin(String email) {
        String key = email.toLowerCase();
        RateLimitEntry entry = loginAttempts.computeIfAbsent(key,
                k -> new RateLimitEntry());

        // Clean old entries if outside window
        if (entry.timestamp.plusMinutes(LOGIN_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
            entry.reset();
        }

        entry.increment();

        if (entry.count >= MAX_LOGIN_ATTEMPTS) {
            // Cast to UUID to resolve ambiguity
            auditService.logAction((UUID) null, "LOGIN_RATE_LIMIT_EXCEEDED", "USER", (UUID) null,
                    "Email: " + email, "system");
            return true; // Rate limit exceeded
        }

        return false;
    }

    /**
     * Clear failed login attempts for email (after successful login)
     *
     * @param email User email
     */
    public void clearFailedLogins(String email) {
        loginAttempts.remove(email.toLowerCase());
    }

    /**
     * Check if login is rate limited
     *
     * @param email User email
     * @return true if rate limited
     */
    public boolean isLoginRateLimited(String email) {
        RateLimitEntry entry = loginAttempts.get(email.toLowerCase());
        if (entry == null) {
            return false;
        }

        // Check if still within window
        if (entry.timestamp.plusMinutes(LOGIN_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
            loginAttempts.remove(email.toLowerCase());
            return false;
        }

        return entry.count >= MAX_LOGIN_ATTEMPTS;
    }

    /**
     * Check and enforce need view rate limit
     *
     * Protects against insider threats browsing many cases
     * Admins are exempt from this limit
     *
     * @param user User attempting to view need
     * @throws RateLimitExceededException if limit exceeded
     */
    public void checkNeedViewRateLimit(User user) throws RateLimitExceededException {
        // Admins exempt from rate limiting
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        UUID userId = user.getId();
        RateLimitEntry entry = needViewCounts.computeIfAbsent(userId,
                k -> new RateLimitEntry());

        // Clean old entries if outside window
        if (entry.timestamp.plusMinutes(NEED_VIEW_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
            entry.reset();
        }

        // Increment first, then check if we've exceeded the limit
        entry.increment();

        // Throw after MAX_NEED_VIEWS_PER_HOUR (allow exactly 20, throw on 21st)
        if (entry.count > MAX_NEED_VIEWS_PER_HOUR) {
            auditService.logAction(userId, "NEED_VIEW_RATE_LIMIT_EXCEEDED", "USER", userId,
                    "Viewed " + entry.count + " needs in 1 hour", "system");
            throw new RateLimitExceededException("Too many need views. Please try again later.");
        }
    }

    /**
     * Check and enforce general API rate limit
     *
     * @param userId User ID
     * @throws RateLimitExceededException if limit exceeded
     */
    public void checkApiRateLimit(UUID userId) throws RateLimitExceededException {
        if (userId == null) {
            return; // Skip for unauthenticated requests (handled by other means)
        }

        RateLimitEntry entry = apiRequestCounts.computeIfAbsent(userId,
                k -> new RateLimitEntry());

        // Clean old entries if outside window
        if (entry.timestamp.plusMinutes(API_REQUEST_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
            entry.reset();
        }

        entry.increment();

        if (entry.count > MAX_API_REQUESTS_PER_MINUTE) {
            auditService.logAction(userId, "API_RATE_LIMIT_EXCEEDED", "USER", userId,
                    "Made " + entry.count + " requests in 1 minute", "system");
            throw new RateLimitExceededException("Too many requests. Please slow down.");
        }
    }

    /**
     * Get remaining login attempts
     *
     * @param email User email
     * @return Number of remaining attempts before lockout
     */
    public int getRemainingLoginAttempts(String email) {
        RateLimitEntry entry = loginAttempts.get(email.toLowerCase());
        if (entry == null) {
            return MAX_LOGIN_ATTEMPTS;
        }

        // Check if window expired
        if (entry.timestamp.plusMinutes(LOGIN_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
            return MAX_LOGIN_ATTEMPTS;
        }

        int remaining = MAX_LOGIN_ATTEMPTS - entry.count;
        return Math.max(0, remaining);
    }

    /**
     * Cleanup expired rate limit entries (scheduled task)
     */
    public void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();

        // Clean login attempts
        loginAttempts.entrySet().removeIf(e ->
                e.getValue().timestamp.plusMinutes(LOGIN_WINDOW_MINUTES).isBefore(now));

        // Clean need view counts
        needViewCounts.entrySet().removeIf(e ->
                e.getValue().timestamp.plusMinutes(NEED_VIEW_WINDOW_MINUTES).isBefore(now));

        // Clean API request counts
        apiRequestCounts.entrySet().removeIf(e ->
                e.getValue().timestamp.plusMinutes(API_REQUEST_WINDOW_MINUTES).isBefore(now));
    }

    /**
     * Internal class to track rate limit entries
     */
    private static class RateLimitEntry {
        int count;
        LocalDateTime timestamp;

        RateLimitEntry() {
            this.count = 0;
            this.timestamp = LocalDateTime.now();
        }

        void increment() {
            this.count++;
        }

        void reset() {
            this.count = 0;
            this.timestamp = LocalDateTime.now();
        }
    }

    /**
     * Exception thrown when rate limit is exceeded
     */
    public static class RateLimitExceededException extends Exception {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
