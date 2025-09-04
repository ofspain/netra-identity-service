package com.netra.authrex.services;

import com.netra.authrex.daos.RefreshTokenDao;
import com.netra.authrex.exceptions.TokenRefreshException;
import com.netra.commons.models.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenDao refreshTokenDao;
    private final IdentityService identityService;

    @Value("${jwt.refresh.expiration:7}")
    private Long refreshTokenExpirationDays;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenDao.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long identityId) {
        // Revoke existing tokens for this user (optional - for better security)
        refreshTokenDao.revokeAllTokensForIdentity(identityId);

        return refreshTokenDao.createRefreshToken(identityId, refreshTokenExpirationDays);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenDao.revokeToken(token.getToken());
            throw new TokenRefreshException("Refresh token was expired");
        }
        return token;
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenDao.revokeToken(token);
    }

    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenDao.deleteExpiredTokens();
    }
}
