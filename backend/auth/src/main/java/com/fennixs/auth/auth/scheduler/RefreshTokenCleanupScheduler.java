package com.fennixs.auth.auth.scheduler;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fennixs.auth.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "${app.timezone}")
    @Transactional
    public void deleteExpiredTokens() {
        try {
            refreshTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
            log.info("Expired refresh token cleanup completed");
        } catch (Exception e) {
            log.error("Expired refresh token cleanup failed", e);
        }
    }
}
