package org.crisisconnect.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Startup health check component that verifies critical endpoints are accessible
 * when the application starts. This helps catch security configuration issues early.
 *
 * Checks performed:
 * 1. Authentication endpoint (/api/auth/login) is publicly accessible (not 403)
 * 2. Health check endpoint is accessible
 * 3. H2 console is accessible (if enabled)
 *
 * If any critical endpoints return 403 Forbidden, a warning is logged.
 */
@Component
public class StartupHealthCheck implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StartupHealthCheck.class);

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("========================================");
        logger.info("Running Startup Health Checks...");
        logger.info("========================================");

        String baseUrl = "http://localhost:" + serverPort + contextPath;

        // Check 1: Authentication endpoint should NOT return 403
        checkAuthenticationEndpoint(baseUrl);

        // Check 2: Health endpoint
        checkHealthEndpoint(baseUrl);

        // Check 3: H2 Console (if enabled)
        if (h2ConsoleEnabled) {
            checkH2Console(baseUrl);
        }

        logger.info("========================================");
        logger.info("Startup Health Checks Completed");
        logger.info("========================================");
    }

    private void checkAuthenticationEndpoint(String baseUrl) {
        String endpoint = baseUrl + "/auth/login";
        try {
            // We don't care if we get 400 or 401, but we should NOT get 403
            ResponseEntity<String> response = restTemplate.postForEntity(
                    endpoint,
                    "{\"email\":\"test@test.com\",\"password\":\"test\"}",
                    String.class
            );

            HttpStatus status = (HttpStatus) response.getStatusCode();
            if (status == HttpStatus.FORBIDDEN) {
                logger.error("❌ CRITICAL: Authentication endpoint returns 403 FORBIDDEN");
                logger.error("   This indicates a security configuration error.");
                logger.error("   Users will NOT be able to log in!");
                logger.error("   Check SecurityConfig.java requestMatchers configuration.");
            } else {
                logger.info(" Authentication endpoint accessible: {} (status: {})", endpoint, status);
            }
        } catch (Exception e) {
            // Check if it's a 403 error
            if (e.getMessage() != null && e.getMessage().contains("403")) {
                logger.error("❌ CRITICAL: Authentication endpoint returns 403 FORBIDDEN");
                logger.error("   Endpoint: {}", endpoint);
                logger.error("   This indicates a security configuration error.");
                logger.error("   Users will NOT be able to log in!");
            } else if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("400"))) {
                // 401 Unauthorized or 400 Bad Request is expected with invalid credentials
                logger.info(" Authentication endpoint accessible: {} (status: 401/400 as expected)", endpoint);
            } else {
                logger.warn("⚠ Authentication endpoint check failed: {}", e.getMessage());
            }
        }
    }

    private void checkHealthEndpoint(String baseUrl) {
        String endpoint = baseUrl.replace(contextPath, "") + "/actuator/health";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info(" Health endpoint accessible: {}", endpoint);
            } else {
                logger.warn("⚠ Health endpoint returned status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.warn("⚠ Health endpoint check failed: {}", e.getMessage());
        }
    }

    private void checkH2Console(String baseUrl) {
        String endpoint = baseUrl + "/h2-console";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
            HttpStatus status = (HttpStatus) response.getStatusCode();

            // H2 console may redirect (302) or return 200
            if (status.is2xxSuccessful() || status.is3xxRedirection()) {
                logger.info(" H2 Console accessible: {}", endpoint);
            } else if (status == HttpStatus.FORBIDDEN) {
                logger.error("❌ H2 Console blocked by security: {}", endpoint);
            } else {
                logger.warn("⚠ H2 Console returned status: {}", status);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("403")) {
                logger.error("❌ H2 Console blocked by security (403 Forbidden)");
            } else {
                logger.debug("H2 Console check: {}", e.getMessage());
            }
        }
    }
}
