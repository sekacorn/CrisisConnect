package org.crisisconnect.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.model.enums.OrganizationType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Organization entity representing NGOs, UN agencies, and other aid providers.
 * Only VERIFIED organizations can access full need details.
 */
@Entity
@Table(name = "organizations", indexes = {
        @Index(name = "idx_org_status", columnList = "status"),
        @Index(name = "idx_org_country", columnList = "country")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrganizationType type;

    @Column(nullable = false, length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrganizationStatus status = OrganizationStatus.PENDING;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(length = 50)
    private String phone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
