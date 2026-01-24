package org.crisisconnect.repository;

import org.crisisconnect.model.entity.UserConsent;
import org.crisisconnect.model.enums.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    List<UserConsent> findByUserId(UUID userId);

    Optional<UserConsent> findByUserIdAndConsentType(UUID userId, ConsentType consentType);

    List<UserConsent> findByUserIdAndGrantedTrue(UUID userId);

    boolean existsByUserIdAndConsentTypeAndGrantedTrue(UUID userId, ConsentType consentType);
}
