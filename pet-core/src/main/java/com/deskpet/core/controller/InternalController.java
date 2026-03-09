package com.deskpet.core.controller;

import com.deskpet.core.dto.*;
import com.deskpet.core.model.Device;
import com.deskpet.core.service.CommandService;
import com.deskpet.core.service.DeviceEventService;
import com.deskpet.core.service.DeviceRequestService;
import com.deskpet.core.service.DeviceService;
import com.deskpet.core.service.TelemetryService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal")
@Hidden
@Slf4j
public class InternalController {
    private final DeviceService deviceService;
    private final TelemetryService telemetryService;
    private final CommandService commandService;
    private final DeviceEventService deviceEventService;
    private final DeviceRequestService deviceRequestService;

    public InternalController(DeviceService deviceService,
                              TelemetryService telemetryService,
                              CommandService commandService,
                              DeviceEventService deviceEventService,
                              DeviceRequestService deviceRequestService) {
        this.deviceService = deviceService;
        this.telemetryService = telemetryService;
        this.commandService = commandService;
        this.deviceEventService = deviceEventService;
        this.deviceRequestService = deviceRequestService;
    }

    @GetMapping("/auth")
    public ResponseEntity<String> auth(@RequestParam String deviceId,
                                       @RequestParam String secret) {
        Device device = deviceService.find(deviceId).orElse(null);
        log.info("Auth request: deviceId={}, secret={}", deviceId, secret);
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

    @PostMapping("/event/{deviceId}")
    public ResponseEntity<Void> event(@PathVariable String deviceId,
                                                     @RequestBody DeviceEventRequest request) {
        deviceEventService.recordEvent(deviceId, request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/request/{deviceId}")
    public ResponseEntity<Void> request(@PathVariable String deviceId,
                                        @RequestBody DeviceRequestEnvelope request) {
        deviceRequestService.handleRequest(deviceId, request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/gateway/cleanup")
    public ResponseEntity<Void> gatewayCleanup(@RequestBody Map<String, String> request) {
        String gatewayInstanceId = request.get("gatewayInstanceId");
        log.info("Gateway cleanup request: instanceId={}", gatewayInstanceId);
        deviceService.markAllOfflineByGateway(gatewayInstanceId);
        return ResponseEntity.accepted().build();
    }
}
