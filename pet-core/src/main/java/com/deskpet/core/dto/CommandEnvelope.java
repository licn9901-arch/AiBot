package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record CommandEnvelope(
        @Schema(description = "协议版本", example = "1")
        int schemaVersion,
        @Schema(description = "命令类型", example = "move")
        String type,
        @Schema(description = "请求ID", example = "b1f2a9c6-4a0e-4b0b-9d3b-1a2b3c4d5e6f")
        String reqId,
        @Schema(description = "发送时间戳（秒）", example = "1730000000")
        long ts,
        @Schema(description = "命令参数", example = "{\"direction\":\"forward\",\"speed\":0.6,\"durationMs\":800}")
        Map<String, Object> payload
) {
    public static CommandEnvelope of(String type, String reqId, Map<String, Object> payload) {
        return new CommandEnvelope(1, type, reqId, Instant.now().getEpochSecond(), payload);
    }
}
