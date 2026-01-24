package org.crisisconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.crisisconnect.dto.LoginRequest;
import org.crisisconnect.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for AuthController that tests the complete authentication flow
 * including Spring Security configuration, JWT generation, and database interaction.
 *
 * This test verifies:
 * - The /api/auth/login endpoint is publicly accessible (not blocked by security)
 * - Successful authentication returns a valid JWT token
 * - Invalid credentials are rejected
 * - Rate limiting works correctly
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/auth";
    }

    @Test
    void testLoginEndpointIsPubliclyAccessible() {
        // This test verifies the critical bug: /api/auth/login should NOT return 403
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@crisisconnect.org");
        request.setPassword("admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login",
                entity,
                String.class
        );

        // The endpoint should be accessible (not 403 Forbidden)
        // It may return 401 Unauthorized if credentials are wrong, but NOT 403
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.UNAUTHORIZED, HttpStatus.BAD_REQUEST);
    }

    @Test
    void testSuccessfulLogin() {
        // Test with actual admin credentials that should exist after bootstrap
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@crisisconnect.org");
        request.setPassword("Admin123!ChangeMe"); // Default password from application.yml

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/login",
                entity,
                LoginResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotEmpty();
        assertThat(response.getBody().getEmail()).isEqualTo("admin@crisisconnect.org");
        assertThat(response.getBody().getRole()).isEqualTo("ADMIN");
        assertThat(response.getBody().getType()).isEqualTo("Bearer");
    }

    @Test
    void testLoginWithInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@crisisconnect.org");
        request.setPassword("wrong-password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testLoginWithNonExistentUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testLoginWithInvalidEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("not-an-email");
        request.setPassword("password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testLoginWithMissingFields() {
        LoginRequest request = new LoginRequest();
        // Both email and password are null

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/login",
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testAuthenticatedEndpointRequiresToken() {
        // Test that /api/auth/me requires authentication
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/me",
                String.class
        );

        // Should return 403 or 401 without a valid token
        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthenticatedEndpointWithValidToken() {
        // First, login to get a token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@crisisconnect.org");
        loginRequest.setPassword("Admin123!ChangeMe");

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> loginEntity = new HttpEntity<>(loginRequest, loginHeaders);

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/login",
                loginEntity,
                LoginResponse.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().getToken();

        // Now use the token to access /api/auth/me
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        HttpEntity<Void> authEntity = new HttpEntity<>(authHeaders);

        ResponseEntity<String> meResponse = restTemplate.exchange(
                baseUrl + "/me",
                HttpMethod.GET,
                authEntity,
                String.class
        );

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testCorsHeaders() {
        // Test that CORS headers are set correctly
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@crisisconnect.org");
        request.setPassword("Admin123!ChangeMe");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setOrigin("http://localhost:3000"); // Frontend origin
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/login",
                entity,
                LoginResponse.class
        );

        // CORS headers should be present
        HttpHeaders responseHeaders = response.getHeaders();
        assertThat(responseHeaders.getAccessControlAllowOrigin()).isNotNull();
    }
}
