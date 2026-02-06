package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AckRequest(
        @Schema(description = "协议版本", example = "1")
        int schemaVersion,
        @Schema(description = "请求ID", example = "b1f2a9c6-4a0e-4b0b-9d3b-1a2b3c4d5e6f")
        String reqId,
        @Schema(description = "是否成功", example = "true")
        boolean ok,
        @Schema(description = "回执码", example = "DONE")
        String code,
        @Schema(description = "回执消息", example = "moved forward")
        String message,
        @Schema(description = "回执时间戳（秒）", example = "1730000002")
        long ts
) {
}
