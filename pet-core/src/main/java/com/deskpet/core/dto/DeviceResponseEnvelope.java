package com.deskpet.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record DeviceResponseEnvelope(
        @Schema(description = "协议版本", example = "1")
        int schemaVersion,
        @Schema(description = "请求ID", example = "7f4d1c9e-8a1a-4d35-8f62-1f7c23b2d111")
        String reqId,
        @Schema(description = "请求类型", example = "getWeather")
        String type,
        @Schema(description = "是否成功", example = "true")
        boolean ok,
        @Schema(description = "结果码", example = "DONE")
        String code,
        @Schema(description = "结果消息", example = "success")
        String message,
        @Schema(description = "响应时间戳（秒）", example = "1730000001")
        long ts,
        @Schema(description = "响应参数")
        Map<String, Object> payload
) {
    public static DeviceResponseEnvelope success(DeviceRequestEnvelope request,
                                                 String message,
                                                 Map<String, Object> payload) {
        return new DeviceResponseEnvelope(
                request.schemaVersion(),
                request.reqId(),
                request.type(),
                true,
                "DONE",
                message,
                currentTs(),
                payload
        );
    }

    public static DeviceResponseEnvelope failure(DeviceRequestEnvelope request,
                                                 String code,
                                                 String message) {
        return new DeviceResponseEnvelope(
                request.schemaVersion(),
                request.reqId(),
                request.type(),
                false,
                code,
                message,
                currentTs(),
                null
        );
    }

    private static long currentTs() {
        return System.currentTimeMillis() / 1000;
    }
}
