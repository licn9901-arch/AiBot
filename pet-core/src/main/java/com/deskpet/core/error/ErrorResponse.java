package com.deskpet.core.error;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
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
