package org.crisisconnect.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Session Tracking
 * Tracks active user sessions for security and session management
 * Enables features like "logout from all devices" and concurrent session limits
 */
@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_session_user_id", columnList = "user_id"),
        @Index(name = "idx_session_token_hash", columnList = "token_hash"),
        @Index(name = "idx_session_expires", columnList = "expires_at"),
        @Index(name = "idx_session_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hash of JWT token for lookup

    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo; // User agent / device description

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    /**
     * Check if session is still valid.
     *
     * @return true if session is active and not expired
     */
    public boolean isValid() {
        return isActive && expiresAt.isAfter(LocalDateTime.now()) && revokedAt == null;
    }

    /**
     * Revoke this session.
     */
    public void revoke() {
        this.isActive = false;
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * Update last activity timestamp.
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
}
