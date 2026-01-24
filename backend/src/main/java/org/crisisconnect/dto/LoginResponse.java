package org.crisisconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private UUID userId;
    private String email;
    private String name;
    private String role;
    private UUID organizationId;
    private boolean mfaRequired = false;

    public LoginResponse(String token, UUID userId, String email, String name, String role, UUID organizationId) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
        this.organizationId = organizationId;
        this.mfaRequired = false;
    }

    // Constructor for MFA required response
    public static LoginResponse mfaRequired(UUID userId, String email) {
        LoginResponse response = new LoginResponse();
        response.setMfaRequired(true);
        response.setUserId(userId);
        response.setEmail(email);
        return response;
    }
}
