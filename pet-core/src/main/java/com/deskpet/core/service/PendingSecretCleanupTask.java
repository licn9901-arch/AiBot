package com.deskpet.core.service;

import com.deskpet.core.repository.PendingDeviceSecretRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 定时清理过期的暂存密钥（超过24小时未确认下载的记录）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingSecretCleanupTask {

    private final PendingDeviceSecretRepository pendingDeviceSecretRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredSecrets() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        long countBefore = pendingDeviceSecretRepository.count();
        pendingDeviceSecretRepository.deleteByCreatedAtBefore(cutoff);
        long countAfter = pendingDeviceSecretRepository.count();
        long deleted = countBefore - countAfter;
        if (deleted > 0) {
            log.info("Cleaned up {} expired pending device secrets (older than 24h)", deleted);
        }
    }
}
