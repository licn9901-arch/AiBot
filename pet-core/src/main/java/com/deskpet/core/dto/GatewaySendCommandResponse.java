package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GatewaySendCommandResponse(
        @Schema(description = "是否成功", example = "true")
        boolean ok,
        @Schema(description = "失败原因（失败时返回）", example = "device offline")
        String reason
) {
}
