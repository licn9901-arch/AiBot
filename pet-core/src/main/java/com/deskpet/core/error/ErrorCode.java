package com.deskpet.core.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 通用错误
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "A0400", "Invalid request"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "A0401", "Validation failed"),
    INVALID_PARAM(HttpStatus.BAD_REQUEST, "A0402", "Invalid parameter"),

    // 认证授权错误
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A0301", "Unauthorized"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A0302", "Invalid token"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A0303", "Token expired"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A0310", "Forbidden"),
    NO_PERMISSION(HttpStatus.FORBIDDEN, "A0311", "No permission"),
    NO_ROLE(HttpStatus.FORBIDDEN, "A0312", "No role"),
    INTERNAL_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A0320", "Invalid internal token"),

    // 用户错误
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A0404", "User not found"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "A0409", "User already exists"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A0410", "Invalid password"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "A0411", "User disabled"),

    // 设备错误
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "A0420", "Device not found"),
    DEVICE_ALREADY_EXISTS(HttpStatus.CONFLICT, "A0421", "Device already exists"),
    DEVICE_ALREADY_BOUND(HttpStatus.CONFLICT, "A0422", "Device already bound"),

    // 授权码错误
    LICENSE_NOT_FOUND(HttpStatus.NOT_FOUND, "A0430", "License not found"),
    LICENSE_ALREADY_USED(HttpStatus.CONFLICT, "A0431", "License already used"),
    LICENSE_EXPIRED(HttpStatus.BAD_REQUEST, "A0432", "License expired"),
    LICENSE_REVOKED(HttpStatus.BAD_REQUEST, "A0433", "License revoked"),

    // 指令错误
    COMMAND_NOT_FOUND(HttpStatus.NOT_FOUND, "A0440", "Command not found"),
    COMMAND_NOT_RETRYABLE(HttpStatus.BAD_REQUEST, "A0441", "Command is not retryable"),

    // 产品/物模型错误
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "A0450", "Product not found"),
    PRODUCT_ALREADY_EXISTS(HttpStatus.CONFLICT, "A0451", "Product already exists"),
    PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "A0452", "Property not found"),
    PROPERTY_ALREADY_EXISTS(HttpStatus.CONFLICT, "A0453", "Property already exists"),
    SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "A0454", "Service not found"),
    SERVICE_ALREADY_EXISTS(HttpStatus.CONFLICT, "A0455", "Service already exists"),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "A0456", "Event not found"),
    EVENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "A0457", "Event already exists"),

    // 系统错误
    GATEWAY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "C0001", "Gateway unavailable"),
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
