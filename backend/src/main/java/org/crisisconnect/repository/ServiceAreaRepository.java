package org.crisisconnect.repository;

import org.crisisconnect.model.entity.ServiceArea;
import org.crisisconnect.model.enums.NeedCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceAreaRepository extends JpaRepository<ServiceArea, UUID> {
    List<ServiceArea> findByOrganizationId(UUID organizationId);
    List<ServiceArea> findByCountry(String country);

    /**
     * Check if organization serves a specific area/category combination
     * Used for need claiming authorization (service area matching)
     * TODO: Fix query - ServiceArea entity needs: isActive field, and fix serviceCategories handling
     * Current entity has: regionOrState (not region), serviceCategories Set (not single category)
     */
    /*
    @Query("SELECT CASE WHEN COUNT(sa) > 0 THEN true ELSE false END " +
           "FROM ServiceArea sa " +
           "WHERE sa.organizationId = :orgId " +
           "AND sa.country = :country " +
           "AND sa.regionOrState = :region " +
           "AND :category MEMBER OF sa.serviceCategories")
    boolean existsByOrganizationIdAndCountryAndRegionAndCategoryAndIsActive(
        @Param("orgId") UUID organizationId,
        @Param("country") String country,
        @Param("region") String region,
        @Param("category") NeedCategory category,
        @Param("isActive") boolean isActive
    );
    */

    /**
     * Find all service areas for an organization (optionally with isActive filter)
     * TODO: Add isActive field to ServiceArea entity before enabling isActive filtering
     */
    // List<ServiceArea> findByOrganizationIdAndIsActive(UUID organizationId, boolean isActive);
}
