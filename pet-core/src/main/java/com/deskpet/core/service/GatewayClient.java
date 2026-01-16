package com.deskpet.core.service;

import com.deskpet.core.dto.GatewaySendCommandRequest;
import com.deskpet.core.dto.GatewaySendCommandResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GatewayClient {
    private final RestTemplate restTemplate;
    private final String gatewayBaseUrl;
    private final String internalToken;

    public GatewayClient(RestTemplate restTemplate,
                         @Value("${gateway.baseUrl:http://localhost:8081}") String gatewayBaseUrl,
                         @Value("${internal.token:}") String internalToken) {
        this.restTemplate = restTemplate;
        this.gatewayBaseUrl = gatewayBaseUrl;
        this.internalToken = internalToken;
    }

    public GatewaySendCommandResponse sendCommand(GatewaySendCommandRequest request) {
        String url = gatewayBaseUrl + "/internal/command/send";
        HttpHeaders headers = new HttpHeaders();
        if (internalToken != null && !internalToken.isBlank()) {
            headers.add("X-Internal-Token", internalToken);
        }
        ResponseEntity<GatewaySendCommandResponse> response = restTemplate
                .postForEntity(url, new HttpEntity<>(request, headers), GatewaySendCommandResponse.class);
        return response.getBody();
    }
}
