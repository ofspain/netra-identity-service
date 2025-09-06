package com.netra.authrex.configs;

import com.netra.authrex.configs.JwtConfig;
import com.netra.authrex.dtos.AuthUser;
import com.netra.authrex.services.IdentityService;
import com.netra.commons.models.Identity;
import com.netra.commons.util.BasicUtil;
import com.netra.commons.util.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final IdentityService userDetailsService;

//    public JwtAuthFilter(JwtConfig jwtConfig, IdentityService userDetailsService) {
//        this.jwtConfig = jwtConfig;
//        this.userDetailsService = userDetailsService;
//    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader(Constants.REQUEST_AUTH_BEARER_KEY);
        final String jwt;
        final String username;
        String authXFactor = request.getHeader(Constants.REQUEST_AUTH_DOMAIN_X_KEY);


        //ensure user default to the least possible privilege domain
        if(!BasicUtil.validString(authXFactor)){
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
            // Extract username from JWT token
            username = jwtConfig.extractUsername(jwt);

            // If username exists and no existing authentication in SecurityContext
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user details from database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                AuthUser authUser = (AuthUser) userDetails;

                Boolean validToken = jwtConfig.isTokenValid(jwt, userDetails);
                Boolean verifiedDomainCompatibility = jwtConfig.validatePlatformDomainConformity(jwt, authUser.getIdentity(), authXFactor);


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
                }
            }else{
                //todo: thrown a platform specific exception here
                throw new BadCredentialsException("JWT token domain mismatch or invalid identity");

            }
        } catch (Exception e) {
            // Log the error if needed
            logger.error("Cannot set user authentication: {}", e);

            // Optionally send error response
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
            return;
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}
