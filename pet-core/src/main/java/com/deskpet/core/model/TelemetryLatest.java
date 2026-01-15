package com.deskpet.core.model;

import java.time.Instant;
import java.util.Map;

public record TelemetryLatest(
        String deviceId,
        Map<String, Object> telemetry,
        Instant updatedAt
) {
}
