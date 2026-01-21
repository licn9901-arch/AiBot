package com.deskpet.core.service;

import com.deskpet.core.model.TelemetryHistory;
import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.repository.TelemetryHistoryRepository;
import com.deskpet.core.repository.TelemetryLatestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelemetryService {
    private final TelemetryLatestRepository telemetryLatestRepository;
    private final TelemetryHistoryRepository telemetryHistoryRepository;

    @Transactional
    public TelemetryLatest updateLatest(String deviceId, Map<String, Object> telemetry) {
        Instant now = Instant.now();
        TelemetryLatest latest = telemetryLatestRepository.save(new TelemetryLatest(deviceId, telemetry, now));
        telemetryHistoryRepository.save(new TelemetryHistory(deviceId, telemetry, now));
        return latest;
    }
}
