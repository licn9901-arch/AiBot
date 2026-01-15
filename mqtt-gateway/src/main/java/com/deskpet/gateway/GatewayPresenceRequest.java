package com.deskpet.gateway;

public record GatewayPresenceRequest(
        String deviceId,
        String gatewayInstanceId,
        String ip
) {
}
