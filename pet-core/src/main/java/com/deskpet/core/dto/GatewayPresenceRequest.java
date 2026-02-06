package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GatewayPresenceRequest(
        @Schema(description = "设备ID", example = "pet001")
        String deviceId,
        @Schema(description = "网关实例ID", example = "gateway-1")
        String gatewayInstanceId,
        @Schema(description = "设备连接IP", example = "192.168.1.10")
        String ip
) {
}
