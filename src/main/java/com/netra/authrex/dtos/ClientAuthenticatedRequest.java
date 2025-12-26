package com.netra.authrex.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
public abstract class ClientAuthenticatedRequest extends AuthRequest {

    @NotBlank
    protected String clientId;

    @NotBlank
    protected String clientSecret;
}

