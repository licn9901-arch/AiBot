package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.deskpet.core.dto.DeviceRegistrationRequest;
import com.deskpet.core.dto.DeviceResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Device;
import com.deskpet.core.model.DeviceSession;
import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.service.DeviceService;
import com.deskpet.core.service.LicenseCodeService;
import com.deskpet.core.service.TimeSeriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "设备管理", description = "设备注册与查询")
public class DeviceController {

    private final DeviceService deviceService;
    private final LicenseCodeService licenseCodeService;

    private TimeSeriesService timeSeriesService;

    public DeviceController(DeviceService deviceService,
                            LicenseCodeService licenseCodeService) {
        this.deviceService = deviceService;
        this.licenseCodeService = licenseCodeService;
    }

    @Autowired(required = false)
    public void setTimeSeriesService(TimeSeriesService timeSeriesService) {
        this.timeSeriesService = timeSeriesService;
    }

    @GetMapping
    @SaCheckPermission("device:list")
    @Operation(summary = "设备列表", description = "管理员返回所有设备，普通用户返回自己绑定的设备")
    public List<DeviceResponse> listDevices() {
        // 管理员查看所有设备
        if (StpUtil.hasRole("ADMIN")) {
            return deviceService.listAll();
        }
        // 普通用户只能查看自己绑定的设备
        long userId = StpUtil.getLoginIdAsLong();
        List<String> deviceIds = licenseCodeService.findDeviceIdsByUserId(userId);
        return deviceService.findByIds(deviceIds);
    }

    @GetMapping("/{deviceId}")
    @SaCheckPermission("device:view")
    @Operation(summary = "设备详情", description = "按 deviceId 查询设备与在线/遥测信息")
    public DeviceResponse getDevice(@PathVariable String deviceId) {
        // 非管理员需校验设备归属
        if (!StpUtil.hasRole("ADMIN")) {
            long userId = StpUtil.getLoginIdAsLong();
            if (!licenseCodeService.hasDevice(userId, deviceId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此设备");
            }
        }

        Device device = deviceService.find(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        DeviceSession session = deviceService.findSession(deviceId).orElse(null);
        TelemetryLatest telemetry = deviceService.findTelemetry(deviceId).orElse(null);
        return DeviceResponse.of(device, session, telemetry);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission("device:register")
    @Operation(summary = "设备注册", description = "注册设备基础信息与密钥（管理员操作）")
    public DeviceResponse register(@Valid @RequestBody DeviceRegistrationRequest request) {
        Device device = deviceService.register(request.deviceId(), request.secret(), request.model(), request.productKey(), request.remark());
        return DeviceResponse.of(device, null, null);
    }

    @GetMapping("/{deviceId}/telemetry/history")
    @SaCheckPermission("device:view")
    @Operation(summary = "遥测历史", description = "查询最近 N 小时的遥测数据（从 TimescaleDB）")
    public List<Map<String, Object>> getTelemetryHistory(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "24") int hours) {
        // 非管理员需校验设备归属
        if (!StpUtil.hasRole("ADMIN")) {
            long userId = StpUtil.getLoginIdAsLong();
            if (!licenseCodeService.hasDevice(userId, deviceId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此设备");
            }
        }
        if (timeSeriesService == null) {
            return List.of();
        }
        return timeSeriesService.queryTelemetryTimeSeries(deviceId,
            Instant.now().minus(Duration.ofHours(hours)));
    }

    @GetMapping("/{deviceId}/events")
    @SaCheckPermission("device:view")
    @Operation(summary = "设备事件历史", description = "查询设备上报的事件（从 TimescaleDB）")
    public Map<String, Object> getDeviceEvents(
            @PathVariable String deviceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 非管理员需校验设备归属
        if (!StpUtil.hasRole("ADMIN")) {
            long userId = StpUtil.getLoginIdAsLong();
            if (!licenseCodeService.hasDevice(userId, deviceId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此设备");
            }
        }
        if (timeSeriesService == null) {
            return Map.of("content", List.of(), "totalElements", 0L, "number", page, "size", size);
        }
        List<Map<String, Object>> content = timeSeriesService.queryDeviceEvents(
                deviceId, eventType, startTime, endTime, size, page * size);
        long total = timeSeriesService.countDeviceEvents(deviceId, eventType, startTime, endTime);

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (total + size - 1) / size);
        result.put("number", page);
        result.put("size", size);
        return result;
    }

    @GetMapping("/{deviceId}/events/stats")
    @SaCheckPermission("device:view")
    @Operation(summary = "设备事件统计", description = "统计设备事件数量（从 TimescaleDB）")
    public Map<String, Object> getDeviceEventStats(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "24") int hours) {
        // 非管理员需校验设备归属
        if (!StpUtil.hasRole("ADMIN")) {
            long userId = StpUtil.getLoginIdAsLong();
            if (!licenseCodeService.hasDevice(userId, deviceId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此设备");
            }
        }
        if (timeSeriesService == null) {
            return Map.of("deviceId", deviceId, "total", 0L);
        }
        Instant since = Instant.now().minus(Duration.ofHours(hours));
        List<Map<String, Object>> stats = timeSeriesService.queryDeviceEventStats(deviceId, since);

        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        long total = 0;
        Map<String, Long> byType = new HashMap<>();
        for (Map<String, Object> row : stats) {
            String type = (String) row.get("event_type");
            Long count = (Long) row.get("count");
            byType.put(type, count);
            total += count;
        }
        result.put("total", total);
        result.put("byType", byType);
        result.put("duration", "PT" + hours + "H");
        return result;
    }

    @GetMapping("/{deviceId}/sessions")
    @SaCheckPermission("device:view")
    @Operation(summary = "设备上下线历史", description = "查询设备上下线记录（需启用 TimescaleDB）")
    public List<Map<String, Object>> getDeviceSessionHistory(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "24") int hours) {
        // 非管理员需校验设备归属
        if (!StpUtil.hasRole("ADMIN")) {
            long userId = StpUtil.getLoginIdAsLong();
            if (!licenseCodeService.hasDevice(userId, deviceId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此设备");
            }
        }
        if (timeSeriesService == null) {
            return List.of();
        }
        return timeSeriesService.queryDeviceSessionHistory(deviceId,
            Instant.now().minus(Duration.ofHours(hours)));
    }

    @GetMapping("/{deviceId}/sessions/stats")
    @SaCheckPermission("device:view")
    @Operation(summary = "设备上下线统计", description = "统计设备上下线次数（需启用 TimescaleDB）")
    public Map<String, Object> getDeviceSessionStats(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "24") int hours) {
        // 非管理员需校验设备归属
        if (!StpUtil.hasRole("ADMIN")) {
            long userId = StpUtil.getLoginIdAsLong();
            if (!licenseCodeService.hasDevice(userId, deviceId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此设备");
            }
        }
        if (timeSeriesService == null) {
            return Map.of("deviceId", deviceId, "onlineCount", 0L, "offlineCount", 0L);
        }
        return timeSeriesService.queryDeviceSessionStats(deviceId,
            Instant.now().minus(Duration.ofHours(hours)));
    }
}
