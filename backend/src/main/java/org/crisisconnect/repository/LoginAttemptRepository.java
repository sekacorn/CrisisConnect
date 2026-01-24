package org.crisisconnect.repository;

import org.crisisconnect.model.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {
    List<LoginAttempt> findByEmailOrderByAttemptedAtDesc(String email);

    @Query("SELECT la FROM LoginAttempt la WHERE la.email = :email AND la.attemptedAt >= :since ORDER BY la.attemptedAt DESC")
    List<LoginAttempt> findRecentAttemptsByEmail(
        @Param("email") String email,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.email = :email AND la.successful = false AND la.attemptedAt >= :since")
    long countFailedAttemptsByEmailSince(
        @Param("email") String email,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT la FROM LoginAttempt la WHERE la.ipAddress = :ip AND la.attemptedAt >= :since")
    List<LoginAttempt> findRecentAttemptsByIp(
        @Param("ip") String ip,
        @Param("since") LocalDateTime since
    );
}
