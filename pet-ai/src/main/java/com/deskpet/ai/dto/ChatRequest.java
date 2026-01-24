package com.deskpet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "对话请求")
public record ChatRequest(
        @Schema(description = "设备ID", example = "pet-001")
        @NotBlank String deviceId,
        @Schema(description = "用户消息", example = "让小宠往前走一点")
        @NotBlank String message,
        @Schema(description = "会话ID（可选，用于多轮对话）", example = "session-uuid")
        String sessionId
) {
}
