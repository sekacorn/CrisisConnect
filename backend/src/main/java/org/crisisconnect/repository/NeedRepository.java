package org.crisisconnect.repository;

import org.crisisconnect.model.entity.Need;
import org.crisisconnect.model.enums.NeedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NeedRepository extends JpaRepository<Need, UUID> {
    List<Need> findByStatus(NeedStatus status);
    List<Need> findByCreatedByUserId(UUID userId);
    List<Need> findByAssignedOrganizationId(UUID organizationId);
    List<Need> findByCountry(String country);

    @Query("SELECT n FROM Need n WHERE n.country = :country AND n.status = :status")
    List<Need> findByCountryAndStatus(@Param("country") String country, @Param("status") NeedStatus status);

    /**
     * Count needs created by user after a specific date
     * Used for fraud detection (anomalous creation rate)
     */
    long countByCreatedByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after);

    /**
     * Find needs by status and closed before a specific date
     * Used for automatic PII deletion (closed > 90 days)
     * TODO: Add closedAt field to Need entity before enabling this method
     */
    // List<Need> findByStatusAndClosedAtBefore(NeedStatus status, LocalDateTime before);

    /**
     * Find needs by multiple statuses (for batch operations)
     */
    @Query("SELECT n FROM Need n WHERE n.status IN :statuses")
    List<Need> findByStatusIn(@Param("statuses") List<NeedStatus> statuses);
}
