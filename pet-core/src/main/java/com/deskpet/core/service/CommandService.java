package com.deskpet.core.service;

import com.deskpet.core.dto.AckRequest;
import com.deskpet.core.dto.CommandEnvelope;
import com.deskpet.core.dto.GatewaySendCommandRequest;
import com.deskpet.core.dto.GatewaySendCommandResponse;
import com.deskpet.core.model.Command;
import com.deskpet.core.model.CommandStatus;
import com.deskpet.core.store.CommandStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CommandService {
    private final CommandStore commandStore;
    private final GatewayClient gatewayClient;
    private final DeviceService deviceService;
    private final ObjectMapper objectMapper;
    private final int timeoutSeconds;

    public CommandService(CommandStore commandStore,
                          GatewayClient gatewayClient,
                          DeviceService deviceService,
                          ObjectMapper objectMapper,
                          @Value("${command.timeoutSec:10}") int timeoutSeconds) {
        this.commandStore = commandStore;
        this.gatewayClient = gatewayClient;
        this.deviceService = deviceService;
        this.objectMapper = objectMapper;
        this.timeoutSeconds = timeoutSeconds;
    }

    public Command createCommand(String deviceId, String type, Map<String, Object> payload) {
        if (deviceService.find(deviceId).isEmpty()) {
            throw new IllegalArgumentException("Device not found");
        }
        String reqId = UUID.randomUUID().toString();
        Command command = new Command(reqId, deviceId, type, payload, CommandStatus.PENDING, null, null, Instant.now(), Instant.now());
        commandStore.save(command);

        String payloadJson;
        try {
            CommandEnvelope envelope = CommandEnvelope.of(type, reqId, payload);
            payloadJson = objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            command = command.withStatus(CommandStatus.FAILED, "SERIALIZE_ERROR", "Failed to serialize command payload");
            return commandStore.save(command);
        }
        GatewaySendCommandResponse response = gatewayClient.sendCommand(
                new GatewaySendCommandRequest(deviceId, "pet/" + deviceId + "/cmd", 1, payloadJson));
        if (response != null && response.ok()) {
            command = command.withStatus(CommandStatus.SENT, null, null);
        } else {
            String reason = response == null ? "NO_RESPONSE" : response.reason();
            command = command.withStatus(CommandStatus.FAILED, "OFFLINE", reason);
        }
        return commandStore.save(command);
    }

    public Optional<Command> findCommand(String reqId) {
        return commandStore.findByReqId(reqId);
    }

    public void handleAck(String reqId, AckRequest ack) {
        commandStore.findByReqId(reqId).ifPresent(command -> {
            CommandStatus next = ack.ok() ? CommandStatus.ACKED : CommandStatus.FAILED;
            Command updated = command.withStatus(next, ack.code(), ack.message());
            commandStore.save(updated);
        });
    }

    @Scheduled(fixedDelayString = "${command.timeoutScanMs:2000}")
    public void timeoutScan() {
        Instant cutoff = Instant.now().minusSeconds(timeoutSeconds);
        Collection<Command> timedOut = commandStore.findSentBefore(cutoff);
        for (Command command : timedOut) {
            Command updated = command.withStatus(CommandStatus.TIMEOUT, "TIMEOUT", "No ack within timeout");
            commandStore.save(updated);
        }
    }
}
