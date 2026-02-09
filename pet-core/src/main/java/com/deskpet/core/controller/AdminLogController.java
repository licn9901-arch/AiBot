package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.deskpet.core.service.TimeSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志管理控制器（管理员）
 * 所有日志数据均从 TimescaleDB 查询
 */
@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "日志管理", description = "操作日志、应用日志、设备事件查询")
public class AdminLogController {

    @Autowired(required = false)
    private TimeSeriesService timeSeriesService;

    @GetMapping
    @SaCheckPermission("log:list")
    @Operation(summary = "操作日志列表", description = "从 TimescaleDB 查询操作日志，支持按用户、设备、操作类型、时间范围筛选")
    public Map<String, Object> list(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "deviceId", required = false) String deviceId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(value = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        if (timeSeriesService == null) {
            return Map.of(
                "content", List.of(),
                "totalElements", 0L,
                "number", page,
                "size", size,
                "message", "TimescaleDB 未启用"
            );
        }
        List<Map<String, Object>> content = timeSeriesService.queryOperationLogs(
                userId, deviceId, action, startTime, endTime, size, page * size);
        long total = timeSeriesService.countOperationLogs(userId, deviceId, action, startTime, endTime);

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (total + size - 1) / size);
        result.put("number", page);
        result.put("size", size);
        return result;
    }

    @GetMapping("/app")
    @SaCheckPermission("log:list")
    @Operation(summary = "应用日志查询", description = "从 TimescaleDB 查询应用日志，需启用 TimescaleDB")
    public Map<String, Object> appLogs(
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "logger", required = false) String logger,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "hours", defaultValue = "24") int hours,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        if (timeSeriesService == null) {
            return Map.of(
                "content", List.of(),
                "totalElements", 0L,
                "number", page,
                "size", size,
                "message", "TimescaleDB 未启用"
            );
        }
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<Map<String, Object>> content = timeSeriesService.queryAppLogs(
                level, logger, search, since, size, page * size);
        long total = timeSeriesService.countAppLogs(level, logger, search, since);

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (total + size - 1) / size);
        result.put("number", page);
        result.put("size", size);
        return result;
    }

    @GetMapping("/app/stats")
    @SaCheckPermission("log:list")
    @Operation(summary = "应用日志统计", description = "按级别分组统计应用日志数量")
    public Map<String, Object> appLogStats(
            @RequestParam(value = "hours", defaultValue = "24") int hours) {
        if (timeSeriesService == null) {
            return Map.of("message", "TimescaleDB 未启用");
        }
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<Map<String, Object>> stats = timeSeriesService.queryAppLogStats(since);

        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> row : stats) {
            result.put((String) row.get("level"), row.get("count"));
        }
        return result;
    }

    @GetMapping("/events")
    @SaCheckPermission("log:list")
    @Operation(summary = "设备事件日志", description = "从 TimescaleDB 查询设备事件")
    public Map<String, Object> deviceEvents(
            @RequestParam(value = "deviceId", required = false) String deviceId,
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "hours", defaultValue = "24") int hours,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        if (timeSeriesService == null) {
            return Map.of(
                "content", List.of(),
                "totalElements", 0L,
                "number", page,
                "size", size,
                "message", "TimescaleDB 未启用"
            );
        }
        Instant startTime = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<Map<String, Object>> content = timeSeriesService.queryDeviceEvents(
                deviceId, eventType, startTime, null, size, page * size);
        long total = timeSeriesService.countDeviceEvents(deviceId, eventType, startTime, null);

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (total + size - 1) / size);
        result.put("number", page);
        result.put("size", size);
        return result;
    }
}
