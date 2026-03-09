package com.deskpet.core.controller;

import com.deskpet.core.dto.AckRequest;
import com.deskpet.core.error.GlobalExceptionHandler;
import com.deskpet.core.service.CommandService;
import com.deskpet.core.service.DeviceEventService;
import com.deskpet.core.service.DeviceRequestService;
import com.deskpet.core.service.DeviceService;
import com.deskpet.core.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalControllerTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private CommandService commandService;

    @Mock
    private DeviceEventService deviceEventService;

    @Mock
    private DeviceRequestService deviceRequestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new InternalController(deviceService, telemetryService, commandService, deviceEventService, deviceRequestService)
            )
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void telemetryAccepted_andDelegated() throws Exception {
        String body = """
                {
                  "schemaVersion": 1,
                  "ts": 1710000000,
                  "firmwareVersion": "1.0.0",
                  "rssi": -40,
                  "battery": 0.9,
                  "lastAction": "move",
                  "extra": {"temp": 36.5}
                }
                """;

        mockMvc.perform(post("/internal/telemetry/pet-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(telemetryService).updateLatest(eq("pet-1"), captor.capture());
        Map<String, Object> map = captor.getValue();
        assertThat(map.get("schemaVersion")).isEqualTo(1);
        assertThat(map.get("firmwareVersion")).isEqualTo("1.0.0");
        assertThat(map.get("extra")).isNotNull();
    }

    @Test
    void ackAccepted_andDelegated() throws Exception {
        String body = """
                {
                  "schemaVersion": 1,
                  "reqId": "req-1",
                  "ok": true,
                  "code": "DONE",
                  "message": "ok",
                  "ts": 1710000001
                }
                """;

        mockMvc.perform(post("/internal/ack/pet-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted());

        ArgumentCaptor<AckRequest> captor = ArgumentCaptor.forClass(AckRequest.class);
        verify(commandService).handleAck(eq("req-1"), captor.capture());
        assertThat(captor.getValue().ok()).isTrue();
    }

    @Test
    void requestAccepted_andDelegated() throws Exception {
        String body = """
                {
                  "schemaVersion": 1,
                  "reqId": "req-weather-1",
                  "type": "getWeather",
                  "ts": 1710000001,
                  "payload": {
                    "location": "Shanghai"
                  }
                }
                """;

        mockMvc.perform(post("/internal/request/pet-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted());

        ArgumentCaptor<com.deskpet.core.dto.DeviceRequestEnvelope> captor =
                ArgumentCaptor.forClass(com.deskpet.core.dto.DeviceRequestEnvelope.class);
        verify(deviceRequestService).handleRequest(eq("pet-1"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("getWeather");
        assertThat(captor.getValue().payload()).containsEntry("location", "Shanghai");
    }
}
