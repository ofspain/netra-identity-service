package com.netra.authrex.services;

import com.netra.authrex.configs.JwtConfig;
import com.netra.authrex.dtos.AuthResponse;
import com.netra.authrex.dtos.AuthUser;
import com.netra.authrex.dtos.TokenExchangeAuthRequest;
import com.netra.authrex.exceptions.TokenExchangeException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.nimbusds.oauth2.sdk.GrantType.TOKEN_EXCHANGE;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenExchangeService {

    private final JwtConfig jwtTokenUtil;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthResponse exchangeToken(TokenExchangeAuthRequest request,
                                      String currentClientId) {
        // 1. Validate incoming subject token
        Claims subjectClaims = jwtTokenUtil.parseToken(request.getSubjectToken());

        // 2. Check if token is exchangeable
        if (!Boolean.TRUE.equals(subjectClaims.get("exchangeable", Boolean.class))) {
            throw new TokenExchangeException("Token is not exchangeable");
        }

        // 3. Validate requesting client is authorized
        String authorizedClient = subjectClaims.get("authorized_client", String.class);
        if (!currentClientId.equals(authorizedClient)) {
            throw new TokenExchangeException("Client not authorized for token exchange");
        }

        // 4. Get user identity from subject token
        String username = subjectClaims.getSubject();
        AuthUser authUser = (AuthUser) customUserDetailsService.loadUserByUsername(username);

        // 5. Generate new delegated token with additional claims
        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put("token_exchange", true);
        additionalClaims.put("original_sub", username);
        additionalClaims.put("exchanged_for", request.getAudience());
        additionalClaims.put("exchanged_scopes", request.getScope());
        additionalClaims.put("exchanged_by", currentClientId);
        additionalClaims.put("is_delegated_token", true);
        additionalClaims.put("acting_on_behalf_of", username);
        additionalClaims.put("grant_type", TOKEN_EXCHANGE);

        String newToken = jwtTokenUtil.generateToken(additionalClaims, authUser);

        // 6. Create response
        Date expiration = jwtTokenUtil.extractExpiration(newToken);
        long expiresIn = (expiration.getTime() - System.currentTimeMillis()) / 1000;

        return AuthResponse.forTokenExchange(newToken, expiresIn, request.getScope());
    }
}