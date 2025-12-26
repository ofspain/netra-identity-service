package com.netra.authrex.dtos;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientRegistrationRequest {

    @NotBlank(message = "Client secret is required")
    @Size(min = 8, max = 100, message = "Client secret must be between 8 and 100 characters")
    private String clientSecret;

    @NotBlank(message = "Client name is required")
    @Size(min = 2, max = 255, message = "Client name must be between 2 and 255 characters")
    private String clientName;

    @NotBlank(message = "Client code is required")
    @Size(min = 3, max = 10, message = "Client code must be between 3 and 10 characters")
    private String clientCode;

    @NotEmpty(message = "At least one scope is required")
    private Set<@NotBlank String> scopes;

    @NotEmpty(message = "At least one grant type is required")
    private Set<@NotBlank String> authorizedGrantTypes;

    @Builder.Default
    private Set<@URL(message = "Redirect URI must be a valid URL") String> redirectUris = Set.of();

    @Builder.Default
    @Min(value = 60, message = "Access token validity must be at least 60 seconds")
    @Max(value = 86400, message = "Access token validity cannot exceed 86400 seconds (24 hours)")
    private Long accessTokenValidity = 3600L; // Default: 1 hour

    @Builder.Default
    @Min(value = 86400, message = "Refresh token validity must be at least 86400 seconds (24 hours)")
    @Max(value = 2592000, message = "Refresh token validity cannot exceed 2592000 seconds (30 days)")
    private Long refreshTokenValidity = 2592000L; // Default: 30 days

    @Builder.Default
    private boolean autoApprove = false;

    @Builder.Default
    private Set<String> autoApproveScopes = Set.of();

    @Builder.Default
    private String description = "";

    @Email(message = "Contact email must be valid")
    private String contactEmail;

    @URL(message = "Client website must be a valid URL")
    private String clientWebsite;

    @Builder.Default
    private String logoUri = "";

    @Builder.Default
    private String policyUri = "";

    @Builder.Default
    private String tosUri = "";
}
