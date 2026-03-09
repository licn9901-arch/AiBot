package com.deskpet.core.service;

import com.deskpet.core.dto.GatewayPublishRequest;
import com.deskpet.core.dto.GatewayPublishResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${gateway.baseUrl:http://localhost:8081}")
    private String gatewayBaseUrl;
    @Value("${internal.token:}")
    private String internalToken;

    public GatewayPublishResponse sendCommand(GatewayPublishRequest request) {
        String url = gatewayBaseUrl + "/internal/command/send";
        return publish(url, request, "发送指令");
    }

    public GatewayPublishResponse sendResponse(GatewayPublishRequest request) {
        String url = gatewayBaseUrl + "/internal/response/send";
        return publish(url, request, "发送设备响应");
    }

    private GatewayPublishResponse publish(String url, GatewayPublishRequest request, String action) {
        log.info("[GW] {}到网关: url={}, deviceId={}, topic={}, qos={}, payload={}",
                action, url, request.deviceId(), request.topic(), request.qos(), request.payload());
        HttpHeaders headers = new HttpHeaders();
        if (internalToken != null && !internalToken.isBlank()) {
            headers.add("X-Internal-Token", internalToken);
        }
        try {
            ResponseEntity<GatewayPublishResponse> response = restTemplate
                    .postForEntity(url, new HttpEntity<>(request, headers), GatewayPublishResponse.class);
            log.info("[GW] 网关响应: statusCode={}, body={}", response.getStatusCode(), response.getBody());
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.warn("[GW] 网关返回错误: statusCode={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                String body = ex.getResponseBodyAsString();
                if (!body.isBlank()) {
                    try {
                        return objectMapper.readValue(body, GatewayPublishResponse.class);
                    } catch (Exception ignore) {
                        log.error("[GW] 解析网关 409 响应失败: body={}", body);
                        return null;
                    }
                }
            }
            throw ex;
        }
    }

    /**
     * 从网关获取 Prometheus 格式的 metrics 文本，解析为 Map
     */
    public Map<String, Object> fetchMetrics() {
        String url = gatewayBaseUrl + "/metrics";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return Map.of();
            }
            return parsePrometheusText(body);
        } catch (Exception ex) {
            log.warn("[GW] 获取网关 metrics 失败: {}", ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> parsePrometheusText(String text) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (String line : text.split("\n")) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\s+", 2);
            if (parts.length == 2) {
                String key = parts[0];
                String val = parts[1];
                try {
                    if (val.contains(".")) {
                        result.put(key, Double.parseDouble(val));
                    } else {
                        result.put(key, Long.parseLong(val));
                    }
                } catch (NumberFormatException e) {
                    result.put(key, val);
                }
            }
        }
        return result;
    }
}
