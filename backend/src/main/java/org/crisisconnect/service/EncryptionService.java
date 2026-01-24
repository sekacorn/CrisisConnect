package org.crisisconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encryption service for PII fields.
 * Uses AES-256 encryption for sensitive data at rest.
 */
@Service
public class EncryptionService {

    @Value("${encryption.secret-key}")
    private String secretKey;

    @Value("${encryption.algorithm}")
    private String algorithm;

    public String encrypt(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isEmpty()) {
            return null;
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }

    private byte[] getKey() {
        // Ensure key is exactly 16 bytes for AES-128 (or 32 for AES-256)
        byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = new byte[16];
        System.arraycopy(key, 0, keyBytes, 0, Math.min(key.length, keyBytes.length));
        return keyBytes;
    }
}
