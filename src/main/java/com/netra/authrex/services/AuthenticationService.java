package com.netra.authrex.services;


import com.netra.authrex.configs.JwtConfig;
import com.netra.authrex.daos.IdentityDao;
import com.netra.authrex.dtos.AuthRequest;
import com.netra.authrex.dtos.AuthResponse;
import com.netra.authrex.dtos.AuthUser;
import com.netra.authrex.exceptions.AuthenticationException;
import com.netra.authrex.exceptions.TokenRefreshException;
import com.netra.commons.models.Identity;
import com.netra.commons.models.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final IdentityService identityService;
    private final IdentityDao identityDao;
    private final JwtConfig jwtTokenUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.refresh.expiration:7}")
    private Long refreshTokenExpirationDays;


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

            // Load user details from authentication
           // UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            AuthUser authUser = (AuthUser) authentication.getPrincipal();
            Identity identity = authUser.getIdentity();

            // Generate JWT token
            String token = jwtTokenUtil.generateToken(authUser);

            // Get token expiration details
            Date expiration = jwtTokenUtil.extractExpiration(token);
            long expiresIn = (expiration.getTime() - System.currentTimeMillis()) / 1000;

//
//            // Update last login timestamp
//            Identity identity = identityDao.findByUsername(request.username())
//                    .orElseThrow(() -> new AuthenticationException("User not found"));
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(identity.getId());


            identityDao.updateLastLogin(identity.getId());

            return new AuthResponse(
                    token,                     // access_token
                    "Bearer",                 // token_type
                    expiresIn,                // expires_in (in seconds)
                    LocalDateTime.now(),       // issued_at
                    refreshToken.getToken()    // refresh_token (optional)
            );

        } catch (LockedException e) {
            throw new AuthenticationException("Account is locked");
        } catch (DisabledException e) {
            throw new AuthenticationException("Account is disabled");
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid username or password");
        } catch (Exception e) {
            throw new AuthenticationException("Authentication failed");
        }
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        String username = jwtTokenUtil.extractUsername(token);
        UserDetails userDetails = identityService.loadUserByUsername(username);
        return jwtTokenUtil.isTokenValid(token, userDetails);
    }


    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));

        // Verify the refresh token is still valid
        refreshTokenService.verifyExpiration(token);

        // Get the identity
        Identity identity = token.getIdentity();
        UserDetails userDetails = identityService.loadUserByUsername(identity.getUsername());

        // Generate new access token
        String newAccessToken = jwtTokenUtil.generateToken((AuthUser) userDetails);
        Date expiration = jwtTokenUtil.extractExpiration(newAccessToken);
        long expiresIn = (expiration.getTime() - System.currentTimeMillis()) / 1000;

        // Optionally create a new refresh token (rotate refresh tokens)
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(identity.getId());

        return new AuthResponse(newAccessToken, expiresIn, newRefreshToken.getToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    public String retrievePublicKey(){
        Resource resource = jwtTokenUtil.getPublicKeyResource();

        try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }catch (Exception ex){
            throw new RuntimeException("can not retrieve public key at the moment");
        }
    }
}