package com.deskpet.core.model;

import java.time.Instant;
import java.util.Map;

public record Command(
        String reqId,
        String deviceId,
        String type,
        Map<String, Object> payload,
        CommandStatus status,
        String ackCode,
        String ackMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public Command withStatus(CommandStatus nextStatus, String code, String message) {
        return new Command(reqId, deviceId, type, payload, nextStatus, code, message, createdAt, Instant.now());
    }
}
