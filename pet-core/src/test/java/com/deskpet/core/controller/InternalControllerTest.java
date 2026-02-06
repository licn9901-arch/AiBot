package com.deskpet.core.controller;

import com.deskpet.core.dto.AckRequest;
import com.deskpet.core.service.CommandService;
import com.deskpet.core.service.DeviceService;
import com.deskpet.core.service.TelemetryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalController.class)
@TestPropertySource(properties = "internal.token=")
class InternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    @MockBean
    private TelemetryService telemetryService;

    @MockBean
    private CommandService commandService;

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
}
