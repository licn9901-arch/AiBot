package com.deskpet.core.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record ErrorResponse(
        @Schema(description = "错误码", example = "A0400")
        String code,
        @Schema(description = "错误信息", example = "Invalid request")
        String message,
        @Schema(description = "错误详情", example = "{\"field\":\"deviceId\"}")
        Map<String, Object> details
) {
    public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, Object> details) {
        String finalMessage = message;
        if (finalMessage == null || finalMessage.isBlank()) {
            finalMessage = errorCode.defaultMessage();
        }
        Map<String, Object> finalDetails = (details == null || details.isEmpty()) ? null : details;
        return new ErrorResponse(errorCode.code(), finalMessage, finalDetails);
    }
}
