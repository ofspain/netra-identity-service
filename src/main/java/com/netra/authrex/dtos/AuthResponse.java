package com.netra.authrex.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@JsonPropertyOrder({"access_token", "token_type", "expires_in", "issued_at"})
@Builder
public record AuthResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("issued_at") LocalDateTime issuedAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("refresh_token") String refreshToken,


        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("scopes") String scopes

) {
    public AuthResponse(String accessToken, long expiresIn, String refreshToken) {
        this(accessToken, "Bearer", expiresIn, LocalDateTime.now(), refreshToken, null);
    }
    public AuthResponse(String accessToken, long expiresIn) {
        this(accessToken, "Bearer", expiresIn, LocalDateTime.now(), null, null);
    }

    public static AuthResponse forClientCredentials(String accessToken, long expiresIn, String requestedScopes) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, LocalDateTime.now(), null, requestedScopes);
    }

    public static AuthResponse forTokenExchange(String accessToken, long expiresIn, String scopes){
        return forClientCredentials(accessToken, expiresIn, scopes);
    }
}
