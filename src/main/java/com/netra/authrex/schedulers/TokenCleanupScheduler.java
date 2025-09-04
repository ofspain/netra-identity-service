package com.netra.authrex.schedulers;

import com.netra.authrex.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "${spring.scheduler.cron.expired-token-cleanup:0 0 2 * * ?}")
    public void cleanUpExpiredTokens() {
        refreshTokenService.deleteExpiredTokens();
    }
}
