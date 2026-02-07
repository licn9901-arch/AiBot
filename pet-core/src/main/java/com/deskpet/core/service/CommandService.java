package com.deskpet.core.service;

import com.deskpet.core.dto.AckRequest;
import com.deskpet.core.dto.CommandEnvelope;
import com.deskpet.core.dto.CommandResponse;
import com.deskpet.core.dto.GatewaySendCommandRequest;
import com.deskpet.core.dto.GatewaySendCommandResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Command;
import com.deskpet.core.model.CommandStatus;
import com.deskpet.core.repository.CommandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
public class CommandService {
    private final CommandRepository commandRepository;
    private final GatewayClient gatewayClient;
    private final DeviceService deviceService;
    private final ObjectMapper objectMapper;

    private TimeSeriesService timeSeriesService;
    private WebSocketPushService webSocketPushService;

    @Value("${command.timeoutSec:10}")
    private int timeoutSeconds;

    public CommandService(CommandRepository commandRepository,
                          GatewayClient gatewayClient,
                          DeviceService deviceService,
                          ObjectMapper objectMapper) {
        this.commandRepository = commandRepository;
        this.gatewayClient = gatewayClient;
        this.deviceService = deviceService;
        this.objectMapper = objectMapper;
    }

    @Autowired(required = false)
    public void setTimeSeriesService(TimeSeriesService timeSeriesService) {
        this.timeSeriesService = timeSeriesService;
    }

    @Autowired(required = false)
    public void setWebSocketPushService(WebSocketPushService webSocketPushService) {
        this.webSocketPushService = webSocketPushService;
    }

    public Command createCommand(String deviceId, String type, Map<String, Object> payload) {
        if (deviceService.find(deviceId).isEmpty()) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }
        String reqId = UUID.randomUUID().toString();
        Command command = new Command(reqId, deviceId, type, payload, CommandStatus.PENDING, null, null, Instant.now(), Instant.now());
        commandRepository.save(command);
        Command result = dispatchCommand(command);
        writeCommandToTimescaleDb(result);
        return result;
    }

    public Command retryCommand(String deviceId, String reqId) {
        Command command = commandRepository.findById(reqId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMAND_NOT_FOUND));
        if (!command.deviceId().equals(deviceId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Command does not belong to device",
                    Map.of("deviceId", deviceId, "reqId", reqId));
        }
        if (command.status() != CommandStatus.TIMEOUT && command.status() != CommandStatus.FAILED) {
            throw new BusinessException(ErrorCode.COMMAND_NOT_RETRYABLE, "Command is not retryable",
                    Map.of("status", command.status().name(), "reqId", reqId));
        }
        return dispatchCommand(command);
    }

    public Optional<Command> findCommand(String reqId) {
        return commandRepository.findById(reqId);
    }

    public void handleAck(String reqId, AckRequest ack) {
        commandRepository.findById(reqId).ifPresent(command -> {
            CommandStatus next = ack.ok() ? CommandStatus.ACKED : CommandStatus.FAILED;
            Command updated = command.withStatus(next, ack.code(), ack.message());
            commandRepository.save(updated);
            pushCommandStatusUpdate(updated);
        });
    }

    @Scheduled(fixedDelayString = "${command.timeoutScanMs:2000}")
    public void timeoutScan() {
        Instant cutoff = Instant.now().minusSeconds(timeoutSeconds);
        Collection<Command> timedOut = commandRepository.findByStatusAndUpdatedAtBefore(CommandStatus.SENT, cutoff);
        for (Command command : timedOut) {
            Command updated = command.withStatus(CommandStatus.TIMEOUT, "TIMEOUT", "No ack within timeout");
            commandRepository.save(updated);
            pushCommandStatusUpdate(updated);
        }
    }

    private Command dispatchCommand(Command command) {
        String payloadJson;
        try {
            CommandEnvelope envelope = CommandEnvelope.of(command.type(), command.reqId(), command.payload());
            payloadJson = objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            Command failed = command.withStatus(CommandStatus.FAILED, "SERIALIZE_ERROR", "Failed to serialize command payload");
            Command saved = commandRepository.save(failed);
            pushCommandStatusUpdate(saved);
            return saved;
        }
        GatewaySendCommandResponse response;
        try {
            response = gatewayClient.sendCommand(
                    new GatewaySendCommandRequest(command.deviceId(), "pet/" + command.deviceId() + "/cmd", 1, payloadJson));
        } catch (RestClientException ex) {
            Command failed = command.withStatus(CommandStatus.FAILED, "GATEWAY_UNAVAILABLE", "Gateway unavailable");
            commandRepository.save(failed);
            pushCommandStatusUpdate(failed);
            throw new BusinessException(
                    ErrorCode.GATEWAY_UNAVAILABLE,
                    "Gateway unavailable",
                    Map.of("reqId", command.reqId())
            );
        }
        Command updated;
        if (response != null && response.ok()) {
            updated = command.withStatus(CommandStatus.SENT, null, null);
        } else {
            String reason = response == null ? "NO_RESPONSE" : response.reason();
            updated = command.withStatus(CommandStatus.FAILED, "OFFLINE", reason);
        }
        Command saved = commandRepository.save(updated);
        pushCommandStatusUpdate(saved);
        return saved;
    }

    private void pushCommandStatusUpdate(Command command) {
        if (webSocketPushService != null) {
            try {
                webSocketPushService.pushCommandStatus(command.deviceId(), CommandResponse.of(command));
            } catch (Exception ignored) {
                // WebSocket 推送失败不影响主流程
            }
        }
    }

    private void writeCommandToTimescaleDb(Command command) {
        if (timeSeriesService != null) {
            try {
                String payloadJson = command.payload() != null ?
                    objectMapper.writeValueAsString(command.payload()) : null;
                timeSeriesService.writeDeviceCommand(command.reqId(), command.deviceId(),
                    command.type(), payloadJson, command.status().name(), command.updatedAt());
            } catch (Exception ignored) {
                // 时序写入失败不影响主流程
            }
        }
    }
}
