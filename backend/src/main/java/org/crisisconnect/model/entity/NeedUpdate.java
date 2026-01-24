package org.crisisconnect.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crisisconnect.model.enums.NeedStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit trail for need status changes
 */
@Entity
@Table(name = "need_updates", indexes = {
        @Index(name = "idx_need_update_need", columnList = "need_id"),
        @Index(name = "idx_need_update_user", columnList = "updated_by_user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NeedUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "need_id", nullable = false)
    private UUID needId;

    @Column(name = "updated_by_user_id", nullable = false)
    private UUID updatedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_from", length = 50)
    private NeedStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to", nullable = false, length = 50)
    private NeedStatus statusTo;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
