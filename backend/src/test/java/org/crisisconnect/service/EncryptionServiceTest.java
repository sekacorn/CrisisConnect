package org.crisisconnect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "secretKey", "test-encryption-key-32-chars!");
        ReflectionTestUtils.setField(encryptionService, "algorithm", "AES");
    }

    @Test
    void testEncryptAndDecrypt() {
        String original = "Sensitive beneficiary information";

        String encrypted = encryptionService.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testEncryptNull() {
        String encrypted = encryptionService.encrypt(null);
        assertNull(encrypted);
    }

    @Test
    void testEncryptEmpty() {
        String encrypted = encryptionService.encrypt("");
        assertNull(encrypted);
    }

    @Test
    void testDecryptNull() {
        String decrypted = encryptionService.decrypt(null);
        assertNull(decrypted);
    }

    @Test
    void testEncryptionProducesDifferentOutput() {
        String original = "Test data";
        String encrypted = encryptionService.encrypt(original);

        assertNotEquals(original, encrypted);
        assertTrue(encrypted.length() > original.length());
    }
}
