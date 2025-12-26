package com.netra.authrex.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class ClientAuthRequest extends ClientAuthenticatedRequest {

    private String scopes;

    @Override
    public AUTHGRANTTYPE getGrantType() {
        return AUTHGRANTTYPE.CLIENT_CREDENTIALS;
    }
}

