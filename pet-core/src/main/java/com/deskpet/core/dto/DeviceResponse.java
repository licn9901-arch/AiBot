package com.deskpet.core.dto;

import com.deskpet.core.model.Device;
import com.deskpet.core.model.DeviceSession;
import com.deskpet.core.model.TelemetryLatest;

import java.time.Instant;
import java.util.Map;

public record DeviceResponse(
        String deviceId,
        String model,
        String remark,
        Instant createdAt,
        boolean online,
        Instant lastSeen,
        Map<String, Object> telemetry
) {
    public static DeviceResponse of(Device device, DeviceSession session, TelemetryLatest telemetry) {
        boolean online = session != null && session.online();
        Instant lastSeen = session == null ? null : session.lastSeen();
        Map<String, Object> latest = telemetry == null ? null : telemetry.telemetry();
        return new DeviceResponse(device.deviceId(), device.model(), device.remark(), device.createdAt(), online, lastSeen, latest);
    }
}
