package org.crisisconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.crisisconnect.model.enums.NeedCategory;
import org.crisisconnect.model.enums.UrgencyLevel;

@Data
public class CreateNeedRequest {
    @NotNull(message = "Category is required")
    private NeedCategory category;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Country is required")
    private String country;

    private String regionOrState;
    private String city;

    @NotNull(message = "Urgency level is required")
    private UrgencyLevel urgencyLevel;

    // Sensitive info fields to be encrypted
    private String encryptedFullName;
    private String encryptedPhone;
    private String encryptedEmail;
    private String encryptedExactLocation;
    private String encryptedNotes;
}
