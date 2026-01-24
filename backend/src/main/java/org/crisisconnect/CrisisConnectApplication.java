package org.crisisconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CrisisConnect - Humanitarian Aid Coordination Platform
 *
 * Main Spring Boot application class.
 * This platform connects field workers with NGOs/UN agencies to coordinate
 * humanitarian assistance while maintaining strict privacy and security controls.
 *
 * @EnableScheduling - Enables scheduled jobs for fraud detection and maintenance:
 *   - Suspicious browsing pattern detection (hourly)
 *   - Anomalous creation rate detection (daily at 8 AM)
 *   - Rate limit cleanup (hourly)
 */
@SpringBootApplication
@EnableScheduling
public class CrisisConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrisisConnectApplication.class, args);
    }
}
