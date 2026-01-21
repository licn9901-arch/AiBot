package com.deskpet.core.service;

import com.deskpet.core.dto.GatewaySendCommandRequest;
import com.deskpet.core.dto.GatewaySendCommandResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

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
        HttpHeaders headers = new HttpHeaders();
        if (internalToken != null && !internalToken.isBlank()) {
            headers.add("X-Internal-Token", internalToken);
        }
        try {
            ResponseEntity<GatewaySendCommandResponse> response = restTemplate
                    .postForEntity(url, new HttpEntity<>(request, headers), GatewaySendCommandResponse.class);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                String body = ex.getResponseBodyAsString();
                if (body != null && !body.isBlank()) {
                    try {
                        return objectMapper.readValue(body, GatewaySendCommandResponse.class);
                    } catch (Exception ignore) {
                        return null;
                    }
                }
            }
            throw ex;
        }
    }
}
