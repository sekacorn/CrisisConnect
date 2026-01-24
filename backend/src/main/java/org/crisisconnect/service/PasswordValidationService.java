package org.crisisconnect.service;

import org.crisisconnect.dto.PasswordValidationResult;
import org.crisisconnect.model.entity.PasswordHistory;
import org.crisisconnect.model.entity.PasswordPolicy;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.PasswordHistoryRepository;
import org.crisisconnect.repository.PasswordPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for validating passwords against NIST SP 800-63B requirements.
 * Validates password strength, complexity, and history to prevent reuse.
 */
@Service
public class PasswordValidationService {

    @Autowired
    private PasswordPolicyRepository passwordPolicyRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Common weak passwords (NIST SP 800-63B Appendix A)
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(List.of(
        "password", "123456", "12345678", "qwerty", "abc123", "monkey", "1234567",
        "letmein", "trustno1", "dragon", "baseball", "111111", "iloveyou", "master",
        "sunshine", "ashley", "bailey", "passw0rd", "shadow", "123123", "654321",
        "superman", "qazwsx", "michael", "football", "password1", "admin", "admin123"
    ));

    /**
     * Validate password for a new user registration.
     *
     * @param password Plain text password
     * @param role User role (determines which policy to apply)
     * @return PasswordValidationResult with validation errors and suggestions
     */
    public PasswordValidationResult validatePassword(String password, UserRole role) {
        return validatePassword(password, role, null);
    }

    /**
     * Validate password for existing user (checks password history).
     *
     * @param password Plain text password
     * @param role User role (determines which policy to apply)
     * @param userId User ID (for password history check)
     * @return PasswordValidationResult with validation errors and suggestions
     */
    public PasswordValidationResult validatePassword(String password, UserRole role, UUID userId) {
        PasswordValidationResult result = new PasswordValidationResult(true);

        // Get active password policy for role (or default)
        PasswordPolicy policy = passwordPolicyRepository.findByRoleAndIsActiveTrue(role)
                .or(() -> passwordPolicyRepository.findTopByIsActiveTrueOrderByCreatedAtDesc())
                .orElse(getDefaultPolicy());

        // 1. Check minimum length (NIST SP 800-63B: minimum 8, recommended 12+)
        if (password.length() < policy.getMinLength()) {
            result.addError(String.format("Password must be at least %d characters long", policy.getMinLength()));
        }

        // 2. Check maximum length (prevent DoS attacks)
        if (password.length() > 128) {
            result.addError("Password must not exceed 128 characters");
        }

        // 3. Check complexity requirements
        if (policy.getRequireUppercase() && !Pattern.compile("[A-Z]").matcher(password).find()) {
            result.addError("Password must contain at least one uppercase letter (A-Z)");
        }

        if (policy.getRequireLowercase() && !Pattern.compile("[a-z]").matcher(password).find()) {
            result.addError("Password must contain at least one lowercase letter (a-z)");
        }

        if (policy.getRequireNumbers() && !Pattern.compile("[0-9]").matcher(password).find()) {
            result.addError("Password must contain at least one number (0-9)");
        }

        if (policy.getRequireSpecialChars() && !Pattern.compile("[^A-Za-z0-9]").matcher(password).find()) {
            result.addError("Password must contain at least one special character (!@#$%^&*-_+=)");
        }

        // 4. Check against common passwords (NIST SP 800-63B requirement)
        if (isCommonPassword(password)) {
            result.addError("This password is too common. Please choose a more unique password");
            result.addSuggestion("Avoid dictionary words and common patterns");
        }

        // 5. Check for repeated characters
        if (hasRepeatedCharacters(password, 3)) {
            result.addError("Password contains too many repeated characters");
        }

        // 6. Check for sequential characters
        if (hasSequentialCharacters(password, 4)) {
            result.addError("Password contains sequential characters (e.g., '1234', 'abcd')");
        }

        // 7. Check password history (if user exists)
        if (userId != null) {
            if (isPasswordReused(password, userId, policy.getPasswordHistoryCount())) {
                result.addError(String.format("Password cannot be the same as your last %d passwords",
                    policy.getPasswordHistoryCount()));
            }
        }

        // 8. Calculate strength score
        int strengthScore = calculateStrengthScore(password);
        result.setStrengthScore(strengthScore);

        // 9. Add suggestions for weak passwords
        if (strengthScore < 60) {
            result.addSuggestion("Consider using a longer password for better security");
            result.addSuggestion("Mix uppercase, lowercase, numbers, and special characters");
            result.addSuggestion("Use a passphrase made of random words");
        }

        return result;
    }

    /**
     * Calculate password strength score (0-100).
     * Based on length, character diversity, and entropy.
     */
    private int calculateStrengthScore(String password) {
        int score = 0;

        // Length score (up to 40 points)
        int lengthScore = Math.min(40, password.length() * 3);
        score += lengthScore;

        // Character diversity score (up to 40 points)
        int diversity = 0;
        if (Pattern.compile("[a-z]").matcher(password).find()) diversity += 10;
        if (Pattern.compile("[A-Z]").matcher(password).find()) diversity += 10;
        if (Pattern.compile("[0-9]").matcher(password).find()) diversity += 10;
        if (Pattern.compile("[^A-Za-z0-9]").matcher(password).find()) diversity += 10;
        score += diversity;

        // Uniqueness score (up to 20 points)
        Set<Character> uniqueChars = new HashSet<>();
        for (char c : password.toCharArray()) {
            uniqueChars.add(c);
        }
        double uniqueRatio = (double) uniqueChars.size() / password.length();
        score += (int) (uniqueRatio * 20);

        return Math.min(100, score);
    }

    /**
     * Check if password is in common password list.
     */
    private boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();

        // Check exact match
        if (COMMON_PASSWORDS.contains(lowerPassword)) {
            return true;
        }

        // Check if password contains common words
        for (String commonWord : COMMON_PASSWORDS) {
            if (lowerPassword.contains(commonWord) && commonWord.length() > 4) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if password has repeated characters (e.g., "aaa", "111").
     */
    private boolean hasRepeatedCharacters(String password, int maxRepeat) {
        for (int i = 0; i < password.length() - maxRepeat + 1; i++) {
            char c = password.charAt(i);
            boolean allSame = true;
            for (int j = 1; j < maxRepeat; j++) {
                if (password.charAt(i + j) != c) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if password has sequential characters (e.g., "1234", "abcd").
     */
    private boolean hasSequentialCharacters(String password, int maxSequence) {
        String lowerPassword = password.toLowerCase();

        for (int i = 0; i < lowerPassword.length() - maxSequence + 1; i++) {
            boolean isSequential = true;
            for (int j = 1; j < maxSequence; j++) {
                if (lowerPassword.charAt(i + j) != lowerPassword.charAt(i + j - 1) + 1) {
                    isSequential = false;
                    break;
                }
            }
            if (isSequential) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if password has been used before.
     */
    private boolean isPasswordReused(String password, UUID userId, int historyCount) {
        List<PasswordHistory> history = passwordHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        // Check against last N passwords
        int checkCount = Math.min(historyCount, history.size());
        for (int i = 0; i < checkCount; i++) {
            if (passwordEncoder.matches(password, history.get(i).getPasswordHash())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get default password policy if none configured.
     */
    private PasswordPolicy getDefaultPolicy() {
        PasswordPolicy defaultPolicy = new PasswordPolicy();
        defaultPolicy.setPolicyName("Default NIST Policy");
        defaultPolicy.setRole(null); // Default for all roles
        defaultPolicy.setMinLength(12);
        defaultPolicy.setRequireUppercase(true);
        defaultPolicy.setRequireLowercase(true);
        defaultPolicy.setRequireNumbers(true);
        defaultPolicy.setRequireSpecialChars(true);
        defaultPolicy.setPasswordExpiryDays(90);
        defaultPolicy.setPasswordHistoryCount(5);
        defaultPolicy.setMaxFailedAttempts(5);
        defaultPolicy.setLockoutDurationMinutes(30);
        defaultPolicy.setIsActive(true);
        defaultPolicy.setIsDefault(true);
        return defaultPolicy;
    }

    /**
     * Get active password policy for a role.
     */
    public PasswordPolicy getPasswordPolicy(UserRole role) {
        return passwordPolicyRepository.findByRoleAndIsActiveTrue(role)
                .or(() -> passwordPolicyRepository.findTopByIsActiveTrueOrderByCreatedAtDesc())
                .orElse(getDefaultPolicy());
    }

    /**
     * Validate if user's password has expired.
     */
    public boolean isPasswordExpired(User user) {
        if (user.getPasswordExpiresAt() == null) {
            return false;
        }
        return user.getPasswordExpiresAt().isBefore(java.time.LocalDateTime.now());
    }

    /**
     * Check if password meets minimum strength threshold.
     */
    public boolean meetsMinimumStrength(String password, int minimumScore) {
        int score = calculateStrengthScore(password);
        return score >= minimumScore;
    }
}
