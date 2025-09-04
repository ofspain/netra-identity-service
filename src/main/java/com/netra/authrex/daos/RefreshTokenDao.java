package com.netra.authrex.daos;

import com.netra.commons.database.EnhancedBeanPropertyRowMapper;
import com.netra.commons.models.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenDao {

    private final JdbcClient jdbcClient;

    public Optional<RefreshToken> findByToken(String token) {
        return jdbcClient.sql("""
                SELECT * FROM refresh_tokens 
                WHERE token = ? AND revoked = false
                """)
                .param(1, token)
                .query(new EnhancedBeanPropertyRowMapper<RefreshToken>())
                .optional();
    }

    public RefreshToken createRefreshToken(Long identityId, long expirationDays) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(expirationDays);

        Long id = jdbcClient.sql("""
                INSERT INTO refresh_tokens (token, identity_id, expires_at)
                VALUES (?, ?, ?)
                RETURNING id
                """)
                .param(1, token)
                .param(2, identityId)
                .param(3, expiresAt)
                .query(Long.class)
                .single();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(id);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(expiresAt);
        // You might want to fetch the full identity if needed

        return refreshToken;
    }

    public void revokeToken(String token) {
        jdbcClient.sql("""
                UPDATE refresh_tokens 
                SET revoked = true, updated_at = NOW()
                WHERE token = ?
                """)
                .param(1, token)
                .update();
    }

    public void revokeAllTokensForIdentity(Long identityId) {
        jdbcClient.sql("""
                UPDATE refresh_tokens 
                SET revoked = true, updated_at = NOW()
                WHERE identity_id = ?
                """)
                .param(1, identityId)
                .update();
    }

    public void deleteExpiredTokens() {
        jdbcClient.sql("""
                DELETE FROM refresh_tokens 
                WHERE expires_at < NOW() OR revoked = true
                """)
                .update();
    }
}