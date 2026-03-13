package com.deskpet.gateway;

public record SendPublishRequest(
        String deviceId,
        String topic,
        int qos,
        String payload
) {
}
