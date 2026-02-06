package com.deskpet.core.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.deskpet.core.dto.DeviceEventResponse;
import com.deskpet.core.dto.DeviceRegistrationRequest;
import com.deskpet.core.dto.DeviceResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Device;
import com.deskpet.core.model.DeviceSession;
import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.service.DeviceEventService;
import com.deskpet.core.service.DeviceService;
import com.deskpet.core.service.LicenseCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "设备注册与查询")
public class DeviceController {

    private final DeviceService deviceService;
    private final LicenseCodeService licenseCodeService;
    private final DeviceEventService deviceEventService;

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
    @Operation(summary = "遥测历史", description = "查询最近 N 小时的遥测数据")
    public List<com.deskpet.core.model.TelemetryHistory> getTelemetryHistory(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "24") int hours) {
        // 非管理员需校验设备归属
        if (!StpUtil.hasRole("ADMIN")) {
            long userId = StpUtil.getLoginIdAsLong();
            if (!licenseCodeService.hasDevice(userId, deviceId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此设备");
            }
        }
        return deviceService.findTelemetryHistory(deviceId, Duration.ofHours(hours));
    }

    @GetMapping("/{deviceId}/events")
    @SaCheckPermission("device:view")
    @Operation(summary = "设备事件历史", description = "查询设备上报的事件")
    public Page<DeviceEventResponse> getDeviceEvents(
            @PathVariable String deviceId,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @PageableDefault(size = 20) Pageable pageable) {
        // 非管理员需校验设备归属
        if (!StpUtil.hasRole("ADMIN")) {
            long userId = StpUtil.getLoginIdAsLong();
            if (!licenseCodeService.hasDevice(userId, deviceId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此设备");
            }
        }
        return deviceEventService.findEvents(deviceId, eventId, eventType, startTime, endTime, pageable);
    }

    @GetMapping("/{deviceId}/events/stats")
    @SaCheckPermission("device:view")
    @Operation(summary = "设备事件统计", description = "统计设备事件数量")
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
        return deviceEventService.getEventStats(deviceId, Duration.ofHours(hours));
    }
}
