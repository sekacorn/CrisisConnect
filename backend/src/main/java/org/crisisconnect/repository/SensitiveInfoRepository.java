package org.crisisconnect.repository;

import org.crisisconnect.model.entity.SensitiveInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensitiveInfoRepository extends JpaRepository<SensitiveInfo, UUID> {
    /**
     * Find sensitive info by Need ID
     * Uses Spring Data JPA naming convention: need_id refers to the @JoinColumn name
     */
    Optional<SensitiveInfo> findByNeed_Id(UUID needId);
}