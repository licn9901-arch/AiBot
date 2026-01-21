package com.deskpet.core.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "A0400", "Invalid request"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "A0401", "Validation failed"),
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "A0404", "Device not found"),
    COMMAND_NOT_FOUND(HttpStatus.NOT_FOUND, "A0405", "Command not found"),
    DEVICE_ALREADY_EXISTS(HttpStatus.CONFLICT, "A0409", "Device already exists"),
    GATEWAY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "C0001", "Gateway unavailable"),
    INTERNAL_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A0301", "Invalid internal token"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "B0001", "Internal server error");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
