package org.crisisconnect.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crisisconnect.model.enums.UserRole;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Password Policy Configuration
 * NIST SP 800-63B compliant password requirements
 * Configurable by administrators
 */
@Entity
@Table(name = "password_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "policy_name", nullable = false, unique = true, length = 100)
    private String policyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50)
    private UserRole role; // Null = default for all roles

    @Column(name = "min_length", nullable = false)
    private Integer minLength = 12;

    @Column(name = "require_uppercase", nullable = false)
    private Boolean requireUppercase = true;

    @Column(name = "require_lowercase", nullable = false)
    private Boolean requireLowercase = true;

    @Column(name = "require_numbers", nullable = false)
    private Boolean requireNumbers = true;

    @Column(name = "require_special_chars", nullable = false)
    private Boolean requireSpecialChars = true;

    @Column(name = "password_expiry_days")
    private Integer passwordExpiryDays = 90; // NIST recommends no forced expiry, but configurable

    @Column(name = "password_history_count", nullable = false)
    private Integer passwordHistoryCount = 5; // Prevent reuse of last 5 passwords

    @Column(name = "max_failed_attempts", nullable = false)
    private Integer maxFailedAttempts = 5;

    @Column(name = "lockout_duration_minutes", nullable = false)
    private Integer lockoutDurationMinutes = 30;

    @Column(name = "session_timeout_minutes", nullable = false)
    private Integer sessionTimeoutMinutes = 15; // Idle timeout

    @Column(name = "max_session_duration_hours", nullable = false)
    private Integer maxSessionDurationHours = 24;

    @Column(name = "enforce_mfa", nullable = false)
    private Boolean enforceMfa = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;
}
