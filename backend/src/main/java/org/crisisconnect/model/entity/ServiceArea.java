package org.crisisconnect.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crisisconnect.model.enums.NeedCategory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service area defining where an organization provides assistance
 * and what categories of needs they handle.
 */
@Entity
@Table(name = "service_areas", indexes = {
        @Index(name = "idx_service_area_org", columnList = "organization_id"),
        @Index(name = "idx_service_area_country", columnList = "country")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceArea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "region_or_state", length = 100)
    private String regionOrState;

    @Column(length = 100)
    private String city;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_area_categories", joinColumns = @JoinColumn(name = "service_area_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private Set<NeedCategory> serviceCategories = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
