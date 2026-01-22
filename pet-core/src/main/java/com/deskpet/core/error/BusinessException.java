package com.deskpet.core.error;

import java.util.Map;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final transient Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
