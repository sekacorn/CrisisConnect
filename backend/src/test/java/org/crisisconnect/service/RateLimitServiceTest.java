package org.crisisconnect.service;

import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RateLimitService rateLimitService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.NGO_STAFF);
    }

    @Test
    void checkNeedViewRateLimit_underLimit_shouldSucceed() {
        for (int i = 0; i < 20; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkNeedViewRateLimit(user));
        }
    }

    @Test
    void checkNeedViewRateLimit_overLimit_shouldThrowException() {
        for (int i = 0; i < 20; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkNeedViewRateLimit(user));
        }
        assertThrows(RateLimitService.RateLimitExceededException.class,
                () -> rateLimitService.checkNeedViewRateLimit(user));
    }

    @Test
    void checkNeedViewRateLimit_adminUser_shouldNotBeRateLimited() {
        user.setRole(UserRole.ADMIN);
        for (int i = 0; i < 100; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkNeedViewRateLimit(user));
        }
    }
}