package com.netra.authrex.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.time.LocalDateTime;

@JsonPropertyOrder({"access_token", "token_type", "expires_in", "issued_at"})
public record AuthResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("issued_at") LocalDateTime issuedAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("refresh_token") String refreshToken

) {
    public AuthResponse(String accessToken, long expiresIn, String refreshToken) {
        this(accessToken, "Bearer", expiresIn, LocalDateTime.now(), refreshToken);
    }
    public AuthResponse(String accessToken, long expiresIn) {
        this(accessToken, "Bearer", expiresIn, LocalDateTime.now(), null);
    }
}
