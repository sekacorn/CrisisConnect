package org.crisisconnect.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sensitive_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "need_id", nullable = false, unique = true)
    private Need need;

    @Column(name = "encrypted_full_name", columnDefinition = "TEXT")
    private String encryptedFullName;

    @Column(name = "encrypted_phone", columnDefinition = "TEXT")
    private String encryptedPhone;

    @Column(name = "encrypted_email", columnDefinition = "TEXT")
    private String encryptedEmail;

    @Column(name = "encrypted_exact_location", columnDefinition = "TEXT")
    private String encryptedExactLocation;

    @Column(name = "encrypted_notes", columnDefinition = "TEXT")
    private String encryptedNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}