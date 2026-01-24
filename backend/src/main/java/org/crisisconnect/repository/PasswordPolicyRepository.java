package org.crisisconnect.repository;

import org.crisisconnect.model.entity.PasswordPolicy;
import org.crisisconnect.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicy, UUID> {
    Optional<PasswordPolicy> findByRoleAndIsActiveTrue(UserRole role);
    Optional<PasswordPolicy> findByRole(UserRole role);
    Optional<PasswordPolicy> findTopByIsActiveTrueOrderByCreatedAtDesc();
}
