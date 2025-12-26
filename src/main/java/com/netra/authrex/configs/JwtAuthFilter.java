package com.netra.authrex.configs;

import com.netra.authrex.dtos.AuthUser;
import com.netra.authrex.services.CustomUserDetailsService;
import com.netra.authrex.services.IdentityService;
import com.netra.commons.util.BasicUtil;
import com.netra.commons.models.Identity;
import com.netra.commons.util.Constants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        String authXFactor = request.getHeader(Constants.REQUEST_AUTH_DOMAIN_X_KEY);

        // Ensure user defaults to the least possible privilege domain
        if (!BasicUtil.validString(authXFactor)) {
            authXFactor = Identity.CUSTOMERUSER_DOMAINCODE;
        }

        // Skip filter if no Authorization header or doesn't start with "Bearer "
        if (!BasicUtil.validString(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token from header
        jwt = authHeader.substring(7);

        try {
            // Parse JWT claims
            Claims claims = jwtConfig.parseToken(jwt);

            // Check if this is a service principal token
            Boolean isServicePrincipal = claims.get("is_service_principal", Boolean.class);

            if (Boolean.TRUE.equals(isServicePrincipal)) {
                // ========== SERVICE PRINCIPAL FLOW ==========
                handleServicePrincipal(claims, request, response, filterChain);
            } else {
                // ========== USER FLOW ==========
                handleUserFlow(jwt, claims, authXFactor, request, response, filterChain);
            }

        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
            return;
        }
    }

    // ========== HANDLE SERVICE PRINCIPAL ==========

    private void handleServicePrincipal(Claims claims, HttpServletRequest request,
                                        HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientId = claims.getSubject();
        Boolean isDelegatedToken = claims.get("is_delegated_token", Boolean.class);

        if (Boolean.TRUE.equals(isDelegatedToken)) {
            // ===== DELEGATED TOKEN (Service acting on user's behalf) =====
            handleDelegatedToken(claims, clientId, request, response, filterChain);
        } else {
            // ===== PURE SERVICE TOKEN (Client credentials) =====
            handlePureServiceToken(claims, clientId, request, response, filterChain);
        }
    }

    private void handleDelegatedToken(Claims claims, String clientId,
                                      HttpServletRequest request, HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {

        // Extract delegation information
        String originalUser = claims.get("original_sub", String.class);
        String actingOnBehalfOf = claims.get("acting_on_behalf_of", String.class);
        String exchangedBy = claims.get("exchanged_by", String.class);

        log.info("Delegated token: Client '{}' acting on behalf of user '{}'",
                clientId, originalUser);

        // Validate the token hasn't expired
        if (jwtConfig.isTokenExpired(claims)) {
            throw new BadCredentialsException("Delegated token has expired");
        }

        // Load the original user's details for context (optional)
        UserDetails userDetails = null;
        try {
            userDetails = userDetailsService.loadUserByUsername(originalUser);
        } catch (Exception e) {
            log.warn("Original user '{}' not found, but proceeding with service context", originalUser);
        }

        // Create service principal authorities
        List<GrantedAuthority> authorities = createServiceAuthorities(claims, true);

        // Build authentication token
        UsernamePasswordAuthenticationToken authToken =
                new DelegatedAuthenticationToken(clientId, null, authorities);

        // Set additional details
        authToken.setDetails(createDelegatedAuthenticationDetails(
                claims, userDetails, request, clientId, originalUser, exchangedBy));

        // Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.debug("Service delegation authentication set for client: {}", clientId);
        filterChain.doFilter(request, response);
    }

    private void handlePureServiceToken(Claims claims, String clientId,
                                        HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

        log.info("Service principal token: Client '{}'", clientId);

        // Validate token expiration
        if (jwtConfig.isTokenExpired(claims)) {
            throw new BadCredentialsException("Service token has expired");
        }

        // Validate grant type
        String grantType = claims.get("grant_type", String.class);
        if (!"client_credentials".equals(grantType)) {
            throw new BadCredentialsException("Invalid service token grant type");
        }

        // Create service principal authorities
        List<GrantedAuthority> authorities = createServiceAuthorities(claims, false);

        // Build authentication token
        UsernamePasswordAuthenticationToken authToken =
                new ServicePrincipalAuthenticationToken(clientId, null, authorities);

        // Set additional details
        authToken.setDetails(createServiceAuthenticationDetails(claims, request, clientId));

        // Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.debug("Service principal authentication set for client: {}", clientId);
        filterChain.doFilter(request, response);
    }

    // ========== HANDLE USER FLOW (Your existing logic) ==========

    private void handleUserFlow(String jwt, Claims claims, String authXFactor,
                                HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {

        String username = jwtConfig.extractUsername(jwt);

        // If username exists and no existing authentication in SecurityContext
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load user details from database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            AuthUser authUser = (AuthUser) userDetails;

            Boolean validToken = jwtConfig.isTokenValid(jwt, userDetails);
            Boolean verifiedDomainCompatibility = jwtConfig.validatePlatformDomainConformity(
                    jwt, authUser.getIdentity(), authXFactor);

            // Validate token
            if (validToken && verifiedDomainCompatibility) {
                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Add request details to authentication token
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                throw new BadCredentialsException("JWT token domain mismatch or invalid identity");
            }
        }

        filterChain.doFilter(request, response);
    }

    // ========== HELPER METHODS ==========

    private List<GrantedAuthority> createServiceAuthorities(Claims claims, boolean isDelegated) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Always add service principal role
        authorities.add(new SimpleGrantedAuthority("ROLE_SERVICE"));

        // Add client-specific role
        String clientId = claims.getSubject();
        authorities.add(new SimpleGrantedAuthority("ROLE_CLIENT_" + clientId.toUpperCase()));

        // Add scopes as authorities
        String scopeClaim = claims.get("scope", String.class);
        if (scopeClaim != null) {
            Arrays.stream(scopeClaim.split("\\s+"))
                    .map(scope -> "SCOPE_" + scope.replace(":", "_").toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        // If delegated, add original user's roles (optional)
        if (isDelegated) {
            @SuppressWarnings("unchecked")
            List<String> originalRoles = claims.get("original_roles", List.class);
            if (originalRoles != null) {
                originalRoles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }
        }

        return authorities;
    }

    private Map<String, Object> createDelegatedAuthenticationDetails(
            Claims claims, UserDetails userDetails, HttpServletRequest request,
            String clientId, String originalUser, String exchangedBy) {

        Map<String, Object> details = new HashMap<>();
        details.put("remoteAddress", request.getRemoteAddr());
        details.put("sessionId", request.getSession(false) != null ?
                request.getSession().getId() : null);
        details.put("isDelegated", true);
        details.put("clientId", clientId);
        details.put("originalUser", originalUser);
        details.put("exchangedBy", exchangedBy);
        details.put("actingOnBehalfOf", claims.get("acting_on_behalf_of", String.class));
        details.put("exchangedFor", claims.get("exchanged_for", String.class));

        if (userDetails instanceof AuthUser authUser) {
            details.put("originalUserId", authUser.getIdentity().getId());
        }

        return details;
    }

    private Map<String, Object> createServiceAuthenticationDetails(
            Claims claims, HttpServletRequest request, String clientId) {

        Map<String, Object> details = new HashMap<>();
        details.put("remoteAddress", request.getRemoteAddr());
        details.put("sessionId", request.getSession(false) != null ?
                request.getSession().getId() : null);
        details.put("isServicePrincipal", true);
        details.put("clientId", clientId);
        details.put("clientName", claims.get("client_name", String.class));
        details.put("grantType", claims.get("grant_type", String.class));
        details.put("scopes", claims.get("scope", String.class));

        return details;
    }

    // ========== CUSTOM AUTHENTICATION TOKENS ==========

    private static class ServicePrincipalAuthenticationToken extends UsernamePasswordAuthenticationToken {
        private final String clientId;

        public ServicePrincipalAuthenticationToken(String clientId, Object credentials,
                                                   Collection<? extends GrantedAuthority> authorities) {
            super(clientId, credentials, authorities);
            this.clientId = clientId;
            setAuthenticated(true);
        }

        public String getClientId() {
            return clientId;
        }
    }

    private static class DelegatedAuthenticationToken extends UsernamePasswordAuthenticationToken {
        private final String clientId;

        public DelegatedAuthenticationToken(String clientId, Object credentials,
                                            Collection<? extends GrantedAuthority> authorities) {
            super(clientId, credentials, authorities);
            this.clientId = clientId;
            setAuthenticated(true);
        }

        public String getClientId() {
            return clientId;
        }
    }
}