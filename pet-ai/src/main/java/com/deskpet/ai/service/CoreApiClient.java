package com.deskpet.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * pet-core API 客户端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoreApiClient {

    private final WebClient coreWebClient;

    /**
     * 获取设备详情
     */
    public DeviceInfo getDevice(String deviceId) {
        try {
            return coreWebClient.get()
                    .uri("/api/devices/{deviceId}", deviceId)
                    .retrieve()
                    .bodyToMono(DeviceInfo.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Device not found: {}", deviceId);
            return null;
        } catch (Exception e) {
            log.error("Failed to get device: {}", deviceId, e);
            throw new RuntimeException("Failed to get device info", e);
        }
    }

    /**
     * 获取设备列表
     */
    public List<DeviceInfo> listDevices() {
        try {
            return coreWebClient.get()
                    .uri("/api/devices")
                    .retrieve()
                    .bodyToFlux(DeviceInfo.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Failed to list devices", e);
            throw new RuntimeException("Failed to list devices", e);
        }
    }

    /**
     * 下发指令
     */
    public CommandResult sendCommand(String deviceId, String type, Map<String, Object> payload) {
        try {
            var request = new CommandRequest(type, payload);
            return coreWebClient.post()
                    .uri("/api/devices/{deviceId}/commands", deviceId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CommandResult.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Failed to send command to device {}: {} - {}", deviceId, e.getStatusCode(), e.getResponseBodyAsString());
            return new CommandResult(null, deviceId, type, payload, "FAILED", null, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send command to device: {}", deviceId, e);
            return new CommandResult(null, deviceId, type, payload, "FAILED", null, e.getMessage());
        }
    }

    /**
     * 查询指令状态
     */
    public CommandResult getCommandStatus(String deviceId, String reqId) {
        try {
            return coreWebClient.get()
                    .uri("/api/devices/{deviceId}/commands/{reqId}", deviceId, reqId)
                    .retrieve()
                    .bodyToMono(CommandResult.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get command status: {}/{}", deviceId, reqId, e);
            return null;
        }
    }

    // DTO Records
    public record DeviceInfo(
            String deviceId,
            String model,
            String remark,
            boolean online,
            String lastSeen,
            Map<String, Object> telemetry
    ) {
    }

    public record CommandRequest(
            String type,
            Map<String, Object> payload
    ) {
    }

    public record CommandResult(
            String reqId,
            String deviceId,
            String type,
            Map<String, Object> payload,
            String status,
            String ackCode,
            String ackMessage
    ) {
    }
}
