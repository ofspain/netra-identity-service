package com.netra.authrex.services;


import com.netra.authrex.configs.JwtConfig;
import com.netra.authrex.daos.IdentityDao;
import com.netra.authrex.dtos.AuthRequest;
import com.netra.authrex.dtos.AuthResponse;
import com.netra.authrex.exceptions.AuthenticationException;
import com.netra.commons.models.Identity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final IdentityService identityService;
    private final IdentityDao identityDao;
    private final JwtConfig jwtTokenUtil;

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        try {
            // Authenticate using Spring Security's AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Load user details
            UserDetails userDetails = identityService.loadUserByUsername(request.username());

            // Generate JWT token
            String token = jwtTokenUtil.generateToken(userDetails);

            // Get token expiration details
            Date expiration = jwtTokenUtil.extractExpiration(token);
            long expiresIn = (expiration.getTime() - System.currentTimeMillis()) / 1000;

            // Update last login timestamp
            Identity identity = identityDao.findByUsername(request.username())
                    .orElseThrow(() -> new AuthenticationException("User not found"));
            identityDao.updateLastLogin(identity.getId());

            return new AuthResponse(
                    token,                     // access_token
                    "Bearer",                 // token_type
                    expiresIn,                // expires_in (in seconds)
                    LocalDateTime.now(),       // issued_at
                    null                      // refresh_token (optional)
            );

        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid username or password");
        }
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        String username = jwtTokenUtil.extractUsername(token);
        UserDetails userDetails = identityService.loadUserByUsername(username);
        return jwtTokenUtil.isTokenValid(token, userDetails);
    }
}