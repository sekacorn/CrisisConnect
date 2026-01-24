package org.crisisconnect.service;

import org.crisisconnect.model.entity.Need;
import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.ServiceArea;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.NeedCategory;
import org.crisisconnect.model.enums.NeedStatus;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.NeedRepository;
import org.crisisconnect.repository.OrganizationRepository;
import org.crisisconnect.repository.ServiceAreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NeedSecurityServiceTest {

    @Mock
    private NeedRepository needRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AuthService authService;

    @Mock
    private ServiceAreaRepository serviceAreaRepository;

    @InjectMocks
    private NeedSecurityService needSecurityService;

    private User adminUser;
    private User fieldWorkerUser;
    private User ngoStaffUser;
    private User otherNgoStaffUser;

    private Organization verifiedOrg;
    private Organization unverifiedOrg;

    private Need createdNeed;
    private Need assignedNeed;

    @BeforeEach
    void setUp() {
        // Users
        adminUser = createUser(UserRole.ADMIN, null);
        fieldWorkerUser = createUser(UserRole.FIELD_WORKER, null);
        verifiedOrg = createOrganization(OrganizationStatus.VERIFIED);
        ngoStaffUser = createUser(UserRole.NGO_STAFF, verifiedOrg.getId());
        unverifiedOrg = createOrganization(OrganizationStatus.PENDING);
        otherNgoStaffUser = createUser(UserRole.NGO_STAFF, unverifiedOrg.getId());

        // Needs
        createdNeed = createNeed(fieldWorkerUser.getId(), null, NeedStatus.NEW);
        assignedNeed = createNeed(fieldWorkerUser.getId(), verifiedOrg.getId(), NeedStatus.ASSIGNED);
    }

    private User createUser(UserRole role, UUID organizationId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        user.setOrganizationId(organizationId);
        return user;
    }

    private Organization createOrganization(OrganizationStatus status) {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setStatus(status);
        return org;
    }

    private Need createNeed(UUID createdBy, UUID assignedToOrg, NeedStatus status) {
        Need need = new Need();
        need.setId(UUID.randomUUID());
        need.setCreatedByUserId(createdBy);
        need.setStatus(status);
        need.setAssignedOrganizationId(assignedToOrg);
        need.setCountry("USA");
        need.setCategory(NeedCategory.FOOD);
        return need;
    }

    private Authentication createAuth(User user) {
        return new UsernamePasswordAuthenticationToken(user.getEmail(), "password");
    }

    // --- Tests for canAccessNeed ---

    @Test
    void canAccessNeed_adminCanAccessAnyNeed() {
        when(needRepository.findById(createdNeed.getId())).thenReturn(Optional.of(createdNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(adminUser);
        assertTrue(needSecurityService.canAccessNeed(createdNeed.getId(), createAuth(adminUser)));
    }

    @Test
    void canAccessNeed_creatorCanAccessOwnNeed() {
        when(needRepository.findById(createdNeed.getId())).thenReturn(Optional.of(createdNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(fieldWorkerUser);
        assertTrue(needSecurityService.canAccessNeed(createdNeed.getId(), createAuth(fieldWorkerUser)));
    }

    @Test
    void canAccessNeed_assignedNgoStaffCanAccessAssignedNeed() {
        when(needRepository.findById(assignedNeed.getId())).thenReturn(Optional.of(assignedNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(ngoStaffUser);
        when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.of(verifiedOrg));
        assertTrue(needSecurityService.canAccessNeed(assignedNeed.getId(), createAuth(ngoStaffUser)));
    }

    @Test
    void canAccessNeed_unassignedNgoStaffCannotAccessUnassignedNeed() {
        when(needRepository.findById(createdNeed.getId())).thenReturn(Optional.of(createdNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(otherNgoStaffUser);
        assertFalse(needSecurityService.canAccessNeed(createdNeed.getId(), createAuth(otherNgoStaffUser)));
    }
    
    // --- Tests for canClaimNeed ---

    @Test
    void canClaimNeed_verifiedNgoStaffCanClaimUnclaimedNeed() {
        when(needRepository.findById(createdNeed.getId())).thenReturn(Optional.of(createdNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(ngoStaffUser);
        when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.of(verifiedOrg));

        // Mock service area to match the need
        ServiceArea serviceArea = new ServiceArea();
        serviceArea.setOrganizationId(verifiedOrg.getId());
        serviceArea.setCountry(createdNeed.getCountry());
        Set<NeedCategory> categories = new HashSet<>();
        categories.add(createdNeed.getCategory());
        serviceArea.setServiceCategories(categories);
        when(serviceAreaRepository.findByOrganizationId(verifiedOrg.getId())).thenReturn(Arrays.asList(serviceArea));

        assertTrue(needSecurityService.canClaimNeed(createdNeed.getId(), createAuth(ngoStaffUser)));
    }

    @Test
    void canClaimNeed_unverifiedNgoStaffCannotClaimNeed() {
        when(needRepository.findById(createdNeed.getId())).thenReturn(Optional.of(createdNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(otherNgoStaffUser);
        when(organizationRepository.findById(unverifiedOrg.getId())).thenReturn(Optional.of(unverifiedOrg));
        assertFalse(needSecurityService.canClaimNeed(createdNeed.getId(), createAuth(otherNgoStaffUser)));
    }
    
    @Test
    void canClaimNeed_fieldWorkerCannotClaimNeed() {
        when(needRepository.findById(createdNeed.getId())).thenReturn(Optional.of(createdNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(fieldWorkerUser);
        assertFalse(needSecurityService.canClaimNeed(createdNeed.getId(), createAuth(fieldWorkerUser)));
    }
    
    // --- Tests for canUpdateNeed ---
    
    @Test
    void canUpdateNeed_adminCanUpdateAnyNeed() {
        when(needRepository.findById(createdNeed.getId())).thenReturn(Optional.of(createdNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(adminUser);
        assertTrue(needSecurityService.canUpdateNeed(createdNeed.getId(), createAuth(adminUser)));
    }
    
    @Test
    void canUpdateNeed_assignedNgoStaffCanUpdateAssignedNeed() {
        when(needRepository.findById(assignedNeed.getId())).thenReturn(Optional.of(assignedNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(ngoStaffUser);
        when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.of(verifiedOrg));
        assertTrue(needSecurityService.canUpdateNeed(assignedNeed.getId(), createAuth(ngoStaffUser)));
    }
    
    @Test
    void canUpdateNeed_unassignedNgoStaffCannotUpdateUnassignedNeed() {
        when(needRepository.findById(createdNeed.getId())).thenReturn(Optional.of(createdNeed));
        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(otherNgoStaffUser);
        assertFalse(needSecurityService.canUpdateNeed(createdNeed.getId(), createAuth(otherNgoStaffUser)));
    }
}
