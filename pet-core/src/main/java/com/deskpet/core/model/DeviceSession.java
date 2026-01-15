package com.deskpet.core.model;

import java.time.Instant;

public record DeviceSession(
        String deviceId,
        boolean online,
        String gatewayInstanceId,
        String ip,
        Instant lastSeen
) {
}
