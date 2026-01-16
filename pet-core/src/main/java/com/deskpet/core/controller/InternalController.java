package com.deskpet.core.controller;

import com.deskpet.core.dto.AckRequest;
import com.deskpet.core.dto.GatewayPresenceRequest;
import com.deskpet.core.dto.TelemetryRequest;
import com.deskpet.core.model.Device;
import com.deskpet.core.service.CommandService;
import com.deskpet.core.service.DeviceService;
import com.deskpet.core.service.TelemetryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal")
public class InternalController {
    private final DeviceService deviceService;
    private final TelemetryService telemetryService;
    private final CommandService commandService;
    private final String internalToken;

    public InternalController(DeviceService deviceService,
                              TelemetryService telemetryService,
                              CommandService commandService,
                              @Value("${internal.token:}") String internalToken) {
        this.deviceService = deviceService;
        this.telemetryService = telemetryService;
        this.commandService = commandService;
        this.internalToken = internalToken;
    }

    @GetMapping("/auth")
    public ResponseEntity<String> auth(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                       @RequestParam String deviceId,
                                       @RequestParam String secret) {
        validateInternalToken(token);
        Device device = deviceService.find(deviceId).orElse(null);
        if (device == null) {
            return ResponseEntity.status(404).body("DEVICE_NOT_FOUND");
        }
        if (!device.secret().equals(secret)) {
            return ResponseEntity.status(401).body("INVALID_SECRET");
        }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/telemetry/{deviceId}")
    public ResponseEntity<Void> telemetry(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                          @PathVariable String deviceId,
                                          @RequestBody TelemetryRequest request) {
        validateInternalToken(token);
        telemetryService.updateLatest(deviceId, request.toMap());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/ack/{deviceId}")
    public ResponseEntity<Void> ack(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                    @PathVariable String deviceId,
                                    @RequestBody AckRequest request) {
        validateInternalToken(token);
        commandService.handleAck(request.reqId(), request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/gateway/deviceOnline")
    public ResponseEntity<Void> deviceOnline(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                             @RequestBody GatewayPresenceRequest request) {
        validateInternalToken(token);
        deviceService.markOnline(request.deviceId(), request.gatewayInstanceId(), request.ip());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/gateway/deviceOffline")
    public ResponseEntity<Void> deviceOffline(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                              @RequestBody GatewayPresenceRequest request) {
        validateInternalToken(token);
        deviceService.markOffline(request.deviceId(), request.gatewayInstanceId(), request.ip());
        return ResponseEntity.accepted().build();
    }

    private void validateInternalToken(String token) {
        if (internalToken == null || internalToken.isBlank()) {
            return;
        }
        if (!internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_TOKEN");
        }
    }
}
