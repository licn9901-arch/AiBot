package com.deskpet.core.controller;

import com.deskpet.core.dto.DeviceRegistrationRequest;
import com.deskpet.core.dto.DeviceResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Device;
import com.deskpet.core.model.DeviceSession;
import com.deskpet.core.model.TelemetryLatest;
import com.deskpet.core.service.DeviceService;
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
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceResponse> listDevices() {
        return deviceService.list().stream()
                .map(device -> DeviceResponse.of(device,
                        deviceService.findSession(device.deviceId()).orElse(null),
                        deviceService.findTelemetry(device.deviceId()).orElse(null)))
                .toList();
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse getDevice(@PathVariable String deviceId) {
        Device device = deviceService.find(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        DeviceSession session = deviceService.findSession(deviceId).orElse(null);
        TelemetryLatest telemetry = deviceService.findTelemetry(deviceId).orElse(null);
        return DeviceResponse.of(device, session, telemetry);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse register(@Valid @RequestBody DeviceRegistrationRequest request) {
        Device device = deviceService.register(request.deviceId(), request.secret(), request.model(), request.remark());
        return DeviceResponse.of(device, null, null);
    }
}
