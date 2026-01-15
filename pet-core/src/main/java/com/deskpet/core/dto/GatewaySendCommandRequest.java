package com.deskpet.core.dto;

public record GatewaySendCommandRequest(
        String deviceId,
        String topic,
        int qos,
        String payload
) {
}
