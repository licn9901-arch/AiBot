package com.deskpet.core.service;

import com.deskpet.core.dto.GatewaySendCommandRequest;
import com.deskpet.core.dto.GatewaySendCommandResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GatewayClient {
    private final RestTemplate restTemplate;
    private final String gatewayBaseUrl;

    public GatewayClient(RestTemplate restTemplate,
                         @Value("${gateway.baseUrl:http://localhost:8081}") String gatewayBaseUrl) {
        this.restTemplate = restTemplate;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public GatewaySendCommandResponse sendCommand(GatewaySendCommandRequest request) {
        String url = gatewayBaseUrl + "/internal/command/send";
        ResponseEntity<GatewaySendCommandResponse> response = restTemplate
                .postForEntity(url, new HttpEntity<>(request), GatewaySendCommandResponse.class);
        return response.getBody();
    }
}
