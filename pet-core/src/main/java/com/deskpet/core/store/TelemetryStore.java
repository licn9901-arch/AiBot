package com.deskpet.core.store;

import com.deskpet.core.model.TelemetryLatest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TelemetryStore {
    private final ConcurrentHashMap<String, TelemetryLatest> latest = new ConcurrentHashMap<>();

    public TelemetryLatest upsert(String deviceId, Map<String, Object> telemetry) {
        TelemetryLatest updated = new TelemetryLatest(deviceId, telemetry, Instant.now());
        latest.put(deviceId, updated);
        return updated;
    }

    public Optional<TelemetryLatest> findByDeviceId(String deviceId) {
        return Optional.ofNullable(latest.get(deviceId));
    }
}
