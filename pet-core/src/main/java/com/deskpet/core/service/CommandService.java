package com.deskpet.core.service;

import com.deskpet.core.dto.AckRequest;
import com.deskpet.core.dto.CommandEnvelope;
import com.deskpet.core.dto.GatewaySendCommandRequest;
import com.deskpet.core.dto.GatewaySendCommandResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Command;
import com.deskpet.core.model.CommandStatus;
import com.deskpet.core.repository.CommandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor()
public class CommandService {
    private final CommandRepository commandRepository;
    private final GatewayClient gatewayClient;
    private final DeviceService deviceService;
    private final ObjectMapper objectMapper;

    @Value("${command.timeoutSec:10}")
    private int timeoutSeconds;

    public Command createCommand(String deviceId, String type, Map<String, Object> payload) {
        if (deviceService.find(deviceId).isEmpty()) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }
        String reqId = UUID.randomUUID().toString();
        Command command = new Command(reqId, deviceId, type, payload, CommandStatus.PENDING, null, null, Instant.now(), Instant.now());
        commandRepository.save(command);

        String payloadJson;
        try {
            CommandEnvelope envelope = CommandEnvelope.of(type, reqId, payload);
            payloadJson = objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            command = command.withStatus(CommandStatus.FAILED, "SERIALIZE_ERROR", "Failed to serialize command payload");
            return commandRepository.save(command);
        }
        GatewaySendCommandResponse response;
        try {
            response = gatewayClient.sendCommand(
                    new GatewaySendCommandRequest(deviceId, "pet/" + deviceId + "/cmd", 1, payloadJson));
        } catch (RestClientException ex) {
            command = command.withStatus(CommandStatus.FAILED, "GATEWAY_UNAVAILABLE", "Gateway unavailable");
            commandRepository.save(command);
            throw new BusinessException(
                    ErrorCode.GATEWAY_UNAVAILABLE,
                    "Gateway unavailable",
                    Map.of("reqId", reqId)
            );
        }
        if (response != null && response.ok()) {
            command = command.withStatus(CommandStatus.SENT, null, null);
        } else {
            String reason = response == null ? "NO_RESPONSE" : response.reason();
            command = command.withStatus(CommandStatus.FAILED, "OFFLINE", reason);
        }
        return commandRepository.save(command);
    }

    public Optional<Command> findCommand(String reqId) {
        return commandRepository.findById(reqId);
    }

    public void handleAck(String reqId, AckRequest ack) {
        commandRepository.findById(reqId).ifPresent(command -> {
            CommandStatus next = ack.ok() ? CommandStatus.ACKED : CommandStatus.FAILED;
            Command updated = command.withStatus(next, ack.code(), ack.message());
            commandRepository.save(updated);
        });
    }

    @Scheduled(fixedDelayString = "${command.timeoutScanMs:2000}")
    public void timeoutScan() {
        Instant cutoff = Instant.now().minusSeconds(timeoutSeconds);
        Collection<Command> timedOut = commandRepository.findByStatusAndUpdatedAtBefore(CommandStatus.SENT, cutoff);
        for (Command command : timedOut) {
            Command updated = command.withStatus(CommandStatus.TIMEOUT, "TIMEOUT", "No ack within timeout");
            commandRepository.save(updated);
        }
    }
}
