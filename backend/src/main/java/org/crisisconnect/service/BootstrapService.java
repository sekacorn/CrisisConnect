package org.crisisconnect.service;

import jakarta.annotation.PostConstruct;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Bootstrap service to create initial admin user.
 * Only runs if enabled in configuration.
 */
@Service
public class BootstrapService {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.enabled}")
    private Boolean bootstrapEnabled;

    @Value("${admin.bootstrap.email}")
    private String adminEmail;

    @Value("${admin.bootstrap.password}")
    private String adminPassword;

    @Value("${admin.bootstrap.name}")
    private String adminName;

    @PostConstruct
    public void init() {
        if (!bootstrapEnabled) {
            logger.info("Admin bootstrap disabled");
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            logger.info("Admin user already exists: {}", adminEmail);
            return;
        }

        try {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(UserRole.ADMIN);
            admin.setIsActive(true);

            userRepository.save(admin);
            logger.info("Bootstrap admin user created: {}", adminEmail);
            logger.warn("SECURITY: Remember to disable admin bootstrap in production!");
        } catch (Exception e) {
            logger.error("Failed to create bootstrap admin user", e);
        }
    }
}
