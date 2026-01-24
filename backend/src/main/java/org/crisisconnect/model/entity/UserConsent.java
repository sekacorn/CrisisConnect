package org.crisisconnect.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crisisconnect.model.enums.ConsentType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Consent Management
 * GDPR Article 7 compliance
 * Tracks user consent for data processing
 */
@Entity
@Table(name = "user_consents", indexes = {
        @Index(name = "idx_consent_user", columnList = "user_id"),
        @Index(name = "idx_consent_type", columnList = "consent_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;

    @Column(nullable = false)
    private Boolean granted = false;

    @Column(name = "consent_text", columnDefinition = "TEXT")
    private String consentText;

    @Column(name = "consent_version", length = 20)
    private String consentVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
