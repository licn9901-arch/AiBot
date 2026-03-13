package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record DeviceRequestEnvelope(
        @Schema(description = "协议版本", example = "1")
        int schemaVersion,
        @Schema(description = "请求ID", example = "7f4d1c9e-8a1a-4d35-8f62-1f7c23b2d111")
        String reqId,
        @Schema(description = "请求类型", example = "getWeather")
        String type,
        @Schema(description = "请求时间戳（秒）", example = "1730000000")
        long ts,
        @Schema(description = "请求参数")
        Map<String, Object> payload
) {
}
