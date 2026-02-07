package com.deskpet.core.service;

import com.deskpet.core.dto.GatewaySendCommandRequest;
import com.deskpet.core.dto.GatewaySendCommandResponse;
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

    public GatewaySendCommandResponse sendCommand(GatewaySendCommandRequest request) {
        String url = gatewayBaseUrl + "/internal/command/send";
        log.info("[GW] 发送指令到网关: url={}, deviceId={}, topic={}, qos={}, payload={}",
                url, request.deviceId(), request.topic(), request.qos(), request.payload());
        HttpHeaders headers = new HttpHeaders();
        if (internalToken != null && !internalToken.isBlank()) {
            headers.add("X-Internal-Token", internalToken);
        }
        try {
            ResponseEntity<GatewaySendCommandResponse> response = restTemplate
                    .postForEntity(url, new HttpEntity<>(request, headers), GatewaySendCommandResponse.class);
            log.info("[GW] 网关响应: statusCode={}, body={}", response.getStatusCode(), response.getBody());
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.warn("[GW] 网关返回错误: statusCode={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                String body = ex.getResponseBodyAsString();
                if (!body.isBlank()) {
                    try {
                        return objectMapper.readValue(body, GatewaySendCommandResponse.class);
                    } catch (Exception ignore) {
                        log.error("[GW] 解析网关 409 响应失败: body={}", body);
                        return null;
                    }
                }
            }
            throw ex;
        }
    }
}
