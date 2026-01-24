package org.crisisconnect.repository;

import org.crisisconnect.model.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserSession entity
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * Find session by token hash.
     *
     * @param tokenHash SHA-256 hash of JWT token
     * @return Optional UserSession
     */
    Optional<UserSession> findByTokenHash(String tokenHash);

    /**
     * Find all active sessions for a user.
     *
     * @param userId User ID
     * @return List of active sessions
     */
    @Query("SELECT s FROM UserSession s WHERE s.userId = ?1 AND s.isActive = true AND s.expiresAt > CURRENT_TIMESTAMP AND s.revokedAt IS NULL")
    List<UserSession> findActiveSessionsByUserId(UUID userId);

    /**
     * Find all sessions for a user (active and inactive).
     *
     * @param userId User ID
     * @return List of all sessions
     */
    List<UserSession> findByUserId(UUID userId);

    /**
     * Count active sessions for a user.
     *
     * @param userId User ID
     * @return Number of active sessions
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = ?1 AND s.isActive = true AND s.expiresAt > CURRENT_TIMESTAMP AND s.revokedAt IS NULL")
    long countActiveSessionsByUserId(UUID userId);

    /**
     * Revoke all sessions for a user.
     *
     * @param userId User ID
     * @param revokedAt Revocation timestamp
     * @return Number of sessions revoked
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.revokedAt = ?2 WHERE s.userId = ?1 AND s.isActive = true")
    int revokeAllSessionsByUserId(UUID userId, LocalDateTime revokedAt);

    /**
     * Delete expired sessions.
     *
     * @param expirationTime Sessions expired before this time
     * @return Number of sessions deleted
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < ?1")
    int deleteExpiredSessions(LocalDateTime expirationTime);

    /**
     * Find sessions that need activity updates.
     * (Sessions not updated in last 5 minutes)
     *
     * @param lastActivityThreshold Threshold timestamp
     * @return List of sessions
     */
    @Query("SELECT s FROM UserSession s WHERE s.isActive = true AND s.lastActivityAt < ?1")
    List<UserSession> findSessionsNeedingActivityUpdate(LocalDateTime lastActivityThreshold);
}
