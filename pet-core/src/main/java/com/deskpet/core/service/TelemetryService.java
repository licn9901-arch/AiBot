package com.deskpet.core.service;

import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.store.TelemetryStore;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TelemetryService {
    private final TelemetryStore telemetryStore;

    public TelemetryService(TelemetryStore telemetryStore) {
        this.telemetryStore = telemetryStore;
    }

    public TelemetryLatest updateLatest(String deviceId, Map<String, Object> telemetry) {
        return telemetryStore.upsert(deviceId, telemetry);
    }
}
