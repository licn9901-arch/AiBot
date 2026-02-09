package com.deskpet.gateway;

import io.vertx.mqtt.MqttEndpoint;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public record EndpointSession(
        String deviceId,
        MqttEndpoint endpoint,
        Instant connectedAt,
        String clientIp,
        AtomicReference<Instant> lastActivityAt
) {
    public EndpointSession(String deviceId, MqttEndpoint endpoint, Instant connectedAt, String clientIp) {
        this(deviceId, endpoint, connectedAt, clientIp, new AtomicReference<>(connectedAt));
    }

    public void updateActivity() {
        lastActivityAt.set(Instant.now());
    }

    public Instant lastActivity() {
        return lastActivityAt.get();
    }
}
