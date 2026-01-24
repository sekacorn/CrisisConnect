package org.crisisconnect.dto;

import lombok.Data;
import org.crisisconnect.model.enums.NeedCategory;
import org.crisisconnect.model.enums.NeedStatus;
import org.crisisconnect.model.enums.UrgencyLevel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full need response - only for authorized users with verified organizations.
 * Includes all need details and sensitive info.
 */
@Data
public class FullNeedResponse {
    private UUID id;
    private UUID createdByUserId;
    private NeedStatus status;
    private NeedCategory category;
    private String description;
    private String country;
    private String regionOrState;
    private String city;
    private UrgencyLevel urgencyLevel;
    private UUID assignedOrganizationId;
    private LocalDateTime assignedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt; // Added based on Need entity update

    // Sensitive info (only if authorized and decrypted)
    private String fullName;
    private String phone;
    private String email;
    private String exactLocation;
    private String notes;
}
