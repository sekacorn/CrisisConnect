package org.crisisconnect.service;

import org.crisisconnect.dto.LoginRequest;
import org.crisisconnect.dto.LoginResponse;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.UserRepository;
import org.crisisconnect.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 *
 * Tests cover:
 * - Successful login flow with JWT generation
 * - Failed login with invalid credentials
 * - Audit logging for login attempts
 * - User retrieval by email
 * - User retrieval from Authentication object
 * - Token generation and user data mapping
 * - Edge cases and error handling
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuditService auditService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private User adminUser;
    private User ngoStaff;
    private LoginRequest validRequest;
    private LoginRequest invalidRequest;
    private String testIpAddress = "192.168.1.100";
    private String testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("user@test.com");
        testUser.setName("Test User");
        testUser.setRole(UserRole.FIELD_WORKER);
        testUser.setIsActive(true);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@crisisconnect.org");
        adminUser.setName("Admin User");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);

        ngoStaff = new User();
        ngoStaff.setId(UUID.randomUUID());
        ngoStaff.setEmail("ngo@organization.org");
        ngoStaff.setName("NGO Staff");
        ngoStaff.setRole(UserRole.NGO_STAFF);
        ngoStaff.setOrganizationId(UUID.randomUUID());
        ngoStaff.setIsActive(true);

        // Create login requests
        validRequest = new LoginRequest();
        validRequest.setEmail("user@test.com");
        validRequest.setPassword("correct-password");

        invalidRequest = new LoginRequest();
        invalidRequest.setEmail("user@test.com");
        invalidRequest.setPassword("wrong-password");
    }

    // ==================== Successful Login Tests ====================

    @Test
    void testLogin_ValidCredentials_ReturnsLoginResponse() {
        // Arrange
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(validRequest.getEmail(), validRequest.getPassword());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authToken);
        when(userRepository.findByEmail(validRequest.getEmail()))
            .thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole().name()))
            .thenReturn(testToken);

        // Act
        LoginResponse response = authService.login(validRequest, testIpAddress);

        // Assert
        assertNotNull(response);
        assertEquals(testToken, response.getToken());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getName(), response.getName());
        assertEquals(testUser.getRole().name(), response.getRole());

        // Verify authentication was attempted
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Verify JWT token was generated
        verify(jwtUtil).generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole().name());

        // Verify successful login was audited
        verify(auditService).logAction(
            eq(testUser.getId()),
            eq("LOGIN"),
            eq("USER"),
            eq(testUser.getId()),
            isNull(),
            eq(testIpAddress)
        );
    }

    @Test
    void testLogin_AdminUser_ReturnsAdminRole() {
        // Arrange
        LoginRequest adminRequest = new LoginRequest();
        adminRequest.setEmail(adminUser.getEmail());
        adminRequest.setPassword("admin-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(adminUser.getEmail()))
            .thenReturn(Optional.of(adminUser));
        when(jwtUtil.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name()))
            .thenReturn(testToken);

        // Act
        LoginResponse response = authService.login(adminRequest, testIpAddress);

        // Assert
        assertEquals("ADMIN", response.getRole());
        assertNull(response.getOrganizationId());
    }

    @Test
    void testLogin_NGOStaff_ReturnsOrganizationId() {
        // Arrange
        LoginRequest ngoRequest = new LoginRequest();
        ngoRequest.setEmail(ngoStaff.getEmail());
        ngoRequest.setPassword("ngo-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(ngoStaff.getEmail()))
            .thenReturn(Optional.of(ngoStaff));
        when(jwtUtil.generateToken(ngoStaff.getId(), ngoStaff.getEmail(), ngoStaff.getRole().name()))
            .thenReturn(testToken);

        // Act
        LoginResponse response = authService.login(ngoRequest, testIpAddress);

        // Assert
        assertEquals("NGO_STAFF", response.getRole());
        assertEquals(ngoStaff.getOrganizationId(), response.getOrganizationId());
    }

    // ==================== Failed Login Tests ====================

    @Test
    void testLogin_InvalidCredentials_ThrowsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.login(invalidRequest, testIpAddress)
        );

        assertEquals("Invalid credentials", exception.getMessage());

        // Verify failed login was audited with email
        verify(auditService).logAction(
            isNull(),
            eq("LOGIN_FAILED"),
            eq("USER"),
            isNull(),
            eq(invalidRequest.getEmail()),
            eq(testIpAddress)
        );

        // Verify no token was generated
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void testLogin_UserNotFoundAfterAuth_ThrowsException() {
        // Arrange - User authenticated but not found in database (edge case)
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(validRequest.getEmail()))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            authService.login(validRequest, testIpAddress)
        );

        // Verify no token was generated
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void testLogin_MultipleFailedAttempts_AllAudited() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act - Attempt login 5 times
        for (int i = 0; i < 5; i++) {
            try {
                authService.login(invalidRequest, testIpAddress);
            } catch (RuntimeException e) {
                // Expected
            }
        }

        // Assert - All 5 failed attempts were audited
        verify(auditService, times(5)).logAction(
            isNull(),
            eq("LOGIN_FAILED"),
            eq("USER"),
            isNull(),
            eq(invalidRequest.getEmail()),
            eq(testIpAddress)
        );
    }

    // ==================== getCurrentUser by Email Tests ====================

    @Test
    void testGetCurrentUser_ByEmail_ReturnsUser() {
        // Arrange
        when(userRepository.findByEmail(testUser.getEmail()))
            .thenReturn(Optional.of(testUser));

        // Act
        User result = authService.getCurrentUser(testUser.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getRole(), result.getRole());

        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void testGetCurrentUser_ByEmail_NotFound_ThrowsException() {
        // Arrange
        String nonExistentEmail = "nonexistent@test.com";
        when(userRepository.findByEmail(nonExistentEmail))
            .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.getCurrentUser(nonExistentEmail)
        );

        assertEquals("User not found", exception.getMessage());
    }

    // ==================== getCurrentUser by Authentication Tests ====================

    @Test
    void testGetCurrentUser_ByAuthentication_ReturnsUser() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail()))
            .thenReturn(Optional.of(testUser));

        // Act
        User result = authService.getCurrentUser(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());

        verify(authentication).isAuthenticated();
        verify(authentication).getName();
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void testGetCurrentUser_NullAuthentication_ThrowsException() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.getCurrentUser((Authentication) null)
        );

        assertEquals("User not authenticated", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void testGetCurrentUser_NotAuthenticated_ThrowsException() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.getCurrentUser(authentication)
        );

        assertEquals("User not authenticated", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void testGetCurrentUser_AuthenticatedButUserNotFound_ThrowsException() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nonexistent@test.com");
        when(userRepository.findByEmail("nonexistent@test.com"))
            .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.getCurrentUser(authentication)
        );

        assertEquals("User not found", exception.getMessage());
    }

    // ==================== IP Address Tracking Tests ====================

    @Test
    void testLogin_DifferentIPAddresses_AllTracked() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(validRequest.getEmail()))
            .thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn(testToken);

        String ip1 = "192.168.1.100";
        String ip2 = "10.0.0.50";
        String ip3 = "203.0.113.42";

        // Act
        authService.login(validRequest, ip1);
        authService.login(validRequest, ip2);
        authService.login(validRequest, ip3);

        // Assert - Verify all 3 IPs were logged
        verify(auditService).logAction(any(), eq("LOGIN"), any(), any(), any(), eq(ip1));
        verify(auditService).logAction(any(), eq("LOGIN"), any(), any(), any(), eq(ip2));
        verify(auditService).logAction(any(), eq("LOGIN"), any(), any(), any(), eq(ip3));
    }

    @Test
    void testLogin_FailedLogin_IPTracked() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act
        try {
            authService.login(invalidRequest, testIpAddress);
        } catch (RuntimeException e) {
            // Expected
        }

        // Assert
        verify(auditService).logAction(
            isNull(),
            eq("LOGIN_FAILED"),
            eq("USER"),
            isNull(),
            eq(invalidRequest.getEmail()),
            eq(testIpAddress)
        );
    }

    // ==================== JWT Token Generation Tests ====================

    @Test
    void testLogin_GeneratesTokenWithCorrectClaims() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(validRequest.getEmail()))
            .thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole().name()))
            .thenReturn(testToken);

        // Act
        authService.login(validRequest, testIpAddress);

        // Assert - Verify token was generated with correct parameters
        verify(jwtUtil).generateToken(
            eq(testUser.getId()),
            eq(testUser.getEmail()),
            eq("FIELD_WORKER")
        );
    }

    @Test
    void testLogin_DifferentRoles_GeneratesDifferentTokens() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(Authentication.class));

        LoginRequest adminRequest = new LoginRequest();
        adminRequest.setEmail(adminUser.getEmail());
        adminRequest.setPassword("password");

        when(userRepository.findByEmail(adminUser.getEmail()))
            .thenReturn(Optional.of(adminUser));
        when(jwtUtil.generateToken(adminUser.getId(), adminUser.getEmail(), "ADMIN"))
            .thenReturn("admin-token");

        // Act
        authService.login(adminRequest, testIpAddress);

        // Assert
        verify(jwtUtil).generateToken(
            eq(adminUser.getId()),
            eq(adminUser.getEmail()),
            eq("ADMIN")
        );
    }

    // ==================== Integration Scenarios ====================

    @Test
    void testLoginWorkflow_FieldWorker_CompleteFlow() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(testUser.getEmail()))
            .thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn(testToken);

        // Act
        LoginResponse response = authService.login(validRequest, testIpAddress);

        // Assert - Verify complete workflow
        // 1. Authentication was performed
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // 2. User was retrieved from database
        verify(userRepository).findByEmail(testUser.getEmail());

        // 3. JWT token was generated
        verify(jwtUtil).generateToken(testUser.getId(), testUser.getEmail(), "FIELD_WORKER");

        // 4. Login was audited
        verify(auditService).logAction(
            eq(testUser.getId()),
            eq("LOGIN"),
            eq("USER"),
            eq(testUser.getId()),
            isNull(),
            eq(testIpAddress)
        );

        // 5. Response contains all necessary data
        assertNotNull(response);
        assertEquals(testToken, response.getToken());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals("FIELD_WORKER", response.getRole());
    }

    @Test
    void testLoginWorkflow_NGOStaff_IncludesOrganizationId() {
        // Arrange
        LoginRequest ngoRequest = new LoginRequest();
        ngoRequest.setEmail(ngoStaff.getEmail());
        ngoRequest.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(ngoStaff.getEmail()))
            .thenReturn(Optional.of(ngoStaff));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn(testToken);

        // Act
        LoginResponse response = authService.login(ngoRequest, testIpAddress);

        // Assert
        assertEquals(ngoStaff.getOrganizationId(), response.getOrganizationId());
        assertEquals("NGO_STAFF", response.getRole());
    }

    // ==================== Edge Cases ====================

    @Test
    void testLogin_EmptyPassword_FailsAuthentication() {
        // Arrange
        LoginRequest emptyPasswordRequest = new LoginRequest();
        emptyPasswordRequest.setEmail("user@test.com");
        emptyPasswordRequest.setPassword("");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            authService.login(emptyPasswordRequest, testIpAddress)
        );

        verify(auditService).logAction(
            isNull(),
            eq("LOGIN_FAILED"),
            eq("USER"),
            isNull(),
            eq(emptyPasswordRequest.getEmail()),
            eq(testIpAddress)
        );
    }

    @Test
    void testLogin_CaseSensitiveEmail_HandledCorrectly() {
        // Arrange
        LoginRequest upperCaseRequest = new LoginRequest();
        upperCaseRequest.setEmail("USER@TEST.COM");
        upperCaseRequest.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("USER@TEST.COM"))
            .thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn(testToken);

        // Act
        LoginResponse response = authService.login(upperCaseRequest, testIpAddress);

        // Assert
        assertNotNull(response);
        verify(userRepository).findByEmail("USER@TEST.COM");
    }
}
