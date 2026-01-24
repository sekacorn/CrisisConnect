package org.crisisconnect.service;

import org.crisisconnect.dto.CreateNeedRequest;
import org.crisisconnect.dto.FullNeedResponse;
import org.crisisconnect.dto.RedactedNeedResponse;
import org.crisisconnect.model.entity.Need;
import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.model.entity.SensitiveInfo;
import org.crisisconnect.model.entity.User;
import org.crisisconnect.model.enums.*;
import org.crisisconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NeedServiceTest {

    @Mock
    private NeedRepository needRepository;

    @Mock
    private SensitiveInfoRepository sensitiveInfoRepository;

    @Mock
    private NeedUpdateRepository needUpdateRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private NeedService needService;

    private User testUser;
    private Need testNeed;
    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID needId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(UserRole.FIELD_WORKER);
        testUser.setOrganizationId(orgId);
        testUser.setIsActive(true);

        testNeed = new Need();
        testNeed.setId(needId);
        testNeed.setCreatedByUserId(userId);
        // Note: organizationId field doesn't exist on Need entity
        testNeed.setStatus(NeedStatus.NEW);
        testNeed.setCategory(NeedCategory.FOOD);
        // Note: description is stored in SensitiveInfo, not on Need entity
        testNeed.setCountry("TestCountry");
        testNeed.setRegionOrState("TestRegion");
        testNeed.setUrgencyLevel(UrgencyLevel.HIGH);

        testOrganization = new Organization();
        testOrganization.setId(orgId);
        testOrganization.setName("Test NGO");
        testOrganization.setStatus(OrganizationStatus.VERIFIED);
        testOrganization.setType(OrganizationType.NGO);
        testOrganization.setCountry("TestCountry");
    }

    @Test
    void testCreateNeed() {
        CreateNeedRequest request = new CreateNeedRequest();
        request.setCategory(NeedCategory.FOOD);
        request.setDescription("Need food");
        request.setCountry("TestCountry");
        request.setUrgencyLevel(UrgencyLevel.HIGH);
        request.setEncryptedFullName("John Doe");  // Fixed: use encryptedFullName instead of beneficiaryName

        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(needRepository.save(any())).thenReturn(testNeed);
        when(sensitiveInfoRepository.save(any())).thenReturn(new SensitiveInfo());
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");

        FullNeedResponse response = needService.createNeed(request, testUser.getId(), "127.0.0.1");

        assertNotNull(response);
        assertEquals(testNeed.getId(), response.getId());
        verify(needRepository, times(1)).save(any());
        verify(sensitiveInfoRepository, times(1)).save(any());
        verify(auditService, times(1)).logAction(any(), eq("CREATE_NEED"), eq("NEED"), any(), any(), anyString());
    }

    @Test
    void testGetAllNeeds() {
        when(needRepository.findAll()).thenReturn(List.of(testNeed));

        List<RedactedNeedResponse> needs = needService.getAllNeeds(testUser.getId());

        assertNotNull(needs);
        assertEquals(1, needs.size());
        assertEquals(testNeed.getId(), needs.get(0).getId());
        assertEquals(testNeed.getCategory(), needs.get(0).getCategory());
        verify(needRepository, times(1)).findAll();
    }

    @Test
    void testGetNeedByIdAsCreator() {
        when(needRepository.findById(any())).thenReturn(Optional.of(testNeed));
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(sensitiveInfoRepository.findByNeed_Id(any())).thenReturn(Optional.of(new SensitiveInfo()));

        Object response = needService.getNeedById(testNeed.getId(), testUser.getId(), "127.0.0.1");

        assertNotNull(response);
        assertTrue(response instanceof FullNeedResponse);
        verify(auditService, times(1)).logNeedAccess(any(), any(), eq(true), any());
    }

    @Test
    void testGetNeedByIdUnauthorized() {
        User differentUser = new User();
        differentUser.setId(UUID.randomUUID());
        differentUser.setRole(UserRole.FIELD_WORKER);

        when(needRepository.findById(any())).thenReturn(Optional.of(testNeed));
        when(userRepository.findById(any())).thenReturn(Optional.of(differentUser));

        Object response = needService.getNeedById(testNeed.getId(), differentUser.getId(), "127.0.0.1");

        assertNotNull(response);
        assertTrue(response instanceof RedactedNeedResponse);
        verify(auditService, times(1)).logNeedAccess(any(), any(), eq(false), any());
    }

    @Test
    void testGetNeedByIdAsAdmin() {
        testUser.setRole(UserRole.ADMIN);

        when(needRepository.findById(any())).thenReturn(Optional.of(testNeed));
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(sensitiveInfoRepository.findByNeed_Id(any())).thenReturn(Optional.of(new SensitiveInfo()));

        Object response = needService.getNeedById(testNeed.getId(), testUser.getId(), "127.0.0.1");

        assertNotNull(response);
        assertTrue(response instanceof FullNeedResponse);
    }

    @Test
    void testCreateNeedUserNotFound() {
        CreateNeedRequest request = new CreateNeedRequest();
        request.setCategory(NeedCategory.FOOD);
        request.setDescription("Need food");
        request.setCountry("TestCountry");
        request.setUrgencyLevel(UrgencyLevel.HIGH);

        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            needService.createNeed(request, UUID.randomUUID(), "127.0.0.1");
        });
    }
}
