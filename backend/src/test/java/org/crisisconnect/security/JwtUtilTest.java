package org.crisisconnect.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-jwt-token-generation-minimum-32-characters");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
        ReflectionTestUtils.setField(jwtUtil, "issuer", "crisisconnect-test");
    }

    @Test
    void testGenerateToken() {
        UUID userId = UUID.randomUUID();
        String username = "test@example.com";
        String role = "ADMIN";

        String token = jwtUtil.generateToken(userId, username, role);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testExtractUsername() {
        UUID userId = UUID.randomUUID();
        String username = "test@example.com";
        String role = "ADMIN";

        String token = jwtUtil.generateToken(userId, username, role);
        String extractedUsername = jwtUtil.extractUsername(token);

        assertEquals(username, extractedUsername);
    }

    @Test
    void testExtractUserId() {
        UUID userId = UUID.randomUUID();
        String username = "test@example.com";
        String role = "ADMIN";

        String token = jwtUtil.generateToken(userId, username, role);
        UUID extractedUserId = jwtUtil.extractUserId(token);

        assertEquals(userId, extractedUserId);
    }

    @Test
    void testExtractRole() {
        UUID userId = UUID.randomUUID();
        String username = "test@example.com";
        String role = "NGO_STAFF";

        String token = jwtUtil.generateToken(userId, username, role);
        String extractedRole = jwtUtil.extractRole(token);

        assertEquals(role, extractedRole);
    }

    @Test
    void testValidateToken() {
        UUID userId = UUID.randomUUID();
        String username = "test@example.com";
        String role = "ADMIN";

        String token = jwtUtil.generateToken(userId, username, role);
        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.token.here";
        boolean isValid = jwtUtil.validateToken(invalidToken);

        assertFalse(isValid);
    }
}
