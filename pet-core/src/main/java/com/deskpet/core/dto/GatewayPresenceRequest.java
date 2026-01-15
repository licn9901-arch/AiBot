package com.deskpet.core.dto;

public record GatewayPresenceRequest(
        String deviceId,
        String gatewayInstanceId,
        String ip
) {
}
