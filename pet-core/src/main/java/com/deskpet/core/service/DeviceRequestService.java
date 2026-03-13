package com.deskpet.core.service;

import com.deskpet.core.dto.DeviceRequestEnvelope;
import com.deskpet.core.dto.DeviceResponseEnvelope;
import com.deskpet.core.dto.GatewayPublishRequest;
import com.deskpet.core.dto.GatewayPublishResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.error.ErrorCode;
import com.deskpet.core.model.Device;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRequestService {
    private static final int DEFAULT_TELEMETRY_INTERVAL_SEC = 5;

    private final DeviceService deviceService;
    private final GatewayClient gatewayClient;
    private final ObjectMapper objectMapper;

    public DeviceResponseEnvelope handleRequest(String deviceId, DeviceRequestEnvelope request) {
        Device device = deviceService.find(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        DeviceResponseEnvelope response = buildResponse(device, request);
        publishResponse(deviceId, response);
        return response;
    }

    private DeviceResponseEnvelope buildResponse(Device device, DeviceRequestEnvelope request) {
        if (request == null || isBlank(request.type())) {
            return DeviceResponseEnvelope.failure(safeRequest(request), "BAD_PAYLOAD", "type is required");
        }
        return switch (request.type()) {
            case "getWeather" -> DeviceResponseEnvelope.success(
                    request,
                    "success",
                    buildWeatherPayload(request)
            );
            case "getConfig" -> DeviceResponseEnvelope.success(
                    request,
                    "success",
                    buildConfigPayload(device)
            );
            case "chat" -> DeviceResponseEnvelope.success(
                    request,
                    "success",
                    buildChatPayload(request)
            );
            default -> DeviceResponseEnvelope.failure(request, "UNSUPPORTED_TYPE", "unsupported request type");
        };
    }

    private Map<String, Object> buildWeatherPayload(DeviceRequestEnvelope request) {
        Map<String, Object> requestPayload = request.payload() == null ? Map.of() : request.payload();
        String location = readString(requestPayload, "location", "unknown");
        int days = readInt(requestPayload, "days", 1);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("location", location);
        payload.put("days", days);
        payload.put("temperature", 26);
        payload.put("condition", "Cloudy");
        payload.put("humidity", 72);
        return payload;
    }

    private Map<String, Object> buildConfigPayload(Device device) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deviceId", device.deviceId());
        payload.put("model", device.model());
        payload.put("productKey", device.productKey());
        payload.put("telemetryIntervalSec", DEFAULT_TELEMETRY_INTERVAL_SEC);
        payload.put("schemaVersion", 1);
        return payload;
    }

    private Map<String, Object> buildChatPayload(DeviceRequestEnvelope request) {
        Map<String, Object> requestPayload = request.payload() == null ? Map.of() : request.payload();
        String text = readString(requestPayload, "text", "");
        if (text.isBlank()) {
            return Map.of("reply", "I am ready.");
        }
        return Map.of("reply", "I heard: " + text);
    }

    private void publishResponse(String deviceId, DeviceResponseEnvelope response) {
        try {
            String payloadJson = objectMapper.writeValueAsString(response);
            GatewayPublishResponse gatewayResponse = gatewayClient.sendResponse(
                    new GatewayPublishRequest(deviceId, "pet/" + deviceId + "/resp", 1, payloadJson)
            );
            if (gatewayResponse == null || !gatewayResponse.ok()) {
                log.warn("[REQ] 响应下发失败: deviceId={}, reqId={}, reason={}",
                        deviceId, response.reqId(), gatewayResponse == null ? "NO_RESPONSE" : gatewayResponse.reason());
            }
        } catch (RestClientException ex) {
            log.error("[REQ] 网关不可达: deviceId={}, reqId={}, error={}", deviceId, response.reqId(), ex.getMessage(), ex);
            throw new BusinessException(ErrorCode.GATEWAY_UNAVAILABLE, "Gateway unavailable",
                    buildErrorDetails(deviceId, response.reqId()));
        } catch (Exception ex) {
            log.error("[REQ] 响应序列化失败: deviceId={}, reqId={}, error={}", deviceId, response.reqId(), ex.getMessage(), ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to serialize request response",
                    buildErrorDetails(deviceId, response.reqId()));
        }
    }

    private DeviceRequestEnvelope safeRequest(DeviceRequestEnvelope request) {
        if (request != null) {
            return request;
        }
        return new DeviceRequestEnvelope(1, null, null, System.currentTimeMillis() / 1000, null);
    }

    private String readString(Map<String, Object> payload, String key, String defaultValue) {
        Object value = payload.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return defaultValue;
    }

    private int readInt(Map<String, Object> payload, String key, int defaultValue) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Map<String, Object> buildErrorDetails(String deviceId, String reqId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("deviceId", deviceId);
        if (reqId != null) {
            details.put("reqId", reqId);
        }
        return details;
    }
}
