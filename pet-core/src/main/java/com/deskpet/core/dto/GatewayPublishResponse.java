package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GatewayPublishResponse(
        @Schema(description = "是否成功", example = "true")
        boolean ok,
        @Schema(description = "失败原因（失败时返回）", example = "OFFLINE")
        String reason
) {
}
