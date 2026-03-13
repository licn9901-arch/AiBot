package com.deskpet.core.service;

import com.deskpet.core.dto.DeviceRequestEnvelope;
import com.deskpet.core.dto.DeviceResponseEnvelope;
import com.deskpet.core.dto.GatewayPublishResponse;
import com.deskpet.core.error.BusinessException;
import com.deskpet.core.model.Device;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceRequestServiceTest {

    private DeviceService deviceService;
    private GatewayClient gatewayClient;
    private DeviceRequestService deviceRequestService;

    @BeforeEach
    void setUp() {
        deviceService = Mockito.mock(DeviceService.class);
        gatewayClient = Mockito.mock(GatewayClient.class);
        deviceRequestService = new DeviceRequestService(deviceService, gatewayClient, new ObjectMapper());
        when(gatewayClient.sendResponse(any())).thenReturn(new GatewayPublishResponse(true, "SENT"));
    }

    @Test
    void handleRequest_getWeather_buildsWeatherResponseAndPublishes() {
        when(deviceService.find("pet-1")).thenReturn(Optional.of(mockDevice("pet-1")));
        DeviceRequestEnvelope request = new DeviceRequestEnvelope(
                1,
                "req-weather-1",
                "getWeather",
                1730000000L,
                Map.of("location", "Shanghai", "days", 1)
        );

        DeviceResponseEnvelope response = deviceRequestService.handleRequest("pet-1", request);

        assertThat(response.ok()).isTrue();
        assertThat(response.code()).isEqualTo("DONE");
        assertThat(response.payload()).containsEntry("location", "Shanghai");
        assertThat(response.payload()).containsEntry("condition", "Cloudy");

        ArgumentCaptor<com.deskpet.core.dto.GatewayPublishRequest> captor =
                ArgumentCaptor.forClass(com.deskpet.core.dto.GatewayPublishRequest.class);
        verify(gatewayClient).sendResponse(captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo("pet/pet-1/resp");
        assertThat(captor.getValue().payload()).contains("\"type\":\"getWeather\"");
    }

    @Test
    void handleRequest_getConfig_returnsDeviceMetadata() {
        when(deviceService.find("pet-1")).thenReturn(Optional.of(mockDevice("pet-1")));

        DeviceResponseEnvelope response = deviceRequestService.handleRequest(
                "pet-1",
                new DeviceRequestEnvelope(1, "req-config-1", "getConfig", 1730000000L, Map.of())
        );

        assertThat(response.ok()).isTrue();
        assertThat(response.payload()).containsEntry("deviceId", "pet-1");
        assertThat(response.payload()).containsEntry("model", "deskpet");
        assertThat(response.payload()).containsEntry("telemetryIntervalSec", 5);
    }

    @Test
    void handleRequest_unknownType_returnsUnsupportedType() {
        when(deviceService.find("pet-1")).thenReturn(Optional.of(mockDevice("pet-1")));

        DeviceResponseEnvelope response = deviceRequestService.handleRequest(
                "pet-1",
                new DeviceRequestEnvelope(1, "req-unknown-1", "unknownAction", 1730000000L, Map.of())
        );

        assertThat(response.ok()).isFalse();
        assertThat(response.code()).isEqualTo("UNSUPPORTED_TYPE");
    }

    @Test
    void handleRequest_missingDevice_throws() {
        when(deviceService.find("pet-1")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> deviceRequestService.handleRequest(
                "pet-1",
                new DeviceRequestEnvelope(1, "req-1", "getWeather", 1730000000L, Map.of())
        ));
    }

    private Device mockDevice(String deviceId) {
        return new Device(deviceId, "hash", "salt", "deskpet", "deskpet-v1", 1L, "demo", Instant.now());
    }
}
