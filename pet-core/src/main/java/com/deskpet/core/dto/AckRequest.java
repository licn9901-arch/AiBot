package com.deskpet.core.dto;

public record AckRequest(
        int schemaVersion,
        String reqId,
        boolean ok,
        String code,
        String message,
        long ts
) {
}
