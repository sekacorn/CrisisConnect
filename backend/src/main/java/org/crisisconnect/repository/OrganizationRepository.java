package org.crisisconnect.repository;

import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByStatus(OrganizationStatus status);
    List<Organization> findByCountry(String country);

    // Admin dashboard queries
    long countByStatus(OrganizationStatus status);
}
