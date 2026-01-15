package com.deskpet.gateway;

public record SendCommandRequest(
        String deviceId,
        String topic,
        int qos,
        String payload
) {
}
