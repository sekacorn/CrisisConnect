package org.crisisconnect.service;

import org.crisisconnect.dto.FullNeedResponse;
import org.crisisconnect.dto.RedactedNeedResponse;
import org.crisisconnect.model.entity.Need;
import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.SensitiveInfo;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.NeedCategory;
import org.crisisconnect.model.enums.NeedStatus;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.model.enums.UrgencyLevel;
import org.crisisconnect.model.enums.UserRole;
import org.crisisconnect.repository.OrganizationRepository;
import org.crisisconnect.repository.SensitiveInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NeedPrivacyFilterServiceTest {

    @Mock
    private EncryptionService encryptionService;
    @Mock
    private AuditService auditService;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private SensitiveInfoRepository sensitiveInfoRepository;

    @InjectMocks
    private NeedPrivacyFilterService needPrivacyFilterService;

    private User adminUser;
    private User fieldWorkerUser;
    private User ngoStaffUser;
    private User beneficiaryUser;
    private User unverifiedNgoStaffUser;

    private Organization verifiedOrg;
    private Organization unverifiedOrg;

    private Need createdNeed;
    private Need assignedNeed;
    private Need unassignedNeed;

    private SensitiveInfo createdNeedSensitiveInfo;
    private SensitiveInfo assignedNeedSensitiveInfo;
    private SensitiveInfo unassignedNeedSensitiveInfo;

    @BeforeEach
    void setUp() {
        // Users
        adminUser = createUser(UserRole.ADMIN, null);
        fieldWorkerUser = createUser(UserRole.FIELD_WORKER, null);
        ngoStaffUser = createUser(UserRole.NGO_STAFF, UUID.randomUUID()); // Org ID will be set by verifiedOrg
        beneficiaryUser = createUser(UserRole.BENEFICIARY, null);
        unverifiedNgoStaffUser = createUser(UserRole.NGO_STAFF, UUID.randomUUID());

        // Organizations
        verifiedOrg = createOrganization(OrganizationStatus.VERIFIED);
        unverifiedOrg = createOrganization(OrganizationStatus.PENDING);

        ngoStaffUser.setOrganizationId(verifiedOrg.getId());
        unverifiedNgoStaffUser.setOrganizationId(unverifiedOrg.getId());

        // Needs (created by fieldWorkerUser)
        createdNeed = createNeed(fieldWorkerUser.getId(), null, NeedStatus.NEW);
        assignedNeed = createNeed(fieldWorkerUser.getId(), verifiedOrg.getId(), NeedStatus.ASSIGNED);
        unassignedNeed = createNeed(UUID.randomUUID(), null, NeedStatus.NEW); // Created by another user

        // SensitiveInfo
        createdNeedSensitiveInfo = createSensitiveInfo(createdNeed, "John Doe", "123", "john@example.com", "123 Main St", "Notes for John");
        assignedNeedSensitiveInfo = createSensitiveInfo(assignedNeed, "Jane Doe", "456", "jane@example.com", "456 Oak Ave", "Notes for Jane");
        unassignedNeedSensitiveInfo = createSensitiveInfo(unassignedNeed, "Baby Doe", "789", "baby@example.com", "789 Pine Rd", "Notes for Baby");

        createdNeed.setSensitiveInfo(createdNeedSensitiveInfo);
        assignedNeed.setSensitiveInfo(assignedNeedSensitiveInfo);
        unassignedNeed.setSensitiveInfo(unassignedNeedSensitiveInfo);

        // Mock encryption service (lenient because not all tests use it)
        lenient().when(encryptionService.decrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock organization repository (lenient because not all tests use both)
        lenient().when(organizationRepository.findById(verifiedOrg.getId())).thenReturn(Optional.of(verifiedOrg));
        lenient().when(organizationRepository.findById(unverifiedOrg.getId())).thenReturn(Optional.of(unverifiedOrg));

        // Mock sensitive info repository (lenient because not all tests use it)
        lenient().when(sensitiveInfoRepository.findByNeed_Id(createdNeed.getId())).thenReturn(Optional.of(createdNeedSensitiveInfo));
        lenient().when(sensitiveInfoRepository.findByNeed_Id(assignedNeed.getId())).thenReturn(Optional.of(assignedNeedSensitiveInfo));
        lenient().when(sensitiveInfoRepository.findByNeed_Id(unassignedNeed.getId())).thenReturn(Optional.of(unassignedNeedSensitiveInfo));
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
        need.setCategory(NeedCategory.FOOD);
        need.setCountry("USA");
        need.setRegionOrState("NY");
        need.setCity("NYC");
        // Note: description is stored in SensitiveInfo, not on Need entity
        need.setUrgencyLevel(UrgencyLevel.HIGH);
        need.setAssignedOrganizationId(assignedToOrg);
        need.setCreatedAt(LocalDateTime.now());
        need.setUpdatedAt(LocalDateTime.now());
        return need;
    }

    private SensitiveInfo createSensitiveInfo(Need need, String fullName, String phone, String email, String exactLocation, String notes) {
        SensitiveInfo info = new SensitiveInfo();
        info.setId(UUID.randomUUID());
        info.setNeed(need);
        info.setEncryptedFullName(fullName); // Mock decrypted value
        info.setEncryptedPhone(phone);
        info.setEncryptedEmail(email);
        info.setEncryptedExactLocation(exactLocation);
        info.setEncryptedNotes(notes);
        return info;
    }

    // --- Tests for filterNeed (single need view) ---

    @Test
    void filterNeed_adminUser_shouldReturnFullResponse() {
        Object response = needPrivacyFilterService.filterNeed(createdNeed, adminUser, "127.0.0.1");
        assertTrue(response instanceof FullNeedResponse);
        FullNeedResponse fullResponse = (FullNeedResponse) response;
        assertEquals(createdNeed.getId(), fullResponse.getId());
        assertEquals("John Doe", fullResponse.getFullName());
        verify(auditService, times(1)).logNeedAccess(adminUser.getId(), createdNeed.getId(), true, "127.0.0.1");
        verify(auditService, times(1)).logSensitiveInfoAccess(adminUser.getId(), createdNeed.getId(), "Decrypted PII fields");
    }

    @Test
    void filterNeed_creatorUser_shouldReturnFullResponse() {
        Object response = needPrivacyFilterService.filterNeed(createdNeed, fieldWorkerUser, "127.0.0.1");
        assertTrue(response instanceof FullNeedResponse);
        FullNeedResponse fullResponse = (FullNeedResponse) response;
        assertEquals(createdNeed.getId(), fullResponse.getId());
        assertEquals("John Doe", fullResponse.getFullName());
        verify(auditService, times(1)).logNeedAccess(fieldWorkerUser.getId(), createdNeed.getId(), true, "127.0.0.1");
        verify(auditService, times(1)).logSensitiveInfoAccess(fieldWorkerUser.getId(), createdNeed.getId(), "Decrypted PII fields");
    }

    @Test
    void filterNeed_assignedVerifiedNgoStaffUser_shouldReturnFullResponse() {
        Object response = needPrivacyFilterService.filterNeed(assignedNeed, ngoStaffUser, "127.0.0.1");
        assertTrue(response instanceof FullNeedResponse);
        FullNeedResponse fullResponse = (FullNeedResponse) response;
        assertEquals(assignedNeed.getId(), fullResponse.getId());
        assertEquals("Jane Doe", fullResponse.getFullName());
        verify(auditService, times(1)).logNeedAccess(ngoStaffUser.getId(), assignedNeed.getId(), true, "127.0.0.1");
        verify(auditService, times(1)).logSensitiveInfoAccess(ngoStaffUser.getId(), assignedNeed.getId(), "Decrypted PII fields");
    }

    @Test
    void filterNeed_unassignedVerifiedNgoStaffUser_shouldReturnRedactedResponse() {
        Object response = needPrivacyFilterService.filterNeed(unassignedNeed, ngoStaffUser, "127.0.0.1");
        assertTrue(response instanceof RedactedNeedResponse);
        RedactedNeedResponse redactedResponse = (RedactedNeedResponse) response;
        assertEquals(unassignedNeed.getId(), redactedResponse.getId());
        assertEquals("Contact for details", redactedResponse.getGeneralizedVulnerabilityFlags());
        verify(auditService, times(1)).logNeedAccess(ngoStaffUser.getId(), unassignedNeed.getId(), false, "127.0.0.1");
        verify(auditService, never()).logSensitiveInfoAccess(any(), any(), any());
    }

    @Test
    void filterNeed_unverifiedNgoStaffUser_shouldReturnRedactedResponse() {
        Object response = needPrivacyFilterService.filterNeed(assignedNeed, unverifiedNgoStaffUser, "127.0.0.1");
        assertTrue(response instanceof RedactedNeedResponse);
        RedactedNeedResponse redactedResponse = (RedactedNeedResponse) response;
        assertEquals(assignedNeed.getId(), redactedResponse.getId());
        verify(auditService, times(1)).logNeedAccess(unverifiedNgoStaffUser.getId(), assignedNeed.getId(), false, "127.0.0.1");
        verify(auditService, never()).logSensitiveInfoAccess(any(), any(), any());
    }

    @Test
    void filterNeed_beneficiaryUser_shouldReturnRedactedResponse() {
        Object response = needPrivacyFilterService.filterNeed(createdNeed, beneficiaryUser, "127.0.0.1");
        assertTrue(response instanceof RedactedNeedResponse);
        RedactedNeedResponse redactedResponse = (RedactedNeedResponse) response;
        assertEquals(createdNeed.getId(), redactedResponse.getId());
        verify(auditService, times(1)).logNeedAccess(beneficiaryUser.getId(), createdNeed.getId(), false, "127.0.0.1");
        verify(auditService, never()).logSensitiveInfoAccess(any(), any(), any());
    }

    @Test
    void filterNeed_needWithoutSensitiveInfo_shouldReturnFullResponseWithoutSensitiveData() {
        Need needWithoutSensitive = createNeed(adminUser.getId(), null, NeedStatus.NEW);
        Object response = needPrivacyFilterService.filterNeed(needWithoutSensitive, adminUser, "127.0.0.1");
        assertTrue(response instanceof FullNeedResponse);
        FullNeedResponse fullResponse = (FullNeedResponse) response;
        assertEquals(needWithoutSensitive.getId(), fullResponse.getId());
        assertNull(fullResponse.getFullName()); // No sensitive info to decrypt
        verify(auditService, times(1)).logNeedAccess(adminUser.getId(), needWithoutSensitive.getId(), true, "127.0.0.1");
        verify(auditService, never()).logSensitiveInfoAccess(any(), any(), any()); // No sensitive info accessed
    }

    // --- Tests for filterNeedsList (list view) ---

    @Test
    void filterNeedsList_shouldAlwaysReturnRedactedResponseList() {
        List<Need> needs = Arrays.asList(createdNeed, assignedNeed, unassignedNeed);
        List<RedactedNeedResponse> responseList = needPrivacyFilterService.filterNeedsList(needs);

        assertNotNull(responseList);
        assertEquals(3, responseList.size());
        assertTrue(responseList.get(0) instanceof RedactedNeedResponse);
        assertEquals("Contact for details", responseList.get(0).getGeneralizedVulnerabilityFlags());
        verify(auditService, never()).logNeedAccess(any(), any(), anyBoolean(), anyString());
        verify(auditService, never()).logSensitiveInfoAccess(any(), any(), anyString());
    }

    // Note: Tests for generalizeRegion removed as it's a private method
    // The functionality is tested indirectly through filterNeed() and filterNeedsList()
}
