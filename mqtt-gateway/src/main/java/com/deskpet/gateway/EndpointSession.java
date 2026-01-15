package com.deskpet.gateway;

import io.vertx.mqtt.MqttEndpoint;

import java.time.Instant;

public record EndpointSession(
        String deviceId,
        MqttEndpoint endpoint,
        Instant connectedAt,
        String clientIp
) {
}
