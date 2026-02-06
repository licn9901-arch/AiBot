package com.deskpet.core.error;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode, ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(errorCode.status()).body(response);
    }

    // ==================== Sa-Token 异常处理 ====================

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ErrorResponse> handleNotLogin(NotLoginException ex) {
        String message = switch (ex.getType()) {
            case NotLoginException.NOT_TOKEN -> "未提供 Token";
            case NotLoginException.INVALID_TOKEN -> "Token 无效";
            case NotLoginException.TOKEN_TIMEOUT -> "Token 已过期";
            case NotLoginException.BE_REPLACED -> "已被顶下线";
            case NotLoginException.KICK_OUT -> "已被踢下线";
            default -> "未登录";
        };
        ErrorCode errorCode = switch (ex.getType()) {
            case NotLoginException.TOKEN_TIMEOUT -> ErrorCode.TOKEN_EXPIRED;
            case NotLoginException.INVALID_TOKEN -> ErrorCode.INVALID_TOKEN;
            default -> ErrorCode.UNAUTHORIZED;
        };
        ErrorResponse response = ErrorResponse.of(errorCode, message, null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ErrorResponse> handleNotPermission(NotPermissionException ex) {
        String message = "无权限: " + ex.getPermission();
        ErrorResponse response = ErrorResponse.of(ErrorCode.NO_PERMISSION, message, null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<ErrorResponse> handleNotRole(NotRoleException ex) {
        String message = "无角色: " + ex.getRole();
        ErrorResponse response = ErrorResponse.of(ErrorCode.NO_ROLE, message, null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ==================== 参数校验异常 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ErrorResponse response = ErrorResponse.of(ErrorCode.VALIDATION_FAILED, null, details);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status()).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                details.put(violation.getPropertyPath().toString(), violation.getMessage()));
        ErrorResponse response = ErrorResponse.of(ErrorCode.VALIDATION_FAILED, null, details);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status()).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadJson(HttpMessageNotReadableException ex) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_REQUEST, "Malformed JSON request", null);
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status()).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, Object> details = Map.of("parameter", ex.getParameterName());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_REQUEST, "Missing request parameter", details);
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_REQUEST, ex.getMessage(), null);
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR, null, null);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status()).body(response);
    }
}
