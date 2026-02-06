package com.deskpet.core.dto;

import com.deskpet.core.model.DeviceEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record DeviceEventResponse(
    @Schema(description = "事件ID", example = "1001")
    Long id,
    @Schema(description = "设备ID", example = "pet001")
    String deviceId,
    @Schema(description = "事件标识", example = "collision")
    String eventId,
    @Schema(description = "事件类型（info/alert/error）", example = "alert")
    String eventType,
    @Schema(description = "事件参数", example = "{\"direction\":\"front\",\"intensity\":75}")
    Map<String, Object> params,
    @Schema(description = "记录时间", example = "2026-01-21T10:35:00Z")
    Instant createdAt
) {
    public static DeviceEventResponse from(DeviceEvent event) {
        return new DeviceEventResponse(
            event.getId(),
            event.getDeviceId(),
            event.getEventId(),
            event.getEventType(),
            event.getParams(),
            event.getCreatedAt()
        );
    }
}
