package com.deskpet.core.dto;

import com.deskpet.core.model.Command;

import java.time.Instant;
import java.util.Map;

public record CommandResponse(
        String reqId,
        String deviceId,
        String type,
        Map<String, Object> payload,
        String status,
        String ackCode,
        String ackMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommandResponse of(Command command) {
        return new CommandResponse(
                command.reqId(),
                command.deviceId(),
                command.type(),
                command.payload(),
                command.status().name(),
                command.ackCode(),
                command.ackMessage(),
                command.createdAt(),
                command.updatedAt()
        );
    }
}
