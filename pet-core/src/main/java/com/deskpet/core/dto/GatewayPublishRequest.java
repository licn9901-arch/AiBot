package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GatewayPublishRequest(
        @Schema(description = "设备ID", example = "pet001")
        String deviceId,
        @Schema(description = "下发Topic", example = "pet/pet001/cmd")
        String topic,
        @Schema(description = "QoS 等级", example = "1")
        int qos,
        @Schema(description = "消息内容（JSON字符串）", example = "{\"schemaVersion\":1}")
        String payload
) {
}
