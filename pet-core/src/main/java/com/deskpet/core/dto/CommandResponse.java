package com.deskpet.core.dto;

import com.deskpet.core.model.Command;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record CommandResponse(
        @Schema(description = "请求ID", example = "b1f2a9c6-4a0e-4b0b-9d3b-1a2b3c4d5e6f")
        String reqId,
        @Schema(description = "设备ID", example = "pet001")
        String deviceId,
        @Schema(description = "命令类型", example = "move")
        String type,
        @Schema(description = "命令参数", example = "{\"direction\":\"forward\",\"speed\":0.6,\"durationMs\":800}")
        Map<String, Object> payload,
        @Schema(description = "指令状态", example = "SENT")
        String status,
        @Schema(description = "回执码", example = "DONE")
        String ackCode,
        @Schema(description = "回执消息", example = "moved forward")
        String ackMessage,
        @Schema(description = "创建时间", example = "2026-01-21T10:30:00Z")
        Instant createdAt,
        @Schema(description = "更新时间", example = "2026-01-21T10:30:01Z")
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
