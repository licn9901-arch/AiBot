package com.deskpet.core.dto;

import com.deskpet.core.model.Device;
import com.deskpet.core.model.DeviceSession;
import com.deskpet.core.model.TelemetryLatest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record DeviceResponse(
        @Schema(description = "设备ID", example = "pet001")
        String deviceId,
        @Schema(description = "设备型号", example = "deskpet-v0.1")
        String model,
        @Schema(description = "产品标识", example = "deskpet-v1")
        String productKey,
        @Schema(description = "备注", example = "桌面测试设备")
        String remark,
        @Schema(description = "注册时间", example = "2026-01-21T10:30:00Z")
        Instant createdAt,
        @Schema(description = "是否在线", example = "true")
        boolean online,
        @Schema(description = "最近心跳时间", example = "2026-01-21T10:31:00Z")
        Instant lastSeen,
        @Schema(description = "最新遥测", example = "{\"battery\":0.87,\"rssi\":-55,\"firmwareVersion\":\"0.1.0\"}")
        Map<String, Object> telemetry
) {
    public static DeviceResponse of(Device device, DeviceSession session, TelemetryLatest telemetry) {
        boolean online = session != null && session.online();
        Instant lastSeen = session == null ? null : session.lastSeen();
        Map<String, Object> latest = telemetry == null ? null : telemetry.telemetry();
        return new DeviceResponse(device.deviceId(), device.model(), device.productKey(), device.remark(), device.createdAt(), online, lastSeen, latest);
    }
}
