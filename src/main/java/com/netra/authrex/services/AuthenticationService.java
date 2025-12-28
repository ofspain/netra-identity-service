package com.netra.authrex.services;

import com.netra.authrex.configs.JwtConfig;
import com.netra.authrex.dtos.*;
import com.netra.authrex.exceptions.AuthenticationException;
import com.netra.authrex.exceptions.TokenExchangeException;
import com.netra.authrex.exceptions.TokenRefreshException;
import com.netra.authrex.model.ClientRegistration;
import com.netra.commons.enums.DomainType;
import com.netra.commons.models.Identity;
import com.netra.commons.models.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.netra.authrex.dtos.AuthRequest.AUTHGRANTTYPE.CLIENT_CREDENTIALS;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final IdentityService identityService;
    private final JwtConfig jwtTokenUtil;
    private final RefreshTokenService refreshTokenService;
    private final ClientRegistrationService clientService;
    private final TokenExchangeService tokenExchangeService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh.expiration:7}")
    private Long refreshTokenExpirationDays;

    // ========== SINGLE ENTRY POINT ==========

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        log.info("Authentication request with grant_type: {}", request.getGrantType());

        return switch (request.getGrantType()) {
            case PASSWORD -> authenticatePassword((PasswordAuthRequest) request);
            case CLIENT_CREDENTIALS -> authenticateClient((ClientAuthRequest) request);
            case TOKEN_EXCHANGE -> authenticateTokenExchange((TokenExchangeAuthRequest) request);
            default -> throw new AuthenticationException(
                    "Unsupported grant_type: " + request.getGrantType());
        };
    }

    // ========== PASSWORD GRANT (Your existing flow) ==========

    private AuthResponse authenticatePassword(PasswordAuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            AuthUser authUser = (AuthUser) authentication.getPrincipal();
            Identity identity = authUser.getIdentity();

            // Build claims with optional exchange capability
            Map<String, Object> additionalClaims = new HashMap<>();
            additionalClaims.put("is_service_principal", false);
            additionalClaims.put("user_id", identity.getId());
            additionalClaims.put("grant_type", AuthRequest.AUTHGRANTTYPE.PASSWORD);


            String token = jwtTokenUtil.generateToken(additionalClaims, authUser);
            Date expiration = jwtTokenUtil.extractExpiration(token);
            long expiresIn = (expiration.getTime() - System.currentTimeMillis()) / 1000;

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(identity.getId());
            identityService.updateLastLogin(identity.getId());

            return AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn)
                    .issuedAt(LocalDateTime.now())
                    .refreshToken(refreshToken.getToken())
                    .build();

        } catch (LockedException e) {
            throw new AuthenticationException("Account is locked");
        } catch (DisabledException e) {
            throw new AuthenticationException("Account is disabled");
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid username or password");
        } catch (Exception e) {
            log.error("Password authentication failed", e);
            throw new AuthenticationException("Authentication failed");
        }
    }

    // ========== CLIENT CREDENTIALS GRANT ==========

    private AuthResponse authenticateClient(ClientAuthRequest request) {
        try {
            // 1. Validate client credentials
            ClientRegistration client = clientService.validateClient(
                    request.getClientId(),
                    request.getClientSecret()
            );

            // 3. Validate requested scopes
            Set<String> requestedScopes = parseScopes(request.getScopes());
            Set<String> clientScopes = client.getScopes();

            if (!requestedScopes.isEmpty() && !clientScopes.containsAll(requestedScopes)) {
                throw new AuthenticationException(
                        "Invalid scope requested. Client scopes: " + String.join(", ", clientScopes));
            }

            // 4. Build service principal claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("client_id", client.getClientId());
            claims.put("grant_type", CLIENT_CREDENTIALS);
            claims.put("is_service_principal", true);
            claims.put("client_name", client.getClientName());
            claims.put("client_permission", client.getAuthorizedPermissions());

            if (!requestedScopes.isEmpty()) {
                claims.put("scope", String.join(" ", requestedScopes));
            }

            Identity identity = new Identity();
            identity.setIdentityUuid(client.getClientId().toString());
            identity.setUsername(client.getClientName());
            identity.setPassword(client.getClientSecret());
            identity.setDisabled(!client.isEnabled());
            identity.setDomainCode(client.getClientCode());
            identity.setDomainType(DomainType.SYSTEM);



            // 5. Create AuthUser-like object for service
            AuthUser servicePrincipal = new AuthUser(identity);

            // 6. Generate token
            String token = jwtTokenUtil.generateToken(claims, servicePrincipal);
            Date expiration = jwtTokenUtil.extractExpiration(token);
            long expiresIn = (expiration.getTime() - System.currentTimeMillis()) / 1000;

            return AuthResponse.forClientCredentials(token, expiresIn, request.getScopes());

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Client credentials authentication failed", e);
            throw new AuthenticationException("Client authentication failed");
        }
    }

    // ========== TOKEN EXCHANGE GRANT ==========

    private AuthResponse authenticateTokenExchange(TokenExchangeAuthRequest request) {
        try {
            // 1. Validate client credentials
            ClientRegistration client = clientService.validateClient(
                    request.getClientId(),
                    request.getClientSecret()
            );

            // 3. Create token exchange request for the service
            TokenExchangeAuthRequest exchangeRequest = new TokenExchangeAuthRequest();

            exchangeRequest.setSubjectToken(request.getSubjectToken());
            exchangeRequest.setAudience(request.getAudience());
            exchangeRequest.setScope(request.getScope());


            // 4. Exchange the token
            return tokenExchangeService.exchangeToken(exchangeRequest, client.getClientId().toString());

        } catch (TokenExchangeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token exchange failed", e);
            throw new AuthenticationException("Token exchange failed");
        }
    }

    // ========== HELPER METHODS ==========

    private Set<String> parseScopes(String scopeString) {
        if (scopeString == null || scopeString.trim().isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(scopeString.split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    // ========== EXISTING METHODS (KEEP AS IS) ==========

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        String username = jwtTokenUtil.extractUsername(token);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        return jwtTokenUtil.isTokenValid(token, userDetails);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));

        refreshTokenService.verifyExpiration(token);

        Identity identity = token.getIdentity();
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(identity.getUsername());

        String newAccessToken = jwtTokenUtil.generateToken((AuthUser) userDetails);
        Date expiration = jwtTokenUtil.extractExpiration(newAccessToken);
        long expiresIn = (expiration.getTime() - System.currentTimeMillis()) / 1000;

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(identity.getId());

        return new AuthResponse(newAccessToken, expiresIn, newRefreshToken.getToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    public String retrievePublicKey() {
        return jwtTokenUtil.retrievePublicKey();
    }
}