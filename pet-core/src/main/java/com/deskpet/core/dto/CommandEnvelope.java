package com.deskpet.core.dto;

import java.time.Instant;
import java.util.Map;

public record CommandEnvelope(
        int schemaVersion,
        String type,
        String reqId,
        long ts,
        Map<String, Object> payload
) {
    public static CommandEnvelope of(String type, String reqId, Map<String, Object> payload) {
        return new CommandEnvelope(1, type, reqId, Instant.now().getEpochSecond(), payload);
    }
}
