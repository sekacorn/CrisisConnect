package org.crisisconnect.repository;

import org.crisisconnect.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<AuditLog> findByActionTypeOrderByCreatedAtDesc(String actionType);

    /**
     * Count audit log entries by user, action, and time range
     * Used for suspicious activity detection
     *
     * @param userId User ID
     * @param action Action type (e.g., "NEED_ACCESSED_FULL", "NEED_CLAIMED")
     * @param after DateTime threshold
     * @return Count of matching audit logs
     */
    long countByUserIdAndActionTypeAndCreatedAtAfter(UUID userId, String action, LocalDateTime after); // Original method name

    /**
     * Find recent audit logs by action type and time range
     * Used for suspicious activity monitoring
     */
    List<AuditLog> findByActionTypeAndCreatedAtAfterOrderByCreatedAtDesc(
        String actionType, LocalDateTime after
    );

    /**
     * Find audit logs by user within time range
     * Used for user activity analysis
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.userId = :userId " +
           "AND al.createdAt >= :startDate " +
           "AND al.createdAt <= :endDate " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findByUserIdAndDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find suspicious activity flags (custom action types)
     * Used for admin dashboard
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.actionType LIKE 'SUSPICIOUS_%' " +
           "AND al.createdAt >= :since " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentSuspiciousActivities(@Param("since") LocalDateTime since);

    /**
     * Count audit logs by action type after a certain time
     * Used for admin dashboard statistics
     */
    long countByActionTypeAndCreatedAtAfter(String actionType, LocalDateTime after);

    /**
     * Find audit logs by action type with pagination
     * Used for admin audit log viewer
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.actionType = :actionType " +
           "ORDER BY al.createdAt DESC")
    org.springframework.data.domain.Page<AuditLog> findByActionType(
        @Param("actionType") String actionType,
        org.springframework.data.domain.Pageable pageable
    );
}
