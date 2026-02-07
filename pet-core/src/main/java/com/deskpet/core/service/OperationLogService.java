package com.deskpet.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

/**
 * 操作日志服务
 * 操作日志写入 TimescaleDB，不再写入 PostgreSQL
 */
@Slf4j
@Service
public class OperationLogService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TimeSeriesService timeSeriesService;

    @Autowired(required = false)
    public void setTimeSeriesService(TimeSeriesService timeSeriesService) {
        this.timeSeriesService = timeSeriesService;
    }

    /**
     * 记录操作日志（异步，写入 TimescaleDB）
     */
    @Async
    public void log(Long userId, String deviceId, String action, Object payload) {
        try {
            String ip = null;
            String userAgent = null;

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ip = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
            }

            Map<String, Object> payloadMap = null;
            if (payload != null) {
                if (payload instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) payload;
                    payloadMap = map;
                } else {
                    payloadMap = Map.of("data", payload.toString());
                }
            }

            String truncatedUserAgent = userAgent != null && userAgent.length() > 500
                ? userAgent.substring(0, 500) : userAgent;

            writeToTimescaleDb(userId, deviceId, action, payloadMap, ip, truncatedUserAgent, Instant.now());
        } catch (Exception e) {
            log.error("Failed to save operation log: userId={}, action={}", userId, action, e);
        }
    }

    /**
     * 同步记录日志（写入 TimescaleDB）
     */
    public void logSync(Long userId, String deviceId, String action, Map<String, Object> payload, String ip, String userAgent) {
        writeToTimescaleDb(userId, deviceId, action, payload, ip, userAgent, Instant.now());
    }

    private void writeToTimescaleDb(Long userId, String deviceId, String action,
                                    Map<String, Object> payload, String ip, String userAgent, Instant time) {
        if (timeSeriesService == null) {
            log.warn("TimeSeriesService not available, operation log dropped: action={}", action);
            return;
        }
        try {
            String payloadJson = payload != null ? objectMapper.writeValueAsString(payload) : null;
            timeSeriesService.writeOperationLog(userId, deviceId, action, payloadJson, ip, userAgent, time);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize operation log payload for TimescaleDB: action={}", action, e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
