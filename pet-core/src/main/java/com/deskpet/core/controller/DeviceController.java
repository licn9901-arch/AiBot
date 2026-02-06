package com.deskpet.core.controller;

import com.deskpet.core.dto.DeviceRegistrationRequest;
import com.deskpet.core.dto.DeviceResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Device;
import com.deskpet.core.model.DeviceSession;
import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "设备管理", description = "设备注册与查询")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    @Operation(summary = "设备列表", description = "返回所有已注册设备与在线/遥测信息")
    public List<DeviceResponse> listDevices() {
        return deviceService.list().stream()
                .map(device -> DeviceResponse.of(device,
                        deviceService.findSession(device.deviceId()).orElse(null),
                        deviceService.findTelemetry(device.deviceId()).orElse(null)))
                .toList();
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "设备详情", description = "按 deviceId 查询设备与在线/遥测信息")
    public DeviceResponse getDevice(@PathVariable String deviceId) {
        Device device = deviceService.find(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        DeviceSession session = deviceService.findSession(deviceId).orElse(null);
        TelemetryLatest telemetry = deviceService.findTelemetry(deviceId).orElse(null);
        return DeviceResponse.of(device, session, telemetry);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "设备注册", description = "注册设备基础信息与密钥")
    public DeviceResponse register(@Valid @RequestBody DeviceRegistrationRequest request) {
        Device device = deviceService.register(request.deviceId(), request.secret(), request.model(), request.remark());
        return DeviceResponse.of(device, null, null);
    }

    @GetMapping("/{deviceId}/telemetry/history")
    @Operation(summary = "遥测历史", description = "查询最近 N 小时的遥测数据")
    public List<com.deskpet.core.model.TelemetryHistory> getTelemetryHistory(@PathVariable String deviceId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "24") int hours) {
        return deviceService.findTelemetryHistory(deviceId, java.time.Duration.ofHours(hours));
    }
}
