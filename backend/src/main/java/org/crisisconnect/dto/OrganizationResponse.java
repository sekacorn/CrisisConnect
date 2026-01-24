package org.crisisconnect.dto;

import lombok.Data;
import org.crisisconnect.model.enums.OrganizationStatus;
import org.crisisconnect.model.enums.OrganizationType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OrganizationResponse {
    private UUID id;
    private String name;
    private OrganizationType type;
    private String country;
    private OrganizationStatus status;
    private String websiteUrl;
    private String phone;
    private LocalDateTime createdAt;
}
