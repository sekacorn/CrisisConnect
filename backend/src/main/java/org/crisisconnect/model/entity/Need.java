package org.crisisconnect.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crisisconnect.model.enums.NeedCategory;
import org.crisisconnect.model.enums.NeedStatus;
import org.crisisconnect.model.enums.UrgencyLevel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Need entity representing a request for humanitarian assistance.
 * PII is stored separately in SensitiveInfo table.
 * Default queries return redacted data; full data only for authorized users.
 */
@Entity
@Table(name = "needs", indexes = {
        @Index(name = "idx_need_status", columnList = "status"),
        @Index(name = "idx_need_assigned_org", columnList = "assigned_organization_id"),
        @Index(name = "idx_need_creator", columnList = "created_by_user_id"),
        @Index(name = "idx_need_country", columnList = "country"),
        @Index(name = "idx_need_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Need {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NeedStatus status = NeedStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NeedCategory category;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "region_or_state", length = 100)
    private String regionOrState;

    @Column(length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false, length = 50)
    private UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;

    @Column(name = "assigned_organization_id")
    private UUID assignedOrganizationId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "need", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private SensitiveInfo sensitiveInfo;
}
