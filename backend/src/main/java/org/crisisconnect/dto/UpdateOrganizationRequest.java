package org.crisisconnect.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.crisisconnect.model.enums.OrganizationStatus;

/**
 * Request to update organization status (admin only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationRequest {
    @NotNull
    private OrganizationStatus status;

    private String verificationNotes;
}
