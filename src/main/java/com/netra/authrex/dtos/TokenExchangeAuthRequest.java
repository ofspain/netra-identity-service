package com.netra.authrex.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
public class TokenExchangeAuthRequest extends ClientAuthenticatedRequest {

    @NotBlank
    private String subjectToken;
    private Set<String> audience;
    private String scope;


    @Override
    public AUTHGRANTTYPE getGrantType() {
        return AUTHGRANTTYPE.TOKEN_EXCHANGE;
    }
}

