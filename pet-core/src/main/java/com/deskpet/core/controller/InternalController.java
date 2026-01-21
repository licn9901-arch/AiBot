package com.deskpet.core.controller;

import com.deskpet.core.dto.AckRequest;
import com.deskpet.core.dto.GatewayPresenceRequest;
import com.deskpet.core.dto.TelemetryRequest;
import com.deskpet.core.model.Device;
import com.deskpet.core.service.CommandService;
import com.deskpet.core.service.DeviceService;
import com.deskpet.core.service.TelemetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalController {
    private final DeviceService deviceService;
    private final TelemetryService telemetryService;
    private final CommandService commandService;

    public InternalController(DeviceService deviceService,
                              TelemetryService telemetryService,
                              CommandService commandService) {
        this.deviceService = deviceService;
        this.telemetryService = telemetryService;
        this.commandService = commandService;
    }

    @GetMapping("/auth")
    public ResponseEntity<String> auth(@RequestParam String deviceId,
                                       @RequestParam String secret) {
        Device device = deviceService.find(deviceId).orElse(null);
        if (device == null) {
            return ResponseEntity.status(404).body("DEVICE_NOT_FOUND");
        }
        if (!deviceService.verifySecret(device, secret)) {
            return ResponseEntity.status(401).body("INVALID_SECRET");
        }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/telemetry/{deviceId}")
    public ResponseEntity<Void> telemetry(@PathVariable String deviceId,
                                          @RequestBody TelemetryRequest request) {
        telemetryService.updateLatest(deviceId, request.toMap());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/ack/{deviceId}")
    public ResponseEntity<Void> ack(@PathVariable String deviceId,
                                    @RequestBody AckRequest request) {
        commandService.handleAck(request.reqId(), request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/gateway/deviceOnline")
    public ResponseEntity<Void> deviceOnline(@RequestBody GatewayPresenceRequest request) {
        deviceService.markOnline(request.deviceId(), request.gatewayInstanceId(), request.ip());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/gateway/deviceOffline")
    public ResponseEntity<Void> deviceOffline(@RequestBody GatewayPresenceRequest request) {
        deviceService.markOffline(request.deviceId(), request.gatewayInstanceId(), request.ip());
        return ResponseEntity.accepted().build();
    }
}
