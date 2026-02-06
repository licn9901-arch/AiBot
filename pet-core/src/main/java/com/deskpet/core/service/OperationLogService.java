package com.deskpet.core.service;

import com.deskpet.core.model.OperationLog;
import com.deskpet.core.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

/**
 * 操作日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    /**
     * 记录操作日志（异步）
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

            OperationLog operationLog = OperationLog.builder()
                .userId(userId)
                .deviceId(deviceId)
                .action(action)
                .payload(payloadMap)
                .ip(ip)
                .userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
                .createdAt(Instant.now())
                .build();

            operationLogRepository.save(operationLog);
        } catch (Exception e) {
            log.error("Failed to save operation log: userId={}, action={}", userId, action, e);
        }
    }

    /**
     * 同步记录日志
     */
    public void logSync(Long userId, String deviceId, String action, Map<String, Object> payload, String ip, String userAgent) {
        OperationLog operationLog = OperationLog.builder()
            .userId(userId)
            .deviceId(deviceId)
            .action(action)
            .payload(payload)
            .ip(ip)
            .userAgent(userAgent)
            .createdAt(Instant.now())
            .build();
        operationLogRepository.save(operationLog);
    }

    /**
     * 查询操作日志
     */
    public Page<OperationLog> findByFilters(Long userId, String deviceId, String action,
                                            Instant startTime, Instant endTime, Pageable pageable) {
        return operationLogRepository.findByFilters(userId, deviceId, action, startTime, endTime, pageable);
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
