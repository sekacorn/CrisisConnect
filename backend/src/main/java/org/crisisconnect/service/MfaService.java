package org.crisisconnect.service;

import org.crisisconnect.model.entity.User;
import org.crisisconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Multi-Factor Authentication Service
 * Implements TOTP (Time-based One-Time Password) RFC 6238
 */
@Service
public class MfaService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    private static final int SECRET_SIZE = 20; // 160 bits
    private static final int TIME_STEP = 30; // 30 seconds
    private static final int CODE_DIGITS = 6;
    private static final int WINDOW = 1; // Allow 1 time step before/after for clock drift

    /**
     * Generate a new MFA secret for a user.
     *
     * @param userId User ID
     * @return Base32-encoded secret
     */
    @Transactional
    public String generateSecret(UUID userId) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SIZE];
        random.nextBytes(bytes);

        String secret = base32Encode(bytes);

        // Store the secret in the user record (but don't enable MFA yet)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setMfaSecret(secret);
        user.setMfaEnabled(false); // User must verify first
        userRepository.save(user);

        auditService.logAction(
            userId,
            "MFA_SECRET_GENERATED",
            "USER",
            userId,
            "MFA secret generated for user",
            null
        );

        return secret;
    }

    /**
     * Enable MFA after successful verification.
     *
     * @param userId User ID
     * @param code TOTP code to verify
     * @param ipAddress IP address
     * @return true if enabled successfully
     */
    @Transactional
    public boolean enableMfa(UUID userId, String code, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getMfaSecret() == null) {
            throw new RuntimeException("MFA secret not generated. Call generateSecret first.");
        }

        // Verify the code before enabling
        if (!verifyCode(user.getMfaSecret(), code)) {
            return false;
        }

        user.setMfaEnabled(true);
        userRepository.save(user);

        auditService.logAction(
            userId,
            "MFA_ENABLED",
            "USER",
            userId,
            "MFA enabled for user",
            ipAddress
        );

        return true;
    }

    /**
     * Disable MFA for a user.
     *
     * @param userId User ID
     * @param code TOTP code to verify (required for security)
     * @param ipAddress IP address
     * @return true if disabled successfully
     */
    @Transactional
    public boolean disableMfa(UUID userId, String code, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getMfaEnabled()) {
            return true; // Already disabled
        }

        // Verify the code before disabling (security measure)
        if (!verifyCode(user.getMfaSecret(), code)) {
            return false;
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null); // Clear the secret
        userRepository.save(user);

        auditService.logAction(
            userId,
            "MFA_DISABLED",
            "USER",
            userId,
            "MFA disabled for user",
            ipAddress
        );

        return true;
    }

    /**
     * Verify a TOTP code.
     *
     * @param secret Base32-encoded secret
     * @param code 6-digit code to verify
     * @return true if code is valid
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }

        try {
            long currentTime = System.currentTimeMillis() / 1000L / TIME_STEP;

            // Check current time window and adjacent windows (for clock drift)
            for (int i = -WINDOW; i <= WINDOW; i++) {
                String expectedCode = generateCode(secret, currentTime + i);
                if (code.equals(expectedCode)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate TOTP code for a given time.
     *
     * @param secret Base32-encoded secret
     * @param time Time counter
     * @return 6-digit TOTP code
     */
    private String generateCode(String secret, long time) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] key = base32Decode(secret);
        byte[] data = new byte[8];
        long value = time;

        // Convert time to byte array (big-endian)
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        // Generate HMAC-SHA1
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        // Dynamic truncation (RFC 4226)
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, CODE_DIGITS);

        // Pad with zeros
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }

    /**
     * Generate QR code URL for authenticator apps.
     *
     * @param userId User ID
     * @param secret Base32-encoded secret
     * @return otpauth:// URL
     */
    public String getQrCodeUrl(UUID userId, String secret) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String issuer = "CrisisConnect";
        String accountName = user.getEmail();

        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
            issuer,
            accountName,
            secret,
            issuer,
            CODE_DIGITS,
            TIME_STEP
        );
    }

    /**
     * Check if user has MFA enabled.
     *
     * @param userId User ID
     * @return true if MFA is enabled
     */
    public boolean isMfaEnabled(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getMfaEnabled();
    }

    /**
     * Base32 encoding (RFC 4648).
     */
    private String base32Encode(byte[] bytes) {
        String base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;

            while (bitsLeft >= 5) {
                result.append(base32Chars.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }

        if (bitsLeft > 0) {
            result.append(base32Chars.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }

        return result.toString();
    }

    /**
     * Base32 decoding (RFC 4648).
     */
    private byte[] base32Decode(String encoded) {
        String base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        encoded = encoded.toUpperCase().replaceAll("\\s", "");

        byte[] result = new byte[encoded.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char c : encoded.toCharArray()) {
            int value = base32Chars.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid Base32 character: " + c);
            }

            buffer = (buffer << 5) | value;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                result[index++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }

        return result;
    }
}
